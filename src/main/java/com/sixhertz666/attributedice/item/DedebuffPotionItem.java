package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
 * DeDebuff Potion — a drinkable item that only resets attributes whose current
 * value is below the vanilla default (base value). Attributes at or above
 * default are left untouched.
 *
 * <p>Crafted on a crafting table with 8 gold ingots surrounding a Random Potion.
 */
public class DedebuffPotionItem extends Item {

    public DedebuffPotionItem(Properties properties) {
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
            int resetCount = AttributeDiceMod.resetBelowDefaultAttributes(player);

            if (resetCount > 0) {
                player.displayClientMessage(
                        Component.translatable("attribute_dice.message.dedebuff_reset", resetCount)
                                .withStyle(ChatFormatting.GREEN),
                        false);
            } else {
                player.displayClientMessage(
                        Component.translatable("attribute_dice.message.dedebuff_nothing")
                                .withStyle(ChatFormatting.GRAY),
                        false);
            }

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
