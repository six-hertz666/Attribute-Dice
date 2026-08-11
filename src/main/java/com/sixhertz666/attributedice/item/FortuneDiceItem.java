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
 * The fortune dice item. Can only be used on the player themselves (no
 * targeting other entities). Right-clicking always rolls a result between
 * 4 and 6 (never 1/2/3). In addition to attribute gains, rolling a 4 grants
 * village toolsmith loot, a 5 grants desert pyramid loot, and a 6 grants
 * bastion remnant loot.
 */
public class FortuneDiceItem extends Item {

    public FortuneDiceItem(Properties properties) {
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
        dice.setFortune(true);

        // Spawn the dice about 2 blocks in front of the player's eyes.
        // Fortune dice can only target the player themselves.
        var lookVec = player.getLookAngle();
        double spawnX = player.getEyePosition().x + lookVec.x * 2.0;
        double spawnY = player.getEyePosition().y + lookVec.y * 2.0;
        double spawnZ = player.getEyePosition().z + lookVec.z * 2.0;
        dice.setPos(spawnX, spawnY, spawnZ);

        dice.setInitialRotation(player.getYRot(), 0.0F);
        serverLevel.addFreshEntity(dice);

        // Fortune dice: only roll 4, 5, or 6 (never 1, 2, or 3).
        int roll = serverLevel.getRandom().nextIntBetweenInclusive(4, 6);
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
