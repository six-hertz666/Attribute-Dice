package com.sixhertz666.attributedice.client;

import com.sixhertz666.attributedice.AttributeDiceMod;
import com.sixhertz666.attributedice.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Client entrypoint. Registers the renderer for the rolling dice entity.
 */
public class AttributeDiceClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AttributeDiceMod.LOGGER.info("Initializing Attribute Dice client");
        EntityRendererRegistry.register(ModEntities.ROLLING_DICE, RollingDiceEntityRenderer::new);
    }
}
