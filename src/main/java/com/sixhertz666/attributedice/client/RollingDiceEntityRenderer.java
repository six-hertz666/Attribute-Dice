package com.sixhertz666.attributedice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sixhertz666.attributedice.AttributeDiceMod;
import com.sixhertz666.attributedice.entity.RollingDiceEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Renders the rolling dice as a spinning cube. All six faces use the same
 * texture, applied as a full quad per face.
 *
 * <p>1.21.11 splits entity rendering into {@code extractRenderState} (gather
 * data from the entity on the render thread) and {@code submit} (emit draw
 * nodes to the {@link SubmitNodeCollector}). There is no longer a
 * {@code render} method or a {@code getTextureLocation} override; the texture
 * identifier is held as a field and passed to {@link RenderTypes#entitySolid}.
 */
public class RollingDiceEntityRenderer extends EntityRenderer<RollingDiceEntity, RollingDiceEntityRenderer.DiceRenderState> {

    private static final Identifier TEXTURE = AttributeDiceMod.id("textures/entity/dice.png");

    /** Half the cube's side length, in blocks. */
    private static final float HALF = 0.5F;

    public RollingDiceEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
    }

    @Override
    public DiceRenderState createRenderState() {
        return new DiceRenderState();
    }

    @Override
    public void extractRenderState(RollingDiceEntity entity, DiceRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.rollResult = entity.getRollResult();
        state.stopped = entity.isStopped();
        state.tickCount = entity.tickCount;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(DiceRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        // Slight hover above the spawn position so the dice is centered on
        // the player's eye line.
        poseStack.translate(0.0, -HALF, 0.0);

        float time = state.tickCount + state.partialTick;
        if (!state.stopped) {
            float spin = time * 25.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            poseStack.mulPose(Axis.YP.rotationDegrees(spin * 0.8F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(spin * 0.5F));
        } else {
            // Settle to a flat orientation showing the rolled face up.
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        }

        // Half-block cube
        poseStack.scale(0.5F, 0.5F, 0.5F);

        int packedLight = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(TEXTURE),
                (pose, consumer) -> drawCube(pose, consumer, packedLight));

        poseStack.popPose();
    }

    /**
     * Emits 6 faces (4 vertices each, CCW when viewed from outside) covering
     * a unit cube spanning [-HALF, HALF] on every axis. Each face uses the
     * full texture.
     */
    private static void drawCube(PoseStack.Pose pose, VertexConsumer consumer, int packedLight) {
        // South face (+Z), normal (0,0,1)
        quad(pose, consumer, packedLight,
                -HALF,  HALF, HALF, 0, 0,
                 HALF,  HALF, HALF, 1, 0,
                 HALF, -HALF, HALF, 1, 1,
                -HALF, -HALF, HALF, 0, 1,
                0, 0, 1);

        // North face (-Z), normal (0,0,-1)
        quad(pose, consumer, packedLight,
                 HALF,  HALF, -HALF, 0, 0,
                -HALF,  HALF, -HALF, 1, 0,
                -HALF, -HALF, -HALF, 1, 1,
                 HALF, -HALF, -HALF, 0, 1,
                0, 0, -1);

        // East face (+X), normal (1,0,0)
        quad(pose, consumer, packedLight,
                 HALF,  HALF,  HALF, 0, 0,
                 HALF,  HALF, -HALF, 1, 0,
                 HALF, -HALF, -HALF, 1, 1,
                 HALF, -HALF,  HALF, 0, 1,
                1, 0, 0);

        // West face (-X), normal (-1,0,0)
        quad(pose, consumer, packedLight,
                -HALF,  HALF, -HALF, 0, 0,
                -HALF,  HALF,  HALF, 1, 0,
                -HALF, -HALF,  HALF, 1, 1,
                -HALF, -HALF, -HALF, 0, 1,
                -1, 0, 0);

        // Top face (+Y), normal (0,1,0)
        quad(pose, consumer, packedLight,
                -HALF, HALF, -HALF, 0, 0,
                 HALF, HALF, -HALF, 1, 0,
                 HALF, HALF,  HALF, 1, 1,
                -HALF, HALF,  HALF, 0, 1,
                0, 1, 0);

        // Bottom face (-Y), normal (0,-1,0)
        quad(pose, consumer, packedLight,
                -HALF, -HALF,  HALF, 0, 0,
                 HALF, -HALF,  HALF, 1, 0,
                 HALF, -HALF, -HALF, 1, 1,
                -HALF, -HALF, -HALF, 0, 1,
                0, -1, 0);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                              float x1, float y1, float z1, float u1, float v1,
                              float x2, float y2, float z2, float u2, float v2,
                              float x3, float y3, float z3, float u3, float v3,
                              float x4, float y4, float z4, float u4, float v4,
                              float nx, float ny, float nz) {
        consumer.addVertex(pose, x1, y1, z1)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x2, y2, z2)
                .setColor(255, 255, 255, 255)
                .setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x3, y3, z3)
                .setColor(255, 255, 255, 255)
                .setUv(u3, v3)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x4, y4, z4)
                .setColor(255, 255, 255, 255)
                .setUv(u4, v4)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }

    /** Custom render state carrying the dice's roll result and spin flags. */
    public static class DiceRenderState extends EntityRenderState {
        public int rollResult = 1;
        public boolean stopped = false;
        public int tickCount = 0;
        public float partialTick = 0.0F;
    }
}
