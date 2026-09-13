package com.acat.client.render;

import com.acat.AcatMod;
import com.acat.entity.BeamAnchorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 光束锚点渲染器：什么都不画（锚点不可见，只有它发出的腐化光束可见）
 */
@OnlyIn(Dist.CLIENT)
public class BeamAnchorRenderer extends EntityRenderer<BeamAnchorEntity> {

    private static final ResourceLocation DUMMY =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "textures/entity/beam_anchor.png");

    public BeamAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(BeamAnchorEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // 不可见：不渲染任何东西
    }

    @Override
    public ResourceLocation getTextureLocation(BeamAnchorEntity entity) {
        return DUMMY;
    }
}
