package com.acat.client.model;

import com.acat.AcatMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 猫头模型 —— 由用户提供的 Blockbench 导出(cathead.java)改编：
 * 仅一个 head 部件(10x8x10 主体 + 嘴筒 + 双耳)，UV 按 64x64；
 * r.png/b.png/y.png 三张贴图复用同一套 UV，变体只换贴图不换模型。
 * 整体朝向(yaw)由 MobRenderer 按实体 yRot 旋转；这里把实体插值后的
 * headPitch(俯仰)/netHeadYaw(偏航差)转到 head 部件上，使猫头会真的
 * “低头/抬头”看向目标(修掉俯冲时僵直平视的问题)。
 */
public class CatHeadModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "cathead"), "main");
    private static final float DEG_TO_RAD = ((float) Math.PI / 180.0F);
    private final ModelPart head;

    public CatHeadModel(ModelPart root) {
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 18).addBox(-3.0F, -0.04F, -8.0F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(20, 18).addBox(-4.0F, -6.0F, 0.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(20, 24).addBox(2.0F, -6.0F, 0.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 20.0F, 1.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 头部绕自身轴心转动：xRot=俯仰(正=低头，与实体 xRot 约定一致)、yRot=偏航差
        this.head.xRot = headPitch * DEG_TO_RAD;
        this.head.yRot = netHeadYaw * DEG_TO_RAD;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
