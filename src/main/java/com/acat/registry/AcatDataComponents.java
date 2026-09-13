package com.acat.registry;

import com.acat.AcatMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * aaacat 自定义数据组件注册。
 * ─────────────────────────────────────────────────────────
 * 1.20.1 用物品根 NBT（ItemStack#getTag/getOrCreateTag）承载的数据，
 * 1.21 起改为「数据组件」。圣金王冠的等级单独做成一个组件，
 * 这样 Goety 仪式配方可以用 neoforge:components 精确匹配「等级」而
 * 不受同一物品其它数据（例如护卫名单）影响 —— 等价于旧版 forge:partial_nbt 的语义。
 *
 *  apollyon_crown_level —— 圣金王冠等级 1~6（缺省 0 = 未定级）
 */
public class AcatDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AcatMod.MOD_ID);

    /** 圣金王冠等级（1~6）。旧版为物品 NBT 的 int「level」。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CROWN_LEVEL =
            DATA_COMPONENTS.register("crown_level", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
}
