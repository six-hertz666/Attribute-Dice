package com.sixhertz666.attributedice.item;

import com.sixhertz666.attributedice.entity.RollingDiceEntity;

/**
 * The armor dice item. Behaves identically to the regular attribute dice
 * (same roll outcomes, same config values, lightning on a 1) except that
 * every roll modifies {@code ARMOR} instead of a random attribute.
 */
public class ArmorDiceItem extends AttributeDiceItem {

    public ArmorDiceItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void configureDice(RollingDiceEntity dice) {
        dice.setArmorDice(true);
    }
}
