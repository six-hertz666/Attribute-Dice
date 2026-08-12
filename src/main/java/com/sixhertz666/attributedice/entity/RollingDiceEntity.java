package com.sixhertz666.attributedice.entity;

import com.sixhertz666.attributedice.AttributeDiceMod;
import net.minecraft.client.multiplayer.ClientLevel;
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
     * Synced flag indicating this is a badluck dice. When true, the dice
     * resolves via {@link AttributeDiceMod#applyBadluckDiceResult} (all outcomes
     * are losses, no lightning on a 1). When false (default), it behaves as a
     * regular attribute dice.
     */
    private static final EntityDataAccessor<Boolean> BADLUCK =
            SynchedEntityData.defineId(RollingDiceEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Synced flag indicating this is a fortune dice. When true, the dice
     * resolves via {@link AttributeDiceMod#applyFortuneDiceResult} (only rolls
     * 4-6, gives structure loot plus attribute gain).
     */
    private static final EntityDataAccessor<Boolean> FORTUNE =
            SynchedEntityData.defineId(RollingDiceEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * 同步到客户端的目标实体 UUID（字符串形式）。
     *
     * <p>关键修复：之前 targetUuid 只是一个普通字段，没有同步到客户端，
     * 导致客户端的 tick() 中 targetUuid 永远是 null，if (targetUuid != null)
     * 永远是 false，客户端根本不执行跟随逻辑。这就是骰子"只有开始和结束
     * 时位置正确"的根本原因——客户端只能依赖服务器端的位置同步包，
     * 而服务器端的位置更新频率受限于网络同步，所以看起来是离散的。
     *
     * <p>将 targetUuid 通过 SynchedEntityData 同步后，客户端在 tick() 中
     * 也能读取到目标 UUID，从而在本地每 tick 更新位置，实现真正的实时跟随。
     */
    private static final EntityDataAccessor<String> TARGET_UUID =
            SynchedEntityData.defineId(RollingDiceEntity.class, EntityDataSerializers.STRING);

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
        builder.define(BADLUCK, false);
        builder.define(FORTUNE, false);
        builder.define(TARGET_UUID, "");
    }

    @Override
    public void tick() {
        super.tick();

        // ===== 客户端 + 服务器端都执行跟随逻辑 =====
        // 关键修复：从 SynchedEntityData 读取 target UUID 字符串，
        // 这样客户端（targetUuid 字段为 null）也能获取到服务器端设置的 target。
        // 之前 targetUuid 只是普通字段没同步，导致客户端 if 判断永远 false，
        // 跟随逻辑根本不执行。
        if (targetUuid == null) {
            String synced = entityData.get(TARGET_UUID);
            if (synced != null && !synced.isEmpty()) {
                try {
                    targetUuid = UUID.fromString(synced);
                } catch (IllegalArgumentException ignored) {
                    targetUuid = null;
                }
            }
        }

        if (targetUuid != null) {
            Level lvl = level();
            Entity entity;
            if (lvl instanceof ServerLevel serverLevel) {
                entity = serverLevel.getEntity(targetUuid);
            } else if (lvl instanceof ClientLevel clientLevel) {
                // 客户端侧：通过 UUID 在客户端 level 中查找实体
                // entitiesForRendering() 是 ClientLevel 上的方法，不在 Level 基类
                entity = null;
                for (Entity e : clientLevel.entitiesForRendering()) {
                    if (e.getUUID().equals(targetUuid)) {
                        entity = e;
                        break;
                    }
                }
            } else {
                entity = null;
            }
            if (entity instanceof LivingEntity living && living.isAlive()) {
                AABB box = living.getBoundingBox();
                double followX = box.getCenter().x;
                double followY = box.maxY + SPAWN_OFFSET_ABOVE_TARGET;
                double followZ = box.getCenter().z;
                setPos(followX, followY, followZ);
            }
        }

        if (!level().isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level();

            ticksRemaining--;
            if (ticksRemaining <= 0 && !entityData.get(STOPPED)) {
                entityData.set(STOPPED, true);
                // Resolve the roll.
                Player owner = resolveOwner(serverLevel);
                if (owner != null) {
                    LivingEntity target = resolveTarget(serverLevel, owner);
                    int roll = entityData.get(ROLL_RESULT);
                    if (entityData.get(FORTUNE)) {
                        AttributeDiceMod.applyFortuneDiceResult(serverLevel, target, owner, roll);
                    } else if (entityData.get(BADLUCK)) {
                        AttributeDiceMod.applyBadluckDiceResult(serverLevel, target, owner, roll);
                    } else {
                        AttributeDiceMod.applyDiceResult(serverLevel, target, owner, roll);
                    }
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

    /**
     * Marks this dice as a badluck dice. Must be called before the dice is
     * added to the world so the resolution logic in {@link #tick()} picks the
     * correct branch.
     */
    public void setBadluck(boolean badluck) {
        entityData.set(BADLUCK, badluck);
    }

    public boolean isBadluck() {
        return entityData.get(BADLUCK);
    }

    /**
     * Marks this dice as a fortune dice. Must be called before the dice is
     * added to the world so the resolution logic in {@link #tick()} picks the
     * correct branch.
     */
    public void setFortune(boolean fortune) {
        entityData.set(FORTUNE, fortune);
    }

    public boolean isFortune() {
        return entityData.get(FORTUNE);
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
    }

    /**
     * Sets the entity that will receive attribute changes when the dice
     * resolves. If not set, the owner (player) is the target.
     *
     * <p>关键修复：同时将 UUID 同步到 SynchedEntityData，这样客户端
     * 的骰子实体也能获取到 target UUID，从而在客户端 tick() 中执行跟随逻辑。
     */
    public void setTarget(LivingEntity target) {
        this.targetUuid = target.getUUID();
        entityData.set(TARGET_UUID, target.getUUID().toString());
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
                // 同步到 SynchedEntityData 以便客户端也能获取
                entityData.set(TARGET_UUID, targetStr);
            } catch (IllegalArgumentException ignored) {
                targetUuid = null;
            }
        }
        ticksRemaining = input.getIntOr("TicksRemaining", 60);
        entityData.set(ROLL_RESULT, input.getIntOr("RollResult", 1));
        entityData.set(STOPPED, input.getBooleanOr("Stopped", false));
        entityData.set(BADLUCK, input.getBooleanOr("Badluck", false));
        entityData.set(FORTUNE, input.getBooleanOr("Fortune", false));
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
        output.putBoolean("Badluck", entityData.get(BADLUCK));
        output.putBoolean("Fortune", entityData.get(FORTUNE));
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
