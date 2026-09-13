package com.acat.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 圣金王冠 3D 模型（Curios 头饰渲染用）。
 * ─────────────────────────────────────────────────────────
 * 几何数据 1:1 取自 Blockbench 导出的「圣金王冠模型.java」（64×64 贴图）：
 *   方块 texOffs(0,0)：addBox(-4.5, -12.0, -4.5, 9, 7, 9, +0.1)
 * 适配 Curios 的方式（与 Goety DarkHatModel 相同套路）：
 *   必须继承 HumanoidModel<LivingEntity>，并把头饰几何挂在 head 部件下，
 *   覆写 renderToBuffer 只渲染 head（避免把整个人体模型再画一遍）。
 *
 * 注意：原始实体模型挂在根部的 Head 部件偏移为 (0,0,0)，与 vanilla 玩家模型
 * head 部件（枢轴 y=0，头方块 -4,-8,-4..4,0,4）坐标系一致，
 * 因此方块坐标原样保留即可，无需任何偏移调整。
 */
public class HolyGoldCrownModel extends HumanoidModel<LivingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("aaacat", "holy_gold_crown"), "main");

    public HolyGoldCrownModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        // HumanoidModel 构造函数会 getChild 所有标准部件(head/hat/body/四肢)，
        // 因此必须用完整的人形骨架打底；再用空 head 替换掉默认头方块，只保留王冠。
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        // ← Blockbench 原始坐标（用户已调好），UV 与 64×64 贴图严格对应，勿改
        head.addOrReplaceChild("crown",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.5F, -12.0F, -4.5F, 9.0F, 7.0F, 9.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay,
                               int color) {
        // 只渲染 head（含其下王冠方块），不渲染身体/四肢/默认头
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
