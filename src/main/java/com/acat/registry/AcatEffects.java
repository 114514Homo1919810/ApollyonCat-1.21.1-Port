package com.acat.registry;

import com.acat.AcatMod;
import com.acat.effect.ApollyonCatBlessingEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 药水效果注册：
 *  apollyon_cat_blessing —— 亚小猫的祝福（喂食仆从获得，30 分钟）
 */
public class AcatEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, AcatMod.MOD_ID);

    /** 亚小猫的祝福：+10% 移速\/伤害、天敌猎杀、死亡触发一次不死图腾 */
    public static final DeferredHolder<MobEffect, ApollyonCatBlessingEffect> APOLLYON_CAT_BLESSING =
            EFFECTS.register("apollyon_cat_blessing", ApollyonCatBlessingEffect::new);
}
