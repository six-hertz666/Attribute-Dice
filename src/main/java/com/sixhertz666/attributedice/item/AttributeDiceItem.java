package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.AttributeDiceMod;
import com.sixhertz666.attributedice.entity.ModEntities;
import com.sixhertz666.attributedice.entity.RollingDiceEntity;
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
 * The dice item. Right-clicking spawns a {@link RollingDiceEntity} in front of
 * the player that spins for a configurable duration before resolving.
 *
 * <p>When the player is sneaking (shift) while right-clicking and a
 * {@link LivingEntity} is in their line of sight, the dice is placed above
 * that entity's head and the roll is applied to it using the same rules
 * that govern player-targeted rolls.
 */
public class AttributeDiceItem extends Item {

    /** Reach distance (in blocks) used when scanning for an entity target. */
    private static final double ENTITY_REACH = 5.0;

    /** Vertical offset above the target entity's bounding box top. */
    private static final double SPAWN_OFFSET_ABOVE_TARGET = 1.5;

    public AttributeDiceItem(Properties properties) {
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

        RollingDiceEntity dice = new RollingDiceEntity(ModEntities.ROLLING_DICE, serverLevel);
        dice.setOwner(player);

        // Shift + right-click: try to target the living entity the player is
        // currently looking at. If found, spawn the dice above its head and
        // mark it as the roll target.
        LivingEntity target = null;
        if (player.isSecondaryUseActive()) {
            target = findEntityUnderCrosshair(serverLevel, player);
        }

        if (target != null && target != player) {
            AABB box = target.getBoundingBox();
            double spawnX = box.getCenter().x;
            double spawnY = box.maxY + SPAWN_OFFSET_ABOVE_TARGET;
            double spawnZ = box.getCenter().z;
            dice.setPos(spawnX, spawnY, spawnZ);
            dice.setTarget(target);
        } else {
            // 对自己使用：设置 target 为玩家自己，
            // 这样骰子会在 tick() 中实时跟随玩家头顶移动。
            dice.setTarget(player);
            AABB box = player.getBoundingBox();
            double spawnX = box.getCenter().x;
            double spawnY = box.maxY + SPAWN_OFFSET_ABOVE_TARGET;
            double spawnZ = box.getCenter().z;
            dice.setPos(spawnX, spawnY, spawnZ);
        }

        dice.setInitialRotation(player.getYRot(), 0.0F);
        // 让子类（伤害/护甲/生命骰子）有机会在实体加入世界前
        // 设置对应的变体标志。基类默认不做任何操作。
        configureDice(dice);
        serverLevel.addFreshEntity(dice);

        // Roll a fair 1-6 result and store it on the entity so the client
        // can render it spinning while the server awaits the tick timer.
        int roll;
        // 幸运值机制：只有普通属性骰子（非子类）才有幸运阈值判断
        if (isLuckEnabled() && player.getLuck() > AttributeDiceMod.CONFIG.luckThreshold) {
            roll = serverLevel.getRandom().nextIntBetweenInclusive(4, 6);
        } else {
            roll = serverLevel.getRandom().nextIntBetweenInclusive(1, 6);
        }
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

    /**
     * Hook invoked just before the dice entity is added to the world.
     * Subclasses (damage/armor/health dice) override this to mark the entity
     * with the appropriate variant flag so the resolution logic in
     * {@link RollingDiceEntity#tick()} dispatches to the correct apply method.
     *
     * <p>The base implementation is a no-op, preserving regular dice behavior.
     *
     * @param dice the dice entity about to be spawned (already positioned and
     *             targeted; roll result not yet set)
     */
    protected void configureDice(RollingDiceEntity dice) {
        // no-op: regular dice has no variant flag
    }

    /**
     * Returns whether the luck mechanism (luck threshold roll restriction)
     * is enabled for this dice type. Only the base attribute dice returns
     * {@code true}; subclasses (damage/armor/health dice) return {@code false}
     * because the luck mechanism does not apply to them.
     */
    protected boolean isLuckEnabled() {
        return true;
    }
}
