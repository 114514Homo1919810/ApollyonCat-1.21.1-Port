package com.acat.client.render;

import com.acat.AcatMod;
import com.acat.client.model.ApollyonCatModel;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 亚小猫光环渲染层（自绘贴图 cat_hole.png）
 * ───────────────────────────────────────────
 * 参照 Goety Apostle 的光环：悬浮在头顶的 45° 倾斜圆环，绕自身法线自转，全亮发光。
 * 光环挂在猫头骨骼上，会跟随头部转动。
 * 数值单位是“模型单位”，渲染器整体 ×0.5 缩小后自动等比缩小。
 * 想调位置/大小：改下面三个常量即可。
 */
@OnlyIn(Dist.CLIENT)
public class ApollyonCatHaloLayer<T extends LivingEntity> extends RenderLayer<T, ApollyonCatModel<T>> {

    private static final ResourceLocation HALO_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "textures/entity/cat_hole.png");

    /** 光环悬浮高度（相对猫头枢纽，模型单位；负数=向上浮，头枢纽在 y≈6，头顶毛在 y≈-2） */
    private static final float HALO_Y = -0.5F;
    /** 光环前后偏移（正=略靠后，制造与 Apostle 一致的倾斜椭圆视角） */
    private static final float HALO_Z = 0.4F;
    /** 光环半边长（整个方块 2*HALF；光环图案约占贴图 75%，视觉直径≈1.5*HALF） */
    private static final float HALF_SIZE = 0.66F;
    /** 与 Apostle 一致：绕 X 轴倾斜 45°，呈现椭圆光环 */
    private static final float TILT_DEG = 45.0F;
    /** 自转速度（Apostle 为 ageInTicks*0.01，这里模型小，稍微快一点） */
    private static final float SPIN_SPEED = 0.08F;

    public ApollyonCatHaloLayer(RenderLayerParent<T, ApollyonCatModel<T>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        // 1. 定位到猫头骨骼（含头部转动）
        this.getParentModel().head().translateAndRotate(poseStack);
        // 2. 抬高到头顶上方
        poseStack.translate(0.0F, HALO_Y, HALO_Z);
        // 3. 参照 Apostle：绕 X 轴倾斜 45°
        poseStack.mulPose(Axis.XP.rotationDegrees(TILT_DEG));
        // 4. 自转
        poseStack.mulPose(Axis.ZP.rotation(ageInTicks * SPIN_SPEED));

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(HALO_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        addVertex(vertexConsumer, pose, -HALF_SIZE, -HALF_SIZE, 0.0F, 0.0F, 0.0F, light);
        addVertex(vertexConsumer, pose, HALF_SIZE, -HALF_SIZE, 0.0F, 1.0F, 0.0F, light);
        addVertex(vertexConsumer, pose, HALF_SIZE, HALF_SIZE, 0.0F, 1.0F, 1.0F, light);
        addVertex(vertexConsumer, pose, -HALF_SIZE, HALF_SIZE, 0.0F, 0.0F, 1.0F, light);

        addVertex(vertexConsumer, pose, -HALF_SIZE, HALF_SIZE, 0.0F, 0.0F, 1.0F, light);
        addVertex(vertexConsumer, pose, HALF_SIZE, HALF_SIZE, 0.0F, 1.0F, 1.0F, light);
        addVertex(vertexConsumer, pose, HALF_SIZE, -HALF_SIZE, 0.0F, 1.0F, 0.0F, light);
        addVertex(vertexConsumer, pose, -HALF_SIZE, -HALF_SIZE, 0.0F, 0.0F, 0.0F, light);
        poseStack.popPose();
    }

    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                  float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
