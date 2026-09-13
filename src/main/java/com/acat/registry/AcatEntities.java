package com.acat.registry;

import com.acat.AcatMod;
import com.acat.entity.BeamAnchorEntity;
import com.acat.entity.ApollyonCatDomainRingEntity;
import com.acat.entity.ApollyonCatEntity;
import com.acat.entity.ApollyonCatHeadEntity;
import com.acat.entity.ApollyonCatServantEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 实体注册中心：
 *  aaacat:aaacat          —— 亚小猫 Boss
 *  aaacat:aaacat_servant  —— 亚小猫 · 仆从版（显示名同样为“亚小猫 / aaacat”，无 Boss 血条/BGM）
 *  aaacat:beam_anchor     —— 三阶段腐化光束锚点（隐形 Mob）
 *  aaacat:domain_ring     —— 三阶段领域光环（纯装饰）
 */
public class AcatEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AcatMod.MOD_ID);

    /**
     * 亚小猫（aaacat）：碰撞箱按猫大小，AI 完整继承 Goety 使徒。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<ApollyonCatEntity>> APOLLYON_CAT =
            ENTITIES.register("aaacat",
                    () -> EntityType.Builder.<ApollyonCatEntity>of(ApollyonCatEntity::new, MobCategory.MONSTER)
                            .fireImmune()
                            .sized(0.6F, 0.7F)       // 原版猫尺寸
                            .clientTrackingRange(8)
                            .build("aaacat")
            );

    /**
     * 亚小猫 · 仆从版（aaacat_servant）：属性/技能/Boss 阶段全部同 Boss 版，
     * 区别见 ApollyonCatServantEntity：无 Boss 血条/BGM、Goety 仆从(IOwned)行为、主动哈气每 3 秒 +1。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<ApollyonCatServantEntity>> APOLLYON_CAT_SERVANT =
            ENTITIES.register("aaacat_servant",
                    () -> EntityType.Builder.<ApollyonCatServantEntity>of(ApollyonCatServantEntity::new, MobCategory.CREATURE)
                            .fireImmune()
                            .sized(0.6F, 0.7F)       // 原版猫尺寸
                            .clientTrackingRange(8)
                            .build("aaacat_servant")
            );

    /**
     * 腐化光束锚点：隐形 Mob，作为 CorruptedBeam 的 owner（updateInterval=1 保证旋转逐 tick 同步到客户端）
     */
    public static final DeferredHolder<EntityType<?>, EntityType<BeamAnchorEntity>> BEAM_ANCHOR =
            ENTITIES.register("beam_anchor",
                    () -> EntityType.Builder.<BeamAnchorEntity>of(BeamAnchorEntity::new, MobCategory.MISC)
                            .fireImmune()
                            .sized(0.1F, 0.1F)
                            .eyeHeight(0.0F)          // 1.21 起眼高由 EntityType 定义（原 getStandingEyeHeight 覆写）
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("beam_anchor")
            );

    /**
     * 领域光环：纯装饰实体，客户端渲染 r16/r20 双环 + 环绕长条
     */
    /**
     * 亚小猫二阶段攻击召唤物 猫头魂体(cat_head)：单实体 + variant(0红\/1蓝\/2黄)区分三种元素，
     * 追踪施法者锁定目标飞行，命中\/未命中就地铺对应十字火(狱火\/冰之火\/终末火)。
     */
    public static final DeferredHolder<EntityType<?>, EntityType<ApollyonCatHeadEntity>> CAT_HEAD =
            ENTITIES.register("cat_head",
                    () -> EntityType.Builder.<ApollyonCatHeadEntity>of(ApollyonCatHeadEntity::new, MobCategory.MISC)
                            .fireImmune()
                            .sized(0.5F, 0.5F)       // 猫头魂体大小
                            .clientTrackingRange(10)
                            .updateInterval(1)        // 高速飞行，逐 tick 同步位置
                            .build("cat_head")
            );

    public static final DeferredHolder<EntityType<?>, EntityType<ApollyonCatDomainRingEntity>> DOMAIN_RING =
            ENTITIES.register("domain_ring",
                    () -> EntityType.Builder.<ApollyonCatDomainRingEntity>of(ApollyonCatDomainRingEntity::new, MobCategory.MISC)
                            .fireImmune()
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                            .build("domain_ring")
            );
}
