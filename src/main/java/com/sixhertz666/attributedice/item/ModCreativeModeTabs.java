package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Registers the mod's creative mode tab and populates it with items.
 */
public class ModCreativeModeTabs {

    public static final ResourceKey<CreativeModeTab> ATTRIBUTE_DICE_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(AttributeDiceMod.MOD_ID, "attribute_dice_tab")
    );

    public static void register() {
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                ATTRIBUTE_DICE_TAB,
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.attribute_dice.attribute_dice_tab"))
                        .icon(() -> new ItemStack(ModItems.ATTRIBUTE_DICE))
                        .displayItems((parameters, output) -> {
                            output.accept(ModItems.ATTRIBUTE_DICE);
                            output.accept(ModItems.BADLUCK_DICE);
                            output.accept(ModItems.FORTUNE_DICE);
                            output.accept(ModItems.DAMAGE_DICE);
                            output.accept(ModItems.ARMOR_DICE);
                            output.accept(ModItems.HEALTH_DICE);
                        })
                        .build()
        );

        AttributeDiceMod.LOGGER.info("Registered creative mode tab for {}", AttributeDiceMod.MOD_ID);
    }
}
