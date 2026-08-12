package com.sixhertz666.attributedice;

import com.sixhertz666.attributedice.item.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Handles boss death events to drop attribute dice. When certain boss
 * entities are killed, a random quantity of attribute dice is dropped at
 * their location.
 *
 * <ul>
 *   <li>Ender Dragon: 2–6 dice</li>
 *   <li>Wither: 2–6 dice</li>
 *   <li>Elder Guardian: 0–3 dice</li>
 *   <li>Warden: 5–10 dice</li>
 * </ul>
 */
public class BossDropHandler {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            RandomSource random = serverLevel.getRandom();
            EntityType<?> type = entity.getType();

            int count = 0;

            if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
                count = 2 + random.nextInt(5); // 2-6
            } else if (type == EntityType.ELDER_GUARDIAN) {
                count = random.nextInt(4); // 0-3
            } else if (type == EntityType.WARDEN) {
                count = 5 + random.nextInt(6); // 5-10
            }

            if (count > 0) {
                ItemStack stack = new ItemStack(ModItems.ATTRIBUTE_DICE, count);
                ItemEntity itemEntity = new ItemEntity(
                    serverLevel,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    stack
                );
                itemEntity.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(itemEntity);
            }
        });
    }
}
