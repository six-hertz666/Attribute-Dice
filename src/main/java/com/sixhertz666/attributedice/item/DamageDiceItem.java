package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.entity.RollingDiceEntity;

/**
 * The damage dice item. Behaves identically to the regular attribute dice
 * (same roll outcomes, same config values, lightning on a 1) except that
 * every roll modifies {@code ATTACK_DAMAGE} instead of a random attribute.
 */
public class DamageDiceItem extends AttributeDiceItem {

    public DamageDiceItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void configureDice(RollingDiceEntity dice) {
        dice.setDamageDice(true);
    }
}
