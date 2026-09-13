package com.acat.client.render;

import com.Polarice3.Goety.client.render.visual.TrailRenderer;
import com.Polarice3.Goety.utils.TrailEffect;
import com.acat.AcatMod;
import com.acat.client.model.CatHeadModel;
import com.acat.entity.ApollyonCatHeadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 猫头魂体渲染器 —— 与 Goety 狱魂(DamnedRenderer)同款的 MobRenderer 管线。
 * 朝向交给引擎标准生物约定 + CatHeadModel.setupAnim 转 pitch/yaw，不再手工转角度。
 *
 * 增强：
 *  - 整体放缩 0.625(比之前 1.25 缩小一半)；
 *  - 俯冲时画一条半透明拖尾(同 Goety VoidShockBomb 的 TrailRenderer 飘带)，
 *    颜色按变体与轨迹线一致：红=金、蓝=淡蓝、黄=淡紫。
 */
@OnlyIn(Dist.CLIENT)
public class CatHeadRenderer extends MobRenderer<ApollyonCatHeadEntity, CatHeadModel<ApollyonCatHeadEntity>> {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "textures/entity/cathead_r.png"),
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "textures/entity/cathead_b.png"),
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "textures/entity/cathead_y.png")
    };
    /** 拖尾贴图：复用 Goety 自带 projectiles/trail.png(同虚空震荡弹) */
    private static final ResourceLocation TRAIL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("goety", "textures/entity/projectiles/trail.png");

    /** 变体 → 拖尾 RGB(0~1)：与轨迹线同色系 */
    private static final float[][] TRAIL_RGB = new float[][]{
            {1.0F, 0.6667F, 0.0F},    // 红=金(狱魂原色 0xFFAA00)
            {0.5294F, 0.8078F, 0.9216F}, // 蓝=淡蓝 0x87CEEB
            {0.8471F, 0.7490F, 0.8471F}  // 黄=淡紫 0xD8BFD8
    };

    public CatHeadRenderer(EntityRendererProvider.Context context) {
        super(context, new CatHeadModel<>(CatHeadModel.createBodyLayer().bakeRoot()), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ApollyonCatHeadEntity entity) {
        int variant = entity.getVariant();
        return TEXTURES[variant >= 0 && variant < TEXTURES.length ? variant : ApollyonCatHeadEntity.VARIANT_YELLOW];
    }

    @Override
    public void render(ApollyonCatHeadEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // —— 俯冲拖尾：半透明飘带(同 VoidShockBombRenderer) ——
        if (entity.isCharging()) {
            poseStack.pushPose();
            float x = (float) Mth.lerp(partialTicks, entity.xOld, entity.getX());
            float y = (float) Mth.lerp(partialTicks, entity.yOld, entity.getY());
            float z = (float) Mth.lerp(partialTicks, entity.zOld, entity.getZ());
            entity.trail.prepareRender(new Vec3(x, y + entity.getBbHeight() / 2.0D, z), partialTicks);
            poseStack.translate(-x, -y, -z);
            int variant = entity.getVariant();
            float[] rgb = TRAIL_RGB[variant >= 0 && variant < TRAIL_RGB.length ? variant : ApollyonCatHeadEntity.VARIANT_YELLOW];
            TrailRenderer.render(entity.trail,
                    buffer.getBuffer(RenderType.entityTranslucent(TRAIL_TEXTURE)),
                    poseStack, TrailEffect.TrailOffsetFunction.FACE_CAMERA, false,
                    rgb[0], rgb[1], rgb[2], 1.0F, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
    }

    /** 猫头整体放缩 0.625 = 原 1.25 的一半(视觉缩小一半) */
    @Override
    protected void scale(ApollyonCatHeadEntity entity, PoseStack poseStack, float partialTickTime) {
        poseStack.scale(0.625F, 0.625F, 0.625F);
    }

    /** 常亮(火焰/冰晶/魔焰质感)，不受环境光照影响 */
    @Override
    protected int getBlockLightLevel(ApollyonCatHeadEntity entity, BlockPos pos) {
        return 15;
    }
}
