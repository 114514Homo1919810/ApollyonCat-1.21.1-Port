package com.acat.util;

import com.acat.config.AcatConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 亚小猫「天敌猎杀」公用逻辑：
 *  仆从本体 / 亚小猫的祝福(玩家) 对 config/aaacat.toml -> killAura.targetEntityIds 列表中的天敌
 *  自动造成 21 亿（≈上限）虚空伤害，
 *  击杀归属由传入的 creditedKiller 决定（仆从 = 仆从击杀；祝福 = 玩家击杀）。
 *
 * 默认天敌 id（可在配置文件中增删改）：
 *  - minecraft:phantom          原版幻翼
 *  - minecraft:creeper          原版苦力怕
 *  - alexsmobs:crimson_mosquito 深渊蚊
 *  - alexsmobs:seagull          海鸥
 *  - iceandfire:if_pixie        仙子
 */
public final class ApollyonCatKillAura {

    /** 21 亿 ≈ 一个“上限数”的虚空伤害（任何生物直接死亡） */
    public static final float VOID_DAMAGE = 2_100_000_000.0F;

    private ApollyonCatKillAura() {
    }

    public static boolean isKillAuraTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()) {
            return false;
        }
        // 1.21 起实体类型注册表查询走 BuiltInRegistries（原 ForgeRegistries.ENTITY_TYPES）。
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (key == null) {
            return false;
        }
        // 与配置中的注册名字符串逐条比对（列表为空 = 不秒杀任何生物）
        for (String id : AcatConfig.KILL_AURA_IDS.get()) {
            if (id != null && key.toString().equals(id.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 以 center 为中心、radius 为半径，秒杀全部天敌。
     *
     * @param center         扫描中心（仆从 / 玩家）
     * @param creditedKiller 击杀归属实体（null = 无归属，仅虚空伤害）
     */
    public static void killTargetsInRange(Level level, Entity center, @Nullable LivingEntity creditedKiller, int radius) {
        if (level.isClientSide || center == null) {
            return;
        }
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(radius), ApollyonCatKillAura::isKillAuraTarget);
        if (targets.isEmpty()) {
            return;
        }
        double radiusSqr = (double) radius * (double) radius;
        for (LivingEntity target : targets) {
            if (target == center || target == creditedKiller) {
                continue;
            }
            if (target.distanceToSqr(center) > radiusSqr) {
                continue;
            }
            target.hurt(voidDamage(level, creditedKiller), VOID_DAMAGE);
        }
    }

    /** 虚空(out_of_the_world)伤害源；带上 killer 时击杀记在 killer 头上 */
    private static DamageSource voidDamage(Level level, @Nullable LivingEntity killer) {
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.FELL_OUT_OF_WORLD);
        if (killer != null) {
            return new DamageSource(holder, killer, killer);
        }
        return new DamageSource(holder, null, null);
    }
}
