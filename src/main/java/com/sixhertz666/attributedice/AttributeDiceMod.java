package com.sixhertz666.attributedice;

import com.sixhertz666.attributedice.entity.ModEntities;
import com.sixhertz666.attributedice.item.ModCreativeModeTabs;
import com.sixhertz666.attributedice.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main entrypoint for the Attribute Dice mod. Handles registration of content
 * and the central {@link #applyDiceResult(ServerLevel, Player, int)} method
 * that resolves a dice roll into attribute changes and side effects.
 */
public class AttributeDiceMod implements ModInitializer {

    public static final String MOD_ID = "attribute_dice";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static AttributeDiceConfig CONFIG;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Attribute Dice");
        CONFIG = AttributeDiceConfig.load();
        ModItems.register();
        ModCreativeModeTabs.register();
        ModEntities.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Resolve a rolled dice value. Called server-side after the dice entity
     * has finished spinning.
     *
     * @param level the server level the dice was rolled in
     * @param player the player that rolled the dice
     * @param roll the dice result (1-6)
     */
    public static void applyDiceResult(ServerLevel level, Player player, int roll) {
        if (roll < 1 || roll > 6) {
            return;
        }

        // Tell the player the result. 1/2/3 -> red, 4/5/6 -> green.
        ChatFormatting color = roll <= 3 ? ChatFormatting.RED : ChatFormatting.GREEN;
        Component message = Component.translatable("attribute_dice.message.rolled", roll).withStyle(color);
        player.displayClientMessage(message, false);

        if (CONFIG == null) {
            CONFIG = AttributeDiceConfig.load();
        }

        RandomSource random = level.getRandom();

        switch (roll) {
            case 6:
                applyAttributeChange(player, random, CONFIG.roll6Gain, false);
                break;
            case 5:
                applyAttributeChange(player, random, randomInRange(random, CONFIG.roll5GainMin, CONFIG.roll5GainMax), false);
                break;
            case 4:
                applyAttributeChange(player, random, randomInRange(random, CONFIG.roll4GainMin, CONFIG.roll4GainMax), false);
                break;
            case 3:
                applyAttributeChange(player, random, randomInRange(random, CONFIG.roll3LossMin, CONFIG.roll3LossMax), true);
                break;
            case 2:
                applyAttributeChange(player, random, randomInRange(random, CONFIG.roll2LossMin, CONFIG.roll2LossMax), true);
                break;
            case 1:
                applyAttributeChange(player, random, CONFIG.roll1Loss, true);
                if (CONFIG.enableLightning) {
                    strikeWithLightning(level, player, CONFIG.lightningDamage);
                }
                break;
        }
    }

    /**
     * Returns a random value in the inclusive range [{@code min}, {@code max}].
     * Assumes {@code min <= max} (clamped in {@link AttributeDiceConfig#load()}).
     */
    private static int randomInRange(RandomSource random, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    /**
     * Picks one of the three tracked attributes at random and applies a
     * transient modifier. Positive amounts add value, negative amounts remove
     * value (when negative flag is true the amount is negated).
     */
    private static void applyAttributeChange(Player player, RandomSource random,
                                              int amount, boolean negative) {
        List<Holder<Attribute>> choices = List.of(
                Attributes.ATTACK_DAMAGE,
                Attributes.ARMOR,
                Attributes.MAX_HEALTH
        );

        Holder<Attribute> chosen = choices.get(random.nextInt(choices.size()));
        double delta = negative ? -amount : amount;

        AttributeInstance instance = player.getAttribute(chosen);
        if (instance == null) {
            return;
        }

        ResourceModifierKey key = ResourceModifierKey.forAttribute(chosen);
        Identifier modifierId = AttributeDiceMod.id(key.path());
        
        // Remove existing modifier if present to prevent conflicts
        if (instance.getModifier(modifierId) != null) {
            instance.removeModifier(modifierId);
        }
        
        AttributeModifier modifier = new AttributeModifier(
                modifierId,
                delta,
                AttributeModifier.Operation.ADD_VALUE
        );
        instance.addTransientModifier(modifier);

        // Heal up to the new max health if we just changed it, so the player
        // sees an immediate benefit.
        if (chosen == Attributes.MAX_HEALTH) {
            if (delta > 0) {
                player.heal((float) delta);
            }
        }

        Component feedback = Component.translatable(
                negative ? "attribute_dice.message.loss" : "attribute_dice.message.gain",
                Math.abs(amount),
                Component.translatable(key.translationKey())
        ).withStyle(negative ? ChatFormatting.RED : ChatFormatting.GREEN);
        player.displayClientMessage(feedback, false);
    }

    private static void strikeWithLightning(ServerLevel level, Player player, float damage) {
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(player.getX(), player.getY(), player.getZ());
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        player.hurtServer(level, level.damageSources().lightningBolt(), damage);
        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
    }

    /**
     * Tiny enum-ish helper to map an attribute holder to a stable modifier
     * resource location and a translation key for the chat feedback.
     */
    private record ResourceModifierKey(String path, String translationKey) {
        static ResourceModifierKey forAttribute(Holder<Attribute> attribute) {
            if (attribute == Attributes.ATTACK_DAMAGE) {
                return new ResourceModifierKey("dice_attack_damage", "attribute_dice.attr.attack_damage");
            }
            if (attribute == Attributes.ARMOR) {
                return new ResourceModifierKey("dice_armor", "attribute_dice.attr.armor");
            }
            if (attribute == Attributes.MAX_HEALTH) {
                return new ResourceModifierKey("dice_max_health", "attribute_dice.attr.max_health");
            }
            return new ResourceModifierKey("dice_generic", "attribute_dice.attr.generic");
        }
    }
}
