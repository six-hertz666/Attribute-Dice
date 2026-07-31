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

        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, GSON.toJson(config));
        } catch (IOException e) {
            AttributeDiceMod.LOGGER.error("Failed to write Attribute Dice config", e);
        }

        return config;
    }
}
