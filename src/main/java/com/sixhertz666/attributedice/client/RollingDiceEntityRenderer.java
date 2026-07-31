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
 * Renders the rolling dice as a spinning cube with 6 different faces.
 * The dice texture is 16×96 pixels, each face occupying 16×16 pixels
 * vertically (faces 1-6 from top to bottom).
 *
 * Face layout (standard dice):
 * - Top (+Y): face 1
 * - Bottom (-Y): face 6
 * - South (+Z): face 2
 * - North (-Z): face 5
 * - East (+X): face 3
 * - West (-X): face 4
 */
public class RollingDiceEntityRenderer extends EntityRenderer<RollingDiceEntity, RollingDiceEntityRenderer.DiceRenderState> {

    private static final Identifier TEXTURE = AttributeDiceMod.id("textures/entity/dice.png");

    /** Half the cube's side length, in blocks. */
    private static final float HALF = 0.5F;

    /** UV height for each face (1/6 of texture height). */
    private static final float FACE_HEIGHT = 1.0F / 6.0F;

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
            // Settle so the face matching rollResult points up (+Y).
            switch (state.rollResult) {
                case 1 -> {} // face 1 already on top (+Y)
                case 2 -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                case 3 -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                case 4 -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                case 5 -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                case 6 -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                default -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            }
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
     * a unit cube spanning [-HALF, HALF] on every axis.
     * Each face uses a different part of the texture (1/6 height each).
     *
     * Face mapping (standard dice):
     * - Top (+Y): face 1 (0)
     * - Bottom (-Y): face 6 (5)
     * - South (+Z): face 2 (1)
     * - North (-Z): face 5 (4)
     * - East (+X): face 3 (2)
     * - West (-X): face 4 (3)
     */
    private static void drawCube(PoseStack.Pose pose, VertexConsumer consumer, int packedLight) {
        // South face (+Z), face index 1 (dice face 2)
        // CCW from +Z: top-left → bottom-left → bottom-right → top-right
        drawFace(pose, consumer, packedLight, 1,
                -HALF,  HALF, HALF,   // top-left
                -HALF, -HALF, HALF,   // bottom-left
                 HALF, -HALF, HALF,   // bottom-right
                 HALF,  HALF, HALF,   // top-right
                0, 0, 1);

        // North face (-Z), face index 4 (dice face 5)
        // CCW from -Z: top-left → bottom-left → bottom-right → top-right
        drawFace(pose, consumer, packedLight, 4,
                 HALF,  HALF, -HALF,   // top-left
                 HALF, -HALF, -HALF,   // bottom-left
                -HALF, -HALF, -HALF,   // bottom-right
                -HALF,  HALF, -HALF,   // top-right
                0, 0, -1);

        // East face (+X), face index 2 (dice face 3)
        // CCW from +X: top-left → bottom-left → bottom-right → top-right
        drawFace(pose, consumer, packedLight, 2,
                 HALF,  HALF,  HALF,   // top-left
                 HALF, -HALF,  HALF,   // bottom-left
                 HALF, -HALF, -HALF,   // bottom-right
                 HALF,  HALF, -HALF,   // top-right
                1, 0, 0);

        // West face (-X), face index 3 (dice face 4)
        // CCW from -X: top-left → bottom-left → bottom-right → top-right
        drawFace(pose, consumer, packedLight, 3,
                -HALF,  HALF, -HALF,   // top-left
                -HALF, -HALF, -HALF,   // bottom-left
                -HALF, -HALF,  HALF,   // bottom-right
                -HALF,  HALF,  HALF,   // top-right
                -1, 0, 0);

        // Top face (+Y), face index 0 (dice face 1)
        // CCW from +Y: top-left(back) → bottom-left(back) → bottom-right(front) → top-right(front)
        drawFace(pose, consumer, packedLight, 0,
                -HALF, HALF, -HALF,   // top-left (back-left when viewed from +Y)
                -HALF, HALF,  HALF,   // bottom-left (front-left)
                 HALF, HALF,  HALF,   // bottom-right (front-right)
                 HALF, HALF, -HALF,   // top-right (back-right)
                0, 1, 0);

        // Bottom face (-Y), face index 5 (dice face 6)
        // CCW from -Y: top-left(front) → bottom-left(front) → bottom-right(back) → top-right(back)
        drawFace(pose, consumer, packedLight, 5,
                -HALF, -HALF,  HALF,   // top-left (front-left when viewed from -Y)
                -HALF, -HALF, -HALF,   // bottom-left (back-left)
                 HALF, -HALF, -HALF,   // bottom-right (back-right)
                 HALF, -HALF,  HALF,   // top-right (front-right)
                0, -1, 0);
    }

    /**
     * Draws a single face of the cube using a specific face texture region.
     * Vertices must be in CCW order when viewed from outside the face.
     *
     * UV mapping (CCW):
     * - Vertex 1 (top-left): (0, vMin)
     * - Vertex 2 (bottom-left): (0, vMax)
     * - Vertex 3 (bottom-right): (1, vMax)
     * - Vertex 4 (top-right): (1, vMin)
     *
     * @param faceIndex 0-5, which 1/6 slice of the texture to use
     */
    private static void drawFace(PoseStack.Pose pose, VertexConsumer consumer, int packedLight,
                                  int faceIndex,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float x4, float y4, float z4,
                                  float nx, float ny, float nz) {
        float vMin = faceIndex * FACE_HEIGHT;
        float vMax = (faceIndex + 1) * FACE_HEIGHT;

        consumer.addVertex(pose, x1, y1, z1)
                .setColor(255, 255, 255, 255)
                .setUv(0.0F, vMin)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x2, y2, z2)
                .setColor(255, 255, 255, 255)
                .setUv(0.0F, vMax)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x3, y3, z3)
                .setColor(255, 255, 255, 255)
                .setUv(1.0F, vMax)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x4, y4, z4)
                .setColor(255, 255, 255, 255)
                .setUv(1.0F, vMin)
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