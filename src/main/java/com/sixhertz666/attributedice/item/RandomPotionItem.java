package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * Random Potion — a drinkable item that applies the same effect as the regular
 * attribute dice (random attribute gain/loss based on a 1-6 roll), but without
 * spawning the visual rolling-dice entity above the player's head.
 *
 * <p>Crafted by brewing an Attribute Dice with a Water Bottle in a brewing stand.
 */
public class RandomPotionItem extends Item {

    public RandomPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            ServerLevel serverLevel = (ServerLevel) level;

            int roll;
            if (AttributeDiceMod.CONFIG != null
                    && player.getLuck() > AttributeDiceMod.CONFIG.luckThreshold) {
                roll = serverLevel.getRandom().nextIntBetweenInclusive(4, 6);
            } else {
                roll = serverLevel.getRandom().nextIntBetweenInclusive(1, 6);
            }

            AttributeDiceMod.applyDiceResult(serverLevel, player, roll);

            level.playSound(null, player.blockPosition(),
                    SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.5F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }
}
