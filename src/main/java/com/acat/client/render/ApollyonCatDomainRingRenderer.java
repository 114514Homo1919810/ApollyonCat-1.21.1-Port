package com.acat.client.render;

import com.acat.AcatMod;
import com.acat.entity.ApollyonCatDomainRingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 三阶段领域光环渲染器
 * ─────────────────────────────────────────────
 * 在领域中心(地面)绘制：
 *  - 半径 16 / 20 两条圆形轮廓（细亮线，参考炼狱魔典圆形火焰的地面圈）
 *  - 两圈各自环绕运动的"长条"（8 条/圈，地面发光长条绕圆心转）
 * 展开期(前 60 tick)半径从 0 生长到目标值；亚小猫死亡实体销毁后自动消失。
 * 数值都集中在常量里，方便手感微调。
 */
@OnlyIn(Dist.CLIENT)
public class ApollyonCatDomainRingRenderer extends EntityRenderer<ApollyonCatDomainRingEntity> {

    private static final ResourceLocation BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "textures/entity/domain_bar.png");

    private static final double RING_16 = 16.0D;
    private static final double RING_20 = 20.0D;
    private static final int BARS_PER_RING = 8;                 // 每圈长条数
    private static final double BAR_HALF_ANGLE_DEG = 10.0D;     // 单根长条的半角宽度(度)
    private static final double BAR_WIDTH = 0.7D;               // 长条径向宽度(格)
    private static final double BAR_Y = 0.32D;                  // 长条离地高度
    private static final double CIRCLE_WIDTH = 0.16D;           // 轮廓线径向宽度
    private static final double CIRCLE_Y = 0.10D;               // 轮廓离地高度
    private static final double BAR_SPEED = 0.045D;             // 长条转速 rad/tick(≈51°/s)
    private static final double CIRCLE_SEG_DEG = 3.0D;          // 轮廓分段

    public ApollyonCatDomainRingRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ApollyonCatDomainRingEntity ring, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (ring.isInvisible()) {
            return;
        }
        float growth = ring.growth(partialTick);
        double r16 = RING_16 * growth;
        double r20 = RING_20 * growth;
        float age = ring.tickCount + partialTick;
        float pulse = 0.55F + 0.25F * (float) Math.sin(age * 0.12D);   // 呼吸感

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(BAR_TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        // —— 轮廓线 ——
        drawCircleOutline(consumer, pose, r16, CIRCLE_Y, CIRCLE_WIDTH,
                1.0F, 0.55F, 0.12F, 0.50F * pulse);
        drawCircleOutline(consumer, pose, r20, CIRCLE_Y, CIRCLE_WIDTH,
                1.0F, 0.28F, 0.22F, 0.45F * pulse);

        // —— 环绕长条 ——
        double rot = age * BAR_SPEED;
        drawBars(consumer, pose, r16, BAR_Y, rot, 1.0F, 0.60F, 0.15F, 0.95F * pulse);
        drawBars(consumer, pose, r20, BAR_Y, rot * 1.0D + 0.6D, 1.0F, 0.34F, 0.28F, 0.90F * pulse);
    }

    private void drawBars(VertexConsumer consumer, PoseStack.Pose pose,
                          double radius, double y, double rot, float r, float g, float b, float a) {
        double step = Math.PI * 2.0D / BARS_PER_RING;
        double halfArc = Math.toRadians(BAR_HALF_ANGLE_DEG);
        for (int i = 0; i < BARS_PER_RING; ++i) {
            double center = rot + i * step;
            double a0 = center - halfArc;
            double a1 = center + halfArc;
            // 4 个角点（t=切向 半长 = 半径*半角 取弧长近似；n=径向）
            double halfLen = radius * halfArc;
            double c0x = radius * Math.sin(a0), c0z = radius * Math.cos(a0);
            double c1x = radius * Math.sin(a1), c1z = radius * Math.cos(a1);
            double tx = c1x - c0x, tz = c1z - c0z;              // 切向总向量
            double len = Math.sqrt(tx * tx + tz * tz);
            if (len < 1.0E-4D) continue;
            double ux = tx / len, uz = tz / len;                // 单位切向
            double nxv = c0x / radius, nzv = c0z / radius;      // 单位径向(以 a0 处近似)
            double wx = nxv * BAR_WIDTH * 0.5D, wz = nzv * BAR_WIDTH * 0.5D;
            // 长条中线中点(取弧中点)：
            double midA = (a0 + a1) / 2.0D;
            double mx = radius * Math.sin(midA), mz = radius * Math.cos(midA);
            double hx = ux * halfLen, hz = uz * halfLen;
            double x1 = mx - hx + wx, z1 = mz - hz + wz;
            double x2 = mx + hx + wx, z2 = mz + hz + wz;
            double x3 = mx + hx - wx, z3 = mz + hz - wz;
            double x4 = mx - hx - wx, z4 = mz - hz - wz;
            addFlatQuad(consumer, pose, x1, y, z1, x2, y, z2, x3, y, z3, x4, y, z4,
                    r, g, b, a, 0.0F, 0.0F, 1.0F, 1.0F);
        }
    }

    private void drawCircleOutline(VertexConsumer consumer, PoseStack.Pose pose,
                                   double radius, double y, double width, float r, float g, float b, float a) {
        double seg = Math.toRadians(CIRCLE_SEG_DEG);
        int n = (int) Math.ceil(Math.PI * 2.0D / seg);
        double w2 = width / 2.0D;
        for (int i = 0; i < n; ++i) {
            double a0 = i * seg;
            double a1 = (i + 1) * seg;
            double x1 = (radius - w2) * Math.sin(a0), z1 = (radius - w2) * Math.cos(a0);
            double x2 = (radius + w2) * Math.sin(a0), z2 = (radius + w2) * Math.cos(a0);
            double x3 = (radius + w2) * Math.sin(a1), z3 = (radius + w2) * Math.cos(a1);
            double x4 = (radius - w2) * Math.sin(a1), z4 = (radius - w2) * Math.cos(a1);
            addFlatQuad(consumer, pose, x1, y, z1, x2, y, z2, x3, y, z3, x4, y, z4,
                    r, g, b, a, 0.0F, 0.0F, 1.0F, 1.0F);
        }
    }

    private static void addFlatQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                    double x1, double y1, double z1,
                                    double x2, double y2, double z2,
                                    double x3, double y3, double z3,
                                    double x4, double y4, double z4,
                                    float r, float g, float b, float a,
                                    float u0, float v0, float u1, float v1) {
        int light = LightTexture.FULL_BRIGHT;
        vertex(consumer, pose, x1, y1, z1, r, g, b, a, u0, v0, light);
        vertex(consumer, pose, x2, y2, z2, r, g, b, a, u1, v0, light);
        vertex(consumer, pose, x3, y3, z3, r, g, b, a, u1, v1, light);
        vertex(consumer, pose, x4, y4, z4, r, g, b, a, u0, v1, light);
        // 双面（地面俯视角度较平，补反面防止从另一侧看不到）
        vertex(consumer, pose, x4, y4, z4, r, g, b, a, u0, v1, light);
        vertex(consumer, pose, x3, y3, z3, r, g, b, a, u1, v1, light);
        vertex(consumer, pose, x2, y2, z2, r, g, b, a, u1, v0, light);
        vertex(consumer, pose, x1, y1, z1, r, g, b, a, u0, v0, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               double x, double y, double z, float r, float g, float b, float a,
                               float u, float v, int light) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(0)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ApollyonCatDomainRingEntity entity) {
        return BAR_TEXTURE;
    }
}
