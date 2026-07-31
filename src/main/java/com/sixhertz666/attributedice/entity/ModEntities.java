package com.sixhertz666.attributedice.entity;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Holds and registers all entities added by the mod.
 */
public class ModEntities {

    public static final EntityType<RollingDiceEntity> ROLLING_DICE = register(
            "rolling_dice",
            EntityType.Builder.<RollingDiceEntity>of(RollingDiceEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(Integer.MIN_VALUE)
                    .fireImmune()
    );

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(
            String path, EntityType.Builder<T> builder) {
        Identifier id = AttributeDiceMod.id(path);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
    }

    public static void register() {
        AttributeDiceMod.LOGGER.info("Registering entities for {}", AttributeDiceMod.MOD_ID);
    }
}
