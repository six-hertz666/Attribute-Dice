package com.sixhertz666.attributedice.entity;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * A non-physical entity that visually spins in the air for a configurable
 * number of ticks before resolving on the server. The result is rolled at
 * spawn time and synced to the client so the renderer can show it once the
 * spin stops.
 *
 * <p>The dice may be owned by either a {@link Player} (rolled on the player
 * themself) or by a player but targeting another {@link LivingEntity} (rolled
 * on that entity, e.g. via shift-right-click). In either case the player
 * receives chat feedback while the target receives attribute changes.
 */
public class RollingDiceEntity extends Entity {

    /** Synced roll result (1-6). Stored so the client renderer can display it. */
    private static final EntityDataAccessor<Integer> ROLL_RESULT =
            SynchedEntityData.defineId(RollingDiceEntity.class, EntityDataSerializers.INT);

    /** Synced flag set once the dice has stopped spinning. */
    private static final EntityDataAccessor<Boolean> STOPPED =
            SynchedEntityData.defineId(RollingDiceEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Vertical offset above the target entity's bounding box top. Kept in
     * sync with {@code AttributeDiceItem.SPAWN_OFFSET_ABOVE_TARGET} so the
     * dice stays at the same height it was spawned at while following the
     * target around.
     */
    private static final double SPAWN_OFFSET_ABOVE_TARGET = 1.5;

    /** The player that threw the dice. Always set; receives chat feedback. */
    private UUID ownerUuid;

    /**
     * The entity that receives attribute changes. May be the same as the owner
     * (when rolling on oneself) or a different entity (shift-right-click on a
     * mob). When null, the owner is treated as the target. While this is set
     * and the target is alive, the dice follows the target each tick so it
     * stays floating above its head.
     */
    private UUID targetUuid;

    /** Number of ticks remaining before the dice resolves. */
    private int ticksRemaining = 60;

    public RollingDiceEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROLL_RESULT, 1);
        builder.define(STOPPED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            // While a target is set and still alive, follow it so the dice
            // stays floating above the entity's head as it moves around. This
            // applies both during the spin and after the dice has stopped, up
            // until the entity is discarded. Position changes are synced to
            // clients through the standard entity sync machinery.
            ServerLevel serverLevel = (ServerLevel) level();
            if (targetUuid != null) {
                Entity entity = serverLevel.getEntity(targetUuid);
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    AABB box = living.getBoundingBox();
                    double followX = box.getCenter().x;
                    double followY = box.maxY + SPAWN_OFFSET_ABOVE_TARGET;
                    double followZ = box.getCenter().z;
                    setPos(followX, followY, followZ);
                }
            }

            ticksRemaining--;
            if (ticksRemaining <= 0 && !entityData.get(STOPPED)) {
                entityData.set(STOPPED, true);
                // Resolve the roll.
                Player owner = resolveOwner(serverLevel);
                if (owner != null) {
                    LivingEntity target = resolveTarget(serverLevel, owner);
                    int roll = entityData.get(ROLL_RESULT);
                    AttributeDiceMod.applyDiceResult(serverLevel, target, owner, roll);
                }
                // Schedule discard a few ticks later so the player can briefly
                // see the stopped dice.
                ticksRemaining = 20;
            } else if (entityData.get(STOPPED) && ticksRemaining <= 0) {
                discard();
            }
        }
    }

    public void setRollResult(int roll) {
        entityData.set(ROLL_RESULT, Math.max(1, Math.min(6, roll)));
    }

    public void setRollDurationTicks(int ticks) {
        this.ticksRemaining = Math.max(1, ticks);
    }

    public int getRollResult() {
        return entityData.get(ROLL_RESULT);
    }

    public boolean isStopped() {
        return entityData.get(STOPPED);
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
    }

    /**
     * Sets the entity that will receive attribute changes when the dice
     * resolves. If not set, the owner (player) is the target.
     */
    public void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
    }

    /**
     * Sets the dice's initial rotation. {@code setRot} is protected on
     * {@link Entity}; this method exposes it to callers such as the item
     * that spawns the dice.
     */
    public void setInitialRotation(float yaw, float pitch) {
        setRot(yaw, pitch);
    }

    private Player resolveOwner(ServerLevel level) {
        if (ownerUuid == null) {
            return null;
        }
        return level.getPlayerByUUID(ownerUuid);
    }

    private LivingEntity resolveTarget(ServerLevel level, Player fallback) {
        if (targetUuid == null) {
            return fallback;
        }
        Entity entity = level.getEntity(targetUuid);
        if (entity instanceof LivingEntity living) {
            return living;
        }
        // Target disappeared (e.g. despawned/died) — fall back to the owner
        // so the dice roll is not lost.
        return fallback;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String ownerStr = input.getStringOr("Owner", "");
        if (!ownerStr.isEmpty()) {
            try {
                ownerUuid = UUID.fromString(ownerStr);
            } catch (IllegalArgumentException ignored) {
                ownerUuid = null;
            }
        }
        String targetStr = input.getStringOr("Target", "");
        if (!targetStr.isEmpty()) {
            try {
                targetUuid = UUID.fromString(targetStr);
            } catch (IllegalArgumentException ignored) {
                targetUuid = null;
            }
        }
        ticksRemaining = input.getIntOr("TicksRemaining", 60);
        entityData.set(ROLL_RESULT, input.getIntOr("RollResult", 1));
        entityData.set(STOPPED, input.getBooleanOr("Stopped", false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("Owner", ownerUuid.toString());
        }
        if (targetUuid != null) {
            output.putString("Target", targetUuid.toString());
        }
        output.putInt("TicksRemaining", ticksRemaining);
        output.putInt("RollResult", entityData.get(ROLL_RESULT));
        output.putBoolean("Stopped", entityData.get(STOPPED));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0 * 64.0;
    }
}
