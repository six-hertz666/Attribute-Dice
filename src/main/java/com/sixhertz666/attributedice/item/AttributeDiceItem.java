package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.AttributeDiceMod;
import com.sixhertz666.attributedice.entity.ModEntities;
import com.sixhertz666.attributedice.entity.RollingDiceEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The dice item. Right-clicking spawns a {@link RollingDiceEntity} in front of
 * the player that spins for a configurable duration before resolving.
 */
public class AttributeDiceItem extends Item {

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

        // Spawn the dice about 2 blocks in front of the player's eyes.
        var lookVec = player.getLookAngle();
        double spawnX = player.getEyePosition().x + lookVec.x * 2.0;
        double spawnY = player.getEyePosition().y + lookVec.y * 2.0;
        double spawnZ = player.getEyePosition().z + lookVec.z * 2.0;

        RollingDiceEntity dice = new RollingDiceEntity(ModEntities.ROLLING_DICE, serverLevel);
        dice.setPos(spawnX, spawnY, spawnZ);
        dice.setInitialRotation(player.getYRot(), 0.0F);
        dice.setOwner(player);
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
}
