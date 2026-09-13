package com.acat.client;

import com.Polarice3.Goety.client.render.WearRenderer;
import com.acat.AcatMod;
import com.acat.client.model.CatHeadModel;
import com.acat.client.model.HolyGoldCrownModel;
import com.acat.client.render.BeamAnchorRenderer;
import com.acat.client.render.CatHeadRenderer;
import com.acat.client.render.ApollyonCatDomainRingRenderer;
import com.acat.client.render.ApollyonCatRenderer;
import com.acat.registry.AcatEntities;
import com.acat.registry.AcatItems;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * 客户端初始化：
 *  - 注册 aaacat 全部实体渲染器（亚小猫 / 仆从版亚小猫 / 光束锚点隐形 / 领域光环）；
 *  - 注册圣金王冠的 Curios 头饰模型分层 + 渲染器（复用 Goety WearRenderer，同黑暗帽子机制）。
 * 仆从版复用 Boss 版渲染器(同模型/同贴图/同光环层)。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = AcatMod.MOD_ID, value = Dist.CLIENT)
public class AcatClient {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AcatEntities.APOLLYON_CAT.get(), context -> new ApollyonCatRenderer<>(context));
        event.registerEntityRenderer(AcatEntities.APOLLYON_CAT_SERVANT.get(), context -> new ApollyonCatRenderer<>(context));
        event.registerEntityRenderer(AcatEntities.BEAM_ANCHOR.get(), BeamAnchorRenderer::new);
        event.registerEntityRenderer(AcatEntities.CAT_HEAD.get(), CatHeadRenderer::new);
        event.registerEntityRenderer(AcatEntities.DOMAIN_RING.get(), ApollyonCatDomainRingRenderer::new);
        AcatMod.LOGGER.info("aaacat: registered aaacat / aaacat_servant / beam_anchor / domain_ring renderers.");
    }

    /** 圣金王冠 3D 模型分层注册（几何来自 Blockbench 导出，坐标原样保留）。 */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HolyGoldCrownModel.LAYER_LOCATION, HolyGoldCrownModel::createBodyLayer);
        event.registerLayerDefinition(CatHeadModel.LAYER_LOCATION, CatHeadModel::createBodyLayer);
        AcatMod.LOGGER.info("aaacat: registered holy_gold_crown model layer.");
    }

    /** Curios 渲染器注册：戴上圣金王冠时在头顶绘制 3D 模型（与 Goety 黑暗帽子同机制）。 */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("curios")) {
            event.enqueueWork(() ->
                    CuriosRendererRegistry.register(AcatItems.HOLY_GOLD_CROWN.get(), () -> new WearRenderer(
                            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "textures/models/curios/holy_gold_crown.png"),
                            new HolyGoldCrownModel(Minecraft.getInstance().getEntityModels()
                                    .bakeLayer(HolyGoldCrownModel.LAYER_LOCATION)))));
            AcatMod.LOGGER.info("aaacat: registered curios renderer for holy_gold_crown.");
        }
    }
}
