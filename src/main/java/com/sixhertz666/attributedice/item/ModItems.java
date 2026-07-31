package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Holds and registers all items added by the mod.
 */
public class ModItems {

    public static final Item ATTRIBUTE_DICE = register(
            "attribute_dice",
            new AttributeDiceItem(new Item.Properties()
                    .stacksTo(16))
    );

    private static Item register(String path, Item item) {
        Identifier id = AttributeDiceMod.id(path);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void register() {
        AttributeDiceMod.LOGGER.info("Registering items for {}", AttributeDiceMod.MOD_ID);
    }
}
