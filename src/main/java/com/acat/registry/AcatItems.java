package com.acat.registry;

import com.Polarice3.Goety.common.items.ServantSpawnEggItem;
import com.acat.AcatMod;
import com.acat.item.HolyGoldCrownItem;
import com.acat.item.HolySoulArkItem;
import com.acat.item.PureCatRingItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品 / 创造模式栏注册。
 * 红色刷怪蛋：aaacat:aaacat_spawn_egg（Boss）
 * 银色刷怪蛋：aaacat:aaacat_servant_spawn_egg（仆从版，潜行右键=绑定主人）
 * 至纯喵环：aaacat:pure_cat_ring（Boss 掉落 / 仆从召唤仪式核心，贴图复用猫头顶光环 cat_hole.png）
 * 圣金魂匣：aaacat:holy_soul_ark（便携版灵魂方舟：背包任意位置生效，潜行右键绑定复活归位点，
 *           终身绑定持有者，防火/防爆/防仙人掌）
 * 圣金王冠：aaacat:holy_gold_crown（Curios 头饰，头戴 3D 模型，锻造仪式合成/升级，
 *           等级写数据组件「aaacat:crown_level」1~6，穿戴后提供全法术减耗 + 仆从增益等，详见 HolyGoldCrownEvents）
 */
public class AcatItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AcatMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AcatMod.MOD_ID);

    /** 红色刷怪蛋（外壳红 / 斑点深红）—— Boss 版 */
    public static final DeferredHolder<Item, DeferredSpawnEggItem> APOLLYON_CAT_SPAWN_EGG =
            ITEMS.register("aaacat_spawn_egg",
                    () -> new DeferredSpawnEggItem(() -> AcatEntities.APOLLYON_CAT.get(),
                            0xB02E26, 0x7A1212, new Item.Properties()));

    /**
     * 银色刷怪蛋（外壳银灰 / 斑点亮灰）—— 仆从版。
     * 复用 Goety ServantSpawnEggItem：潜行右键使用会把它绑定为你的仆从(IOwned.setTrueOwner)；
     * 不潜行则生成无主(野生)版。
     */
    public static final DeferredHolder<Item, ServantSpawnEggItem> APOLLYON_CAT_SERVANT_SPAWN_EGG =
            ITEMS.register("aaacat_servant_spawn_egg",
                    () -> new ServantSpawnEggItem(AcatEntities.APOLLYON_CAT_SERVANT,
                            0x9C9C9C, 0xE6E6E6, new Item.Properties()));

    /** 至纯喵环：亚小猫(Boss/仆从)掉落物 + 仆从召唤仪式核心(激活物)。名称红色，单件不可堆叠。 */
    public static final DeferredHolder<Item, PureCatRingItem> PURE_CAT_RING =
            ITEMS.register("pure_cat_ring",
                    () -> new PureCatRingItem(new Item.Properties().stacksTo(1).fireResistant()));

    /**
     * 圣金魂匣：便携版灵魂方舟。单件不可堆叠；潜行右键绑定坐标；放入背包任意位置生效；
     * fireResistant() → 掉落物不燃(免疫火焰/岩浆)；配合 canBeHurtBy 覆写免疫爆炸/仙人掌。
     */
    public static final DeferredHolder<Item, HolySoulArkItem> HOLY_SOUL_ARK =
            ITEMS.register("holy_soul_ark",
                    () -> new HolySoulArkItem(new Item.Properties().stacksTo(1).fireResistant()));

    /**
     * 圣金王冠：Curios 头饰（head 槽），右键可佩戴，戴上后渲染 3D 模型于头顶。
     * 继承 Goety MagicHatItem（全法术施法时间 -50% 底座），等级存数据组件「aaacat:crown_level」(1~6)。
     * 构造走无参（MagicHatItem → SingleStackItem 默认 stacksTo(1)）。
     */
    public static final DeferredHolder<Item, HolyGoldCrownItem> HOLY_GOLD_CROWN =
            ITEMS.register("holy_gold_crown", HolyGoldCrownItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("aaacat", () -> CreativeModeTab.builder()
                    .icon(() -> APOLLYON_CAT_SPAWN_EGG.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.aaacat"))
                    .displayItems((parameters, output) -> {
                        output.accept(APOLLYON_CAT_SPAWN_EGG.get());
                        output.accept(APOLLYON_CAT_SERVANT_SPAWN_EGG.get());
                        output.accept(PURE_CAT_RING.get());
                        output.accept(HOLY_SOUL_ARK.get());
                        output.accept(HOLY_GOLD_CROWN.get());
                    })
                    .build());
}
