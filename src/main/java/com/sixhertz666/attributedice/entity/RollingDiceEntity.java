package com.sixhertz666.attributedice.entity;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * A non-physical entity that visually spins in the air for a configurable
 * number of ticks before resolving on the server. The result is rolled at
 * spawn time and synced to the client so the renderer can show it once the
 * spin stops.
 */
public class RollingDiceEntity extends Entity {

    /** Synced roll result (1-6). Stored so the client renderer can display it. */
    private static final EntityDataAccessor<Integer> ROLL_RESULT =
            SynchedEntityData.defineId(RollingDiceEntity.class, EntityDataSerializers.INT);

    /** Synced flag set once the dice has stopped spinning. */
    private static final EntityDataAccessor<Boolean> STOPPED =
            SynchedEntityData.defineId(RollingDiceEntity.class, EntityDataSerializers.BOOLEAN);

    /** The player that threw the dice. Only meaningful server-side. */
    private UUID ownerUuid;

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
            ticksRemaining--;
            if (ticksRemaining <= 0 && !entityData.get(STOPPED)) {
                entityData.set(STOPPED, true);
                // Resolve the roll.
                ServerLevel serverLevel = (ServerLevel) level();
                Player owner = resolveOwner(serverLevel);
                if (owner != null) {
                    int roll = entityData.get(ROLL_RESULT);
                    AttributeDiceMod.applyDiceResult(serverLevel, owner, roll);
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
        ticksRemaining = input.getIntOr("TicksRemaining", 60);
        entityData.set(ROLL_RESULT, input.getIntOr("RollResult", 1));
        entityData.set(STOPPED, input.getBooleanOr("Stopped", false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("Owner", ownerUuid.toString());
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
