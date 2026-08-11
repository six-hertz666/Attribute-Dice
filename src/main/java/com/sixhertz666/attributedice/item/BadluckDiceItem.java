package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.AttributeDiceMod;
import com.sixhertz666.attributedice.entity.ModEntities;
import com.sixhertz666.attributedice.entity.RollingDiceEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * The badluck dice item. Right-clicking a living entity (no Shift required)
 * spawns a {@link RollingDiceEntity} floating above its head, marked as a
 * badluck dice. When the dice resolves, the target takes a random loss to
 * one of attack damage / armor / max health. Rolling a 1 does NOT spawn
 * lightning.
 *
 * <p>If the player right-clicks without a target entity in their line of
 * sight, the action fails and the item is not consumed.
 */
public class BadluckDiceItem extends Item {

    /** Reach distance (in blocks) used when scanning for an entity target. */
    private static final double ENTITY_REACH = 5.0;

    /** Vertical offset above the target entity's bounding box top. */
    private static final double SPAWN_OFFSET_ABOVE_TARGET = 1.5;

    public BadluckDiceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            // Swing arm locally; server handles actual spawn.
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        // 霉运骰子：直接右键实体即可（不需要 Shift）。必须瞄准其他活体实体。
        LivingEntity target = findEntityUnderCrosshair(serverLevel, player);

        if (target == null || target == player) {
            // 没找到有效目标，给玩家提示，不消耗物品。
            player.displayClientMessage(
                    Component.translatable("attribute_dice.message.badluck_no_target")
                            .withStyle(ChatFormatting.RED),
                    false);
            return InteractionResult.FAIL;
        }

        RollingDiceEntity dice = new RollingDiceEntity(ModEntities.ROLLING_DICE, serverLevel);
        dice.setOwner(player);
        dice.setBadluck(true);

        AABB box = target.getBoundingBox();
        double spawnX = box.getCenter().x;
        double spawnY = box.maxY + SPAWN_OFFSET_ABOVE_TARGET;
        double spawnZ = box.getCenter().z;
        dice.setPos(spawnX, spawnY, spawnZ);
        dice.setTarget(target);
        dice.setInitialRotation(player.getYRot(), 0.0F);
        serverLevel.addFreshEntity(dice);

        // Roll a fair 1-6 result and store it on the entity so the client
        // can render it spinning while the server awaits the tick timer.
        int roll = serverLevel.getRandom().nextIntBetweenInclusive(1, 6);
        dice.setRollResult(roll);
        dice.setRollDurationTicks(AttributeDiceMod.CONFIG.rollDurationTicks);

        level.playSound(null, dice.blockPosition(),
                SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.8F, 1.2F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.swing(hand);
        return InteractionResult.CONSUME;
    }

    /**
     * Performs an entity ray-trace from the player's eyes along their look
     * vector and returns the closest living entity hit (excluding the player
     * themself). Returns {@code null} if nothing is in range.
     */
    private static LivingEntity findEntityUnderCrosshair(ServerLevel level, Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(ENTITY_REACH));

        AABB searchBox = new AABB(eyePos, endPos).inflate(1.0);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, searchBox,
                e -> e.isAlive() && e != player
        );

        LivingEntity closest = null;
        double closestDist = ENTITY_REACH * ENTITY_REACH;
        for (LivingEntity candidate : candidates) {
            AABB entityBox = candidate.getBoundingBox()
                    .inflate(candidate.getPickRadius());
            Optional<Vec3> hit = entityBox.clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = candidate;
                }
            }
        }
        return closest;
    }
}
