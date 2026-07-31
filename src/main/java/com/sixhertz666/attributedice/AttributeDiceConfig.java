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
 */
public class AttributeDiceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Minimum value rolled for a positive gain (4/5/6). */
    public int gainMin = 1;
    /** Maximum value rolled for a positive gain (4/5/6). */
    public int gainMax = 10;
    /** Minimum value rolled for a negative loss (1/2/3). */
    public int lossMin = 1;
    /** Maximum value rolled for a negative loss (1/2/3). */
    public int lossMax = 5;
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
        config.gainMin = Math.max(0, config.gainMin);
        config.gainMax = Math.max(config.gainMin, config.gainMax);
        config.lossMin = Math.max(0, config.lossMin);
        config.lossMax = Math.max(config.lossMin, config.lossMax);
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
