package com.acat.effect;

import com.acat.util.ApollyonCatKillAura;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.acat.AcatMod;

/**
 * 亚小猫的祝福（aaacat:apollyon_cat_blessing）—— 喂食亚小猫仆从后玩家获得（30 分钟）：
 *  1. +10% 移速（属性修饰符，由本效果自带；随效果移除自动消失）；
 *  2. +10% 伤害（出伤加成走 NeoForge LivingHurtEvent，见 ApollyonCatBlessingEvents，避免只加近战）；
 *  3. 每 1 秒自动秒杀身边 64 格内的 5 种天敌（幻翼\/苦力怕\/深渊蚊\/海鸥\/仙子），击杀算玩家；
 *  4. 玩家死亡时触发一次“不死图腾”效果并移除本祝福（见 ApollyonCatBlessingEvents）。
 */
public class ApollyonCatBlessingEffect extends MobEffect {

    /** 30 分钟 = 30 * 60 * 20 tick */
    public static final int DURATION = 30 * 60 * 20;

    /** 祝福天敌猎杀半径（与仆从本体一致） */
    public static final int KILL_AURA_RADIUS = 64;

    /** 1.21 起属性修饰符改用 ResourceLocation 标识（原 UUID 已移除）。 */
    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "blessing_speed");

    public ApollyonCatBlessingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF472B6);
        // +10% 移动速度（MULTIPLY_TOTAL）
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID,
                0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0 && duration % 20 == 0;   // 每秒结算一次猎杀
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !entity.isAlive()) {
            return false;
        }
        // 祝福天敌猎杀：以玩家为中心 64 格，击杀归属玩家
        ApollyonCatKillAura.killTargetsInRange(entity.level(), entity, entity, KILL_AURA_RADIUS);
        return true;
    }
}
