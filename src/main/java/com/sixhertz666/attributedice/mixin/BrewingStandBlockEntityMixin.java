package com.sixhertz666.attributedice.mixin;

import com.sixhertz666.attributedice.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for {@link BrewingStandBlockEntity} that adds support for brewing
 * Random Potions using an Attribute Dice as the ingredient and Water Bottles
 * as the input.
 *
 * <p>Because the vanilla {@link PotionBrewing} system only supports potion-type
 * conversions (not arbitrary item outputs), we intercept the brewing stand's
 * {@code isBrewable} and {@code doBrew} methods to handle our custom recipe.
 */
@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin {

    /** Slot index for the ingredient (top slot) in a brewing stand. */
    private static final int INGREDIENT_SLOT = 3;

    /**
     * Intercepts {@code isBrewable} to return {@code true} when the ingredient
     * is an Attribute Dice and at least one bottle slot contains a Water Bottle.
     * For all other cases, the original method is allowed to execute.
     */
    @Inject(method = "isBrewable", at = @At("HEAD"), cancellable = true)
    private static void attributeDice$isBrewable(PotionBrewing brewing,
                                                  NonNullList<ItemStack> items,
                                                  CallbackInfoReturnable<Boolean> cir) {
        ItemStack ingredient = items.get(INGREDIENT_SLOT);
        if (ingredient.is(ModItems.ATTRIBUTE_DICE)) {
            for (int i = 0; i < 3; i++) {
                ItemStack bottle = items.get(i);
                if (!bottle.isEmpty() && isWaterBottle(bottle)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        // Fall through to original method for all other cases.
    }

    /**
     * Intercepts {@code doBrew} to convert Water Bottles into Random Potions
     * when the ingredient is an Attribute Dice. Only Water Bottles are
     * converted; other potions in the remaining slots are left unchanged.
     */
    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void attributeDice$doBrew(Level level, BlockPos pos,
                                              NonNullList<ItemStack> items,
                                              CallbackInfo ci) {
        ItemStack ingredient = items.get(INGREDIENT_SLOT);
        if (!ingredient.is(ModItems.ATTRIBUTE_DICE)) {
            return; // Let original method handle normal brewing.
        }

        boolean brewed = false;
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = items.get(i);
            if (!bottle.isEmpty() && isWaterBottle(bottle)) {
                items.set(i, new ItemStack(ModItems.RANDOM_POTION));
                brewed = true;
            }
        }

        if (brewed) {
            ingredient.shrink(1);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            ci.cancel(); // Prevent original doBrew from running.
        }
    }

    /**
     * Checks whether the given stack is a Water Bottle (a potion with the
     * {@code minecraft:water} potion type).
     */
    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null
                && contents.potion().isPresent()
                && contents.potion().get().is(Potions.WATER);
    }
}
