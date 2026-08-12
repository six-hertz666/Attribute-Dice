package com.sixhertz666.attributedice;

import com.sixhertz666.attributedice.entity.ModEntities;
import com.sixhertz666.attributedice.item.ModCreativeModeTabs;
import com.sixhertz666.attributedice.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

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
        BossDropHandler.register();
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
        applyDiceResult(level, player, player, roll);
    }

    /**
     * Resolve a rolled dice value applied to a (possibly non-player) target.
     * Feedback messages are sent to {@code user}; attribute modifications are
     * applied to {@code target}. When {@code target} is a non-player entity,
     * the chat messages include the entity's name.
     *
     * @param level the server level the dice was rolled in
     * @param target the entity that receives the attribute change
     * @param user the player that rolled the dice (receives chat feedback)
     * @param roll the dice result (1-6)
     */
    public static void applyDiceResult(ServerLevel level, LivingEntity target, Player user, int roll) {
        applyDiceResult(level, target, user, roll, null);
    }

    /**
     * Resolve a rolled dice value applied to a (possibly non-player) target,
     * optionally forcing a specific attribute instead of randomly picking one.
     *
     * <p>When {@code fixedAttribute} is non-null (used by the damage/armor/health
     * dice variants), every outcome modifies that single attribute. When null,
     * the attribute is chosen at random from attack damage / armor / max health
     * (regular dice behavior).
     *
     * @param level the server level the dice was rolled in
     * @param target the entity that receives the attribute change
     * @param user the player that rolled the dice (receives chat feedback)
     * @param roll the dice result (1-6)
     * @param fixedAttribute the attribute to modify, or null for random selection
     */
    public static void applyDiceResult(ServerLevel level, LivingEntity target, Player user, int roll,
                                        Holder<Attribute> fixedAttribute) {
        if (roll < 1 || roll > 6) {
            return;
        }

        boolean targetingOther = target != user;

        // Tell the player the result. 1/2/3 -> red, 4/5/6 -> green.
        ChatFormatting color = roll <= 3 ? ChatFormatting.RED : ChatFormatting.GREEN;
        Component message = targetingOther
                ? Component.translatable("attribute_dice.message.rolled_entity", target.getDisplayName(), roll)
                        .withStyle(color)
                : Component.translatable("attribute_dice.message.rolled", roll).withStyle(color);
        user.displayClientMessage(message, false);

        if (CONFIG == null) {
            CONFIG = AttributeDiceConfig.load();
        }

        RandomSource random = level.getRandom();

        // 专用骰子（伤害/护甲/生命）使用各自独立前缀，避免与普通骰子修改器冲突。
        // 普通骰子仍使用 "dice_" 前缀。
        String modifierPrefix = fixedAttribute == null ? "dice_" : prefixForAttribute(fixedAttribute);

        switch (roll) {
            case 6:
                applyAttributeChange(target, random, CONFIG.roll6Gain, false, user, targetingOther, false, modifierPrefix, fixedAttribute);
                break;
            case 5:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.roll5GainMin, CONFIG.roll5GainMax), false, user, targetingOther, false, modifierPrefix, fixedAttribute);
                break;
            case 4:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.roll4GainMin, CONFIG.roll4GainMax), false, user, targetingOther, false, modifierPrefix, fixedAttribute);
                break;
            case 3:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.roll3LossMin, CONFIG.roll3LossMax), true, user, targetingOther, false, modifierPrefix, fixedAttribute);
                break;
            case 2:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.roll2LossMin, CONFIG.roll2LossMax), true, user, targetingOther, false, modifierPrefix, fixedAttribute);
                break;
            case 1:
                applyAttributeChange(target, random, CONFIG.roll1Loss, true, user, targetingOther, false, modifierPrefix, fixedAttribute);
                if (CONFIG.enableLightning) {
                    strikeWithLightning(level, target, CONFIG.lightningDamage);
                }
                break;
        }
    }

    /**
     * Resolve a damage dice roll. Behaves identically to the regular dice but
     * always modifies {@link Attributes#ATTACK_DAMAGE}.
     */
    public static void applyDamageDiceResult(ServerLevel level, LivingEntity target, Player user, int roll) {
        applyDiceResult(level, target, user, roll, Attributes.ATTACK_DAMAGE);
    }

    /**
     * Resolve an armor dice roll. Behaves identically to the regular dice but
     * always modifies {@link Attributes#ARMOR}.
     */
    public static void applyArmorDiceResult(ServerLevel level, LivingEntity target, Player user, int roll) {
        applyDiceResult(level, target, user, roll, Attributes.ARMOR);
    }

    /**
     * Resolve a health dice roll. Behaves identically to the regular dice but
     * always modifies {@link Attributes#MAX_HEALTH}.
     */
    public static void applyHealthDiceResult(ServerLevel level, LivingEntity target, Player user, int roll) {
        applyDiceResult(level, target, user, roll, Attributes.MAX_HEALTH);
    }

    /**
     * Returns the modifier-id prefix for a fixed-attribute dice variant.
     * Each variant gets its own prefix so its transient modifiers don't
     * overwrite (or get overwritten by) those of other dice types.
     */
    private static String prefixForAttribute(Holder<Attribute> attribute) {
        if (attribute == Attributes.ATTACK_DAMAGE) {
            return "damage_dice_";
        }
        if (attribute == Attributes.ARMOR) {
            return "armor_dice_";
        }
        if (attribute == Attributes.MAX_HEALTH) {
            return "health_dice_";
        }
        return "dice_";
    }

    /**
     * Resolve a rolled badluck dice value applied to a target entity. All
     * outcomes are losses (negative attribute changes). Rolling a 1 does NOT
     * trigger lightning (unlike the regular dice). The result is always
     * announced in red.
     *
     * @param level the server level the dice was rolled in
     * @param target the entity that receives the attribute change
     * @param user the player that rolled the dice (receives chat feedback)
     * @param roll the dice result (1-6)
     */
    public static void applyBadluckDiceResult(ServerLevel level, LivingEntity target, Player user, int roll) {
        if (roll < 1 || roll > 6) {
            return;
        }

        boolean targetingOther = target != user;

        // 霉运骰子：所有结果都是减少，统一红色提示。
        ChatFormatting color = ChatFormatting.RED;
        Component message = targetingOther
                ? Component.translatable("attribute_dice.message.rolled_entity", target.getDisplayName(), roll)
                        .withStyle(color)
                : Component.translatable("attribute_dice.message.rolled", roll).withStyle(color);
        user.displayClientMessage(message, false);

        if (CONFIG == null) {
            CONFIG = AttributeDiceConfig.load();
        }

        RandomSource random = level.getRandom();

        // 霉运骰子使用 "badluck_dice_" 前缀修改器ID
        // permanent=true：每次生成唯一的修改器 ID 并永久保存，
        // 这样减少效果会累积，不会因为替换旧修改器而导致属性值反弹。
        String modifierPrefix = "badluck_dice_";

        switch (roll) {
            case 6:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.badluck6LossMin, CONFIG.badluck6LossMax), true, user, targetingOther, true, modifierPrefix);
                break;
            case 5:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.badluck5LossMin, CONFIG.badluck5LossMax), true, user, targetingOther, true, modifierPrefix);
                break;
            case 4:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.badluck4LossMin, CONFIG.badluck4LossMax), true, user, targetingOther, true, modifierPrefix);
                break;
            case 3:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.badluck3LossMin, CONFIG.badluck3LossMax), true, user, targetingOther, true, modifierPrefix);
                break;
            case 2:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.badluck2LossMin, CONFIG.badluck2LossMax), true, user, targetingOther, true, modifierPrefix);
                break;
            case 1:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.badluck1LossMin, CONFIG.badluck1LossMax), true, user, targetingOther, true, modifierPrefix);
                // 霉运骰子 1 点不触发闪电。
                break;
        }
    }

    /**
     * Resolve a rolled fortune dice value. Fortune dice can only roll 4-6,
     * and all outcomes are positive (attribute gains). In addition:
     * <ul>
     *   <li>Rolling a 4 grants village toolsmith chest loot</li>
     *   <li>Rolling a 5 grants desert pyramid chest loot</li>
     *   <li>Rolling a 6 grants bastion remnant treasure chest loot</li>
     * </ul>
     * The result is always announced in gold.
     *
     * @param level the server level the dice was rolled in
     * @param target the entity that receives the attribute change (should be the user)
     * @param user the player that rolled the dice (receives chat feedback and loot)
     * @param roll the dice result (4-6)
     */
    public static void applyFortuneDiceResult(ServerLevel level, LivingEntity target, Player user, int roll) {
        if (roll < 4 || roll > 6) {
            return;
        }

        // 财富骰子：所有结果都是增加，金色提示
        ChatFormatting color = ChatFormatting.GOLD;
        Component message = Component.translatable("attribute_dice.message.fortune_rolled", roll)
                .withStyle(color);
        user.displayClientMessage(message, false);

        if (CONFIG == null) {
            CONFIG = AttributeDiceConfig.load();
        }

        RandomSource random = level.getRandom();

        // 财富骰子使用独立的 "fortune_dice_" 前缀修改器ID，避免与普通骰子冲突
        // 同时使用财富骰子专属的配置数值
        //
        // 关键修复：permanent=true，每次生成唯一的修改器 ID（含 UUID）并调用
        // addPermanentModifier，使增益永久累积。之前 permanent=false 使用固定 ID，
        // 每次掷骰子会先 removeModifier 再 addTransientModifier，若新值 < 旧值，
        // 属性反而降低（这就是"财富骰子数值降低"BUG 的根因）。
        String modifierPrefix = "fortune_dice_";

        switch (roll) {
            case 6:
                applyAttributeChange(target, random, CONFIG.fortune6Gain, false, user, false, true, modifierPrefix);
                grantLoot(level, user, "minecraft:chests/bastion/treasure");
                break;
            case 5:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.fortune5GainMin, CONFIG.fortune5GainMax), false, user, false, true, modifierPrefix);
                grantLoot(level, user, "minecraft:chests/desert_pyramid");
                break;
            case 4:
                applyAttributeChange(target, random, randomInRange(random, CONFIG.fortune4GainMin, CONFIG.fortune4GainMax), false, user, false, true, modifierPrefix);
                grantLoot(level, user, "minecraft:chests/village/village_toolsmith");
                break;
        }
    }

    /**
     * Generates items from a loot table and gives them to the player. Items
     * that don't fit in the player's inventory are dropped at the player's
     * feet.
     *
     * @param level the server level
     * @param player the player to receive the items
     * @param lootTableId the loot table resource location (e.g. "minecraft:chests/desert_pyramid")
     */
    private static void grantLoot(ServerLevel level, Player player, String lootTableId) {
        Identifier id = Identifier.tryParse(lootTableId);
        if (id == null) {
            return;
        }
        ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, id);
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
        if (lootTable == null || lootTable == LootTable.EMPTY) {
            return;
        }

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);

        List<ItemStack> loot = lootTable.getRandomItems(params);
        for (ItemStack stack : loot) {
            boolean added = player.getInventory().add(stack);
            if (!added) {
                // Inventory full: drop at player's feet
                player.drop(stack, false);
            }
        }

        // Send loot notification
        Component lootMsg = Component.translatable("attribute_dice.message.loot_received",
                Component.translatable("attribute_dice.loot." + lootTableNameFromId(lootTableId))
        ).withStyle(ChatFormatting.GOLD);
        player.displayClientMessage(lootMsg, false);
    }

    /**
     * Extracts a short, stable name from a loot table ID for use in the
     * translation key. e.g. "minecraft:chests/village/village_toolsmith"
     * becomes "village_toolsmith".
     */
    private static String lootTableNameFromId(String lootTableId) {
        int lastSlash = lootTableId.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < lootTableId.length() - 1) {
            return lootTableId.substring(lastSlash + 1);
        }
        return "generic";
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
     * modifier. Positive amounts add value, negative amounts remove
     * value (when negative flag is true the amount is negated).
     *
     * @param target the entity whose attribute is modified
     * @param random source of randomness for attribute selection
     * @param amount magnitude of the change
     * @param negative if true, the amount is subtracted instead of added
     * @param user the player that rolled the dice (receives chat feedback)
     * @param targetingOther if true, the feedback message includes the target's name
     * @param permanent if true, uses a unique modifier id and a permanent
     *                  modifier so that effects accumulate across multiple
     *                  rolls instead of replacing the previous modifier
     * @param modifierPrefix prefix used for the modifier resource id; different
     *                       dice types use different prefixes so their modifiers
     *                       don't overwrite each other (prevents "gain replaced
     *                       by smaller gain = stat loss" bugs)
     */
    private static void applyAttributeChange(LivingEntity target, RandomSource random,
                                              int amount, boolean negative,
                                              Player user, boolean targetingOther,
                                              boolean permanent, String modifierPrefix) {
        applyAttributeChange(target, random, amount, negative, user, targetingOther, permanent, modifierPrefix, null);
    }

    /**
     * Applies an attribute modifier, either to a random attribute (when
     * {@code chosenAttribute} is null) or to a fixed attribute (used by the
     * damage/armor/health dice variants).
     *
     * @param chosenAttribute the attribute to modify, or null to pick randomly
     *                         from attack damage / armor / max health
     * @see #applyAttributeChange(LivingEntity, RandomSource, int, boolean,
     *      Player, boolean, boolean, String)
     */
    private static void applyAttributeChange(LivingEntity target, RandomSource random,
                                              int amount, boolean negative,
                                              Player user, boolean targetingOther,
                                              boolean permanent, String modifierPrefix,
                                              Holder<Attribute> chosenAttribute) {
        Holder<Attribute> chosen;
        if (chosenAttribute != null) {
            chosen = chosenAttribute;
        } else {
            List<Holder<Attribute>> choices = List.of(
                    Attributes.ATTACK_DAMAGE,
                    Attributes.ARMOR,
                    Attributes.MAX_HEALTH
            );
            chosen = choices.get(random.nextInt(choices.size()));
        }
        double delta = negative ? -amount : amount;

        AttributeInstance instance = target.getAttribute(chosen);
        if (instance == null) {
            return;
        }

        ResourceModifierKey key = ResourceModifierKey.forAttribute(chosen, modifierPrefix);
        Identifier modifierId;
        if (permanent) {
            // 每次生成唯一的修改器 ID，使效果累积而不会替换旧修改器。
            modifierId = AttributeDiceMod.id(key.path() + "_" + UUID.randomUUID());
        } else {
            // 固定 ID，仅替换同前缀的旧修改器（不同骰子类型互不干扰）。
            modifierId = AttributeDiceMod.id(key.path());
            if (instance.getModifier(modifierId) != null) {
                instance.removeModifier(modifierId);
            }
        }

        AttributeModifier modifier = new AttributeModifier(
                modifierId,
                delta,
                AttributeModifier.Operation.ADD_VALUE
        );
        if (permanent) {
            instance.addPermanentModifier(modifier);
        } else {
            instance.addTransientModifier(modifier);
        }

        // Heal up to the new max health if we just changed it, so the target
        // sees an immediate benefit.
        if (chosen == Attributes.MAX_HEALTH && delta > 0) {
            target.heal((float) delta);
        }

        Component attrName = Component.translatable(key.translationKey());
        ChatFormatting feedbackColor = negative ? ChatFormatting.RED : ChatFormatting.GREEN;
        Component feedback;
        if (targetingOther) {
            feedback = Component.translatable(
                    negative ? "attribute_dice.message.loss_entity" : "attribute_dice.message.gain_entity",
                    target.getDisplayName(),
                    Math.abs(amount),
                    attrName
            ).withStyle(feedbackColor);
        } else {
            feedback = Component.translatable(
                    negative ? "attribute_dice.message.loss" : "attribute_dice.message.gain",
                    Math.abs(amount),
                    attrName
            ).withStyle(feedbackColor);
        }
        user.displayClientMessage(feedback, false);
    }

    private static void strikeWithLightning(ServerLevel level, LivingEntity target, float damage) {
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(target.getX(), target.getY(), target.getZ());
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        target.hurtServer(level, level.damageSources().lightningBolt(), damage);
        level.playSound(null, target.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
    }

    /**
     * Tiny enum-ish helper to map an attribute holder to a stable modifier
     * resource location and a translation key for the chat feedback.
     */
    private record ResourceModifierKey(String path, String translationKey) {
        static ResourceModifierKey forAttribute(Holder<Attribute> attribute, String modifierPrefix) {
            if (attribute == Attributes.ATTACK_DAMAGE) {
                return new ResourceModifierKey(modifierPrefix + "attack_damage", "attribute_dice.attr.attack_damage");
            }
            if (attribute == Attributes.ARMOR) {
                return new ResourceModifierKey(modifierPrefix + "armor", "attribute_dice.attr.armor");
            }
            if (attribute == Attributes.MAX_HEALTH) {
                return new ResourceModifierKey(modifierPrefix + "max_health", "attribute_dice.attr.max_health");
            }
            return new ResourceModifierKey(modifierPrefix + "generic", "attribute_dice.attr.generic");
        }
    }
}
