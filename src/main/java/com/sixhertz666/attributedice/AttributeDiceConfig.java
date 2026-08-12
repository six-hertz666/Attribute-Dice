package com.sixhertz666.attributedice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for Attribute Dice. Loaded from
 * {@code config/attribute_dice.json}. If the file does not exist a default
 * configuration is written and used.
 *
 * <p>Each dice face has its own configurable outcome:
 * <ul>
 *   <li>6 - fixed gain ({@link #roll6Gain})</li>
 *   <li>5 - gain in range [{@link #roll5GainMin}, {@link #roll5GainMax}]</li>
 *   <li>4 - gain in range [{@link #roll4GainMin}, {@link #roll4GainMax}]</li>
 *   <li>3 - loss in range [{@link #roll3LossMin}, {@link #roll3LossMax}]</li>
 *   <li>2 - loss in range [{@link #roll2LossMin}, {@link #roll2LossMax}]</li>
 *   <li>1 - fixed loss ({@link #roll1Loss})</li>
 * </ul>
 */
public class AttributeDiceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Fixed amount gained when rolling a 6. */
    public int roll6Gain = 10;

    /** Minimum (inclusive) amount gained when rolling a 5. */
    public int roll5GainMin = 5;
    /** Maximum (inclusive) amount gained when rolling a 5. */
    public int roll5GainMax = 9;

    /** Minimum (inclusive) amount gained when rolling a 4. */
    public int roll4GainMin = 1;
    /** Maximum (inclusive) amount gained when rolling a 4. */
    public int roll4GainMax = 4;

    /** Minimum (inclusive) amount lost when rolling a 3. */
    public int roll3LossMin = 1;
    /** Maximum (inclusive) amount lost when rolling a 3. */
    public int roll3LossMax = 2;

    /** Minimum (inclusive) amount lost when rolling a 2. */
    public int roll2LossMin = 3;
    /** Maximum (inclusive) amount lost when rolling a 2. */
    public int roll2LossMax = 4;

    /** Fixed amount lost when rolling a 1. */
    public int roll1Loss = 5;

    /** Lightning damage dealt when the player rolls a 1. */
    public float lightningDamage = 10.0F;
    /** Whether rolling a 1 spawns lightning and damages the player. */
    public boolean enableLightning = true;
    /** How long (in ticks) the dice spins before resolving. 20 ticks = 1 second. */
    public int rollDurationTicks = 60;

    // ===== 霉运骰子（Badluck Dice）配置：所有结果均为减少 =====
    /** Minimum (inclusive) amount lost when a badluck dice rolls 6. Range [9, 10]. */
    public int badluck6LossMin = 9;
    /** Maximum (inclusive) amount lost when a badluck dice rolls 6. */
    public int badluck6LossMax = 10;

    /** Minimum (inclusive) amount lost when a badluck dice rolls 5. Range [7, 9]. */
    public int badluck5LossMin = 7;
    /** Maximum (inclusive) amount lost when a badluck dice rolls 5. */
    public int badluck5LossMax = 9;

    /** Minimum (inclusive) amount lost when a badluck dice rolls 4. Range [6, 7]. */
    public int badluck4LossMin = 6;
    /** Maximum (inclusive) amount lost when a badluck dice rolls 4. */
    public int badluck4LossMax = 7;

    /** Minimum (inclusive) amount lost when a badluck dice rolls 3. Range [4, 6]. */
    public int badluck3LossMin = 4;
    /** Maximum (inclusive) amount lost when a badluck dice rolls 3. */
    public int badluck3LossMax = 6;

    /** Minimum (inclusive) amount lost when a badluck dice rolls 2. Range [3, 4]. */
    public int badluck2LossMin = 3;
    /** Maximum (inclusive) amount lost when a badluck dice rolls 2. */
    public int badluck2LossMax = 4;

    /** Minimum (inclusive) amount lost when a badluck dice rolls 1. Range [1, 3]. */
    public int badluck1LossMin = 1;
    /** Maximum (inclusive) amount lost when a badluck dice rolls 1. */
    public int badluck1LossMax = 3;

    // ===== 财富骰子（Fortune Dice）配置：所有结果均为增加 =====
    /** Fixed amount gained when a fortune dice rolls 6. Default: 12. */
    public int fortune6Gain = 12;

    /** Minimum (inclusive) amount gained when a fortune dice rolls 5. Default: 8. */
    public int fortune5GainMin = 8;
    /** Maximum (inclusive) amount gained when a fortune dice rolls 5. Default: 11. */
    public int fortune5GainMax = 11;

    /** Minimum (inclusive) amount gained when a fortune dice rolls 4. Default: 4. */
    public int fortune4GainMin = 4;
    /** Maximum (inclusive) amount gained when a fortune dice rolls 4. Default: 7. */
    public int fortune4GainMax = 7;

    // ===== 幸运值（Luck）机制配置 =====
    /** 普通属性骰子开出6点时增加幸运值的最小值（包含）。默认：0。 */
    public int luckOnRoll6Min = 0;
    /** 普通属性骰子开出6点时增加幸运值的最大值（包含）。默认：2。 */
    public int luckOnRoll6Max = 2;
    /** 幸运值超过此阈值时，骰子只会 rolls 4/5/6 点。默认：100。 */
    public int luckThreshold = 100;

    public static AttributeDiceConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDir.resolve("attribute_dice.json");

        AttributeDiceConfig config;
        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                config = GSON.fromJson(json, AttributeDiceConfig.class);
                if (config == null) {
                    config = new AttributeDiceConfig();
                }
            } catch (IOException e) {
                AttributeDiceMod.LOGGER.error("Failed to read Attribute Dice config, using defaults", e);
                config = new AttributeDiceConfig();
            }
        } else {
            config = new AttributeDiceConfig();
        }

        // Clamp values to safe ranges.
        config.roll6Gain = Math.max(0, config.roll6Gain);
        config.roll1Loss = Math.max(0, config.roll1Loss);
        config.roll5GainMin = Math.max(0, config.roll5GainMin);
        config.roll5GainMax = Math.max(config.roll5GainMin, config.roll5GainMax);
        config.roll4GainMin = Math.max(0, config.roll4GainMin);
        config.roll4GainMax = Math.max(config.roll4GainMin, config.roll4GainMax);
        config.roll3LossMin = Math.max(0, config.roll3LossMin);
        config.roll3LossMax = Math.max(config.roll3LossMin, config.roll3LossMax);
        config.roll2LossMin = Math.max(0, config.roll2LossMin);
        config.roll2LossMax = Math.max(config.roll2LossMin, config.roll2LossMax);
        config.rollDurationTicks = Math.max(1, config.rollDurationTicks);
        config.lightningDamage = Math.max(0.0F, config.lightningDamage);

        // 霉运骰子范围 clamp
        config.badluck6LossMin = Math.max(0, config.badluck6LossMin);
        config.badluck6LossMax = Math.max(config.badluck6LossMin, config.badluck6LossMax);
        config.badluck5LossMin = Math.max(0, config.badluck5LossMin);
        config.badluck5LossMax = Math.max(config.badluck5LossMin, config.badluck5LossMax);
        config.badluck4LossMin = Math.max(0, config.badluck4LossMin);
        config.badluck4LossMax = Math.max(config.badluck4LossMin, config.badluck4LossMax);
        config.badluck3LossMin = Math.max(0, config.badluck3LossMin);
        config.badluck3LossMax = Math.max(config.badluck3LossMin, config.badluck3LossMax);
        config.badluck2LossMin = Math.max(0, config.badluck2LossMin);
        config.badluck2LossMax = Math.max(config.badluck2LossMin, config.badluck2LossMax);
        config.badluck1LossMin = Math.max(0, config.badluck1LossMin);
        config.badluck1LossMax = Math.max(config.badluck1LossMin, config.badluck1LossMax);

        // 财富骰子范围 clamp
        config.fortune6Gain = Math.max(0, config.fortune6Gain);
        config.fortune5GainMin = Math.max(0, config.fortune5GainMin);
        config.fortune5GainMax = Math.max(config.fortune5GainMin, config.fortune5GainMax);
        config.fortune4GainMin = Math.max(0, config.fortune4GainMin);
        config.fortune4GainMax = Math.max(config.fortune4GainMin, config.fortune4GainMax);

        // 幸运值机制 clamp
        config.luckOnRoll6Min = Math.max(0, config.luckOnRoll6Min);
        config.luckOnRoll6Max = Math.max(config.luckOnRoll6Min, config.luckOnRoll6Max);
        config.luckThreshold = Math.max(0, config.luckThreshold);

        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, GSON.toJson(config));
        } catch (IOException e) {
            AttributeDiceMod.LOGGER.error("Failed to write Attribute Dice config", e);
        }

        return config;
    }
}
