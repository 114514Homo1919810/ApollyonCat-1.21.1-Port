package com.acat.event;

import com.acat.AcatMod;
import com.acat.registry.AcatEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 亚小猫的祝福 配套事件：
 *  1. 出伤 +10%：任意来源（近战\/远程\/魔法）对敌人造成伤害时，若伤害发起者是带祝福的玩家 → 放大 1.1 倍；
 *  2. 死亡触发一次“不死图腾”：玩家带着祝福死亡 → 免死(仿原版图腾：回 1 血 + 清空效果 +
 *     再生\/吸收\/防火)，同时移除祝福；此后不再触发。
 *
 * NeoForge 1.21 起：原 Forge 的 LivingHurtEvent 由 LivingDamageEvent.Pre 取代
 * （护甲\/药水结算之后、真正扣血之前），字段改名 getNewDamage\/setNewDamage。
 */
@EventBusSubscriber(modid = AcatMod.MOD_ID)
public class ApollyonCatBlessingEvents {

    /** +10% 伤害（事件发生在目标身上，攻击方 = source.getEntity()） */
    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Player player
                && player != victim
                && player.hasEffect(AcatEffects.APOLLYON_CAT_BLESSING)) {
            event.setNewDamage(event.getNewDamage() * 1.1F);
        }
    }

    /** 死亡免死一次：仿不死图腾，随后祝福消失 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dying = event.getEntity();
        if (dying.level().isClientSide || !(dying instanceof Player player)) {
            return;
        }
        if (!player.hasEffect(AcatEffects.APOLLYON_CAT_BLESSING)) {
            return;
        }
        event.setCanceled(true);   // 免死
        player.removeEffect(AcatEffects.APOLLYON_CAT_BLESSING);   // 祝福消耗，仅此一次
        player.removeAllEffects();  // 仿原版图腾：清空全部效果
        player.setHealth(1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        // 客户端播放图腾使用动画 + 音效
        player.level().broadcastEntityEvent(player, (byte) 35);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
