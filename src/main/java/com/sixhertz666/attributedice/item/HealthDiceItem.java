package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.entity.RollingDiceEntity;

/**
 * The health dice item. Behaves identically to the regular attribute dice
 * (same roll outcomes, same config values, lightning on a 1) except that
 * every roll modifies {@code MAX_HEALTH} instead of a random attribute.
 */
public class HealthDiceItem extends AttributeDiceItem {

    public HealthDiceItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void configureDice(RollingDiceEntity dice) {
        dice.setHealthDice(true);
    }

    @Override
    protected boolean isLuckEnabled() {
        return false;
    }
}
