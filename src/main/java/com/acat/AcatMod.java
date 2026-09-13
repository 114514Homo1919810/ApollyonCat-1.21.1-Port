package com.acat;

import com.Polarice3.Goety.api.ritual.RitualType;
import com.acat.compat.AcatCuriosCompat;
import com.acat.config.AcatConfig;
import com.acat.entity.BeamAnchorEntity;
import com.acat.entity.ApollyonCatHeadEntity;
import com.acat.entity.ApollyonCatEntity;
import com.acat.entity.ApollyonCatServantEntity;
import com.acat.network.AcatNetwork;
import com.acat.registry.AcatDataComponents;
import com.acat.registry.AcatEffects;
import com.acat.registry.AcatEntities;
import com.acat.registry.AcatItemDefaults;
import com.acat.registry.AcatItems;
import com.acat.ritual.ApollyonCatSabbathRitualType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * aaacat (亚小猫)
 * ─────────────────────────────────────────────────────────
 * 外观：原版猫模型 + 自绘贴图 a_cat.png + 自绘光环 cat_hole.png
 * 声音：原版猫（CAT_*）
 * 行为：基于 Goety 使徒 (Apostle) 改制的专属 Boss
 *
 * 运行依赖：Goety 3 (1.21.1 NeoForge)（mods 目录需同时放置 Curios API）
 *
 * 实体：
 *  - aaacat:aaacat          亚小猫 Boss（仪式召唤 / Boss 刷怪蛋）
 *  - aaacat:aaacat_servant  亚小猫 · 仆从版（仆从刷怪蛋，潜行右键绑定主人）
 *
 * 召唤(Boss)：
 *  - 原：使徒安息仪式 25% 替换（见 RitualCatSpawnEvents）
 *  - 新增独立配方 summon_apollyon_cat：安息仪式同款布置(apollyon_cat_ritual)，
 *    6 生鲑鱼 + 6 生鳕鱼(祭坛) + 1 下界合金锭(手持激活)，仅限下界 Y>127
 *
 * 配置：自动生成 config/aaacat.toml（COMMON，构造期即加载）
 *  —— Boss/仆从生命值、三阶段扣血、圣金魂匣、圣金王冠 1~6 级数值、天敌秒杀列表，详见 AcatConfig。
 */
@Mod(AcatMod.MOD_ID)
public class AcatMod {

    public static final String MOD_ID = "aaacat";
    public static final Logger LOGGER = LogManager.getLogger();

    public AcatMod(IEventBus modBus, ModContainer modContainer) {
        // ★ 注册配置文件（文件名与 mod id 一致：config/aaacat.toml；COMMON 构造期即加载）
        modContainer.registerConfig(ModConfig.Type.COMMON, AcatConfig.SPEC, "aaacat.toml");

        // ★ 网络通道（C2S：王冠护卫选取 / 魂匣空气解绑），客户端与服务端两侧共用同一注册顺序
        AcatNetwork.init(modBus);

        AcatEntities.ENTITIES.register(modBus);
        AcatItems.ITEMS.register(modBus);
        AcatItems.TABS.register(modBus);
        AcatEffects.EFFECTS.register(modBus);
        AcatDataComponents.DATA_COMPONENTS.register(modBus);
        // ★ 注册期给圣金王冠补默认「防火」组件（1.21 起 Item#isFireResistant() 已删除）
        modBus.addListener(AcatItemDefaults::onModifyDefaultComponents);
        modBus.addListener(this::onEntityAttributeCreation);

        // ★ 注册亚小猫专属召唤仪式类型(安息仪式同款)：门槛检查(下界 + Y>127 + sabbath 结构)在激活祭坛时触发
        RitualType.addRitualType(ApollyonCatSabbathRitualType.NAME, new ApollyonCatSabbathRitualType());

        // ★ Curios：goety 硬依赖 curios，正常必已加载；仍加守卫，避免未装 curios 时类加载崩溃
        if (ModList.get().isLoaded("curios")) {
            modBus.addListener(AcatCuriosCompat::onRegisterCapabilities);
            LOGGER.info("aaacat: curios integration registered (holy_gold_crown -> head slot).");
        }

        LOGGER.info("aaacat(亚小猫) mod loaded.");
    }

    @SubscribeEvent
    public void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        // 仆从版属性与 Boss 版一致(血量读取 config/aaacat.toml)
        event.put(AcatEntities.APOLLYON_CAT.get(), ApollyonCatEntity.createAttributes().build());
        event.put(AcatEntities.APOLLYON_CAT_SERVANT.get(), ApollyonCatServantEntity.createAttributes().build());
        event.put(AcatEntities.BEAM_ANCHOR.get(), BeamAnchorEntity.createAttributes().build());
        event.put(AcatEntities.CAT_HEAD.get(), ApollyonCatHeadEntity.createAttributes().build());
        LOGGER.info("aaacat: attributes registered for aaacat & aaacat_servant & beam_anchor & cat_head.");
    }
}
