package com.acat.compat;

import com.acat.item.HolyGoldCrownItem;
import com.acat.registry.AcatItems;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios 集成（服务端 / 通用侧）。
 * ─────────────────────────────────────────────────────────
 * 模仿 Goety 的做法：在 RegisterCapabilitiesEvent（mod 事件总线）中调用
 * CuriosApi.registerCurio，把圣金王冠的物品能力(ICurioItem)显式注册给 Curios。
 *
 * 注意：本类引用了 Curios API 类，只能在检测到 curios 已加载后由
 * AcatMod 挂接（通过 isLoaded 守卫避免类加载崩溃）。
 */
public class AcatCuriosCompat {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        HolyGoldCrownItem crown = AcatItems.HOLY_GOLD_CROWN.get();
        CuriosApi.registerCurio(crown, crown);
    }
}
