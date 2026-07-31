package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

/**
 * Holds and registers all items added by the mod.
 */
public class ModItems {

    public static final Item ATTRIBUTE_DICE = register(
            "attribute_dice",
            AttributeDiceItem::new,
            new Item.Properties().stacksTo(16)
    );

    public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, AttributeDiceMod.id(name));
        T item = itemFactory.apply(settings.setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);
        return item;
    }

    public static void register() {
        AttributeDiceMod.LOGGER.info("Registering items for {}", AttributeDiceMod.MOD_ID);
    }
}
