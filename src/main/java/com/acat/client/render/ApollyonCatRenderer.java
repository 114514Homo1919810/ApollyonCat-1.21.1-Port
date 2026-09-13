package com.acat.client.render;

import com.acat.AcatMod;
import com.acat.client.model.ApollyonCatModel;
import com.acat.entity.IApollyonCat;
import net.minecraft.world.entity.Mob;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 亚小猫渲染器：Blockbench 自建猫模型 + 自绘贴图 a_cat.png + 自绘光环 cat_hole.png
 * 模型整体是原版猫的 2 倍大，这里以原点 (0,0,0) 为中心整体缩小 2 倍（×0.5）。
 */
@OnlyIn(Dist.CLIENT)
public class ApollyonCatRenderer<T extends Mob & IApollyonCat> extends MobRenderer<T, ApollyonCatModel<T>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "textures/entity/a_cat.png");

    /** 模型放大倍率（Blockbench 里是原版猫的 2 倍），以 0,0,0 为中心缩回 1/2 */
    private static final float MODEL_SCALE = 0.5F;

    public ApollyonCatRenderer(EntityRendererProvider.Context context) {
        super(context, new ApollyonCatModel<>(ApollyonCatModel.createBodyLayer().bakeRoot()), 0.4F);
        this.addLayer(new ApollyonCatHaloLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTicks) {
        super.scale(entity, poseStack, partialTicks);
        // 以 (0,0,0) 为缩放中心：不做任何位移，直接整体缩放 0.5
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}
