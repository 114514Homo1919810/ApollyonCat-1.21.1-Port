package com.acat.registry;

import com.acat.item.HolyGoldCrownItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * aaacat 物品的默认数据组件补丁（注册期，mod 事件总线）。
 * ─────────────────────────────────────────────────────────
 * 1.21 起「防火」不再是 {@code Item#isFireResistant()} 覆写，而是物品默认组件
 * {@code minecraft:fire_resistant}。圣金王冠必须继承 Goety 的 MagicHatItem
 * （施法时间 -50% 底座），而它只有无参构造拿不到 Item.Properties，
 * 所以这里用 ModifyDefaultComponentsEvent 给王冠补上该组件——
 * 等价旧版 isFireResistant()：掉落物在火焰/岩浆里不燃烧、不被烧毁。
 */
public class AcatItemDefaults {

    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modifyMatching(item -> item instanceof HolyGoldCrownItem,
                builder -> builder.set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE));
    }
}
