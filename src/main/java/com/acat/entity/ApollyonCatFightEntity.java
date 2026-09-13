package com.acat.entity;

/**
 * ApollyonCatFightEntity —— 亚小猫·仆从战斗基座（抽象类，不注册实体）
 * ─────────────────────────────────────────────
 * 继承链：ApollyonCatServantEntity → 本类 → Summoned(Owned) → PathfinderMob。
 * 仆从拥有与 Boss 版(ApollyonCatEntity) 等效、但各自独立维护的完整战斗实现：
 * 哈气/风筝/二阶段/三阶段领域/全部技能与 Boss 版行为一致；
 * 使徒(Apostle)被动底座(仪式/回血/卡墙传送/二阶段流星/灼烧/煮沸水/转化村民/禁飞/施法烟雾)
 * 在类内自建等价实现；仆从系统(主人/跟随/还击/索敌/指令/区块加载)由 Summoned(Owned) 提供。
 * 注意：Boss 与仆从刻意不共享战斗代码（行为分叉点多），改动技能需两侧同步。
 */

import com.Polarice3.Goety.common.entities.ally.MiniGhast;
import com.Polarice3.Goety.common.entities.ModEntityType;
import com.Polarice3.Goety.common.entities.boss.Apostle;
import com.Polarice3.Goety.common.entities.hostile.cultists.SpellCastingCultist;
import com.Polarice3.Goety.common.entities.hostile.servants.ObsidianMonolith;
import com.Polarice3.Goety.common.entities.neutral.AbstractObsidianMonolith;
import com.Polarice3.Goety.common.entities.projectiles.CorruptedBeam;
import com.Polarice3.Goety.common.entities.projectiles.HellBlast;
import com.Polarice3.Goety.common.entities.projectiles.HellCloud;
import com.Polarice3.Goety.common.entities.util.FireTornadoTrap;
import com.Polarice3.Goety.common.entities.util.VoidRift;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.common.network.ModNetwork;
import com.Polarice3.Goety.common.network.server.SThunderBoltPacket;
import com.Polarice3.Goety.client.particles.GatherTrailParticleOption;
import com.Polarice3.Goety.client.particles.ModParticleTypes;
import com.Polarice3.Goety.config.SpellConfig;
import com.Polarice3.Goety.common.magic.spells.geomancy.QuakingSpell;
import com.Polarice3.Goety.common.magic.spells.wind.RazorWindSpell;
import com.Polarice3.Goety.common.magic.spells.wind.WindBlastSpell;
import com.Polarice3.Goety.config.AttributesConfig;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.config.MobsConfig;
import com.Polarice3.Goety.utils.BlockFinder;
import com.Polarice3.Goety.utils.MathHelper;
import com.Polarice3.Goety.utils.MiscCapHelper;
import com.Polarice3.Goety.utils.ColorUtil;
import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.Goety.utils.ModDamageSource;
import com.Polarice3.Goety.utils.ServerParticleUtil;
import com.Polarice3.Goety.utils.SpellExplosion;
import com.Polarice3.Goety.utils.WandUtil;
import com.acat.AcatMod;
import com.acat.config.AcatConfig;
import com.acat.registry.AcatEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import java.util.EnumSet;
import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.Polarice3.Goety.common.entities.projectiles.FireTornado;
import com.Polarice3.Goety.common.entities.projectiles.IceBouquet;
import com.Polarice3.Goety.common.entities.projectiles.SteamMissile;
import com.Polarice3.Goety.common.entities.projectiles.VoidShockBomb;
import com.Polarice3.Goety.common.entities.util.FireBlastTrap;
import com.Polarice3.Goety.common.items.ModItems;
import com.Polarice3.Goety.common.items.UnholyBloodItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.client.particles.*;
import com.Polarice3.Goety.common.effects.GoetyEffects;
import com.Polarice3.Goety.common.entities.hostile.cultists.Cultist;
import com.Polarice3.Goety.common.entities.projectiles.*;
import com.Polarice3.Goety.common.entities.util.*;
import com.Polarice3.Goety.common.network.server.SApostleSmitePacket;
import com.Polarice3.Goety.config.MainConfig;
import com.Polarice3.Goety.utils.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3f;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import com.Polarice3.Goety.common.entities.ally.Summoned;

public abstract class ApollyonCatFightEntity extends Summoned implements IApollyonCat {


    /* ==================== 常量 ==================== */
    // 仆从最大生命：实际读取 config/aaacat.toml -> entities.servantMaxHealth(默认 333)，见 createAttributes() 与 setConfigurableAttributes()
    /** 猫头高度：整套使徒施法(烟雾/弹道/光束)都按 3 格高使徒写死 1.62 眼高，猫只有 0.7 高，必须压回猫头 */
    public static final float CAT_EYE_HEIGHT = 0.55F;
    private static final double HISS_SQUARED_RANGE = 2.5D * 2.5D;  // 哈气半径 2.5 格(平方)
    private static final int HISS_REUSE_GAP = 10;                   // 哈气/触发技能内置冷却 0.5 秒
    private static final int CORRUPT_RISE_TICKS = 20;               // 浮空 9 格耗时
    private static final int CORRUPT_BEAM_TICKS = 120;              // 6 秒腐化光束
    private static final int CAST_WARMUP = 10;
    /* 撕裂(虚空裂隙)：自管生命周期，手动控制"基础 3 点 + 结束爆发 20 点" */
    private static final int RUPTURE_WARMUP_TICKS = 40;             // 裂隙展开耗时 2 秒
    private static final int RUPTURE_ACTIVE_TICKS = 220;            // 裂隙撕扯 11 秒
    private static final int FLEE_TICKS = 25;                        // 哈气/腐化结束后的强制远离时长 1.25 秒

    /* ==================== 技能伤害：config/aaacat.toml -> [skillDamage]（Boss/仆从共用） ====================
     * 换算依据 Goety 2.5.57.3 实际结算公式（已反编译核对）：
     *  - VoidRift 每tick            = 主人攻伤/2 + extra
     *  - SteamMissile/FireBlastTrap = 主人攻伤 + extra
     *  - CorruptedBeam              = SpellConfig.CorruptedBeamDamage×damageMultiply + extra
     *  - RazorWind                  = SpellConfig.RazorWindDamage×damageMultiply + potency
     *  - FireTornado                = 4.0 + setDamage
     *  - HellBlast                  = setDamage + extra（owner 非玩家时）
     * 这里把配置里的「最终伤害」反推回我们唯一能控制的那一项，保证无加成时实际伤害=配置值。 */
    /** 撕裂裂隙 每tick最终伤害 -> extra（Goety: 每tick = 主人攻伤/2 + extra）。 */
    private float ruptureTickExtra() {
        float atk = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return (float) (AcatConfig.SKILL_RUPTURE_TICK.get() - atk / 2.0D);
    }

    /** 「攻伤 + extra」型弹射物（蒸汽弹 / 火圈）最终伤害 -> extra。 */
    private float attackBasedExtra(float finalDamage) {
        return (float) (finalDamage - this.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    /** 腐化光束 最终伤害 -> extra（抹平 Goety 侧基础随配置的波动）。 */
    private float corruptBeamExtra() {
        float goetyBase = SpellConfig.CorruptedBeamDamage.get().floatValue() * WandUtil.damageMultiply();
        return (float) (AcatConfig.SKILL_CORRUPT_BEAM.get() - goetyBase);
    }

    /** 风刃 最终伤害 -> SpellStat.potency（无附魔focus/staff 时 RazorWind = 基础 + potency）。 */
    private int windBladePotency() {
        float base = SpellConfig.RazorWindDamage.get().floatValue() * WandUtil.damageMultiply();
        return (int) Math.round(AcatConfig.SKILL_WIND_BLADE.get() - base);
    }

    private enum ScriptType { NONE, WIND_BLAST, QUAKE, SONIC_BOOM, KILLING, RUPTURE }

    /* ==================== Boss 状态字段 ==================== */
    public int hissCount;                 // 哈气累计（12 归零）
    public boolean lockBloodUsed;         // 二阶段锁血是否已用（整场一次）
    public boolean thirdPhase;            // ★ 三阶段已触发标记：整场一次；领域结束后仍保持 true（读档/清理/掉落判定用）

    /* ==================== ★ 三阶段(领域) 常量 ==================== */
    public static final int P3_SETUP_TICKS = 60;                   // 展开期 3 秒
    public static final double P3_INNER_R = 16.0D;                 // 领域半径(内)
    public static final double P3_WALL_R = 20.0D;                  // 阻力带外缘
    public static final double P3_SUCK_RANGE = 64.0D;              // 展开期强制吸入半径
    public static final double P3_HOVER_RISE = 10.0D;              // 悬浮高度 +10 格
    public static final double P3_TOP_EXTRA = 3.0D;                // 领域顶 = 悬浮高度再 +3
    // 三阶段每秒直扣 %：实际读取 config/aaacat.toml -> phase3.drainPercentPerSecond(默认 2%/秒)
    public static final int P3_BOLT_COUNT = 198;                   // 环绕蒸汽弹(蒸腾弹射物)数量(在132颗基础上再加66颗)
    public static final double P3_BOLT_OMEGA_MIN = Math.toRadians(8.0D);    // 蒸汽弹公转角速度下限 8°/s
    public static final double P3_BOLT_OMEGA_MAX = Math.toRadians(55.0D);   // 蒸汽弹公转角速度上限 55°/s(每颗独立随机)
    public static final int P3_BEAM_COUNT = 6;                     // 腐化射线条数
    public static final double P3_BEAM_DEG_PER_TICK = 1.5D;        // 30°/s 旋转(已减半)

    /* ==================== ★ 三阶段(领域) 规律攻击几何 ==================== */
    private static final int P3_SWEEP_INTERVAL_TICKS = 2;        // 同一条线上相邻攻击间隔 0.1 秒(2 tick)
    private static final int P3_SWEEP_GAP_TICKS = 20;            // 两批之间的停火间隔 1 秒
    private static final double P3_SWEEP_R0 = 2.0D;              // 扫描起始半径(内)
    private static final double P3_SWEEP_R_STEP = 2.0D;          // 同一条线上相邻攻击沿半径外移 2 格
    private static final double P3_SWEEP_R_MAX = 16.0D;          // 每条线由内到外放 8 个(r=2~16)
    private static final double P3_SWEEP_ROT_DEG = 20.0D;        // 每组三条线各自完整放完一整轮(每条线由内到外连放 8 个)后，整组才固定旋转 20°；扫射期间角度不变

    /* ==================== ★ 三阶段(领域) 运行状态 ==================== */
    public boolean phase3Active;                                   // 领域进行中(仅服务端使用)
    private boolean p3SetupDone;
    private int p3Ticks;
    private double p3CenterX;
    private double p3CenterZ;
    private double p3BaseY;                                        // 领域地面 y(回满血瞬间的猫脚底)
    private int p3LastStage = -1;
    /* 规律扫描(流星批 + 火圈/冰火批 交替、由内向外) 状态 */
    private int p3SweepStartTick = -1;          // 当前一轮开始 tick；-1 = 等 setup 完成后懒启动
    /* A组(流星 0/120/240)与B组(火圈/冰火 60/180/300)各自累计旋转角(度)：
       每组每齐射一轮(三线各打 1 次攻击)固定 +20°，三三一组持续整体旋转 */
    private double p3SweepRotA;
    private double p3SweepRotB;
    /* 阶段3 随机三连(下界流星/冰之火/虚空震荡弹不分裂) 状态 */
    private int p3MixStep;                      // 0 下界流星, 1 冰之火, 2 虚空震荡弹
    private int p3NextMixTick = -1;             // 下一次随机三连触发 tick
    private final double[] p3BoltAngles = new double[P3_BOLT_COUNT];
    private final double[] p3BoltRadii = new double[P3_BOLT_COUNT];
    private final double[] p3BoltYs = new double[P3_BOLT_COUNT];   // 每颗独立随机高度
    private final double[] p3BoltOmegas = new double[P3_BOLT_COUNT];  // 每颗独立随机角速度(rad/tick)，spawn 时定下
    private final SteamMissile[] p3Bolts = new SteamMissile[P3_BOLT_COUNT];
    private final BeamAnchorEntity[] p3Anchors = new BeamAnchorEntity[P3_BEAM_COUNT];
    private final CorruptedBeam[] p3Beams = new CorruptedBeam[P3_BEAM_COUNT];
    private Entity p3Ring;
    private final List<Entity> p3Spawns = new ArrayList<>();
    /** ★ 三阶段困禁登记(按 UUID，最准)：任何非玩家生物一旦身处领域圆柱内
     *  (r≤P3_WALL_R 且 y∈[领域底-2, 领域顶+2])即被登记，此后每 tick 无视距离/击飞/传送
     *  强制拖回圈内并受 16~20 阻力带推回；直到 死亡/被移除/离开本维度/领域结束 才解锁 */
    private final Set<UUID> p3LockedIds = new LinkedHashSet<>();
    /** 玩家跟踪(仅阻力带/墙体用)：记录上一 tick 位置以识别「传送」——
     *  传送(单 tick 位移>4 格)出去的玩家放行、不拉回；走/飞/被击飞一律穿不过阻力带 */
    private final Map<UUID, Vec3> p3PlayerPrev = new HashMap<>();
    /** ★ 三阶段死亡瞬间：领域内是否有玩家 —— 决定 亚小猫专属掉落(有) 还是 下界使徒掉落(无)。
     *   由 die() 在首次死亡(血量为 0 那一刻)定格，只判定一次 */
    private boolean p3CustomDrops = true;
    private boolean p3DeathChecked;

    /* 哈气近战状态机（贴脸持续触发：每 0.5 秒一次，不再要求"离开过范围"） */
    private int lastHissTick = -100000;
    private boolean scriptActive;         // 哈气施法进行中(含读条)
    private ScriptType scriptType = ScriptType.NONE;
    private int scriptWarmup;
    private int quakeStep;
    /* 哈气/腐化落回后的强制远离窗口：到该 tick 为止只后退不接近目标 */
    private int fleeUntilTick = -1;

    /* 12 次腐化：浮空+光束 */
    private boolean corrupting;
    private int corruptPhase;             // 0 上升, 1 光束, 2 结束
    private int corruptTimer;
    private double corruptRiseFromY;
    private double corruptTargetY;
    private CorruptedBeam corruptBeam;

    /* 9 次撕裂：自管虚空裂隙（到点手动 20 点爆发，替换库内写死的公式爆炸） */
    private VoidRift ruptureRift;

    /* 技能轮换冷却 */
    private int monolithSkillCooldown;
    private int fireRainSkillCooldown;
    private int tornadoSkillCooldown;
    private int catHeadSummonCooldown;    // 二阶段攻击召唤 内置冷却(8 秒 = 160 tick)
    private int catHeadSummonQueue;       // 待出场的猫头数(一次 5 只，逐个登场)
    private int catHeadSummonSpawnTicks;  // 距下一只出场的 tick(每 0.5 秒 = 10 tick 一只)

    private enum CastChoice { NONE, FIREBALL, MONOLITH, FIRE_RAIN, FIRE_TORNADO, SUMMON_CAT_HEAD }

    public ApollyonCatFightEntity(EntityType<? extends ApollyonCatFightEntity> type, Level level) {
        super(type, level);
        // Boss 名：红色加粗（名牌常显）
        this.setCustomName(this.redBoldName());   // Boss 名红色加粗(构造/二/三阶段/读档统一)
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();   // 召唤后不会自然消失
        this.setRegen(true);             // 被动回血（下界口径），二阶段一样会回
    }

    /** 红色加粗 Boss 名：构造、进二阶段、进三阶段、读档回填 都走这里，保证任何路径都不丢样式 */
    private Component redBoldName() {
        return Component.translatable("entity.aaacat.aaacat")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    /** 二阶段(含读档回填)进入瞬间：保持红色加粗名牌 */
    public void setSecondPhase(boolean secondPhase) {
        this.setSecondPhaseInternal(secondPhase);
        if (secondPhase && !this.level().isClientSide) {
            this.setCustomName(this.redBoldName());
        }
    }

    /** 属性：血量 333，其余沿用使徒设置（护甲/韧性跟随 Goety 配置） */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, (float) (double) AcatConfig.get(AcatConfig.SERVANT_MAX_HEALTH))   // 默认 333，可配置
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ARMOR, AttributesConfig.get(AttributesConfig.ApostleArmor))
                .add(Attributes.ARMOR_TOUGHNESS, AttributesConfig.get(AttributesConfig.ApostleToughness))
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.STEP_HEIGHT, 1.0F)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);   // 撕裂裂隙基础 3 = 攻伤/2(1.5)+extra(1.5)
    }

    /* ==================== AI ====================
     * 覆写 registerGoals 并且【不调用 super】：
     * 把使徒的弓术 / 诅咒者 / 远程仆从 / 咆哮等 goal 全部剔除，
     * 只保留技能轮换、猫形风筝(走位+风刃)、哈气与基础目标。
     * 注：使徒真正的"走位/风筝"就是 registerGoals 里带 Flag.MOVE 的
     *     StrafeCastGoal / ApostleBowGoal —— 亚小猫这里用 CatKiteGoal 补齐。      */
    @Override
    protected void registerGoals() {
        super.registerGoals();   // Summoned 仆从系统：主人还击/索敌联动/跟随/同主仆从互不攻击
        this.goalSelector.addGoal(1, new CatSecondPhaseIndicatorGoal());
        this.goalSelector.addGoal(1, new CatCastingPoseGoal());
        this.goalSelector.addGoal(2, new CatSpellGoal());
        this.goalSelector.addGoal(3, new CatKiteGoal());      // MOVE+LOOK：环绕走位 + 风刃远程
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.targetSelector.addGoal(4, new WildPlayerTargetGoal());   // 无主(野生)才主动攻击玩家
    }

    /* ==================== 强制「下界」表现 ==================== */
    public boolean isInNether() {
        return true;   // 亚小猫只在(且按)下界规则战斗
    }

    /** 血量固定 333：使徒基类在存档重载(Cultist.readAdditionalSaveData)时会按 Goety 配置
     *  (AttributesConfig.ApostleHealth, 默认 320) 重置血上限，这里强制钉回 333，
     *  保证 2 阶段/3 阶段/任何时刻上限都是 333。 */
    @Override
    public void setConfigurableAttributes() {
        MobUtil.setBaseAttributes(this.getAttribute(Attributes.MAX_HEALTH), (float) (double) AcatConfig.SERVANT_MAX_HEALTH.get());
    }

    /* ==================== 伤害：限伤继承使徒(默认20) + 二阶段致命伤锁血 1 点 ==================== */
    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        // ★ 三阶段：全程无敌 —— 非 BYPASSES 来源一律吞掉(内部每秒扣血走 genericKill=bypass)
        if (this.phase3Active && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        // ★ 二阶段：受到致命伤害 → 锁血 1 点，并复刻"转二阶段"仪式（无敌/排斥/回满血），
        //   仪式完成即进三阶段。判定先按使徒限伤压一次(与父类一致)，避免“本该死却没死”的偏差。
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.isSecondPhase() && !this.lockBloodUsed
                && !this.isSettingUpSecond() && this.isAlive()
                && Math.min(amount, AttributesConfig.ApostleDamageCap.get().floatValue()) >= this.getHealth()) {
            this.lockBloodUsed = true;
            this.setHealth(1.0F);
            this.moddedInvul = 40;
            this.resetHitTime();
            this.setSettingUpSecond(true);
            this.playSound(SoundEvents.CAT_HISS, 3.0F, 1.0F);
            return;   // 这次致命伤害被吞掉
        }
        // ★ 限伤与结算：直接继承父类 Apostle.actuallyHurt —— 单次限伤 = AttributesConfig.ApostleDamageCap(默认20)，
        //   护甲/魔抗/吸收/受击无敌帧全走使徒现成管线，不再自建。
        this.apostleActuallyHurt(source, amount);
    }
    /** 困难魔法抗性强制生效（不依赖世界难度）；自身伤害归零（索命反噬安全） */
    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float damage) {
        damage = super.getDamageAfterMagicAbsorb(source, damage);
        if (source.getEntity() == this) {
            damage = 0.0F;
        }
        if (MobsConfig.ApostleHardMagicResistance.get()) {
            if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
                damage = (float) ((double) damage * 0.15D);
            }
        }
        return damage;
    }

    /* ==================== 杀死玩家后不消失（继承使徒会自毁） ==================== */
    @Override
    public boolean killedEntity(ServerLevel pLevel, LivingEntity pEntity) {
        boolean flag = super.killedEntity(pLevel, pEntity);
        if (pEntity instanceof Player) {
            this.killedPlayer = false;
            this.lastKilledPlayer = 0;
        }
        return flag;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPELL_DATA, (byte) 0);
        builder.define(SECOND_SETUP, false);
        // ★ 射击指示器同步位必须一并 define：只 defineId 不 define 会令客户端
        //   entityData.get() 取到 null DataItem → clientApostlePassive() 空指针崩溃(仆从召唤即崩)
        builder.define(SHOOT_INDICATOR_END, new Vector3f());
        builder.define(SHOOT_INDICATOR_PROGRESS, 0.0F);
    }

    /* ==================== 数据存取 ==================== */
    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("SpellTicks", this.spellTicks);
        pCompound.putInt("toTeleportTime", this.toTeleportTime);
        pCompound.putInt("HissCount", this.hissCount);
        pCompound.putBoolean("LockBloodUsed", this.lockBloodUsed);
        pCompound.putBoolean("ThirdPhase", this.thirdPhase);
        pCompound.putBoolean("Phase3Active", this.phase3Active);
        pCompound.putInt("Phase3Ticks", this.p3Ticks);
        pCompound.putDouble("Phase3CenterX", this.p3CenterX);
        pCompound.putDouble("Phase3CenterZ", this.p3CenterZ);
        pCompound.putDouble("Phase3BaseY", this.p3BaseY);
        ListTag lockedList = new ListTag();
        for (UUID id : this.p3LockedIds) {
            lockedList.add(StringTag.valueOf(id.toString()));
        }
        pCompound.put("Phase3LockedIds", lockedList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.spellTicks = pCompound.getInt("SpellTicks");
        this.toTeleportTime = pCompound.getInt("toTeleportTime");
        this.setConfigurableAttributes();   // 血上限钉回 333(Cultist 读档重置的等价处理)
        this.hissCount = pCompound.getInt("HissCount");
        this.lockBloodUsed = pCompound.getBoolean("LockBloodUsed");
        this.thirdPhase = pCompound.getBoolean("ThirdPhase");
        this.phase3Active = pCompound.getBoolean("Phase3Active");
        if (this.phase3Active || this.isSecondPhase()) {
            this.setCustomName(this.redBoldName());   // 读档回填：二/三阶段名牌保持红色加粗
        }
        this.p3Ticks = pCompound.getInt("Phase3Ticks");
        this.p3CenterX = pCompound.getDouble("Phase3CenterX");
        this.p3CenterZ = pCompound.getDouble("Phase3CenterZ");
        this.p3BaseY = pCompound.getDouble("Phase3BaseY");
        this.p3LockedIds.clear();
        ListTag lockedList = pCompound.getList("Phase3LockedIds", 8);
        for (int k = 0; k < lockedList.size(); ++k) {
            try {
                this.p3LockedIds.add(UUID.fromString(lockedList.getString(k)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /* ==================== 清理（光束/裂隙跟随实体一起消失） ==================== */
    @Override
    public void remove(RemovalReason pReason) {
        if (!this.level().isClientSide) {
            this.cleanupPhase3Domain();
        }
        if (this.corruptBeam != null) {
            if (this.corruptBeam.isAlive()) {
                this.corruptBeam.discard();
            }
            this.corruptBeam = null;
        }
        if (this.ruptureRift != null) {
            if (this.ruptureRift.isAlive()) {
                this.ruptureRift.discard();
            }
            this.ruptureRift = null;
        }
        // —— 复刻 Apostle.remove 的收尾：清 64 格内归属本猫的陷阱/法术实体/火焰龙卷 ——
        if (!this.level().isClientSide) {
            for (AbstractTrap trap : this.level().getEntitiesOfClass(AbstractTrap.class, this.getBoundingBox().inflate(64))) {
                if (trap.getOwner() == this) trap.discard();
            }
            for (SpellEntity spellEntity : this.level().getEntitiesOfClass(SpellEntity.class, this.getBoundingBox().inflate(64))) {
                if (spellEntity.getOwner() == this) spellEntity.discard();
            }
            for (FireTornado fireTornado : this.level().getEntitiesOfClass(FireTornado.class, this.getBoundingBox().inflate(64))) {
                fireTornado.discard();
            }
        }
        super.remove(pReason);
    }

    /* ==================== 音效：原版猫 ==================== */
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.random.nextInt(4) == 0) {
            return SoundEvents.CAT_PURREOW;
        }
        return SoundEvents.CAT_STRAY_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }

    /** 使徒「真死亡」音效：爆炸飞天→落地彻底死亡(或非华丽分支收尾)时走 getTrueDeathSound，
     *  原继承会放 APOSTLE_DEATH(音量5.0)，这里同样换成原版猫死亡音效。 */
    protected SoundEvent getTrueDeathSound() {
        return SoundEvents.CAT_DEATH;
    }

    /* ==================== 主 tick：哈气判定放最前，脚本推进放最后 ==================== */
    @Override
    public void aiStep() {
        if (this.level().isClientSide) {
            super.aiStep();
            this.clientApostlePassive();   // 客户端被动(二阶段仪式烟雾)的等价实现
        } else if (this.phase3Active) {
            // ★ 三阶段：领域状态机独占（跳过使徒全部被动/施法/回血/传送/召唤物逻辑）
            this.tickPhase3();
        } else if (this.isServantPeaceful()) {
            // ★ 侨从在主人床上睡觉/放松：不战斗 —— 清空敌人并仅驱动 goal（走到床边蹲下），跳过全部战斗编排器
            this.setTarget(null);
            super.aiStep();
        } else {
            this.updateHissProximity();   // 必须先于 super(goal tick)，避免同 tick 双施法
            super.aiStep();
            this.servantApostleAI();      // Apostle 被动等价：仪式/回血/卡墙传送/二阶段流星天候/灼烧等
            this.tickSecondPhaseSetup();  // ★ 转阶段仪式兜底收尾(见方法注释)
            this.tickScripts();           // 哈气读条/地震/腐化浮空/撕裂裂隙 推进
            // ★ 三阶段启动检测：二阶段"锁血 1 点"(lockBloodUsed)后那次转二阶段流程(排斥+回满)完成的瞬间
            if (this.isSecondPhase() && this.lockBloodUsed
                    && !this.isSettingUpSecond() && !this.thirdPhase && this.isAlive()) {
                this.startPhase3();
            }
            this.tickServantAutoHiss();   // ★ 侨从版主动哈气积累钩子（Boss 默认空实现）
            this.tickServantKillAura();   // ★ 侨从版天敌猎杀钩子（幻翼/苦力帕等 5 种，默认空实现）
        }
        if (!this.level().isClientSide) {
            boolean wantGlow = this.isSecondPhase() || this.phase3Active;
            if (wantGlow != this.hasGlowingTag()) {
                this.setGlowingTag(wantGlow);
            }
        }
        if (this.shouldSyncBossMusicTarget()) {
            // ★ Boss BGM 目标同步：仆从版覆写为 false，客户端就不会把它当成目标锁定玩家而播 apostle_theme
            MiscCapHelper.updateMobTarget(this);
        }
        // ★ 仆从睡眠动画钩子：每帧两端都更新卧巨量（客户端渲染器也要读取）
        this.servantTickSleep();
    }

    /** 把使徒写死的 1.62 “眼高”改成小猫头高：风刃/风爆/索命/腐化光束的出生点才会落在猫头，而不是头顶。
     *  1.21 起 getStandingEyeHeight 已被删除、getDimensions(Pose) 为 final，眼高只能经
     *  getDefaultDimensions(Pose).withEyeHeight(...) 覆写(与 Goety-3 同款做法)。 */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(CAT_EYE_HEIGHT);
    }

    /* ==================== 施法烟雾修正（继承自 Apostle 的位置不对） ====================
     * SpellCastingCultist.tick 在客户端给 instanceof Apostle 的实体喷 CULT_SPELL 施法烟，
     * 位置写死 身体+1.8 / 身侧±0.6 —— 对 3 格高使徒是对的，对 0.7 高小猫就全冒在头顶上方。
     * 修复：施法期间先让父类不喷，再在猫头前方补冒同款烟雾。 */

    @Override
    public void tick() {
        super.tick();
        this.floatLikeApostle();   // 岩浆漂浮(Apostle.tick 里的 floatApostle 等价，仆从保持一致)
        // ★ 全免疫负面效果：canBeAffected 只拦“新施加”，这里再兜底清掉身上残留的
        //   非增益效果(中毒/缓慢/虚弱/凋零/失明等)——施加不了 + 残留不留，双保险。
        if (!this.level().isClientSide && this.isAlive() && !this.isRemoved()) {
            this.purgeNonBeneficialEffects();
        }
        // 施法烟雾修正：SpellCastingCultist.tick 给 Apostle 的冒烟点在 身体+1.8，
        // 对 0.7 高的小猫全冒在头顶上方 —— 本类不继承该 tick，改在猫头前方补喷同款烟雾。
        if (this.level().isClientSide && this.isAlive()
                && this.isSpellcasting() && this.getSpellType() != SpellCastingCultist.SpellType.NONE) {
            this.spawnCastSmokeAtHeadFront();
        }
    }


    private void spawnCastSmokeAtHeadFront() {
        double[] rgb = castSmokeRgb(this.getSpellType());
        if (rgb == null) {
            return;
        }
        // 猫头前方(沿头朝向) 0.5 格，高度取猫头略下(嘴部附近)
        float f = this.yHeadRot * ((float) Math.PI / 180F);
        double px = this.getX() - Math.sin(f) * 0.5D;
        double py = this.getY() + CAT_EYE_HEIGHT - 0.08D;
        double pz = this.getZ() + Math.cos(f) * 0.5D;
        for (int i = 0; i < 2; ++i) {
            this.level().addParticle(ModParticleTypes.CULT_SPELL.get(),
                    px + this.random.nextGaussian() * 0.1D,
                    py + this.random.nextGaussian() * 0.06D,
                    pz + this.random.nextGaussian() * 0.1D,
                    rgb[0], rgb[1], rgb[2]);
        }
    }

    /** 与 SpellCastingCultist.SpellType.particleSpeed 一致的三元组（烟雾颜色/飘速） */
    private double[] castSmokeRgb(SpellCastingCultist.SpellType type) {
        return switch (type) {
            case FIRE -> new double[]{1.0D, 0.6D, 0.0D};
            case ZOMBIE -> new double[]{0.1D, 0.1D, 0.8D};
            case ROAR -> new double[]{0.8D, 0.3D, 0.8D};
            case TORNADO -> new double[]{1.0D, 0.1D, 0.1D};
            case RANGED -> new double[]{0.5D, 0.5D, 0.5D};
            case CLOUD -> new double[]{0.3D, 0.0D, 0.0D};
            case SACRIFICE -> new double[]{0.1D, 0.1D, 0.1D};
            default -> null;   // NONE：不该出现在这里
        };
    }

    /* ==================== 哈气触发：2.5 格内即刻生效，内置 0.5 秒冷却 ==================== */
    private void updateHissProximity() {
        if (this.level().isClientSide || this.isNoAi() || this.phase3Active || !this.isAlive()) {
            return;
        }
        LivingEntity target = this.getTarget();
        boolean close = target != null && target.isAlive()
                && !this.isSettingUpSecond()
                && !this.isCasting() && !this.isScriptBusy()
                && this.distanceToSqr(target) <= HISS_SQUARED_RANGE;
        // 玩家一进 2.5 格范围就哈气；只要一直贴脸（期间技能脚本不占位），每 0.5 秒一次
        if (close && this.tickCount - this.lastHissTick >= HISS_REUSE_GAP) {
            this.triggerHiss();
        }
    }

    /** 哈气：计数并决定本次释放的聚晶 */
    private void triggerHiss() {
        this.lastHissTick = this.tickCount;
        ++this.hissCount;
        if (this.getTarget() != null) {
            MobUtil.instaLook(this, this.getTarget().position());
        }
        this.playSound(SoundEvents.CAT_HISS, 2.0F, 1.0F);

        // 第 12 次：浮空 9 格 + 6 秒腐化聚晶，计数归零
        if (this.hissCount % 12 == 0) {
            this.hissCount = 0;
            this.startCorruptionSequence();
            return;
        }
        this.setCasting(true);
        this.scriptActive = true;
        this.scriptWarmup = CAST_WARMUP;

        ScriptType type;
        if (this.hissCount % 3 == 0) {
            // 3/6/9 → 音爆 / 索命 / 撕裂
            type = switch (this.hissCount / 3) {
                case 1 -> ScriptType.SONIC_BOOM;
                case 2 -> ScriptType.KILLING;
                default -> ScriptType.RUPTURE;
            };
        } else {
            // 风爆 / 地震 轮换（1,4,7,10 → 风爆；2,5,8,11 → 地震）
            type = (this.hissCount % 2 == 1) ? ScriptType.WIND_BLAST : ScriptType.QUAKE;
        }
        this.scriptType = type;
    }

    /* ==================== 仆从版主动哈气（仅计数，不释放风爆/地震） ====================
     * 用法：仆从版锁定敌人后每 3 秒调用一次 addAutoHiss()。
     *  - 非里程碑(非 3 的倍数)只累计哈气值，不施放任何东西；
     *  - 累计到 3/6/9 时照常释放 音爆/索命/撕裂，12 时浮空腐化并归零，
     *    与贴脸哈气共用同一套 hissCount 计数与脚本状态机。 */
    protected void addAutoHiss() {
        if (this.level().isClientSide || this.isNoAi() || this.phase3Active || !this.isAlive()) {
            return;
        }
        ++this.hissCount;
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            MobUtil.instaLook(this, target.position());
        }
        // 第 12 次：浮空 9 格 + 6 秒腐化聚晶，计数归零
        if (this.hissCount % 12 == 0) {
            this.playSound(SoundEvents.CAT_HISS, 2.0F, 1.0F);
            this.hissCount = 0;
            this.startCorruptionSequence();
            return;
        }
        // 非里程碑：只累计哈气值，不放风爆/地震
        if (this.hissCount % 3 != 0) {
            return;
        }
        // 3/6/9 → 音爆 / 索命 / 撕裂
        this.playSound(SoundEvents.CAT_HISS, 2.0F, 1.0F);
        this.setCasting(true);
        this.scriptActive = true;
        this.scriptWarmup = CAST_WARMUP;
        this.scriptType = switch (this.hissCount / 3) {
            case 1 -> ScriptType.SONIC_BOOM;
            case 2 -> ScriptType.KILLING;
            default -> ScriptType.RUPTURE;
        };
    }

    /** 仆从版每 tick 扩展钩子：由 aiStep 调用（Boss 默认空实现） */
    protected void tickServantAutoHiss() {
    }

    /** 仆从版天敌猎杀钩子：由 aiStep 调用（默认空实现，仆从版覆写为自动秒杀 5 种天敌） */
    protected void tickServantKillAura() {
    }

    /** Boss BGM 目标同步开关：仆从版覆写为 false（关闭 apostle_theme） */
    protected boolean shouldSyncBossMusicTarget() {
        return true;
    }

    /** ★ 侨从版治安模式：在主人床上睡觉/放松时为 true，此时不进行战斗。默认 Boss 为 false。 */
    protected boolean isServantPeaceful() {
        return false;
    }

    /** ★ 侨从版睡眠动画钩子：每幅调用以更新卧巨量(供渲染器读取)。默认空实现。 */
    protected void servantTickSleep() {
    }


    /** ★ 不洁之血复位：二阶段(含转阶段仪式中) -> 恢复第一阶段。
     *  - 转阶段仪式(isSettingUpSecond)过程中也能打断取消；
     *  - 回满血：否则血量仍 ≤50% 下个 tick 就会被 CatSecondPhaseIndicatorGoal 拉回二阶段；
     *  - 重置 lockBloodUsed：只要还没正式开启三阶段(领域未展开)就恢复一次“锁血”机会；
     *  - 已正式进入三阶段(phase3Active / thirdPhase)或本就不在二阶段 -> 返回 false(不消耗不洁之血)；
     *  - 只停止“本猫今后不再强制雷暴/操控天气”，不复位、不修改世界天气(由调用方决定)。 */
    public boolean forceFirstPhase() {
        if (this.level().isClientSide || this.phase3Active || this.thirdPhase) {
            return false;                    // 领域已正式展开 -> 无效(不消耗)
        }
        if (!this.isSecondPhase() && !this.isSettingUpSecond()) {
            return false;                    // 还没进入二阶段 -> 无效(不消耗)
        }
        // —— 打断二阶段仪式 / 施法 / 浮空 / 脚本(哈气/腐化/裂隙) ——
        this.setSettingUpSecond(false);
        this.setSecondPhaseInternal(false);
        this.endScript();
        this.endCorruption();
        this.endRuptureRift(false);
        this.corrupting = false;
        this.ruptureRift = null;
        this.setCasting(false);
        this.setSpellType(SpellCastingCultist.SpellType.NONE);
        // —— 取消延迟传送与传送计时 ——
        if (this.pendingTeleportSearch != null) {
            this.pendingTeleportSearch.cancel(true);
            this.pendingTeleportSearch = null;
        }
        this.toTeleportPos = null;
        this.toTeleportTime = 0;
        // —— 复位战斗状态 ——
        this.moddedInvul = 0;
        this.obsidianInvul = 0;
        this.antiRegen = 0;
        this.antiRegenTotal = 0;
        this.lockBloodUsed = false;          // 未进三阶段：锁血机会一并恢复
        this.resetHitTime();
        this.resetCoolDown();
        this.setSpellCycle(0);
        this.stuckTime = 0;
        this.fleeUntilTick = -1;
        this.setNoGravity(false);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.setHealth(this.getMaxHealth()); // 回满：避免立刻再次触发转二阶段
        // 名牌由仆从子类自己的覆写维护(构造/二阶段/读档都写回仆从文案)，此处无需改名
        return true;
    }

    /* ==================== 脚本(哈气)推进 ==================== */
    private void tickScripts() {
        if (this.level().isClientSide) {
            return;
        }
        if (this.phase3Active) {
            return;
        }
        if (this.monolithSkillCooldown > 0) --this.monolithSkillCooldown;
        if (this.fireRainSkillCooldown > 0) --this.fireRainSkillCooldown;
        if (this.tornadoSkillCooldown > 0) --this.tornadoSkillCooldown;
        if (this.catHeadSummonCooldown > 0) --this.catHeadSummonCooldown;
        if (this.catHeadSummonQueue > 0) {
            // ★ 修复「一旦开召就永续发射」：队列只在二阶段且目标存活时逐只放行；
            //   没有敌人/仪式中/非二阶段 → 立即取消剩余队列，不再凭空召唤
            LivingEntity summonTarget = this.getTarget();
            boolean canSpawnCatHead = this.isSecondPhase()
                    && !this.isSettingUpSecond()
                    && summonTarget != null && summonTarget.isAlive()
                    && !summonTarget.isRemoved() && !summonTarget.isSpectator();
            if (!canSpawnCatHead) {
                this.catHeadSummonQueue = 0;
                this.catHeadSummonSpawnTicks = 0;
            } else if (--this.catHeadSummonSpawnTicks <= 0) {
                this.spawnOneCatHead();               // 0.5 秒一只陆续登场
                --this.catHeadSummonQueue;            // ★ 放一只扣一只，5 只放完即止(此前只加不减导致永续)
                this.catHeadSummonSpawnTicks = 10;    // 10 tick = 0.5 秒
            }
        }

        if (this.isSettingUpSecond()) {
            if (this.scriptActive) this.endScript();
            if (this.corrupting) this.endCorruption();
            if (this.ruptureRift != null) this.endRuptureRift(false);
            return;
        }
        if (this.corrupting) {
            this.tickCorruption();
            return;
        }
        this.tickRuptureRift();        // 撕裂裂隙：到点手动 20 点爆发并回收
        if (this.scriptActive) {
            this.tickScript();
        }
    }

    private void tickScript() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.scriptType == ScriptType.QUAKE) {
            if (this.scriptWarmup > 0) {
                --this.scriptWarmup;
                if (this.scriptWarmup == 0) this.quakeStep = 1;
            } else if (this.quakeStep <= 12) {
                this.castQuakeRing(this.quakeStep);
                ++this.quakeStep;
            } else {
                this.endScript();
            }
            return;
        }
        if (--this.scriptWarmup <= 0) {
            this.castScriptSpell();
            this.endScript();
        }
    }

    private void castScriptSpell() {
        switch (this.scriptType) {
            case WIND_BLAST -> this.castWindBlast();
            case SONIC_BOOM -> this.castSonicBoomFromHead();      // 配置10 + potency5 = 15 伤害，起点=猫头
            case KILLING -> this.castKilling();                      // 索命：目标当前血量(≈100%最大生命)
            case RUPTURE -> this.castRupture();                    // 撕裂：基础3点/次 + 结束爆发20点
            default -> { }
        }
    }

    /** 索命聚晶：伤害=目标当前生命值，演出复刻 Goety KillingSpell.SpellResult（红雷+聚集粒子）。
     *  不走 KillingSpell.mobSpellResult —— 它必须先让施法者自己吃到 1.1 倍反噬伤害(caster.hurt)才出手；
     *  而小猫只能在 2.5 格贴脸(第 6 次哈气)时放它，贴脸=正在被玩家持续命中，受击后的无敌帧
     *  (invulnerableTime>10 且本次伤害<=lastHurt)会让 hurt() 直接返回 false，整发索命被静默吞掉，
     *  表现就是“从不释放索命聚晶”。这里直接对目标结算；反噬伤害按本类 getDamageAfterMagicAbsorb
     *  本来就是归零的，所以跳过反噬门槛与设计等价，且不再依赖施法者的受击状态。 */
    private void castKilling() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        ColorUtil colorUtil = new ColorUtil(0xd91516);
        Vec3 src = this.getEyePosition();
        Vec3 dst = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2.0D, target.getZ());
        DamageSource damageSource = ModDamageSource.deathCurse(this);
        float killDamage = target.getHealth() * (float) (AcatConfig.SKILL_KILLING_PERCENT.get() / 100.0D);
        if (target.hurt(damageSource, killDamage)) {
            for (int i = 0; i < 8; ++i) {
                Vec3 scatter = new Vec3(target.getX(), target.getEyeY(), target.getZ())
                        .offsetRandom(target.getRandom(), 8.0F);
                serverLevel.sendParticles(new GatherTrailParticleOption(colorUtil, scatter),
                        target.getX(), target.getEyeY(), target.getZ(),
                        0, 0.0D, 0.0D, 0.0D, 0.5F);
                ServerParticleUtil.windParticle(serverLevel, colorUtil, 1.0F, 0.0F,
                        target.getId(), target.position());
            }
            ModNetwork.sendToALL(new SThunderBoltPacket(src, dst, colorUtil, 10));
            serverLevel.playSound(null, src.x, src.y, src.z,
                    ModSounds.THUNDERBOLT.get(), SoundSource.PLAYERS, 3.0F, 0.75F);
            serverLevel.playSound(null, src.x, src.y, src.z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 3.0F, 0.75F);
        }
    }

    private void castWindBlast() {
        // 风爆聚晶：无伤害纯吹飞，威力调大
        new WindBlastSpell().mobSpellResult(this, ItemStack.EMPTY,
                new SpellStat(4, 0, 14, 0.0D, 0, 0.0F));
    }

    /** 地震聚晶：前方逐格震颤（伤害+掀飞），复用 QuakingSpell 的 tremor 段 */
    private void castQuakeRing(int distance) {
        float damage = AcatConfig.SKILL_QUAKE.get().floatValue(); // 每段 <- config [skillDamage].quakePerHit (default 6)
        QuakingSpell.tremor(this, distance, 3, 0.0F, damage, 0.1F);
        QuakingSpell.tremor(this, distance, 3, 1.5F, damage, 0.1F);
        QuakingSpell.tremor(this, distance, 3, -1.5F, damage, 0.1F);
    }

    private void endScript() {
        this.scriptActive = false;
        this.scriptType = ScriptType.NONE;
        this.setCasting(false);
        this.setSpellType(SpellCastingCultist.SpellType.NONE);
        this.fleeUntilTick = this.tickCount + FLEE_TICKS;   // 哈气后主动远离敌人
    }

    /* ==================== 撕裂聚晶：虚空裂隙（基础3点/次 + 结束爆发20点） ====================
     * Goety 的 VoidRiftSpell 会把每 tick 伤害按"owner 攻伤/2+potency"算，且结束爆炸伤害
     * 是写死的半径公式(≈57)，无法调到 20 —— 因此这里不整段走 spell，而是自己种裂隙、
     * 监视其生命周期，在它原生自爆前 1 tick 手动用 SpellExplosion(20) 引爆并回收。         */
    private void castRupture() {
        if (this.level() instanceof ServerLevel serverLevel) {
            LivingEntity living = this.getTarget();
            double x = living != null ? living.getX() : this.getX();
            double y = living != null ? living.getY() + 1.0D : this.getY() + 1.0D;
            double z = living != null ? living.getZ() : this.getZ();
            VoidRift rift = new VoidRift(serverLevel, x, y, z);
            rift.setOwner(this);
            rift.setDuration(RUPTURE_ACTIVE_TICKS);
            rift.setWarmUp(RUPTURE_WARMUP_TICKS);   // 会把总时长加到 active+warmup
            rift.setStaff(false);
            rift.setSize(3.0F);
            rift.setExtraDamage(this.ruptureTickExtra()); // 每tick最终伤害 <- config [skillDamage].rupturePerTick (default 3)
            serverLevel.addFreshEntity(rift);
            this.ruptureRift = rift;
        }
    }

    private void tickRuptureRift() {
        if (this.ruptureRift == null) {
            return;
        }
        VoidRift rift = this.ruptureRift;
        if (!rift.isAlive() || rift.level() != this.level()) {
            this.ruptureRift = null;
            return;
        }
        // 原生自爆在 tickCount == duration+20；提前 1 tick 接管，改放 20 点爆发
        if (rift.tickCount >= rift.getDuration() + 19) {
            this.endRuptureRift(true);
        }
    }

    private void endRuptureRift(boolean explode) {
        VoidRift rift = this.ruptureRift;
        if (rift == null) {
            return;
        }
        this.ruptureRift = null;
        if (explode && rift.isAlive() && this.level() instanceof ServerLevel serverLevel) {
            float range = 8.0F * (rift.getSize() + 1.0F);
            DamageSource damageSource = this.damageSources().indirectMagic(rift, this);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    rift.getX(), rift.getY() + 0.5D, rift.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            this.level().playSound(null, rift.getX(), rift.getY(), rift.getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), this.getSoundSource(), 5.0F, 0.5F);
            new SpellExplosion(serverLevel, this, damageSource,
                    rift.blockPosition(), range / 4.0F, (float) (double) AcatConfig.SKILL_RUPTURE_BURST.get());   // 固定 20 点
        }
        if (rift.isAlive()) {
            rift.discard();
        }
    }

    /* ==================== 第 12 次哈气：腐化浮空 6 秒 ==================== */
    private void startCorruptionSequence() {
        this.corrupting = true;
        this.scriptActive = false;
        this.setCasting(true);
        this.setNoGravity(true);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.corruptPhase = 0;
        this.corruptTimer = 0;
        this.corruptRiseFromY = this.getY();
        this.corruptTargetY = this.getY() + 9.0D;
    }

    private void tickCorruption() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.snapLookTowards(target.getEyePosition());   // 光束锁定敌人：直接压俯仰，正下方贴脸也能命中
        }
        this.stuckTime = 0;                               // 浮空期间不触发卡墙传送
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (this.corruptPhase == 0) {
            // 浮空 9 格
            ++this.corruptTimer;
            double progress = Math.min((double) this.corruptTimer / CORRUPT_RISE_TICKS, 1.0D);
            this.setPos(this.getX(), Mth.lerp(progress, this.corruptRiseFromY, this.corruptTargetY), this.getZ());
            if (this.corruptTimer >= CORRUPT_RISE_TICKS) {
                this.corruptPhase = 1;
                this.corruptTimer = 0;
                this.spawnCorruptBeam();
            }
        } else if (this.corruptPhase == 1) {
            // 6 秒腐化光束（每次伤害 = 配置10 + extra0 = 10 点）
            ++this.corruptTimer;
            this.setPos(this.getX(), this.corruptTargetY, this.getZ());
            if (this.corruptTimer >= CORRUPT_BEAM_TICKS) {
                this.corruptPhase = 2;
                this.endCorruption();   // 落下
            }
        }
    }

    private void spawnCorruptBeam() {
        if (!this.level().isClientSide) {
            Vec3 look = this.getViewVector(1.0F);
            CorruptedBeam beam = new CorruptedBeam(ModEntityType.CORRUPTED_BEAM.get(), this.level(), this);
            beam.moveTo(this.getX() + look.x / 2.0D, this.getEyeY() - 0.2D, this.getZ() + look.z / 2.0D,
                    this.getYRot(), this.getXRot());
            beam.setOwner(this);
            beam.setExtraDamage(this.corruptBeamExtra()); // 每次命中 <- config [skillDamage].corruptBeamPerTick (default 10)

            beam.setItemBase(false);
            this.level().addFreshEntity(beam);
            this.corruptBeam = beam;
        }
    }

    private void endCorruption() {
        if (this.corruptBeam != null) {
            if (this.corruptBeam.isAlive()) {
                this.corruptBeam.discard();
            }
            this.corruptBeam = null;
        }
        this.corrupting = false;
        this.setCasting(false);
        this.setSpellType(SpellCastingCultist.SpellType.NONE);
        this.setNoGravity(false);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.fleeUntilTick = this.tickCount + FLEE_TICKS;   // 落回后立刻远离
    }

    /* ==================== 修复工具：直转视线 / 猫头音爆 ==================== */
    /** 无视原版 40°/tick 的俯仰限速直接瞄准：腐化浮空时玩家站在正下方也要能压到 -90° */
    private void snapLookTowards(Vec3 point) {
        double dx = point.x - this.getX();
        double dy = point.y - this.getEyeY();
        double dz = point.z - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (-Math.toDegrees(Mth.atan2(dx, dz)));
        float pitch = (float) (-Math.toDegrees(Mth.atan2(dy, Math.max(horiz, 1.0E-4D))));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.yHeadRotO = yaw;
        this.xRotO = pitch;
    }

    /** 音爆：Goety 的 SonicBoomSpell 把起点写死在 脚底+1.6(人类眼高)，小猫会从头顶喷出。
     *  这里复刻同样的伤害/击退/音爆粒子，起点改用猫头(getEyePosition 已被压到 CAT_EYE_HEIGHT)。 */
    private void castSonicBoomFromHead() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float damage = AcatConfig.SKILL_SONIC_BOOM.get().floatValue(); // 最终伤害 <- config [skillDamage].sonicBoom (default 15)
        Vec3 src = this.getEyePosition(1.0F);
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            Vec3 vecTo = target.getEyePosition().subtract(src);
            Vec3 dir = vecTo.normalize();
            for (int i = 1; i < Mth.floor(vecTo.length()) + 7; ++i) {
                Vec3 p = src.add(dir.scale(i));
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            target.hurt(target.damageSources().sonicBoom(this), damage);
            double d1 = 0.5D * (1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            double d0 = 2.5D * (1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            target.push(dir.x * d0, dir.y * d1, dir.z * d0);
        } else {
            // 无锁定目标：沿当前视线放音爆(与 Goety 一致)
            Vec3 look = this.getViewVector(1.0F);
            for (int i = 1; i < 31; ++i) {
                Vec3 p = src.add(look.scale(i));
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (MobUtil.getSingleTarget(serverLevel, this, 24.0D, 3.0D) instanceof LivingEntity target1) {
                target1.hurt(target1.damageSources().sonicBoom(this), damage);
                double d0 = target1.getX() - this.getX();
                double d1 = target1.getZ() - this.getZ();
                double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
                MobUtil.push(target1, d0 / d2 * 4.0D, 0.2D, d1 / d2 * 4.0D);
            }
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.0F);
    }

    /* ==================== 传送保护：施法/浮空期间不被拉走 ==================== */
    private boolean isScriptBusy() {
        return this.scriptActive || this.corrupting;
    }

    protected void teleport() {
        if (this.isScriptBusy()) return;
        this.apostleTeleport();
    }

    public void teleportTowards(Entity entity) {
        if (this.isScriptBusy()) return;
        this.apostleTeleportTowards(entity);
    }

    protected void escapeTeleport() {
        if (this.isScriptBusy()) return;
        this.apostleEscapeTeleport();
    }

    /* ==================== 内部小工具 ==================== */

    private int monolithCount() {
        int count = 0;
        for (AbstractObsidianMonolith monolith : this.level().getEntitiesOfClass(AbstractObsidianMonolith.class,
                this.getBoundingBox().inflate(64.0D))) {
            if (monolith.getTrueOwner() == this) ++count;
        }
        return count;
    }

    private int hellCloudCount() {
        int count = 0;
        for (HellCloud cloud : this.level().getEntitiesOfClass(HellCloud.class,
                this.getBoundingBox().inflate(64.0D))) {
            if (cloud.getOwner() == this) ++count;
        }
        return count;
    }

    private int tornadoTrapCount() {
        int count = 0;
        for (FireTornadoTrap trap : this.level().getEntitiesOfClass(FireTornadoTrap.class,
                this.getBoundingBox().inflate(64.0D))) {
            if (trap.getOwner() == this) ++count;
        }
        return count;
    }

    /* ==================== Goal：二阶段触发（与使徒一致） ==================== */
    class CatSecondPhaseIndicatorGoal extends Goal {
        @Override
        public boolean canUse() {
            return ApollyonCatFightEntity.this.getHealth() <= ApollyonCatFightEntity.this.getMaxHealth() / 2
                    && ApollyonCatFightEntity.this.getTarget() != null
                    && !ApollyonCatFightEntity.this.isSecondPhase()
                    && !ApollyonCatFightEntity.this.isSettingUpSecond();
        }

        @Override
        public void tick() {
            ApollyonCatFightEntity.this.setSettingUpSecond(true);
        }
    }

    /* ==================== Goal：施法时站立注视（配合 UseSpellGoal 框架） ==================== */
    class CatCastingPoseGoal extends CatCastingPoseBaseGoal {
        @Override
        public void tick() {
            if (ApollyonCatFightEntity.this.getTarget() != null) {
                ApollyonCatFightEntity.this.getLookControl().setLookAt(ApollyonCatFightEntity.this.getTarget(),
                        (float) ApollyonCatFightEntity.this.getMaxHeadYRot(),
                        (float) ApollyonCatFightEntity.this.getMaxHeadXRot());
            }
        }
    }

    /* ==================== 施法基类（镜像使徒 CastingGoal 的门控） ==================== */
    abstract class CatCastGoal extends CatUseSpellGoal {
        @Override
        public boolean canUse() {
            return super.canUse()
                    && !ApollyonCatFightEntity.this.isCasting()
                    && !ApollyonCatFightEntity.this.isScriptBusy()
                    && !ApollyonCatFightEntity.this.isSettingUpSecond()
                    && ApollyonCatFightEntity.this.toTeleportTime <= 0
                    && !ApollyonCatFightEntity.this.isNoAi()
                    && !ApollyonCatFightEntity.this.phase3Active;
        }

        @Override
        public void start() {
            super.start();
            ApollyonCatFightEntity.this.setCasting(true);
            if (ApollyonCatFightEntity.this.isUsingItem()) {
                ApollyonCatFightEntity.this.stopUsingItem();
            }
        }

        @Override
        public void stop() {
            super.stop();
            ApollyonCatFightEntity.this.setCasting(false);
        }

        @Override
        protected float castingVolume() {
            return 2.0F;
        }

        @Override
        protected int getCastingInterval() {
            return 0;
        }
    }

    /* ==================== Goal：技能轮换
     * 一阶段：火球(地狱爆裂弹)
     * 二阶段：黑曜石巨柱 → 火雨云 → 火焰龙卷 轮换（另有 6 连击怒不可遏时立即龙卷）
     * 目标进入 2.5 格哈气圈内时不施放远程法术，把 isCasting 让给哈气近战脚本。      */
    class CatSpellGoal extends CatCastGoal {
        private CastChoice choice = CastChoice.NONE;

        @Override
        public boolean canUse() {
            this.choice = CastChoice.NONE;
            if (!super.canUse()) {
                return false;
            }
            LivingEntity living = ApollyonCatFightEntity.this.getTarget();
            if (living == null || !ApollyonCatFightEntity.this.getSensing().hasLineOfSight(living)) {
                return false;
            }
            // 贴脸(≤2.5格)交给哈气体系，远程技能只在拉开距离时放
            if (ApollyonCatFightEntity.this.distanceToSqr(living) <= HISS_SQUARED_RANGE) {
                return false;
            }
            if (!ApollyonCatFightEntity.this.isSecondPhase()) {
                if (ApollyonCatFightEntity.this.getSpellCycle() == 0) {
                    this.choice = CastChoice.FIREBALL;
                }
            } else {
                int mc = ApollyonCatFightEntity.this.monolithCount();
                int hc = ApollyonCatFightEntity.this.hellCloudCount();
                int tc = ApollyonCatFightEntity.this.tornadoTrapCount();
                if (ApollyonCatFightEntity.this.getHitTimes() >= 6 && tc < 1) {
                    this.choice = CastChoice.FIRE_TORNADO;   // 被打急眼：直接龙卷
                } else if (ApollyonCatFightEntity.this.catHeadSummonCooldown <= 0
                        && ApollyonCatFightEntity.this.catHeadCount() < 9
                        && ApollyonCatFightEntity.this.catHeadSummonQueue <= 0
                        && ApollyonCatFightEntity.this.getCoolDown() >= ApollyonCatFightEntity.this.spellStart()) {
                    this.choice = CastChoice.SUMMON_CAT_HEAD;   // 攻击召唤：每 8 秒一批 5 只(每 0.5 秒 1 只登场)，颜色随机
                } else if (ApollyonCatFightEntity.this.monolithSkillCooldown <= 0 && mc < 4
                        && ApollyonCatFightEntity.this.getCoolDown() >= ApollyonCatFightEntity.this.spellStart()) {
                    this.choice = CastChoice.MONOLITH;
                } else if (ApollyonCatFightEntity.this.fireRainSkillCooldown <= 0 && hc < 2
                        && ApollyonCatFightEntity.this.getCoolDown() >= ApollyonCatFightEntity.this.spellStart()) {
                    this.choice = CastChoice.FIRE_RAIN;
                } else if (ApollyonCatFightEntity.this.tornadoSkillCooldown <= 0 && tc < 1
                        && ApollyonCatFightEntity.this.getCoolDown() >= ApollyonCatFightEntity.this.spellStart()) {
                    this.choice = CastChoice.FIRE_TORNADO;
                }
            }
            return this.choice != CastChoice.NONE;
        }

        @Override
        protected int getCastingTime() {
            return switch (this.choice) {
                case FIREBALL, MONOLITH -> 40;
                case SUMMON_CAT_HEAD -> 10;
                case FIRE_RAIN -> 20;
                case FIRE_TORNADO -> 60;
                default -> 20;
            };
        }

        @Nullable
        @Override
        protected SoundEvent getSpellPrepareSound() {
            return null;
        }

        @Override
        protected SpellCastingCultist.SpellType getSpellType() {
            return switch (this.choice) {
                case FIREBALL -> SpellCastingCultist.SpellType.FIRE;
                case MONOLITH -> SpellCastingCultist.SpellType.ZOMBIE;
                case SUMMON_CAT_HEAD -> SpellCastingCultist.SpellType.ZOMBIE;
                case FIRE_RAIN -> SpellCastingCultist.SpellType.CLOUD;
                case FIRE_TORNADO -> SpellCastingCultist.SpellType.TORNADO;
                default -> SpellCastingCultist.SpellType.NONE;
            };
        }

        @Override
        public void castSpell() {
            switch (this.choice) {
                case FIREBALL -> castFireball();
                case MONOLITH -> castMonolith();
                case SUMMON_CAT_HEAD -> ApollyonCatFightEntity.this.startCatHeadSummon();
                case FIRE_RAIN -> castFireRain();
                case FIRE_TORNADO -> castFireTornado();
                default -> { }
            }
        }

        /* 一阶段火球：地狱爆裂弹（困难/下界口径，固定 HellBlast 且可破坏地形） */
        private void castFireball() {
            LivingEntity living = ApollyonCatFightEntity.this.getTarget();
            if (living != null) {
                double d1 = living.getX() - ApollyonCatFightEntity.this.getX();
                double d2 = living.getY(0.5D) - ApollyonCatFightEntity.this.getY(0.5D);
                double d3 = living.getZ() - ApollyonCatFightEntity.this.getZ();
                AbstractHurtingProjectile fireball = new HellBlast(ApollyonCatFightEntity.this, d1, d2, d3, ApollyonCatFightEntity.this.level());
                // HellBlast 继承 WaterHurtingProjectile，不是 ExplosiveProjectile：旧的 instanceof 分支从不执行，
                // 导致配置的直击伤害从未生效。这里直接对 HellBlast 本体设置命中伤害(爆炸/狱火归 Goety 内部结算)。
                if (fireball instanceof HellBlast hellBlast) {
                    hellBlast.setDamage((float) (double) AcatConfig.SKILL_FIREBALL.get()); // 直接命中 <- config [skillDamage].fireballPerHit (default 6)
                }
                fireball.setPos(fireball.getX(), ApollyonCatFightEntity.this.getY(0.5), fireball.getZ());
                ApollyonCatFightEntity.this.level().addFreshEntity(fireball);
                ApollyonCatFightEntity.this.level().levelEvent(null, 1016, ApollyonCatFightEntity.this.blockPosition(), 0);
                if (ApollyonCatFightEntity.this.teleportChance()) {
                    ApollyonCatFightEntity.this.teleport();
                }
                ApollyonCatFightEntity.this.resetCoolDown();
                ApollyonCatFightEntity.this.setSpellCycle(1);
            }
        }

        /* 黑曜石巨柱（12~24 格随机召唤 1 根，间隔 20 秒，所有者=亚小猫） */
        private void castMonolith() {
            if (ApollyonCatFightEntity.this.level() instanceof ServerLevel serverLevel) {
                LivingEntity living = ApollyonCatFightEntity.this.getTarget();
                RandomSource random = serverLevel.getRandom();
                if (living != null) {
                    int amount = 1;   // 每批固定 1 根（下调：原 1~2 根随机）
                    for (int p = 0; p < amount; ++p) {
                        int k = (12 + random.nextInt(12)) * (random.nextBoolean() ? -1 : 1);
                        int l = (12 + random.nextInt(12)) * (random.nextBoolean() ? -1 : 1);
                        BlockPos.MutableBlockPos pos = ApollyonCatFightEntity.this.blockPosition().mutable().move(k, 0, l);
                        ObsidianMonolith monolith = new ObsidianMonolith(ModEntityType.OBSIDIAN_MONOLITH.get(), serverLevel);
                        BlockPos summonPos = BlockFinder.SummonRadiusSight(pos, ApollyonCatFightEntity.this, monolith, serverLevel, 5);
                        monolith.moveTo(summonPos, 0.0F, 0.0F);
                        monolith.setTrueOwner(ApollyonCatFightEntity.this);
                        monolith.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos),
                                MobSpawnType.MOB_SUMMONED, null);
                        serverLevel.addFreshEntity(monolith);
                        // —— 巨柱落地时额外召唤 6 只 goety:mini_ghast 仆从，围着巨柱一圈 ——
                        for (int gh = 0; gh < 6; ++gh) {
                            MiniGhast miniGhast = new MiniGhast(ModEntityType.MINI_GHAST.get(), serverLevel);
                            double ghAngle = gh * (2.0D * Math.PI / 6.0D);
                            double ghRadius = 3.0D;
                            BlockPos ghastPos = summonPos.offset(
                                    (int) Math.round(Math.cos(ghAngle) * ghRadius),
                                    2,
                                    (int) Math.round(Math.sin(ghAngle) * ghRadius));
                            miniGhast.moveTo(ghastPos.getX() + 0.5D, ghastPos.getY(), ghastPos.getZ() + 0.5D,
                                    0.0F, 0.0F);
                            miniGhast.setTrueOwner(ApollyonCatFightEntity.this);
                            miniGhast.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(ghastPos),
                                    MobSpawnType.MOB_SUMMONED, null);
                            miniGhast.setLimitedLife(MobUtil.getSummonLifespan(serverLevel));
                            if (living != null) {
                                miniGhast.setTarget(living);
                            }
                            serverLevel.addFreshEntity(miniGhast);
                        }
                    }
                }
            }
            ApollyonCatFightEntity.this.postSpellCast();
            ApollyonCatFightEntity.this.monolithSkillCooldown = MathHelper.secondsToTicks(20);
        }

        /* 火雨云（地狱火云，半径 3，1200 tick） */
        private void castFireRain() {
            LivingEntity living = ApollyonCatFightEntity.this.getTarget();
            if (living != null) {
                HellCloud hellCloud = new HellCloud(ApollyonCatFightEntity.this.level(), ApollyonCatFightEntity.this, living);
                hellCloud.setRadius(3.0F);
                hellCloud.setLifeSpan(1200);
                ApollyonCatFightEntity.this.level().addFreshEntity(hellCloud);
            }
            ApollyonCatFightEntity.this.postSpellCast();
            ApollyonCatFightEntity.this.fireRainSkillCooldown = MathHelper.secondsToTicks(8);
        }

        /* 火焰龙卷：在目标脚下放置龙卷陷阱 */
        private void castFireTornado() {
            LivingEntity living = ApollyonCatFightEntity.this.getTarget();
            if (living != null) {
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(living.getX(), living.getY(), living.getZ());
                while (pos.getY() > ApollyonCatFightEntity.this.level().getMinBuildHeight()
                        && !ApollyonCatFightEntity.this.level().getBlockState(pos).blocksMotion()) {
                    pos.move(Direction.DOWN);
                }
                FireTornadoTrap tornado = new FireTornadoTrap(ModEntityType.FIRE_TORNADO_TRAP.get(), ApollyonCatFightEntity.this.level());
                tornado.setPos(pos.getX(), pos.getY() + 1, pos.getZ());
                tornado.setOwner(ApollyonCatFightEntity.this);
                tornado.setDuration(60);
                ApollyonCatFightEntity.this.level().addFreshEntity(tornado);
            }
            ApollyonCatFightEntity.this.postSpellCast();
            ApollyonCatFightEntity.this.tornadoSkillCooldown = MathHelper.secondsToTicks(10);
        }
    }

    /* ==================== 二阶段攻击召唤：猫头魂体（红/蓝/黄 随机，每 8 秒一批 5 只，同场上限 9）
     * 单实体 + variant 区分三元素，参考 Goety 使徒召唤狱魂/VoidShock：
     *   红 = 火焰伤害+点燃，铺 5 朵狱火十字(HellBlast 口径)；
     *   蓝 = 冰冻伤害+满霜冻，铺十字冰之火(IceBouquet ×4，三阶段口径)；
     *   黄 = 魔法伤害，铺 5 朵终末火十字(YaVoidShockBomb 口径)。
     * 命中/未命中/撞墙/超时都由 ApollyonCatHeadEntity 自己处理，这里只负责"放出去"。 ==================== */
    /** 二阶段攻击召唤：排队 5 只猫头，每 0.5 秒登场 1 只；不占后续技能位，召唤期间可放其他技能 */
    private void startCatHeadSummon() {
        this.catHeadSummonQueue = 5;
        this.catHeadSummonSpawnTicks = 1;   // 下一 tick 立即出第一只
        this.catHeadSummonCooldown = MathHelper.secondsToTicks(8);   // 8 秒一批
        this.postSpellCast();
    }

    /** 放出一只猫头(红/蓝/黄 随机)，出生在亚小猫周围 ±3 格地面点 */
    private void spawnOneCatHead() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ApollyonCatHeadEntity head = new ApollyonCatHeadEntity(AcatEntities.CAT_HEAD.get(), this.level());
        BlockPos offset = this.blockPosition().offset(
                serverLevel.random.nextIntBetweenInclusive(-3, 3),
                0,
                serverLevel.random.nextIntBetweenInclusive(-3, 3));
        BlockPos pos = ApollyonCatHeadEntity.summonPositionNear(this, offset);
        head.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                this.getYHeadRot(), 0.0F);
        head.setTrueOwner(this);
        head.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos),
                MobSpawnType.MOB_SUMMONED, null);
        head.setVariant(serverLevel.random.nextInt(3));   // 红/蓝/黄 随机
        LivingEntity living = this.getTarget();
        if (living != null && living.isAlive() && !living.isSpectator()) {
            head.setTarget(living);                        // 锁定当前攻击目标
        }
        head.setLimitedLife(ApollyonCatHeadEntity.DEFAULT_LIFETIME);
        serverLevel.addFreshEntity(head);
    }

    /** 二阶段猫头召唤物在场数(归属=本猫)，同场上限 9 只避免刷屏 */
    private int catHeadCount() {
        int count = 0;
        for (ApollyonCatHeadEntity head : this.level().getEntitiesOfClass(ApollyonCatHeadEntity.class,
                this.getBoundingBox().inflate(64.0D))) {
            if (head.getOwnerId() != null && head.getOwnerId().equals(this.getUUID())) {
                ++count;
            }
        }
        return count;
    }

    /* ==================== Goal：猫形风筝（远离型走位 + 风刃）
     * 参照使徒的 ApostleBowGoal / SurroundGoal(StrafeCastGoal)，整体偏向"远离敌人"：
     *   - 以亚小猫为圆心 22 格为风筝圈：目标 ≤22 格【绝不主动追近】，只环绕或后退；
     *     只有目标逃出 22 格才 moveTo 拉回射程 —— 仪式召唤时玩家至多站 16 格内，
     *     所以刚召唤出来的亚小猫只会原地环绕，绝不会主动冲脸；
     *   - 目标进入 6.5 格(近身威胁区)强制后退远离，把 2.5 格哈气圈留给哈气脚本；
     *   - 每次哈气/腐化落回结束后有 flee 窗口(1.25s)：只后退不接近，边退边放风刃；
     *     玩家强行追入 2.5 格圈 → 触发哈气 → 放完又后撤，形成"哈气→逃离"循环；
     *   - 走位期间有视线就按节奏放【风刃聚晶】(配置6 + potency10 = 16 伤害)。          */
    class CatKiteGoal extends Goal {
        private static final double KITE_SQR = 22.0D * 22.0D;            // 风筝圈半径 22 格（圈内不追近）
        private static final double KITE_BACK_OFF_SQR = 6.5D * 6.5D;     // 6.5 格内强制后退远离
        private static final double KITE_APPROACH_SQR = 16.5D * 16.5D;   // 超过 16.5 格才允许向前压回
        private static final double FIRE_RANGE_SQR = 40.0D * 40.0D;      // 风刃最远射程
        private int attackTime = -1;
        private int seeTime;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        CatKiteGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return ApollyonCatFightEntity.this.getTarget() != null
                    && !ApollyonCatFightEntity.this.isCasting()
                    && !ApollyonCatFightEntity.this.isScriptBusy()
                    && !ApollyonCatFightEntity.this.isSettingUpSecond()
                    && !ApollyonCatFightEntity.this.isNoAi()
                    && !ApollyonCatFightEntity.this.phase3Active
                    && ApollyonCatFightEntity.this.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity living = ApollyonCatFightEntity.this.getTarget();
            if (living == null) {
                return;
            }
            boolean see = ApollyonCatFightEntity.this.getSensing().hasLineOfSight(living);
            if (see) {
                ++this.seeTime;
            } else {
                --this.seeTime;
                if (this.seeTime < -100) {
                    this.seeTime = -100;
                }
            }
            double d0 = ApollyonCatFightEntity.this.distanceToSqr(living.getX(), living.getY(), living.getZ());
            boolean fleeing = ApollyonCatFightEntity.this.tickCount < ApollyonCatFightEntity.this.fleeUntilTick;

            if (fleeing) {
                // —— 哈气/腐化刚结束：只顾后撤远离，绝不接近（下方共用段仍可边退边放风刃）——
                ApollyonCatFightEntity.this.getNavigation().stop();
                ApollyonCatFightEntity.this.getLookControl().setLookAt(living, 30.0F, 30.0F);
                ApollyonCatFightEntity.this.getMoveControl().strafe(-0.85F,
                        this.strafingClockwise ? 0.45F : -0.45F);
            } else if (d0 > KITE_SQR) {
                // —— 目标逃出风筝圈：moveTo 拉回射程（召唤 16 格内不会发生，不会开局冲脸）——
                ApollyonCatFightEntity.this.getNavigation().moveTo(living, 1.0F);
                this.strafingTime = -1;
            } else {
                // —— 圈内(≤22格)：停导航，环绕走位；近身则强制后退 ——
                ApollyonCatFightEntity.this.getNavigation().stop();
                ++this.strafingTime;
                if (this.strafingTime >= 20) {
                    if ((double) ApollyonCatFightEntity.this.getRandom().nextFloat() < 0.3D) {
                        this.strafingClockwise = !this.strafingClockwise;
                    }
                    if ((double) ApollyonCatFightEntity.this.getRandom().nextFloat() < 0.3D) {
                        this.strafingBackwards = !this.strafingBackwards;
                    }
                    this.strafingTime = 0;
                }
                // ≤6.5格：强制后退；>16.5格：允许向前压回；中间：随机前后环绕
                if (d0 < KITE_BACK_OFF_SQR) {
                    this.strafingBackwards = true;
                } else if (d0 > KITE_APPROACH_SQR) {
                    this.strafingBackwards = false;
                }
                ApollyonCatFightEntity.this.getLookControl().setLookAt(living, 30.0F, 30.0F);
                ApollyonCatFightEntity.this.getMoveControl().strafe(
                        this.strafingBackwards ? -0.65F : 0.35F,
                        this.strafingClockwise ? 0.55F : -0.55F);
            }

            // —— 远程风刃（flee 后撤中同样会放，形成"边退边风筝"）——
            if (see && this.seeTime >= 10
                    && d0 < FIRE_RANGE_SQR
                    && !ApollyonCatFightEntity.this.isCasting()
                    && !ApollyonCatFightEntity.this.isScriptBusy()) {
                if (--this.attackTime <= 0) {
                    this.attackTime = this.interval();
                    this.fireRazorWind(living);
                }
            }
        }

        private int interval() {
            if (ApollyonCatFightEntity.this.monolithWeakened()) {
                return 40;
            }
            return ApollyonCatFightEntity.this.isSecondPhase() ? 10 : 16;
        }

        private void fireRazorWind(LivingEntity target) {
            if (ApollyonCatFightEntity.this.level().isClientSide) {
                return;
            }
            MobUtil.instaLook(ApollyonCatFightEntity.this, target.position());
            new RazorWindSpell().mobSpellResult(ApollyonCatFightEntity.this, ItemStack.EMPTY,
                    new SpellStat(ApollyonCatFightEntity.this.windBladePotency(), 0, 0, 1.2D, 0, 0.0F));   // 配置6 + potency10 = 16 伤害
        }
    }
    /* ==================== ★★★★★ 三阶段(领域) 实现 ★★★★★ ==================== */

    private double p3TopY() {
        return this.p3BaseY + P3_HOVER_RISE + P3_TOP_EXTRA;
    }

    private double p3RiseY() {
        if (this.p3Ticks <= P3_SETUP_TICKS) {
            return Mth.lerp((double) this.p3Ticks / (double) P3_SETUP_TICKS,
                    this.p3BaseY, this.p3BaseY + P3_HOVER_RISE);
        }
        return this.p3BaseY + P3_HOVER_RISE;
    }

    private int p3Stage() {
        float hp = this.getHealth();
        float max = Math.max(1.0F, this.getMaxHealth());
        if (hp > max * 0.66F) return 0;      // 100%~66%
        if (hp > max * 0.33F) return 1;      // 66%~33%
        return 2;                            // 33%~0%
    }

    /** 三阶段启动：锁定坐标、记录领域地面、清理旧召唤物、出光环实体 */
    private void startPhase3() {
        this.thirdPhase = true;
        this.phase3Active = true;
        this.setCustomName(this.redBoldName());   // 三阶段名牌保持红色加粗
        this.p3Ticks = 0;
        this.p3SetupDone = false;
        this.p3LastStage = -1;
        this.p3CenterX = this.getX();
        this.p3CenterZ = this.getZ();
        this.p3BaseY = this.getY();
        this.p3SweepStartTick = -1;          // setup 完成后由 tickPhase3Sweep 懒启动
        this.p3SweepRotA = 0.0D;             // 三阶段新开一场：两组旋转角从 0 开始（子阶段切换不归零）
        this.p3SweepRotB = 0.0D;
        this.p3MixStep = 0;
        this.p3NextMixTick = -1;
        this.p3LockedIds.clear();     // 三阶段重新开局：登记清空
        this.p3PlayerPrev.clear();
        // 停掉二阶段遗留的脚本与召唤物
        this.endScript();
        this.endCorruption();
        this.endRuptureRift(false);
        this.discardOwnedOldSpawns();
        this.setCasting(false);
        this.setSpellType(SpellCastingCultist.SpellType.NONE);
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        // ★ 回血封锁：关使徒被动回血开关 + 清残留回复效果 —— 三阶段只允许 2%/s 直扣
        this.setRegen(false);
        this.removeAllEffects();
        this.playSound(SoundEvents.CAT_HISS, 3.0F, 0.4F);
        this.spawnDomainRing();
    }

    private void spawnDomainRing() {
        if (this.level().isClientSide) return;
        if (this.p3Ring != null && this.p3Ring.isAlive()) return;
        // 存档重载后旧光环实体仍留在世界(跟踪字段已空) -> 直接接管，避免重复生成第二个光环
        for (ApollyonCatDomainRingEntity old : this.level().getEntitiesOfClass(ApollyonCatDomainRingEntity.class,
                new AABB(this.p3CenterX - 2.0D, this.p3BaseY - 2.0D, this.p3CenterZ - 2.0D,
                        this.p3CenterX + 2.0D, this.p3BaseY + 6.0D, this.p3CenterZ + 2.0D))) {
            if (old.isAlive()) {
                this.p3Ring = old;
                return;
            }
        }
        ApollyonCatDomainRingEntity ring = new ApollyonCatDomainRingEntity(AcatEntities.DOMAIN_RING.get(), this.level());
        ring.moveTo(this.p3CenterX, this.p3BaseY + 0.05D, this.p3CenterZ, 0.0F, 0.0F);
        this.level().addFreshEntity(ring);
        this.p3Ring = ring;
    }

    /** 三阶段每 tick（服务端）：扣血、锁坐标悬浮、力场、各阶段机制 */
    private void tickPhase3() {
        ++this.p3Ticks;
        if (this.p3Ticks > P3_SETUP_TICKS) {
            this.p3SetupDone = true;
        }
        // —— 无敌浮空：锁定 xz，y 在 3 秒内升到 +10 ——
        this.setNoGravity(true);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.setPos(this.p3CenterX, this.p3RiseY(), this.p3CenterZ);
        // —— 每秒直扣 2% 最大生命：setHealth 直改，不吃减伤/魔抗/无敌，也不受任何回血影响 ——
        if (this.p3Ticks % 20 == 0) {
            this.setHealth(this.getHealth() - this.getMaxHealth() * (float) (AcatConfig.P3_DRAIN_PERCENT.get() / 100.0D));   // 默认 2%/秒
            if (this.getHealth() <= 0.0F) {
                this.die(this.damageSources().genericKill());
                this.onPhase3Death();
                return;
            }
        }
        this.refreshPhase3Target();
        // —— 光环实体兜底（存档读档后重建） ——
        this.spawnDomainRing();
        // —— 领域力场 ——
        this.tickPhase3Field();
        // —— 阶段机制 ——
        this.tickPhase3StageMechanics();
    }

    private void onPhase3Death() {
        if (!this.phase3Active) return;
        this.phase3Active = false;
        this.cleanupPhase3Domain();     // 领域结束：灵魂束/光环/龙卷/锚点/生成物全部清场
    }

    private void refreshPhase3Target() {
        LivingEntity t = this.getTarget();
        if (t != null && t.isAlive() && !t.isRemoved() && !t.isSpectator()) {
            return;
        }
        // 不再强制只锁玩家：任何非友方的活体(玩家/敌对生物/其他生物)都能成为锁定目标
        double best = Double.MAX_VALUE;
        LivingEntity pick = null;
        for (LivingEntity le : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(96.0D))) {
            if (le == this || le.isSpectator() || !le.isAlive() || le.isRemoved()) continue;
            if (!this.isPhase3Targetable(le)) continue;          // 友方/锚点不锁；其它使徒(Boss)算敌人可锁
            double d = this.distanceToSqr(le);
            if (d < best) {
                best = d;
                pick = le;
            }
        }
        this.setTarget(pick);
    }

    /** 是否可作为三阶段锁定目标(含玩家)。其它 Apostle(使徒 Boss)强制视为敌对 ——
     *  Goety 的 Apostle.isAlliedTo 会把同类(双方都无队伍)判为同盟，导致 MobUtil.areAllies
     *  把其它使徒误认成友方，领域既不解锁它们也不拉入。这里把「非本猫的 Apostle」一律当敌人。 */
    protected boolean isPhase3Targetable(LivingEntity le) {
        if (le == this) return false;
        if (le instanceof BeamAnchorEntity) return false;        // 隐形光束锚点
        if (le instanceof IApollyonCat) return false;         // 另一只亚小猫：不互拉
        if (le instanceof Player) return true;                   // 玩家始终是目标
        if (le instanceof Apostle) return true;                  // 其它使徒类 Boss：按敌人拉入困住
        return !MobUtil.areAllies(this, le);                     // 猫自己的召唤物/友方豁免
    }

    /** 领域登记/困禁的敌人判定(不含玩家 —— 玩家走 tickPhase3Player 单独处理) */
    private boolean isPhase3Enemy(LivingEntity le) {
        return !(le instanceof Player) && this.isPhase3Targetable(le);
    }

    /** 该实体是否属于"领域主动拉取对象"：被亚小猫索敌的生物 或 正在索敌亚小猫的生物。
     *  (附近玩家属于另一类拉取对象，由展开期单独处理，不在此列) */
    private boolean isPhase3PullInterest(LivingEntity le) {
        if (le == this.getTarget()) {
            return true;
        }
        return le instanceof Mob mob && mob.getTarget() == this;
    }

    /** 三阶段是否把该玩家视作敌人/受困对象：Boss 恒 true；仆从版覆写为 false ——
     *  主人及同盟玩家不受领域吸入/阻力带/困禁影响。 */
    protected boolean isPhase3PlayerEnemy(Player player) {
        return true;
    }

    /** 领域力场：
     *  ① 玩家：展开期(前3秒)把 64 内玩家吸进 r≤16，此后不再吸。阻力带对玩家长期生效：
     *     走/飞/被击飞都穿不过 16~20(越深推力越强、r>20 硬顶回、封顶封底)；
     *     唯一例外是「传送」(单 tick 水平位移>4 格：珍珠/紫颂/指令等)——传送出去放行、不拉回。
     *  ② 非玩家困禁(按 UUID 登记)：任何非玩家生物一旦身处领域圆柱内即被登记，
     *     此后每 tick 无视距离/击飞/传送强制拖回圈内(硬边界+阻力带+封顶封底)，
     *     死亡/被移除/换维度/领域结束才解锁——进来就出不去，传送出去也会被拉回。
     *  ③ 展开期把「被猫索敌/索敌猫」的兴趣生物从 64 内吸进 r≤16。 */
    private void tickPhase3Field() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;   // 领域力场只在服务端结算
        }
        final double minY = this.p3BaseY - 0.9D;
        final double topY = this.p3TopY();
        final boolean setup = this.p3Ticks <= P3_SETUP_TICKS;

        // 扫描盒：水平覆盖拉取半径 64；垂直覆盖 领域地面下方 ~ 穹顶上方(含空中拉取对象)
        AABB box = new AABB(this.p3CenterX - P3_SUCK_RANGE, minY - 8.0D, this.p3CenterZ - P3_SUCK_RANGE,
                this.p3CenterX + P3_SUCK_RANGE, topY + 32.0D, this.p3CenterZ + P3_SUCK_RANGE);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, box);

        // ============ 1) 玩家：展开期吸入 + 常驻阻力带/墙体(传送豁免) ============
        for (LivingEntity le : nearby) {
            if (!(le instanceof Player player)) continue;
            if (player.isSpectator() || !player.isAlive() || player.isRemoved() || player.level() != this.level()) continue;
            if (!this.isPhase3PlayerEnemy(player)) {
                this.p3PlayerPrev.remove(player.getUUID());
                continue;
            }
            this.tickPhase3Player(player, minY, topY, setup);
        }

        // ============ 2) 非玩家：登记(按 UUID) + 兴趣生物拉入 ============
        for (LivingEntity le : nearby) {
            if (le instanceof Player) continue;
            if (le == this || le.isSpectator() || !le.isAlive() || le.isRemoved()) continue;
            if (!this.isPhase3Enemy(le)) continue;               // 锚点/猫的召唤物豁免；其它使徒(Boss)按敌人拉入困住

            double dx = le.getX() - this.p3CenterX;
            double dz = le.getZ() - this.p3CenterZ;
            double r = Math.sqrt(dx * dx + dz * dz);
            double y = le.getY();

            // 身处领域圆柱内(含贴墙) → 按 UUID 登记(幂等，此后无论被传送/击飞到哪都拖回)
            if (r <= P3_WALL_R && y >= minY - 2.0D && y <= topY + 2.0D) {
                this.p3LockedIds.add(le.getUUID());
            }
            // 拉入：全期拉「兴趣生物」，仅半径 64 内生效(玩家由①负责且仅展开期)
            if (r > P3_INNER_R && r <= P3_SUCK_RANGE && this.isPhase3PullInterest(le)) {
                double step;
                if (setup) {
                    int left = Math.max(1, P3_SETUP_TICKS - this.p3Ticks + 1);
                    step = (r - P3_INNER_R) / left;        // 按剩余时间均分，保证 3 秒内到位
                } else {
                    step = 0.5D;
                }
                step = Math.min(step, 1.2D);
                this.phasePush(le,
                        le.getX() - dx / r * step,
                        Math.max(minY, Math.min(topY, le.getY())),
                        le.getZ() - dz / r * step);
            }
        }

        // ============ 3) 清理失效登记(死亡/被移除/旁观/换维度)；实体未加载则保留等待回笼 ============
        this.p3LockedIds.removeIf(id -> {
            Entity e = serverLevel.getEntity(id);
            if (e instanceof LivingEntity le) {
                return le.isRemoved() || !le.isAlive() || le.isSpectator() || le.level() != this.level();
            }
            return false;   // 没找到(可能区块未加载/暂不可见) → 先保留
        });

        // ============ 4) 已登记困禁生物：每 tick 无视距离强制回笼(传送/击飞/卡墙都拖回) ============
        for (UUID id : new ArrayList<>(this.p3LockedIds)) {
            Entity e = serverLevel.getEntity(id);
            if (e instanceof LivingEntity le
                    && le.isAlive() && !le.isRemoved() && le.level() == this.level()) {
                this.phase3Contain(le);
            }
        }

        // ============ 5) 玩家跟踪清理(离场/死亡/换维度) ============
        this.p3PlayerPrev.entrySet().removeIf(entry -> {
            Player p = serverLevel.getPlayerByUUID(entry.getKey());
            return p == null || p.isRemoved() || !p.isAlive() || p.isSpectator();
        });
    }

    /** 玩家墙体：展开期吸入 + 16~20 阻力带 + r>20 硬顶回 + 封顶封底。
     *  只有「传送」(单 tick 水平位移 > 4 格：末影珍珠/紫颂果/指令等)能离开领域且不被拉回；
     *  常规移动(走/跑/飞/被击飞)在带内会被越推越强的向心力顶回，无法穿过。 */
    private void tickPhase3Player(Player player, double minY, double topY, boolean setup) {
        UUID id = player.getUUID();
        Vec3 prev = this.p3PlayerPrev.get(id);
        boolean tracked = prev != null;
        boolean teleported = tracked
                && Math.hypot(player.getX() - prev.x, player.getZ() - prev.z) > 4.0D;

        // —— 展开期：把 64 内玩家吸进 r≤16(传送离开的玩家放行) ——
        if (setup) {
            double dx0 = player.getX() - this.p3CenterX;
            double dz0 = player.getZ() - this.p3CenterZ;
            double r0 = Math.sqrt(dx0 * dx0 + dz0 * dz0);
            if (teleported && r0 > P3_WALL_R) {
                this.p3PlayerPrev.remove(id);   // 传送走了 → 不再吸、不拉回
                return;
            }
            if (r0 > P3_INNER_R && r0 <= P3_SUCK_RANGE) {
                int left = Math.max(1, P3_SETUP_TICKS - this.p3Ticks + 1);
                double step = Math.min((r0 - P3_INNER_R) / left, 1.2D);
                this.phasePush(player,
                        player.getX() - dx0 / r0 * step,
                        Math.max(minY, Math.min(topY, player.getY())),
                        player.getZ() - dz0 / r0 * step);
            }
        }
        // —— 传送判定(基于吸完后的当前位置) ——
        double dx = player.getX() - this.p3CenterX;
        double dz = player.getZ() - this.p3CenterZ;
        double r = Math.sqrt(dx * dx + dz * dz);
        if (teleported) {
            if (r > P3_WALL_R) {
                this.p3PlayerPrev.remove(id);   // 传送出去 → 放行，不拉回
            } else {
                this.p3PlayerPrev.put(id, player.position());   // 圈内传送 → 继续跟踪
            }
            return;
        }
        // —— 常规移动：封顶封底 + 硬边界 + 16~20 阻力带 ——
        if (r <= P3_WALL_R && (player.getY() > topY || player.getY() < minY)) {
            this.phasePush(player, player.getX(),
                    Math.max(minY, Math.min(topY, player.getY())), player.getZ());
            dx = player.getX() - this.p3CenterX;
            dz = player.getZ() - this.p3CenterZ;
            r = Math.sqrt(dx * dx + dz * dz);
        }
        if (r > P3_WALL_R) {
            if (!tracked) {
                this.p3PlayerPrev.remove(id);       // 本来就在圈外，没进过领域 → 不管
            } else if (r <= P3_WALL_R + 10.0D) {
                // 常规移动被顶出 20(如爆炸/击飞) → 拉回圈内
                this.phasePush(player,
                        this.p3CenterX + dx / r * (P3_WALL_R - 1.0D),
                        player.getY(),
                        this.p3CenterZ + dz / r * (P3_WALL_R - 1.0D));
                this.p3PlayerPrev.put(id, player.position());
            } else {
                this.p3PlayerPrev.remove(id);       // 常规移动不可能瞬间到 r>30；保险放行
            }
            return;
        }
        // 圈内(r≤20)：记录跟踪
        this.p3PlayerPrev.put(id, player.position());
        // 16~20 阻力带：越深向心推力越强 —— 撞墙手感(进入被顺势吸入，离开被顶回)
        if (r >= P3_INNER_R) {
            double pull = Math.min(0.55D, 0.08D + (r - P3_INNER_R) * 0.25D);
            this.phasePush(player,
                    player.getX() - dx / r * pull,
                    player.getY(),
                    player.getZ() - dz / r * pull);
            this.p3PlayerPrev.put(id, player.position());
        }
    }

    /** 领域墙体/封顶封底(困禁生物专用)：把实体限制在圆柱领域(水平≤20、高度 minY~topY)内。
     *  越出 19.6 硬边界 → 拖回 r=19；16~20 阻力带向心推(撞墙手感)；并封顶封底。
     *  无视当前距离强制生效 —— 锁死生物哪怕被传送/击飞到任何地方都会被拖回。 */
    private void phase3Contain(LivingEntity le) {
        double minY = this.p3BaseY - 0.9D;
        double topY = this.p3TopY();
        double dx = le.getX() - this.p3CenterX;
        double dz = le.getZ() - this.p3CenterZ;
        double r = Math.sqrt(dx * dx + dz * dz);
        double y = le.getY();
        boolean needY = false;
        if (y > topY) {
            y = topY;
            needY = true;
        } else if (y < minY) {
            y = minY;
            needY = true;
        }
        // 硬边界：无论如何不许越过 20 格(传送/击飞/走出都拉回)
        if (r > P3_WALL_R - 0.4D) {
            double nr = P3_WALL_R - 1.0D;
            this.phasePush(le, this.p3CenterX + dx / r * nr, needY ? y : le.getY(),
                    this.p3CenterZ + dz / r * nr);
            dx = le.getX() - this.p3CenterX;
            dz = le.getZ() - this.p3CenterZ;
            r = Math.sqrt(dx * dx + dz * dz);
        } else if (needY) {
            this.phasePush(le, le.getX(), y, le.getZ());
        }
        // 阻力带(16~20)：越深越强的向心推回 —— “撞墙”
        if (r >= P3_INNER_R) {
            double pull = Math.min(0.55D, 0.06D + (r - P3_INNER_R) * 0.22D);
            this.phasePush(le,
                    le.getX() - dx / r * pull,
                    le.getY(),
                    le.getZ() - dz / r * pull);
        }
    }


    /** 玩家走 connection.teleport(强同步，防拉抽回弹)，其余实体 moveTo */
    private void phasePush(Entity entity, double x, double y, double z) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.teleport(x, y, z, serverPlayer.getYRot(), serverPlayer.getXRot());
        } else {
            entity.moveTo(x, y, z, entity.getYRot(), entity.getXRot());
        }
    }

    private void tickPhase3StageMechanics() {
        int stage = this.p3Stage();
        if (stage != this.p3LastStage) {
            this.p3LastStage = stage;
            if (stage == 2) {
                // 33%~0%：停掉前两阶段的流星/火圈/冰火/龙卷，只留腐化光束 + 随机三连
                this.clearAllStageHazards();
                this.p3MixStep = 0;
                this.p3NextMixTick = this.p3Ticks + 20;
            } else if (stage == 1) {
                // 66%~33%：火圈批换成冰之火批；扫描相位重置，从内圈重新开始
                this.p3SweepStartTick = -1;
            }
        }
        if (!this.p3SetupDone) {
            return;
        }
        this.upkeepBolts();
        if (stage == 0) {
            this.tickPhase3Sweep(true);      // 流星批(0/120/240) + 火圈批(60/180/300) 交替
        } else if (stage == 1) {
            this.tickPhase3Sweep(false);     // 流星批 + 冰之火批(代替火圈) 交替
            this.upkeepTornado();            // 中心常驻龙卷（追踪玩家）
        } else {
            this.upkeepStage3RandomWaves();  // 流星/冰火/虚空弹 随机三连（每类 2 颗、间隔 1 秒）
            this.upkeepBeams();              // 腐化光束保留
        }
    }
    /** 132 颗蒸汽弹(SteamMissile,蒸腾聚晶弹射物)「边界墙」：不锁敌、不追人，
     *  每颗独立随机分布在 16~20 环带内(半径16~20随机、高度贴地~领域顶随机、初相位随机)，
     *  每颗再独立随机一个角速度(8°~55°/s)绕领域中心旋转，速度参差交错更生动；
     *  只在敌人穿越边界带时被碰到才受伤(攻伤3+extra2=5)，豁免友方。 */
    private void upkeepBolts() {
        if (this.p3Ticks % 20 == 1) {
            this.sweepStrayBolts();   // 兜底：清掉重载后残留的旧蒸汽弹(不在驱动数组里的)
        }
        for (int i = 0; i < P3_BOLT_COUNT; ++i) {
            SteamMissile b = this.p3Bolts[i];
            if (b == null || !b.isAlive() || b.level() != this.level()) {
                if (b != null && b.isAlive()) b.discard();
                this.p3BoltAngles[i] = this.random.nextDouble() * Math.PI * 2.0D;      // 初相位随机
                this.p3BoltRadii[i] = 16.0D + this.random.nextDouble() * 4.0D;         // 半径 16~20 随机
                this.p3BoltYs[i] = this.p3BaseY + 0.4D
                        + this.random.nextDouble() * (P3_HOVER_RISE + P3_TOP_EXTRA - 0.9D); // 高度 base+0.4 ~ base+12.5 随机
                this.p3BoltOmegas[i] = P3_BOLT_OMEGA_MIN
                        + this.random.nextDouble() * (P3_BOLT_OMEGA_MAX - P3_BOLT_OMEGA_MIN); // 每颗独立角速度 8°~55°/s
                SteamMissile nb = new SteamMissile(this, 0.0D, 0.0D, 0.0D, this.level());
                nb.setExtraDamage(this.attackBasedExtra((float) (double) AcatConfig.SKILL_P3_BOLT.get())); // 每颗命中 <- config [skillDamage].phase3BoltPerHit (default 5)
                double a = this.p3BoltAngles[i];
                double r = this.p3BoltRadii[i];
                nb.setPos(this.p3CenterX + r * Math.sin(a), this.p3BoltYs[i], this.p3CenterZ + r * Math.cos(a));
                this.level().addFreshEntity(nb);
                this.p3Bolts[i] = nb;
                b = nb;
            }
            if (b != null && b.isAlive()) {
                double stepA = this.p3BoltOmegas[i] / 20.0D;   // 本颗角速度：每 tick 转 ω/20 弧度
                this.p3BoltAngles[i] += stepA;
                double a = this.p3BoltAngles[i];
                double r = this.p3BoltRadii[i];
                b.setPos(this.p3CenterX + r * Math.sin(a), this.p3BoltYs[i], this.p3CenterZ + r * Math.cos(a));
                double v = r * stepA;
                b.setDeltaMovement(Math.cos(a) * v, 0.0D, -Math.sin(a) * v);
            }
        }
    }


    /** 100%~33%：规律「下界流星 + 火圈/冰之火」两批交替连发（整批一次性由内向外扫完）。
     *  流星批：0/120/240° 三条辐线【同时】开始，每条线由内圈(r=2)向边界(r=16)
     *          连放 8 颗下界流星，同一条线相邻两颗间隔 0.1 秒(2 tick)，三线同步推进，
     *          约 0.7 秒整批放完；
     *  放完后停火 1 秒(20 tick)；
     *  火圈/冰火批：60/180/300° 三条辐线【同时】开始，同节奏由内向外连放 8 个
     *          火圈(阶段1)/冰之火(阶段2，每朵为十字 4 连)，间隔同为 0.1 秒，放完再停火 1 秒；
     *  两批如此交替循环（辐线归属固定，不交换）。
     *  ★ 旋转：A组(0/120/240° 流星)与 B组(60/180/300° 火圈/冰火)各自独立累计旋转角，
     *    只有某一组三条线完整放完一整轮(每条线由内到外连放 8 个)后，该组才整体固定旋转 20°，
     *    扫射过程中角度保持不变(弹道笔直、不随单发偏移)；子阶段切换(100-66%→66-33%)不归零。 */
    private void tickPhase3Sweep(boolean fireRingBatch) {
        int layers = (int) Math.round((P3_SWEEP_R_MAX - P3_SWEEP_R0) / P3_SWEEP_R_STEP) + 1;  // r=2~16 共 8 个
        int waveTicks = layers * P3_SWEEP_INTERVAL_TICKS;      // 单批连放耗时：0.1s×8 = 0.7s(16 tick)
        int roundLen = waveTicks * 2 + P3_SWEEP_GAP_TICKS * 2; // 流星批 + 1s + 火圈/冰火批 + 1s
        if (this.p3SweepStartTick < 0) {
            this.p3SweepStartTick = this.p3Ticks;
        }
        int elapsed = this.p3Ticks - this.p3SweepStartTick;
        if (elapsed >= roundLen) {
            this.p3SweepStartTick += roundLen;
            elapsed -= roundLen;
        }
        int secondWaveStart = waveTicks + P3_SWEEP_GAP_TICKS;  // 火圈/冰火批起点：流星批放完再等 1 秒
        boolean inMeteorWave = elapsed < waveTicks;
        boolean inSecondWave = elapsed >= secondWaveStart && elapsed < secondWaveStart + waveTicks;
        if (!inMeteorWave && !inSecondWave) {
            return;                                            // 两批之间的 1 秒停火期
        }
        if (elapsed % P3_SWEEP_INTERVAL_TICKS != 0) {
            return;                                            // 只在对齐 0.1s 的 tick 开火
        }
        int layer = inMeteorWave
                ? elapsed / P3_SWEEP_INTERVAL_TICKS
                : (elapsed - secondWaveStart) / P3_SWEEP_INTERVAL_TICKS;
        double r = P3_SWEEP_R0 + layer * P3_SWEEP_R_STEP;
        if (inMeteorWave) {
            // 0/120/240° 三线(带 A 组旋转角)同时开火：本层(半径 r)三颗一起落
            this.spawnSweepMeteor(0, this.p3SweepRotA, r);
            this.spawnSweepMeteor(2, this.p3SweepRotA, r);
            this.spawnSweepMeteor(4, this.p3SweepRotA, r);
            if (layer == layers - 1) {
                // 一整轮放完(每条线由内到外 8 个全放完) → A 组三线才整体固定旋转 20°，扫射中角度不变
                this.p3SweepRotA = (this.p3SweepRotA + P3_SWEEP_ROT_DEG) % 360.0D;
            }
        } else if (fireRingBatch) {
            // 60/180/300° 三线(带 B 组旋转角)同时放火圈
            this.spawnSweepFireRing(1, this.p3SweepRotB, r);
            this.spawnSweepFireRing(3, this.p3SweepRotB, r);
            this.spawnSweepFireRing(5, this.p3SweepRotB, r);
            if (layer == layers - 1) {
                // 一整轮放完 → B 组三线才整体固定旋转 20°
                this.p3SweepRotB = (this.p3SweepRotB + P3_SWEEP_ROT_DEG) % 360.0D;
            }
        } else {
            // 60/180/300° 三线(带 B 组旋转角)同时放冰之火(阶段2，每朵十字 4 连)
            this.spawnSweepIceFire(1, this.p3SweepRotB, r);
            this.spawnSweepIceFire(3, this.p3SweepRotB, r);
            this.spawnSweepIceFire(5, this.p3SweepRotB, r);
            if (layer == layers - 1) {
                // 一整轮放完 → B 组三线才整体固定旋转 20°
                this.p3SweepRotB = (this.p3SweepRotB + P3_SWEEP_ROT_DEG) % 360.0D;
            }
        }
    }
    private void spawnSweepMeteor(int spoke, double rotDeg, double r) {
        double a = Math.toRadians(spoke * 60.0D + rotDeg);
        double tx = this.p3CenterX + Math.sin(a) * r;
        double tz = this.p3CenterZ + Math.cos(a) * r;
        double ty = this.groundYAt(tx, tz);
        if (ty < this.p3BaseY - 2.0D) return;
        double sy = ty + 26.0D + this.random.nextInt(14);
        HellBlast meteor = new HellBlast(tx, sy, tz, 0.0D, ty - sy, 0.0D, this.level());
        meteor.setOwner(this);
        meteor.setDamage((float) (double) AcatConfig.SKILL_P3_METEOR.get()); // 命中伤害 <- config [skillDamage].phase3MeteorPerHit (default 10)

        meteor.setFiery(1);
        this.level().addFreshEntity(meteor);
        this.p3Spawns.add(meteor);
    }
    private void spawnSweepFireRing(int spoke, double rotDeg, double r) {
        double a = Math.toRadians(spoke * 60.0D + rotDeg);
        double x = this.p3CenterX + Math.sin(a) * r;
        double z = this.p3CenterZ + Math.cos(a) * r;
        double gy = this.groundYAt(x, z);
        if (gy < this.p3BaseY - 2.0D) return;
        FireBlastTrap trap = new FireBlastTrap(this.level(), x, gy + 0.05D, z);
        trap.setOwner(this);
        trap.setAreaOfEffect(2.0F);
        trap.setExtraDamage(this.attackBasedExtra((float) (double) AcatConfig.SKILL_P3_FIRE_RING.get())); // 每次伤害 <- config [skillDamage].phase3FireRingPerTick (default 10)
        trap.setBurning(1);
        this.level().addFreshEntity(trap);
        this.p3Spawns.add(trap);
    }
    /** 阶段2(66%~33%)：在 60/180/300° 三根辐线的原火圈位置放冰之火。
     *  每打 1 次改为一次出 4 朵十字型：以落点为中心，东西南北各偏移 1.5 格各放 1 朵。 */
    private void spawnSweepIceFire(int spoke, double rotDeg, double r) {
        double a = Math.toRadians(spoke * 60.0D + rotDeg);
        double cx = this.p3CenterX + Math.sin(a) * r;
        double cz = this.p3CenterZ + Math.cos(a) * r;
        double gy = this.groundYAt(cx, cz);
        if (gy < this.p3BaseY - 2.0D) gy = this.p3BaseY;
        // ★ 十字型 4 朵：中心点不放，东/西/南/北各 1.5 格各 1 朵
        this.spawnIceBouquetAt(cx, cz, gy, 1.5D, 0.0D, false);
        this.spawnIceBouquetAt(cx, cz, gy, -1.5D, 0.0D, false);
        this.spawnIceBouquetAt(cx, cz, gy, 0.0D, 1.5D, false);
        this.spawnIceBouquetAt(cx, cz, gy, 0.0D, -1.5D, false);
    }
    private void spawnIceBouquetAt(double cx, double cz, double gy, double xshift, double zshift, boolean center) {
        IceBouquet ice = new IceBouquet(this.level(), cx + xshift, gy + 0.5D, cz + zshift, this);
        ice.setExtraDamage(5.0F);
        ice.addLifeSpan(120);
        ice.setCenter(center);
        MobUtil.moveDownToGround(ice);
        this.level().addFreshEntity(ice);
        this.p3Spawns.add(ice);
    }

    /** 66%~33%：领域中心常驻一个追踪玩家的火焰龙卷，消失即补 */
    private void upkeepTornado() {
        boolean any = false;
        for (FireTornado ft : this.level().getEntitiesOfClass(FireTornado.class,
                this.getBoundingBox().inflate(40.0D))) {
            if (ft.getTrueOwner() == this) {
                any = true;
                break;
            }
        }
        if (any) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        double dx = target.getX() - this.p3CenterX;
        double dy = target.getY() - this.p3BaseY;
        double dz = target.getZ() - this.p3CenterZ;
        FireTornado tornado = new FireTornado(this.level(), this, dx, dy, dz);
        tornado.setOwnerId(this.getUUID());
        tornado.setTotalLife(1200);
        tornado.setDamage((float) (double) (AcatConfig.SKILL_P3_TORNADO.get() - 4.0D)); // 每次伤害(最终=4+此值) <- config [skillDamage].phase3TornadoPerTick (default 6)
        tornado.setPos(this.p3CenterX, this.p3BaseY + 1.0D, this.p3CenterZ);
        this.level().addFreshEntity(tornado);
        this.p3Spawns.add(tornado);
    }

    /** 33%~0%：随机三连 —— 依次出 下界流星 → 冰之火 → 虚空震荡弹(不分裂)，
     *  每种间隔 1 秒(20 tick)，每种 2 个：1 个领域内随机落点、1 个瞄准玩家当前位置
     *  (无玩家可瞄时两个都随机)。落点范围为领域内半径 2~14 格。 */
    private void upkeepStage3RandomWaves() {
        if (this.p3Ticks < this.p3NextMixTick) return;
        this.p3NextMixTick = this.p3Ticks + 20;
        Vec3 randomPos = this.randomDomainPoint();
        Vec3 aimPos = this.phase3AimPoint();
        Vec3 second = aimPos != null ? aimPos : this.randomDomainPoint();
        switch (this.p3MixStep) {
            case 0 -> {        // 下界流星 ×2（天上随机掉 + 1 颗瞄玩家）
                this.spawnStage3Meteor(randomPos.x, randomPos.z);
                this.spawnStage3Meteor(second.x, second.z);
            }
            case 1 -> {        // 冰之火 ×2（地上随机长 + 1 朵瞄玩家）
                this.spawnStage3Ice(randomPos.x, randomPos.z);
                this.spawnStage3Ice(second.x, second.z);
            }
            default -> {       // 虚空震荡弹 ×2（不分裂追踪小弹）
                this.spawnStage3VoidBomb(randomPos.x, randomPos.z);
                this.spawnStage3VoidBomb(second.x, second.z);
            }
        }
        this.p3MixStep = (this.p3MixStep + 1) % 3;
    }

    /** 领域内随机落点(半径 2~14 格)，返回 (x, 地面y, z) */
    private Vec3 randomDomainPoint() {
        double a = this.random.nextDouble() * Math.PI * 2.0D;
        double r = 2.0D + this.random.nextDouble() * 12.0D;
        double x = this.p3CenterX + Math.sin(a) * r;
        double z = this.p3CenterZ + Math.cos(a) * r;
        return new Vec3(x, this.groundYAt(x, z), z);
    }

    /** 瞄准点：当前锁定目标(通常是领域内玩家)的当前位置；取不到则返回 null */
    private Vec3 phase3AimPoint() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && !target.isRemoved() && !target.isSpectator()) {
            double gy = this.groundYAt(target.getX(), target.getZ());
            return new Vec3(target.getX(), gy, target.getZ());
        }
        return null;
    }

    /** 从天上向 (tx,tz) 落 1 颗下界流星 */
    private void spawnStage3Meteor(double tx, double tz) {
        double gy = this.groundYAt(tx, tz);
        if (gy < this.p3BaseY - 2.0D) gy = this.p3BaseY;
        double sy = gy + 26.0D + this.random.nextInt(14);
        HellBlast meteor = new HellBlast(tx, sy, tz, 0.0D, gy - sy, 0.0D, this.level());
        meteor.setOwner(this);
        meteor.setDamage((float) (double) AcatConfig.SKILL_P3_METEOR.get()); // 命中伤害 <- config [skillDamage].phase3MeteorPerHit (default 10)

        meteor.setFiery(1);
        this.level().addFreshEntity(meteor);
        this.p3Spawns.add(meteor);
    }

    /** 在 (tx,tz) 地面长 1 朵冰之火 */
    private void spawnStage3Ice(double tx, double tz) {
        double gy = this.groundYAt(tx, tz);
        if (gy < this.p3BaseY - 2.0D) gy = this.p3BaseY;
        this.spawnIceBouquetAt(tx, tz, gy, 0.0D, 0.0D, true);
    }

    /** 从天上向 (tx,tz) 掉 1 颗虚空震荡弹（YaVoidShockBomb：继承魔焰扩散、不分裂追踪小震荡弹） */
    private void spawnStage3VoidBomb(double tx, double tz) {
        double gy = this.groundYAt(tx, tz);
        if (gy < this.p3BaseY - 2.0D) gy = this.p3BaseY;
        double sy = gy + 26.0D + this.random.nextInt(14);
        YaVoidShockBomb bomb = new YaVoidShockBomb(this, this.level());
        bomb.setBaseDamage((float) (double) AcatConfig.SKILL_P3_VOID_BOMB.get()); // 爆炸伤害 <- config [skillDamage].phase3VoidBombPerBoom (default 7)
        bomb.setPos(tx, sy, tz);
        bomb.setDeltaMovement(0.0D, -0.4D, 0.0D);
        this.level().addFreshEntity(bomb);
        this.p3Spawns.add(bomb);
    }
    /** 33%~0%：6 个隐形锚点(钉在猫 xz、y=锁定敌人当前高度)各发一条腐化光束，60° 间隔 60°/s 旋转扫射；无敌人则扫领域地面。
     *  锚点/光束按「归属猫 UUID」管理：每次先清理领域中心附近的残留旧实体(世界重载/玩家重进后
     *  旧锚点+旧光束仍原样载入而猫的数组是空的)，再按槽补齐缺失 —— 不会重复生成，旧的也不会永久存在。 */
    private void upkeepBeams() {
        // 光束高度 = 锁定敌人当前高度(取身体中段)，实时跟随敌人走，不再固定在玩家高度
        LivingEntity target = this.getTarget();
        double beamY;
        if (target != null && target.isAlive() && !target.isRemoved() && !target.isSpectator()) {
            beamY = Mth.clamp(target.getY() + target.getBbHeight() * 0.5D,
                    this.p3BaseY + 0.2D, this.p3TopY() - 0.5D);
        } else {
            // 没有敌人：默认扫亚小猫下方 10 格(领域地面)，略抬 0.5 格避免贴着地面方块被立刻截断
            beamY = this.p3BaseY + 0.5D;
        }
        // 先清残留：只保留数组里正在驱动的锚点；世界重载后数组为空 → 旧锚点/光束一并清掉再按槽补齐
        this.sweepStrayBeams();
        for (int i = 0; i < P3_BEAM_COUNT; ++i) {
            BeamAnchorEntity anchor = this.p3Anchors[i];
            boolean needAnchor = anchor == null || !anchor.isAlive() || anchor.level() != this.level()
                    || !this.getUUID().equals(anchor.getOwnerUuid());
            if (needAnchor) {
                if (anchor != null && anchor.isAlive()) anchor.discard();
                if (this.p3Beams[i] != null && this.p3Beams[i].isAlive()) this.p3Beams[i].discard();
                this.p3Beams[i] = null;
                anchor = new BeamAnchorEntity(AcatEntities.BEAM_ANCHOR.get(), this.level());
                anchor.moveTo(this.p3CenterX, beamY, this.p3CenterZ, 0.0F, 0.0F);
                anchor.bindOwner(this.getUUID());
                this.level().addFreshEntity(anchor);
                this.p3Anchors[i] = anchor;
                CorruptedBeam beam = new CorruptedBeam(ModEntityType.CORRUPTED_BEAM.get(), this.level(), anchor);
                beam.setExtraDamage(this.corruptBeamExtra()); // 每次命中 <- config [skillDamage].corruptBeamPerTick (default 10)

                beam.setItemBase(false);
                this.level().addFreshEntity(beam);
                this.p3Beams[i] = beam;
            } else {
                // 锚点还在：光束若丢了就补一条(先找 owner=锚点的旧光束复用，不重复生成)
                if (this.p3Beams[i] == null || !this.p3Beams[i].isAlive()
                        || this.p3Beams[i].level() != this.level()
                        || this.p3Beams[i].getOwner() != anchor) {
                    if (this.p3Beams[i] != null && this.p3Beams[i].isAlive()) this.p3Beams[i].discard();
                    CorruptedBeam beam = null;
                    for (CorruptedBeam cb : this.level().getEntitiesOfClass(CorruptedBeam.class,
                            this.getBoundingBox().inflate(8.0D))) {
                        if (cb.getOwner() == anchor) {
                            beam = cb;
                            break;
                        }
                    }
                    if (beam == null || !beam.isAlive()) {
                        beam = new CorruptedBeam(ModEntityType.CORRUPTED_BEAM.get(), this.level(), anchor);
                        beam.setExtraDamage(this.corruptBeamExtra()); // 每次命中 <- config [skillDamage].corruptBeamPerTick (default 10)

                        beam.setItemBase(false);
                        this.level().addFreshEntity(beam);
                    }
                    this.p3Beams[i] = beam;
                }
            }
            anchor = this.p3Anchors[i];
            if (anchor == null || !anchor.isAlive()) continue;
            anchor.setPos(this.p3CenterX, beamY, this.p3CenterZ);   // 锚点实时钉在目标中段高度
            // 腐化光束：6 条一律水平旋转扫射(转速已减半为 30°/s)，高度钉在锁定敌人当前高度(身体中段)
            float yaw = (float) (i * 60.0D + this.p3Ticks * P3_BEAM_DEG_PER_TICK);
            anchor.setYRot(yaw);
            anchor.setXRot(0.0F);
            anchor.yBodyRot = yaw;
            anchor.yHeadRot = yaw;
            anchor.setDeltaMovement(0.0D, 0.0D, 0.0D);

        }
    }

    /** 清掉领域中心附近「未被 p3Anchors 数组驱动」的残留锚点与它们的光束。
     *  玩家退世界重进(存档重载)时，旧锚点+旧光束实体仍会原样载入、而猫的数组是空的 ——
     *  若不清掉就会补生成新的一套、旧的变永久存在。保留：属于本猫且在数组里的锚点；
     *  其余(含旧版无主锚点)连同其光束一并清除。别的亚小猫绑定的锚点不受影响。 */
    private void sweepStrayBeams() {
        if (this.level().isClientSide) return;
        AABB box = new AABB(this.p3CenterX - 10.0D, this.p3BaseY - 3.0D, this.p3CenterZ - 10.0D,
                this.p3CenterX + 10.0D, this.p3TopY() + 3.0D, this.p3CenterZ + 10.0D);
        Set<UUID> kept = new LinkedHashSet<>();
        for (int i = 0; i < P3_BEAM_COUNT; ++i) {
            BeamAnchorEntity a = this.p3Anchors[i];
            if (a != null && a.isAlive()) kept.add(a.getUUID());
        }
        List<BeamAnchorEntity> toKill = new ArrayList<>();
        for (BeamAnchorEntity a : this.level().getEntitiesOfClass(BeamAnchorEntity.class, box)) {
            if (kept.contains(a.getUUID())) continue;
            UUID owner = a.getOwnerUuid();
            if (owner != null && !owner.equals(this.getUUID())) continue;   // 别的亚小猫的锚点
            toKill.add(a);
        }
        if (toKill.isEmpty()) return;
        // 随附光束(owner=锚点)一并清，避免残留扫射
        for (CorruptedBeam cb : this.level().getEntitiesOfClass(CorruptedBeam.class, box)) {
            if (cb.getOwner() instanceof BeamAnchorEntity ba && toKill.contains(ba)) {
                cb.discard();
            }
        }
        for (BeamAnchorEntity a : toKill) {
            if (a.isAlive()) a.discard();
        }
    }


    /** 取 (x,z) 处地面高度(用于火圈/冰/流星落点) */
    private double groundYAt(double x, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x,
                this.p3BaseY + P3_HOVER_RISE + P3_TOP_EXTRA + 4.0D, z);
        while (pos.getY() > this.level().getMinBuildHeight() + 2
                && pos.getY() > this.p3BaseY - 10.0D) {
            if (this.level().getBlockState(pos).blocksMotion()) {
                return pos.getY() + 1.0D;
            }
            pos.move(Direction.DOWN);
        }
        return this.p3BaseY;
    }

    /** 阶段切换\死亡时清掉所有领域生成物 */
    private void clearAllStageHazards() {
        for (Entity e : this.p3Spawns) {
            if (e != null && e.isAlive()) e.discard();
        }
        this.p3Spawns.clear();
    }

    // ==================== 重载遗留清扫 ====================

    /** 领域清扫盒：以 p3Center 为中心、覆盖领域柱体及上下余量 */
    private AABB p3SweepBox(double padXZ, double padDown, double padUp) {
        double minY = this.p3BaseY - padDown;
        double topY = this.p3TopY() + padUp;
        return new AABB(this.p3CenterX - padXZ, minY, this.p3CenterZ - padXZ,
                this.p3CenterX + padXZ, topY, this.p3CenterZ + padXZ);
    }

    /** 清扫残留蒸汽弹：重载后旧弹不在 p3Bolts 数组里(数组是瞬态字段不存档)，
     *  只认「领域圆柱内的 SteamMissile 且 归属=本猫 或 无主(重载丢失归属)」——
     *  正在被数组驱动的 132 颗受 tracked 集合保护，绝不会误删。 */
    private void sweepStrayBolts() {
        if (this.level().isClientSide) return;
        Set<SteamMissile> tracked = new LinkedHashSet<>();
        for (int i = 0; i < P3_BOLT_COUNT; ++i) {
            SteamMissile b = this.p3Bolts[i];
            if (b != null && b.isAlive()) tracked.add(b);
        }
        for (SteamMissile sm : this.level().getEntitiesOfClass(SteamMissile.class,
                this.p3SweepBox(P3_WALL_R + 4.0D, 4.0D, 4.0D))) {
            if (!sm.isAlive() || tracked.contains(sm)) continue;
            Entity owner = sm.getOwner();
            if (owner != this && owner != null) continue;   // 别人的弹(有主且非本猫)不动
            sm.discard();
        }
    }

    /** 清扫残留领域实体：光环实体(领域中心附近全部——本猫只会在自家中心放一个)、
     *  以及 火圈/冰火/震荡弹/龙卷陷阱 中归属=本猫 的残留(重载遗留)。 */
    private void sweepStrayDomainEntities() {
        if (this.level().isClientSide) return;
        for (ApollyonCatDomainRingEntity ring : this.level().getEntitiesOfClass(ApollyonCatDomainRingEntity.class,
                new AABB(this.p3CenterX - 3.0D, this.p3BaseY - 2.0D, this.p3CenterZ - 3.0D,
                        this.p3CenterX + 3.0D, this.p3BaseY + 10.0D, this.p3CenterZ + 3.0D))) {
            if (ring.isAlive()) ring.discard();
        }
        AABB big = this.p3SweepBox(48.0D, 12.0D, 24.0D);
        for (FireBlastTrap t : this.level().getEntitiesOfClass(FireBlastTrap.class, big)) {
            if (t.isAlive() && t.getOwner() == this) t.discard();
        }
        for (IceBouquet ic : this.level().getEntitiesOfClass(IceBouquet.class, big)) {
            if (ic.isAlive() && ic.getOwner() == this) ic.discard();
        }
        for (VoidShockBomb vb : this.level().getEntitiesOfClass(VoidShockBomb.class, big)) {
            if (vb.isAlive() && vb.getOwner() == this) vb.discard();
        }
        for (FireTornadoTrap tt : this.level().getEntitiesOfClass(FireTornadoTrap.class, big)) {
            if (tt.isAlive() && tt.getOwner() == this) tt.discard();
        }
    }


    /** 亚小猫死亡\领域结束：蒸汽弹、光环、锚点光束、龙卷、全部生成物一并清场 */
    private void cleanupPhase3Domain() {
        for (int i = 0; i < P3_BOLT_COUNT; ++i) {
            SteamMissile b = this.p3Bolts[i];
            if (b != null && b.isAlive()) b.discard();
            this.p3Bolts[i] = null;
        }
        for (int i = 0; i < P3_BEAM_COUNT; ++i) {
            if (this.p3Beams[i] != null && this.p3Beams[i].isAlive()) this.p3Beams[i].discard();
            if (this.p3Anchors[i] != null && this.p3Anchors[i].isAlive()) this.p3Anchors[i].discard();
            this.p3Beams[i] = null;
            this.p3Anchors[i] = null;
        }
        this.sweepStrayBeams();   // 领域结束：清掉非数组记录的残留锚点/光束(存档重载遗留的旧实体)
        if (this.p3Ring != null && this.p3Ring.isAlive()) this.p3Ring.discard();
        this.p3Ring = null;
        for (FireTornado ft : this.level().getEntitiesOfClass(FireTornado.class,
                this.getBoundingBox().inflate(48.0D))) {
            if (ft.getTrueOwner() == this) ft.discard();
        }
        this.clearAllStageHazards();
        this.p3LockedIds.clear();      // 领域结束：困禁登记全部解锁(不再拉回)
        this.p3PlayerPrev.clear();      // 玩家跟踪一并清空
        // ★ 兜底：存档重载后跟踪数组/字段为空而旧实体仍残留 —— 领域范围 + 归属全量清扫。
        //   只对经历过三阶段(thirdPhase)的猫执行，避免无关实体被误清。
        if (this.thirdPhase) {
            this.sweepStrayBolts();
            this.sweepStrayDomainEntities();
        }
    }

    /** 三阶段开始前：清掉二阶段遗留的巨柱\火雨云\龙卷陷阱\龙卷 */
    private void discardOwnedOldSpawns() {
        for (AbstractObsidianMonolith m : this.level().getEntitiesOfClass(AbstractObsidianMonolith.class,
                this.getBoundingBox().inflate(80.0D))) {
            if (m.getTrueOwner() == this) m.discard();
        }
        for (HellCloud c : this.level().getEntitiesOfClass(HellCloud.class,
                this.getBoundingBox().inflate(80.0D))) {
            if (c.getOwner() == this) c.discard();
        }
        for (FireTornadoTrap t : this.level().getEntitiesOfClass(FireTornadoTrap.class,
                this.getBoundingBox().inflate(80.0D))) {
            if (t.getOwner() == this) t.discard();
        }
        for (FireTornado ft : this.level().getEntitiesOfClass(FireTornado.class,
                this.getBoundingBox().inflate(80.0D))) {
            if (ft.getTrueOwner() == this) ft.discard();
        }
    }

    /** 三阶段：无敌（仅放行 BYPASSES 来源 = 内部扣血 / kill）；
     *  风暴类(shock)与霜冻类(freeze)聚晶攻击对亚小猫造成 200% 伤害。 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.phase3Active && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (ModDamageSource.shockAttacks(source) || ModDamageSource.freezeAttacks(source)) {
            amount *= 2.0F;
        }
        return this.apostleHurt(source, amount);
    }

    @Override
    protected float getSoundVolume() {
        return this.phase3Active ? 0.0F : super.getSoundVolume();
    }

    /** 经验：默认流程归零，三阶段死亡时在掉落阶段手动发 3333。
     *  1.21 起 LivingEntity#getExperienceReward(ServerLevel, Entity) 为 final，
     *  可覆写的钩子改为 getBaseExperienceReward()，语义不变(恒返回 0)。 */
    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    /** 三阶段死亡瞬间(第一次 die，即血量归零那一刻)定格战利品判定：
     *  当时领域内还有玩家 → 亚小猫专属掉落；没有 → 改用下界使徒掉落。
     *  使徒基类有“华丽死亡”二段流程(tickDeath 中会再次调用 die)，这里只判定一次，
     *  避免死亡动画结束后(玩家已离开/领域已清场)把结果重算成错误分支。 */
    @Override
    public void die(DamageSource pCause) {
        if (this.thirdPhase && !this.p3DeathChecked && !this.level().isClientSide) {
            this.p3DeathChecked = true;
            this.p3CustomDrops = this.isAnyPlayerInsideDomain();
        }
        this.apostleDie(pCause);
    }

    /** 此刻是否还有玩家停留在领域内(水平 r≤P3_WALL_R 且高度落在领域柱体内)。
     *  旁观者/已移除/已离开本维度的玩家不计；死亡但尚未复活的玩家(尸体还在原地)计入。 */
    private boolean isAnyPlayerInsideDomain() {
        double minY = this.p3BaseY - 0.9D;
        double topY = this.p3TopY();
        AABB box = new AABB(this.p3CenterX - P3_SUCK_RANGE, minY - 8.0D, this.p3CenterZ - P3_SUCK_RANGE,
                this.p3CenterX + P3_SUCK_RANGE, topY + 8.0D, this.p3CenterZ + P3_SUCK_RANGE);
        for (Player player : this.level().getEntitiesOfClass(Player.class, box)) {
            if (player.isSpectator() || player.isRemoved() || player.level() != this.level()) continue;
            double dx = player.getX() - this.p3CenterX;
            double dz = player.getZ() - this.p3CenterZ;
            if (Math.sqrt(dx * dx + dz * dz) > P3_WALL_R) continue;
            double y = player.getY();
            if (y >= minY && y <= topY) {
                return true;
            }
        }
        return false;
    }

    /** 三阶段战利品：
     *  死亡瞬间领域内有玩家 → 不吃使徒默认 APOSTLE_HARD 战利品表(留空)，专属 4 样走 dropCustomDeathLoot；
     *  死亡瞬间领域内没有玩家 → 改用下界使徒的掉落(APOSTLE_HARD 战利品表 + 使徒自定义掉落)。
     *  1.21 起战利品表改为 ResourceKey<LootTable>。 */
    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        if (this.thirdPhase && this.p3CustomDrops) {
            return BuiltInLootTables.EMPTY;
        }
        return this.apostleLootTable();
    }

    /** 掉落：领域内有玩家 = 1 下界合金块 + 6 至纯之血 + 3 暗夜信标 + 1 黑暗金属块
     *  + 3 附魔金苹果 + 33 生鲑鱼 + 33 生鳕鱼 + 1 红色羊毛，经验 3333；
     *  领域内没有玩家 = 不额外掉落，交给下界使徒的 APOSTLE_HARD 战利品表与 super 自定义掉落。
     *  1.21 起 dropCustomDeathLoot 首参改为 ServerLevel、去掉 pLooting。 */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource pSource, boolean pRecentlyHit) {
        if (this.thirdPhase && this.p3CustomDrops) {
            this.spawnBossDrop(new ItemStack(Items.NETHERITE_BLOCK));
            ItemStack pure = new ItemStack(ModItems.UNHOLY_BLOOD.get(), 6);
            UnholyBloodItem.addPure(pure);
            this.spawnBossDrop(pure);
            this.spawnBossDrop(new ItemStack(ModBlocks.NIGHT_BEACON.get().asItem(), 3));
            this.spawnBossDrop(new ItemStack(ModBlocks.DARK_ALLOY_BLOCK.get().asItem()));
            this.spawnBossDrop(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 3));   // 3 附魔金苹果
            this.spawnBossDrop(new ItemStack(Items.SALMON, 33));                  // 33 生鲑鱼
            this.spawnBossDrop(new ItemStack(Items.COD, 33));                     // 33 生鳕鱼
            this.spawnBossDrop(new ItemStack(Items.RED_WOOL));                    // 1 红色羊毛
            ExperienceOrb.award(level, this.position(), 3333);
            return;
        }
        this.apostleDropCustomDeathLoot(level, pSource, pRecentlyHit);
    }

    private void spawnBossDrop(ItemStack stack) {
        ItemEntity itemEntity = this.spawnAtLocation(stack);
        if (itemEntity != null) {
            itemEntity.setExtendedLifetime();
        }
    }

    @Override
    public void heal(float amount) {
        if (this.phase3Active) {
            return;      // 三阶段不允许任何回血
        }
        this.apostleHeal(amount);
    }


    /* =====================================================================
     * ★ 底座：Apostle / SpellCastingCultist 被动机理的本地等价实现
     * ---------------------------------------------------------------------
     * 原 Boss 继承 Apostle 时免费获得：二阶段仪式、回血、卡墙/受击传送、
     * 二阶段天候与流星轰炸、灼烧、煮沸水、转化村民、禁飞、施法站姿与烟雾……
     * 仆从版改挂 Summoned 后，这些全部在本类自建，数值/节奏与 Apostle 版一致，
     * 保证仆从版与 Boss 版行为一致。仆从系统的数据/跟随/还击/索敌由
     * Summoned(Owned) 免费提供，此处不再重复实现。
     * ===================================================================== */

    // ---------- Apostle 状态字段 ----------
    public int hitTimes;
    public int coolDown;
    public int tornadoCoolDown;
    public int infernoCoolDown;
    public int monolithCoolDown;
    public int damnedCoolDown;
    public int spellCycle;
    public int stuckTime;
    public int lastKilledPlayer;
    public Vec3 prevVecPos;
    public boolean roarParticles;
    public boolean fireArrows;
    public boolean regen;
    public boolean killedPlayer;
    public Holder<MobEffect> arrowEffect;
    public int antiRegen;
    public int antiRegenTotal;
    public int deathTime = 0;
    public int moddedInvul = 0;
    public int obsidianInvul = 0;
    public int toTeleportTime = 0;
    public double prevX;
    public double prevY;
    public double prevZ;
    public RandomSource apostleRandom = RandomSource.create();
    public Vec3 toTeleportPos = null;
    public DamageSource deathBlow = this.damageSources().generic();
    public CompletableFuture<Vec3> pendingTeleportSearch = null;
    public final Predicate<Entity> ALIVE = Entity::isAlive;

    // 1.21 起 AttributeModifier 改用 ResourceLocation 作 id(不再有 UUID/名字参数)，与 Goety-3 Apostle 同款命名
    public static final ResourceLocation SPEED_MODIFIER_CASTING_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "apostle_casting_speed_penalty");
    public static final AttributeModifier SPEED_MODIFIER_CASTING =
            new AttributeModifier(SPEED_MODIFIER_CASTING_ID, -1.0D, AttributeModifier.Operation.ADD_VALUE);
    public static final ResourceLocation SPEED_MODIFIER_MONOLITH_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "apostle_monoliths_speed_penalty");
    public static final AttributeModifier SPEED_MODIFIER_MONOLITH =
            new AttributeModifier(SPEED_MODIFIER_MONOLITH_ID, -0.25D, AttributeModifier.Operation.ADD_VALUE);
    public static final ResourceLocation WEAK_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "apostle_weaker_armor_out_of_nether");
    public static final AttributeModifier WEAK_ARMOR_MODIFIER =
            new AttributeModifier(WEAK_ARMOR_ID, -0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    // ---------- 阶段 / 施法标志 ----------
    private boolean secondPhaseFlag;
    private boolean settingUpSecondFlag;
    /** ★ 转阶段仪式(二阶段 / 锁血那次)的卡死检测状态 —— 仅用于"仪式卡死"兜底。 */
    private int setupSecondTicks;
    private float setupPrevHealth = -1.0F;
    private int setupNoHealTicks;
    /** 仪式兜底阈值：正常仪式(从 1 点血回满)约需 16 秒(320 tick)，超过这个时长必定是卡住了。 */
    private static final int SETUP_WATCHDOG_TICKS = 600;
    private boolean castingFlag;
    private boolean monolithPowerFlag;
    protected static final EntityDataAccessor<Byte> SPELL_DATA =
            SynchedEntityData.defineId(ApollyonCatFightEntity.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Boolean> SECOND_SETUP =
            SynchedEntityData.defineId(ApollyonCatFightEntity.class, EntityDataSerializers.BOOLEAN);
    protected int spellTicks;
    private SpellCastingCultist.SpellType activeSpell = SpellCastingCultist.SpellType.NONE;

    // ---------- 客户端射击指示器同步(供被动复刻，猫渲染器不画，仅保持一致) ----------
    protected static final EntityDataAccessor<Vector3f> SHOOT_INDICATOR_END =
            SynchedEntityData.defineId(ApollyonCatFightEntity.class, EntityDataSerializers.VECTOR3);
    protected static final EntityDataAccessor<Float> SHOOT_INDICATOR_PROGRESS =
            SynchedEntityData.defineId(ApollyonCatFightEntity.class, EntityDataSerializers.FLOAT);
    public float clientShootIndicatorProgress, oClientShootIndicatorProgress;
    public Vec3 clientShootIndicatorEnd = Vec3.ZERO, oClientShootIndicatorEnd = Vec3.ZERO;

    // ================= 阶段 / 施法访问器 =================

    public void setSecondPhaseInternal(boolean secondPhase) {
        this.secondPhaseFlag = secondPhase;
    }

    public boolean isSecondPhase() {
        return this.secondPhaseFlag;
    }

    public void setSettingUpSecond(boolean settingUpSecond) {
        this.settingUpSecondFlag = settingUpSecond;
        this.entityData.set(SECOND_SETUP, settingUpSecond);
    }

    public boolean isSettingUpSecond() {
        return this.level().isClientSide ? this.entityData.get(SECOND_SETUP) : this.settingUpSecondFlag;
    }

    public void setCasting(boolean casting) {
        this.castingFlag = casting;
    }

    public boolean isCasting() {
        return this.castingFlag;
    }

    public void setMonolithPower(boolean monolithPower) {
        this.monolithPowerFlag = monolithPower;
    }

    public boolean isMonolithPower() {
        return this.monolithPowerFlag;
    }

    public boolean isSpellcasting() {
        return this.level().isClientSide ? this.entityData.get(SPELL_DATA) > 0 : this.spellTicks > 0;
    }

    public void setSpellType(SpellCastingCultist.SpellType spellType) {
        this.activeSpell = spellType;
        this.entityData.set(SPELL_DATA, (byte) spellType.ordinal());   // SpellType.id 在别类为 private，改用 ordinal(id 0..7 与 ordinal 一致)
    }

    protected SpellCastingCultist.SpellType getSpellType() {
        return !this.level().isClientSide ? this.activeSpell
                : SpellCastingCultist.SpellType.getFromId(this.entityData.get(SPELL_DATA));
    }

    protected int getSpellTicks() {
        return this.spellTicks;
    }

    protected SoundEvent getCastingSoundEvent() {
        return ModSounds.APOSTLE_CAST_SPELL.get();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.spellTicks > 0) {
            --this.spellTicks;
        }
    }

    // ================= 被动：回血 / 命中 / 冷却 访问器(Apostle 同名语义) =================

    public void setRegen(boolean regen) {
        this.regen = regen;
    }

    public boolean Regen() {
        return this.regen;
    }

    public void setFireArrow(boolean fireArrow) {
        this.fireArrows = fireArrow;
    }

    public boolean getFireArrow() {
        return this.fireArrows;
    }

    public void setArrowEffect(Holder<MobEffect> effect) {
        this.arrowEffect = effect;
    }

    public Holder<MobEffect> getArrowEffect() {
        return this.arrowEffect;
    }

    public void setFiring(boolean firing) {
        this.roarParticles = firing;
    }

    public boolean isFiring() {
        return this.roarParticles;
    }

    public boolean isSmited() {
        return this.antiRegen > 0;
    }

    public int hitTimeTeleport() {
        return this.isSecondPhase() ? 2 : 4;
    }

    public void resetHitTime() {
        this.hitTimes = 0;
    }

    public void increaseHitTime() {
        ++this.hitTimes;
    }

    public int getHitTimes() {
        return this.hitTimes;
    }

    public void resetCoolDown() {
        this.setCoolDown(0);
    }

    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }

    public int getCoolDown() {
        return this.coolDown;
    }

    public void setSpellCycle(int spellCycle) {
        this.spellCycle = spellCycle;
    }

    public int getSpellCycle() {
        return this.spellCycle;
    }

    public void setTornadoCoolDown(int coolDown) {
        this.tornadoCoolDown = coolDown;
    }

    public int getTornadoCoolDown() {
        return this.tornadoCoolDown;
    }

    public void setInfernoCoolDown(int coolDown) {
        this.infernoCoolDown = coolDown;
    }

    public int getInfernoCoolDown() {
        return this.infernoCoolDown;
    }

    public void setMonolithCoolDown(int coolDown) {
        this.monolithCoolDown = coolDown;
    }

    public int getMonolithCoolDown() {
        return this.monolithCoolDown;
    }

    public void setDamnedCoolDown(int coolDown) {
        this.damnedCoolDown = coolDown;
    }

    public int getDamnedCoolDown() {
        return this.damnedCoolDown;
    }

    public int getAntiRegen() {
        return this.antiRegen;
    }

    public int getAntiRegenTotal() {
        return this.antiRegenTotal;
    }

    public boolean monolithWeakened() {
        return this.obsidianInvul > 0 || this.monolithCoolDown > MathHelper.secondsToTicks(45);
    }

    public boolean teleportChance() {
        return this.level().getRandom().nextFloat() <= 0.25F;
    }

    public int spellStart() {
        return this.isSecondPhase() && this.getHealth() <= this.getMaxHealth() / 4 ? 12
                : this.isSecondPhase() ? 20 : 40;
    }

    public int coolDownLimit() {
        return this.isSecondPhase() && this.getHealth() <= this.getMaxHealth() / 4 ? 20
                : this.isSecondPhase() ? 25 : 50;
    }

    public void postSpellCast() {
        if (this.teleportChance()) {
            this.apostleTeleport();
        }
        this.resetCoolDown();
        this.setSpellCycle(0);
    }

    public boolean aboutToShoot() {
        boolean gonnaShoot = false;
        if (this.isUsingItem()) {
            if (this.getTicksUsingItem() >= 10) {
                gonnaShoot = true;
            }
        }
        return gonnaShoot;
    }

    // ================= 伤害管线(Apostle.hurt / actuallyHurt / heal 移植) =================

    protected boolean apostleHurt(DamageSource pSource, float pAmount) {
        LivingEntity livingEntity = this.getTarget();
        if (!this.level().isClientSide) {
            if (livingEntity != null && pSource.getEntity() instanceof LivingEntity) {
                this.increaseHitTime();
            }
        }
        if (pSource.getDirectEntity() instanceof LivingEntity living) {
            // 1.21 起 Enchantments.SMITE 是 ResourceKey<Enchantment>，改用 Goety 的 MobUtil 包装(与 Goety-3 Apostle 一致)
            int smite = MobUtil.getEnchantmentLevel(living, Enchantments.SMITE);
            if (smite > 0) {
                int smite2 = Mth.clamp(smite, 1, 5);
                int duration = MathHelper.secondsToTicks(smite2);
                this.antiRegenTotal = duration;
                this.antiRegen = duration;
                if (this.level() instanceof ServerLevel serverLevel) {
                    ModNetwork.sendToALL(new SApostleSmitePacket(this.getId(), duration));
                    if (this.getCoolDown() < this.coolDownLimit()) {
                        this.coolDown += 10;
                    }
                }
            }
        }
        if (pSource.is(DamageTypes.FELL_OUT_OF_WORLD) && pSource.getEntity() == null) {
            this.discard();
        }
        if (pSource.is(DamageTypes.LIGHTNING_BOLT) || pSource.is(DamageTypes.FALL) || pSource.is(DamageTypes.IN_WALL)) {
            return false;
        }
        if (pSource.getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() == this) {
            return false;
        }
        if (this.isSettingUpSecond()) {
            return false;
        }
        if (pSource.getDirectEntity() instanceof NetherMeteor || pSource.getEntity() instanceof NetherMeteor) {
            return false;
        }
        if (this.moddedInvul > 0 || this.obsidianInvul > 0) {
            return false;
        }
        float trueAmount = this.isInNether()
                ? pAmount * (1.0F - (MobsConfig.ApostleNetherDamageReduction.get() / 100.0F))
                : pAmount;
        if (!this.isNoAi()) {
            if (this.getHealth() > trueAmount) {
                if (this.getHitTimes() >= this.hitTimeTeleport()) {
                    this.apostleTeleport();
                } else if (pSource.getEntity() == null) {
                    if (this.level().getRandom().nextBoolean()) {
                        this.apostleTeleport();
                    }
                }
            }
        }
        if (!this.level().getNearbyPlayers(TargetingConditions.forCombat().selector(MobUtil.NO_CREATIVE_OR_SPECTATOR),
                this, this.getBoundingBox().inflate(32)).isEmpty()) {
            if (!(pSource.getEntity() instanceof Player)) {
                trueAmount = trueAmount / 2;
            }
        }
        if (this.isDeadOrDying()) {
            this.deathBlow = pSource;
        }
        return super.hurt(pSource, trueAmount);
    }

    protected void apostleActuallyHurt(DamageSource source, float amount) {
        float initialAmount = amount;
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount = Math.min(initialAmount, AttributesConfig.ApostleDamageCap.get().floatValue());
        }
        if (this.moddedInvul <= 0) {
            super.actuallyHurt(source, amount);
            if (source.getEntity() != null) {
                this.moddedInvul = MobsConfig.BossInvulnerabilityTime.get();
            }
        }
    }

    protected void apostleHeal(float amount) {
        if (!this.isSmited()) {
            super.heal(amount);
        }
    }

    /** ★ 全免疫：任何非增益状态效果(负面+中性：中毒/缓慢/虚弱/凋零/燃烧/失明/漂浮…)
     *   一律无法施加到亚小猫仆从身上；增益(力量/抗火/再生/隐身…照常生效)。
     *   召唤前已挂在身上的残留负面效果由 tick() 的 purgeNonBeneficialEffects() 兜底清除。 */
    @Override
    public boolean canBeAffected(MobEffectInstance pPotioneffect) {
        // 1.21 起 getEffect() 返回 Holder<MobEffect>
        if (!pPotioneffect.getEffect().value().isBeneficial()) {
            return false;   // ★ 全免疫：非增益效果直接拒绝
        }
        return !pPotioneffect.getEffect().is(GoetyEffects.WILD_RAGE) && super.canBeAffected(pPotioneffect);
    }

    /** 服务端每 tick 调用：清掉身上残留的非增益效果(配合 canBeAffected 构成双保险) */
    private void purgeNonBeneficialEffects() {
        for (MobEffectInstance active : new ArrayList<>(this.getActiveEffects())) {
            if (!active.getEffect().value().isBeneficial()) {
                this.removeEffect(active.getEffect());
            }
        }
    }

    // ================= 死亡流程(Apostle.die / tickDeath 移植；Boss 与仆从一致) =================

    protected void apostleDie(DamageSource cause) {
        if (this.deathTime > 0) {
            super.die(cause);
        } else {
            this.deathBlow = cause;
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 1) {
            if (this.pendingTeleportSearch != null) {
                this.pendingTeleportSearch.cancel(true);
                this.pendingTeleportSearch = null;
            }
            this.antiRegen = 0;
            this.antiRegenTotal = 0;
            this.toTeleportTime = 0;
            this.toTeleportPos = null;
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        if (MobsConfig.FancierApostleDeath.get() || (this.isInNether() && MobsConfig.ApostleNetherFancyDeath.get())) {
            this.setNoGravity(true);
            if (this.getKillCredit() instanceof Player) {
                this.lastHurtByPlayerTime = 100;   // 保留玩家击杀时间(可访问)；lastHurtByMobTimestamp 是 Goety AT 私有字段，仆从无掉落/经验，不影响
            }
            if (this.deathTime < 180) {
                if (this.deathTime > 20) {
                    this.move(MoverType.SELF, new Vec3(0.0D, 0.1D, 0.0D));
                }
                ExplosionUtil.lootExplode(this.level(), this, this.getRandomX(1.0D), this.getRandomY(),
                        this.getRandomZ(1.0D), 0.0F, false, Explosion.BlockInteraction.KEEP, LootingExplosion.Mode.LOOT);
            } else if (this.deathTime != 200) {
                this.move(MoverType.SELF, new Vec3(0.0D, 0.0D, 0.0D));
            }
            if (this.deathTime >= 200) {
                this.move(MoverType.SELF, new Vec3(0.0D, -4.0D, 0.0D));
                if (this.onGround() || this.getY() <= this.level().getMinBuildHeight()) {
                    if (this.level() instanceof ServerLevel serverLevel) {
                        // ★ 仆从版不操控天气：死亡也不清雷暴
                        for (int k = 0; k < 200; ++k) {
                            float f2 = this.random.nextFloat() * 4.0F;
                            float f1 = this.random.nextFloat() * ((float) Math.PI * 2F);
                            double d1 = Mth.cos(f1) * f2;
                            double d2 = 0.01D + this.random.nextDouble() * 0.5D;
                            double d3 = Mth.sin(f1) * f2;
                            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                    this.getX() + d1 * 0.1D, this.getY() + 0.3D, this.getZ() + d3 * 0.1D,
                                    0, d1, d2, d3, 0.5F);
                            serverLevel.sendParticles(ParticleTypes.FLAME,
                                    this.getX() + d1 * 0.1D, this.getY() + 0.3D, this.getZ() + d3 * 0.1D,
                                    0, d1, d2, d3, 0.5F);
                        }
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                                this.getX(), this.getY(), this.getZ(), 0, 1.0F, 0.0F, 0.0F, 0.5F);
                    }
                    this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F,
                            (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F);
                    this.playSound(this.getTrueDeathSound(), 5.0F,
                            (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                    this.die(this.deathBlow);
                    this.remove(RemovalReason.KILLED);
                }
            }
        } else {
            this.move(MoverType.SELF, new Vec3(0.0D, 0.0D, 0.0D));
            if (this.deathTime == 1) {
                if (!this.level().isClientSide) {
                    ServerLevel serverLevel = (ServerLevel) this.level();
                    // ★ 仆从版不操控天气：死亡也不清雷暴
                    for (int k = 0; k < 200; ++k) {
                        float f2 = this.random.nextFloat() * 4.0F;
                        float f1 = this.random.nextFloat() * ((float) Math.PI * 2F);
                        double d1 = Mth.cos(f1) * f2;
                        double d2 = 0.01D + this.random.nextDouble() * 0.5D;
                        double d3 = Mth.sin(f1) * f2;
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                this.getX() + d1 * 0.1D, this.getY() + 0.3D, this.getZ() + d3 * 0.1D,
                                0, d1, d2, d3, 0.5F);
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                                this.getX() + d1 * 0.1D, this.getY() + 0.3D, this.getZ() + d3 * 0.1D,
                                0, d1, d2, d3, 0.5F);
                    }
                    for (int l = 0; l < 16; ++l) {
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                                this.getRandomX(1.0F), this.getRandomY() - 0.25F, this.getRandomZ(1.0F),
                                1, 0, 0, 0, 0);
                    }
                }
                this.die(this.deathBlow);
                this.playSound(this.getTrueDeathSound(), 5.0F,
                        (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }
            if (this.deathTime >= 30) {
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    // ================= 传送(Apostle.teleport 族移植；施法/浮空时被 Boss 覆写拦截) =================

    protected void apostleTeleport() {
        if (!this.level().isClientSide() && !this.isNoAi() && this.isAlive()
                && this.toTeleportPos == null && !this.isSettingUpSecond() && !this.isCasting()) {
            if (MobsConfig.ApostleDelayedTeleport.get()) {
                if (this.pendingTeleportSearch != null) {
                    if (this.pendingTeleportSearch.isDone()) {
                        final double startY = this.getTarget() != null ? this.getTarget().getY() : this.getY();
                        Vec3 result = this.pendingTeleportSearch.getNow(null);
                        this.pendingTeleportSearch = null;
                        Vec3 vec3 = findTeleportGroundY(this.level(), result.x, result.y + 3, result.z, startY);
                        if (vec3 != null) {
                            this.toTeleportPos = vec3;
                            this.playSound(ModSounds.APOSTLE_PRE_TELEPORT.get(), 2.0F, 1.0F);
                            this.resetHitTime();
                        }
                    }
                } else {
                    final double startX = this.getX();
                    final double startY = this.getTarget() != null ? this.getTarget().getY() : this.getY();
                    final double startZ = this.getZ();
                    final RandomSource randomSource = this.apostleRandom;
                    this.prevX = startX;
                    this.prevY = this.getY();
                    this.prevZ = startZ;
                    this.pendingTeleportSearch = CompletableFuture.supplyAsync(() -> {
                        double d0 = 32.0D;
                        double d1 = startX + (randomSource.nextDouble() - 0.5D) * d0;
                        double d2 = startY + 3.0D;
                        double d3 = startZ + (randomSource.nextDouble() - 0.5D) * d0;
                        return new Vec3(d1, d2, d3);
                    });
                }
            } else {
                for (int i = 0; i < 128; ++i) {
                    double d3 = this.getX() + (this.level().getRandom().nextDouble() - 0.5D) * 32.0D;
                    double d4 = this.getTarget() != null ? this.getTarget().getY() : this.getY();
                    double d5 = this.getZ() + (this.level().getRandom().nextDouble() - 0.5D) * 32.0D;
                    BlockPos blockPos = BlockPos.containing(d3, d4, d5);
                    boolean flag = this.getTarget() == null || i >= 64 || BlockFinder.canSeeBlock(this.getTarget(), blockPos);
                    if (flag && this.randomTeleport(d3, d4, d5, false)) {
                        this.teleportHits();
                        this.resetHitTime();
                        break;
                    }
                }
            }
        }
    }

    protected void apostleTeleportTowards(Entity entity) {
        if (!this.level().isClientSide() && !this.isNoAi() && this.isAlive()
                && this.toTeleportPos == null && !this.isSettingUpSecond()) {
            if (MobsConfig.ApostleDelayedTeleport.get()) {
                if (this.pendingTeleportSearch != null) {
                    if (this.pendingTeleportSearch.isDone()) {
                        Vec3 result = this.pendingTeleportSearch.getNow(null);
                        this.pendingTeleportSearch = null;
                        if (result != null) {
                            Vec3 vec3 = findTeleportGroundY(this.level(), result.x, result.y + 3, result.z, entity.getY());
                            if (vec3 != null) {
                                this.toTeleportPos = vec3;
                                this.playSound(ModSounds.APOSTLE_PRE_TELEPORT.get(), 2.0F, 1.0F);
                            }
                        }
                    }
                } else {
                    final double targetX = entity.getX();
                    final double targetY = entity.getEyeY();
                    final double targetZ = entity.getZ();
                    final double selfX = this.getX();
                    final double selfY = this.getY();
                    final double selfZ = this.getZ();
                    final RandomSource randomSource = this.apostleRandom;
                    this.prevX = selfX;
                    this.prevY = selfY;
                    this.prevZ = selfZ;
                    this.pendingTeleportSearch = CompletableFuture.supplyAsync(() -> {
                        Vec3 vec3 = new Vec3(selfX - targetX, selfY * 0.5D - targetY, selfZ - targetZ).normalize();
                        double d0 = 16.0D;
                        double d1 = selfX + (randomSource.nextDouble() - 0.5D) * 8.0D - vec3.x * d0;
                        double d2 = selfY + (randomSource.nextInt(16) - 8) - vec3.y * d0;
                        double d3 = selfZ + (randomSource.nextDouble() - 0.5D) * 8.0D - vec3.z * d0;
                        return new Vec3(d1, d2, d3);
                    });
                }
            } else {
                for (int i = 0; i < 128; ++i) {
                    Vec3 vector3d = new Vec3(this.getX() - entity.getX(),
                            this.getY(0.5D) - entity.getEyeY(), this.getZ() - entity.getZ()).normalize();
                    double d0 = 16.0D;
                    double d1 = this.getX() + (this.level().getRandom().nextDouble() - 0.5D) * 8.0D - vector3d.x * d0;
                    double d2 = this.getY() + (this.level().getRandom().nextInt(16) - 8) - vector3d.y * d0;
                    double d3 = this.getZ() + (this.level().getRandom().nextDouble() - 0.5D) * 8.0D - vector3d.z * d0;
                    if (BlockFinder.canSeeBlock(entity, BlockPos.containing(d1, d2, d3))
                            && this.randomTeleport(d1, d2, d3, false)) {
                        this.teleportHits();
                        break;
                    }
                }
            }
        }
    }

    protected void apostleEscapeTeleport() {
        if (!this.level().isClientSide() && !this.isNoAi() && this.isAlive()
                && !this.isSettingUpSecond() && !this.isCasting()) {
            this.prevX = this.getX();
            this.prevY = this.getY();
            this.prevZ = this.getZ();
            for (int i = 0; i < 128; ++i) {
                double blockRange = 128.0D;
                double d3 = this.getX() + (this.getRandom().nextDouble() - 0.5D) * blockRange;
                double d4 = this.getY() + (this.getRandom().nextDouble() - 0.5D) * (blockRange / 2.0D);
                double d5 = this.getZ() + (this.getRandom().nextDouble() - 0.5D) * blockRange;
                if (this.randomTeleport(d3, d4, d5, false)) {
                    this.stopUsingItem();
                    this.stuckTime = 0;
                    this.level().broadcastEntityEvent(this, (byte) 100);
                    this.level().gameEvent(GameEvent.TELEPORT, this.position(), GameEvent.Context.of(this));
                    if (!this.isSilent()) {
                        this.level().playSound(null, this.prevX, this.prevY, this.prevZ,
                                ModSounds.APOSTLE_TELEPORT.get(), this.getSoundSource(), 1.0F, 1.0F);
                        this.playSound(ModSounds.APOSTLE_TELEPORT.get(), 1.0F, 1.0F);
                        this.level().playSound(null, this.prevX, this.prevY, this.prevZ,
                                ModSounds.ROAR_SPELL.get(), this.getSoundSource(), 3.0F, 0.25F);
                    }
                    this.resetHitTime();
                    break;
                }
            }
        }
    }

    public void teleportHits() {
        this.stopUsingItem();
        this.stuckTime = 0;
        this.level().broadcastEntityEvent(this, (byte) 100);
        this.level().gameEvent(GameEvent.TELEPORT, this.position(), GameEvent.Context.of(this));
        if (this.isSecondPhase()) {
            if (this.getTarget() != null) {
                if (this.level().getDifficulty() == Difficulty.HARD || this.isInNether()) {
                    FireBlastTrap fireBlastTrap = new FireBlastTrap(this.level(), this.prevX, this.prevY + 0.25D, this.prevZ);
                    fireBlastTrap.setOwner(this);
                    fireBlastTrap.setAreaOfEffect(3.0F);
                    this.level().addFreshEntity(fireBlastTrap);
                } else {
                    FireBlastTrap fireBlastTrap = new FireBlastTrap(this.level(), this.prevX, this.prevY + 0.25D, this.prevZ);
                    fireBlastTrap.setOwner(this);
                    fireBlastTrap.setAreaOfEffect(1.5F);
                    this.level().addFreshEntity(fireBlastTrap);
                }
            }
        }
        if (!this.isSilent()) {
            this.level().playSound(null, this.prevX, this.prevY, this.prevZ,
                    ModSounds.APOSTLE_TELEPORT.get(), this.getSoundSource(), 2.0F, 1.0F);
            this.playSound(ModSounds.APOSTLE_TELEPORT.get(), 2.0F, 1.0F);
        }
    }

    @Nullable
    private static Vec3 findTeleportGroundY(Level level, double x, double startY, double z, double targetY) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(startY), Mth.floor(z));
        if (!level.isLoaded(mutableBlockPos)) {
            return null;
        }
        int limit = 64;
        while (limit-- > 0 && mutableBlockPos.getY() > level.getMinBuildHeight()) {
            BlockState state = level.getBlockState(mutableBlockPos);
            BlockState above = level.getBlockState(mutableBlockPos.above());
            BlockState below = level.getBlockState(mutableBlockPos.below());
            boolean canStand = state.getCollisionShape(level, mutableBlockPos).isEmpty()
                    && above.getCollisionShape(level, mutableBlockPos.above()).isEmpty()
                    && !below.getCollisionShape(level, mutableBlockPos.below()).isEmpty();
            if (canStand) {
                if (mutableBlockPos.getY() <= targetY + 3.0) {
                    return new Vec3(mutableBlockPos.getX(), mutableBlockPos.getY(), mutableBlockPos.getZ());
                }
            }
            mutableBlockPos.move(Direction.DOWN);
        }
        return null;
    }

    @Override
    public void handleEntityEvent(byte pId) {
        if (pId == 100) {
            int i = 128;
            for (int j = 0; j < i; ++j) {
                double d0 = (double) j / (i - 1);
                float f = (this.random.nextFloat() - 0.5F) * 0.2F;
                float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
                float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
                double d1 = Mth.lerp(d0, this.prevX, this.getX()) + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth() * 2.0D;
                double d2 = Mth.lerp(d0, this.prevY, this.getY()) + this.random.nextDouble() * (double) this.getBbHeight();
                double d3 = Mth.lerp(d0, this.prevZ, this.getZ()) + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth() * 2.0D;
                this.level().addParticle(ParticleTypes.SMOKE, d1, d2, d3, (double) f, (double) f1, (double) f2);
            }
        } else if (pId == 101) {
            this.setMonolithPower(true);
        } else if (pId == 102) {
            this.setMonolithPower(false);
        } else if (pId == 103) {
            this.setSecondPhase(true);
        } else {
            super.handleEntityEvent(pId);
        }
    }

    // ================= 战利品(Apostle 口径；仆从子类覆写为无掉落) =================

    /** 使徒战利品表(Apostle 口径)：1.21 起 getDefaultLootTable() 返回 ResourceKey<LootTable>，
     *  而 Goety-3 的 ModLootTables.APOSTLE_HARD 仍是 ResourceLocation，需 ResourceKey.create 包装
     *  (与 Goety-3 Apostle 同款写法)。 */
    protected ResourceKey<LootTable> apostleLootTable() {
        if (this.isInNether()) {
            return ResourceKey.create(Registries.LOOT_TABLE, ModLootTables.APOSTLE_HARD);
        }
        return super.getDefaultLootTable();
    }

    protected void apostleDropCustomDeathLoot(ServerLevel level, DamageSource pSource, boolean pRecentlyHit) {
        super.dropCustomDeathLoot(level, pSource, pRecentlyHit);
        ItemStack itemStack = new ItemStack(ModItems.UNHOLY_BLOOD.get());
        if (this.isInNether()) {
            UnholyBloodItem.addPure(itemStack);
        }
        ItemEntity itementity = this.spawnAtLocation(itemStack);
        if (itementity != null) {
            itementity.setExtendedLifetime();
        }
        if (this.isInNether()) {
            if (MainConfig.EnableNightBeacon.get()) {
                ItemEntity itementity2 = this.spawnAtLocation(ModBlocks.NIGHT_BEACON.get().asItem());
                if (itementity2 != null) {
                    itementity2.setExtendedLifetime();
                }
            }
        }
    }

    // ================= 被动复刻：Apostle.aiStep 的服务端部分 =================

    /* ==================== 转阶段仪式兜底（防止"仪式卡死 → 永久无敌"） ====================
     * 本类自建的仪式只在 getHealth() >= getMaxHealth() 那一刻收尾(解除无敌/排斥、进入二阶段)。
     * 一旦这条不成立(仪式回血被 apostleHeal 的 Smite 禁疗闸门拦下、或血量被外部改回)，
     * 亚小猫会永久停在「无敌 + 排斥 + 不施法(因此也不召唤仆从/猫头)」的状态。
     * 兜底分两层，且都不会改变正常流程：
     *   1) 仪式回血连续 2 秒一点没涨 → 判定基础回血被卡住，本方法按同样速率(每 5 tick 1.5625%)
     *      自行补血，保证仪式能走完；
     *   2) 血已满 或 仪式持续超过 SETUP_WATCHDOG_TICKS → 直接补一次收尾(补满血 + 进二阶段)。
     * 正常流程下 servantApostleAI 自己会先收尾，本方法不产生任何额外效果。 */
    protected void tickSecondPhaseSetup() {
        if (!this.isSettingUpSecond()) {
            this.setupSecondTicks = 0;
            this.setupPrevHealth = -1.0F;
            this.setupNoHealTicks = 0;
            return;
        }
        ++this.setupSecondTicks;
        float health = this.getHealth();
        if (this.setupPrevHealth < 0.0F) {
            this.setupPrevHealth = health;
        }
        if (health > this.setupPrevHealth + 0.0001F) {
            this.setupNoHealTicks = 0;          // 基础回血仍在生效
        } else {
            ++this.setupNoHealTicks;            // 回血停住了
        }
        this.setupPrevHealth = health;
        // ① 基础回血被卡住(连续 40 tick 没涨)：按同样的速率自行补血
        if (this.setupNoHealTicks >= 40 && health < this.getMaxHealth() && this.tickCount % 5 == 0) {
            this.setHealth(Math.min(this.getMaxHealth(), health + 0.015625F * this.getMaxHealth()));
        }
        // ② 血已满 或 仪式超时：补一次收尾
        if (health >= this.getMaxHealth() || this.setupSecondTicks >= SETUP_WATCHDOG_TICKS) {
            this.setHealth(this.getMaxHealth());
            this.setSettingUpSecond(false);
            this.setSecondPhase(true);
            this.setupSecondTicks = 0;
            this.setupPrevHealth = -1.0F;
            this.setupNoHealTicks = 0;
        }
    }

    protected void servantApostleAI() {
        // ★ 和平模式不清除仆从：使徒(Apostle)原版靠这一行在和平难度自删(Boss 专属语义)。
        //   本类(ApollyonCatFightEntity)是仆从专用战斗基座，唯一实现 ApollyonCatServantEntity
        //   的需求是"仆从在和平难度也不消失"（见仆从版覆写的 shouldDespawnInPeaceful()），
        //   因此这里不能再无条件 remove —— 否则绑定版/野生版仆从切到和平难度都会被直接清掉。
        if (this.moddedInvul > 0) {
            --this.moddedInvul;
        }
        if (this.obsidianInvul > 0) {
            --this.obsidianInvul;
        }
        if (!this.level().isClientSide) {
            AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attributeinstance != null) {
                // 1.21 起 hasModifier/removeModifier(ResourceLocation) 按 id 判定/移除
                if (attributeinstance.hasModifier(SPEED_MODIFIER_CASTING.id())) {
                    attributeinstance.removeModifier(SPEED_MODIFIER_CASTING.id());
                }
                if (this.isCasting() && !this.isInNether()) {
                    attributeinstance.addTransientModifier(SPEED_MODIFIER_CASTING);
                }
                // ★ 移除黑曜石巨柱移速惩罚：巨柱在场期间仆从仍以正常速度移动(不再 -0.25 移速)
                if (attributeinstance.hasModifier(SPEED_MODIFIER_MONOLITH.id())) {
                    attributeinstance.removeModifier(SPEED_MODIFIER_MONOLITH.id());
                }
            }
            AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
            AttributeInstance toughness = this.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (armor != null && toughness != null) {
                if (armor.hasModifier(WEAK_ARMOR_MODIFIER.id())) {
                    armor.removeModifier(WEAK_ARMOR_MODIFIER.id());
                }
                if (toughness.hasModifier(WEAK_ARMOR_MODIFIER.id())) {
                    toughness.removeModifier(WEAK_ARMOR_MODIFIER.id());
                }
                if (!this.isInNether() && MobsConfig.ApostleHalvedArmor.get()) {
                    armor.addTransientModifier(WEAK_ARMOR_MODIFIER);
                    toughness.addTransientModifier(WEAK_ARMOR_MODIFIER);
                }
            }
            if (this.obsidianInvul > 0) {
                this.level().broadcastEntityEvent(this, (byte) 101);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 102);
            }
        }
        if (this.isSettingUpSecond()) {
            this.antiRegen = 0;
            this.antiRegenTotal = 0;
            this.toTeleportTime = 0;
            this.toTeleportPos = null;
            this.setFiring(false);
            this.stopUsingItem();
            if (this.tickCount % 5 == 0) {
                this.heal(0.015625F * this.getMaxHealth());
            }
            for (Entity entity : this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(3.0D), ALIVE)) {
                if (!MobUtil.areAllies(this, entity)) {
                    this.barrier(entity, this);
                }
            }
            if (!this.level().isClientSide) {
                this.resetHitTime();
                this.resetCoolDown();
                this.setSpellCycle(0);
                if (this.level() instanceof ServerLevel serverLevel) {
                    ServerParticleUtil.windParticle(serverLevel, ColorUtil.BLACK, 2.0F, 1.5F, this.getId(), this.position());
                    ServerParticleUtil.windParticle(serverLevel, ColorUtil.BLACK, 4.0F, 0.5F, this.getId(), this.position());
                }
            }
            if (this.getHealth() >= this.getMaxHealth()) {
                if (!this.level().isClientSide) {
                    ServerLevel serverLevel = (ServerLevel) this.level();
                    // ★ 仆从版不再强制雷暴(不操控世界天气) —— 去掉 Apostle 原版的进二阶段开雷暴
                    for (int k = 0; k < 60; ++k) {
                        float f2 = this.random.nextFloat() * 4.0F;
                        float f1 = this.random.nextFloat() * ((float) Math.PI * 2F);
                        double d1 = Mth.cos(f1) * f2;
                        double d2 = 0.01D + this.random.nextDouble() * 0.5D;
                        double d3 = Mth.sin(f1) * f2;
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                                this.getX() + d1 * 0.1D, this.getY() + 0.3D, this.getZ() + d3 * 0.1D,
                                0, d1, d2, d3, 0.25F);
                    }
                }
                this.setSettingUpSecond(false);
                this.setSecondPhase(true);
                this.level().broadcastEntityEvent(this, (byte) 103);
            }
        } else {
            if (this.tickCount % 10 == 0) {
                this.prevVecPos = this.position();
            }
            if (this.toTeleportPos != null) {
                if (!MobsConfig.ApostleDelayedTeleport.get()) {
                    this.toTeleportPos = null;
                } else {
                    ++this.toTeleportTime;
                    int time = this.isSecondPhase() ? 20 : 40;
                    if (this.toTeleportTime >= time) {
                        this.teleportTo(this.toTeleportPos.x, this.toTeleportPos.y, this.toTeleportPos.z);
                        this.teleportHits();
                        this.toTeleportPos = null;
                    } else {
                        if (this.level() instanceof ServerLevel serverLevel) {
                            ServerParticleUtil.addParticlesAroundMiddleSelf(serverLevel, ParticleTypes.SMOKE, this);
                            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                                    this.toTeleportPos.x, this.toTeleportPos.y + 0.25F, this.toTeleportPos.z, 1, 0, 0, 0, 0);
                            ServerParticleUtil.windParticle(serverLevel, ColorUtil.BLACK, 2.0F, 0.25F, -1, this.toTeleportPos);
                            for (int i = 0; i < 16; ++i) {
                                Vec3 vec3 = this.toTeleportPos.add(
                                        (double) this.getBbWidth() * (2.0D * this.random.nextDouble() - 1.0D) * 0.5F,
                                        (double) this.getBbHeight() * this.random.nextDouble(),
                                        (double) this.getBbWidth() * (2.0D * this.random.nextDouble() - 1.0D) * 0.5F);
                                serverLevel.sendParticles(new AbsorbTrailParticleOption(vec3, 0xff7700, 10),
                                        this.getRandomX(0.5F), this.getRandomY(), this.getRandomZ(0.5F), 1, 0, 0, 0, 1.0F);
                            }
                        }
                    }
                }
            } else {
                if (this.toTeleportTime > 0) {
                    this.toTeleportTime = 0;
                }
            }
            if (this.getTarget() != null && !this.isCasting()) {
                if (this.tickCount % 40 == 0) {
                    if (this.getTarget().isVisuallyCrawling() && !this.getSensing().hasLineOfSight(this.getTarget())) {
                        FireBlastTrap fireBlastTrap = new FireBlastTrap(this.level(),
                                this.getTarget().getX(), this.getTarget().getY() + 0.25D, this.getTarget().getZ());
                        fireBlastTrap.setOwner(this);
                        fireBlastTrap.setAreaOfEffect(2.0F);
                        this.level().addFreshEntity(fireBlastTrap);
                    }
                }
                if (this.isInWall() || (this.prevVecPos != null && this.prevVecPos.distanceTo(this.position()) <= 0.1D)) {
                    ++this.stuckTime;
                } else {
                    if (this.level().getBlockStates(this.getBoundingBox().inflate(1.0F))
                            .anyMatch(blockState1 -> blockState1.getBlock() instanceof MovingPistonBlock)) {
                        this.stuckTime += 20;
                        this.apostleTeleport();
                    } else {
                        if (this.stuckTime > 0) {
                            --this.stuckTime;
                        }
                    }
                }
                if (this.toTeleportPos == null) {
                    if (this.stuckTime > 50) {
                        if (this.level() instanceof ServerLevel serverLevel) {
                            ServerParticleUtil.addParticlesAroundSelf(serverLevel, ParticleTypes.LARGE_SMOKE, this);
                        }
                    }
                    if (this.stuckTime >= 100) {
                        this.apostleEscapeTeleport();
                        this.stuckTime = 0;
                    }
                }
            } else {
                this.stuckTime = 0;
            }
        }
        LivingEntity target = this.getTarget();
        if (this.getMainHandItem().isEmpty() && this.isAlive()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        }
        if (this.isSmited()) {
            --this.antiRegen;
        }
        if (this.getTornadoCoolDown() > 0) {
            --this.tornadoCoolDown;
        }
        if (this.getInfernoCoolDown() > 0) {
            --this.infernoCoolDown;
        }
        if (this.getMonolithCoolDown() > 0) {
            --this.monolithCoolDown;
        }
        if (this.getDamnedCoolDown() > 0) {
            --this.damnedCoolDown;
        }
        if (!this.isSmited()) {
            int count = this.isSecondPhase() ? 200 : 400;
            if (MobsConfig.ApostleQuickerRegen.get()) {
                count = this.isSecondPhase() ? 20 : 40;
            }
            if (this.isInNether()) {
                if (this.Regen()) {
                    if (this.tickCount % (count / 2) == 0) {
                        if (this.getHealth() < this.getMaxHealth()) {
                            this.heal(1.0F);
                        }
                    }
                } else {
                    if (this.tickCount % count == 0) {
                        if (this.getHealth() < this.getMaxHealth()) {
                            this.heal(1.0F);
                        }
                    }
                }
            } else {
                if (this.Regen()) {
                    if (this.tickCount % count == 0) {
                        if (this.getHealth() < this.getMaxHealth()) {
                            this.heal(1.0F);
                        }
                    }
                }
            }
            if (this.obsidianInvul > 0) {
                int obsidianRegenTime = this.isSecondPhase() ? 40 : 80;
                if (this.tickCount % obsidianRegenTime == 0) {
                    if (this.getHealth() < this.getMaxHealth()) {
                        this.heal(1.0F);
                    }
                }
            }
        }
        if (this.isSecondPhase()) {
            // ★ 仆从版不再强制雷暴：二阶段照常放流星，但不再每 100 tick 把天气拉回雷暴
            if (this.tickCount % 20 == 0) {
                BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(
                        this.getRandomX(0.2), this.getY(), this.getRandomZ(0.2));
                while (blockPos.getY() < this.getY() + 64.0D
                        && !this.level().getBlockState(blockPos).isSolidRender(this.level(), blockPos)) {
                    blockPos.move(Direction.UP);
                }
                if (blockPos.getY() > this.getY() + 32.0D) {
                    NetherMeteor fireball = this.getNetherMeteor();
                    fireball.setDangerous(EventHooks.canEntityGrief(this.level(), this)
                            && MobsConfig.ApocalypseMode.get());
                    fireball.setPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                    this.level().addFreshEntity(fireball);
                }
            }
        }
        if (!this.level().isClientSide) {
            if (this.getCoolDown() < this.coolDownLimit()) {
                ++this.coolDown;
            } else {
                if (!this.isSecondPhase() || this.getDamnedCoolDown() <= 0) {
                    this.setSpellCycle(0);
                } else {
                    this.setSpellCycle(1);
                }
            }
            if (this.getSpellCycle() == 1) {
                if (this.level().getRandom().nextBoolean()) {
                    this.setSpellCycle(2);
                } else {
                    this.setSpellCycle(3);
                }
            }
        }
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(32))) {
            if (MobsConfig.ApostleBoilsWater.get()) {
                if (!(living instanceof Cultist) && !(living instanceof Witch)
                        && !(living instanceof IOwned owned && owned.getTrueOwner() == this)) {
                    if (living.isInWater()) {
                        living.hurt(living.damageSources().hotFloor(), 1.0F);
                    }
                }
            }
            if (MobsConfig.ApostleConvertsVillagers.get()) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    if (living instanceof AbstractVillager villager) {
                        float chance = 0.25F;
                        if (MobUtil.isDirectlyLooking(this, villager)) {
                            chance = 0.75F;
                        }
                        if (this.tickCount % 100 == 0 && this.random.nextFloat() <= chance) {
                            boolean flag = true;
                            if (villager instanceof Villager villager1) {
                                if (villager1.getVillagerData().getLevel() >= 5) {
                                    flag = false;
                                }
                            }
                            if (flag) {
                                float chance2 = this.random.nextFloat();
                                Mob mob = EntityType.WITCH.create(serverLevel);
                                if (this.random.nextBoolean()) {
                                    mob = ModEntityType.WARLOCK.get().create(serverLevel);
                                }
                                if (chance2 <= 0.25F) {
                                    mob = ModEntityType.HERETIC.get().create(serverLevel);
                                }
                                if (villager instanceof WanderingTrader) {
                                    mob = ModEntityType.MAVERICK.get().create(serverLevel);
                                    if (this.random.nextBoolean()) {
                                        mob = ModEntityType.REPROBATE.get().create(serverLevel);
                                    }
                                }
                                if (mob != null) {
                                    mob.moveTo(villager.getX(), villager.getY(), villager.getZ(),
                                            villager.getYRot(), villager.getXRot());
                                    EventHooks.finalizeMobSpawn(mob, serverLevel,
                                            serverLevel.getCurrentDifficultyAt(mob.blockPosition()),
                                            MobSpawnType.CONVERSION, null);
                                    mob.setNoAi(villager.isNoAi());
                                    if (villager.hasCustomName()) {
                                        mob.setCustomName(villager.getCustomName());
                                        mob.setCustomNameVisible(villager.isCustomNameVisible());
                                    }
                                    mob.setPersistenceRequired();
                                    EventHooks.onLivingConvert(villager, mob);
                                    serverLevel.addFreshEntityWithPassengers(mob);
                                    if (villager instanceof Villager villager1) {
                                        MobUtil.releaseAllPois(villager1);
                                    }
                                    villager.discard();
                                }
                            }
                        }
                    }
                }
            }
            if (living instanceof Player player) {
                player.getAbilities().flying &= player.isCreative();
            }
        }
        if (target == null) {
            if (this.getHitTimes() > 0) {
                this.resetHitTime();
            }
            boolean flag = false;
            if (this.killedPlayer) {
                --this.lastKilledPlayer;
                if (this.lastKilledPlayer == 0) {
                    this.remove(RemovalReason.DISCARDED);
                }
            } else {
                flag = true;
            }
            if (flag) {
                for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16))) {
                    if (player.isSpectator() || player.isRemoved() || !player.isAlive()) {
                        continue;
                    }
                    if (this.canAttack(player, TargetingConditions.DEFAULT) && !MobUtil.areAllies(this, player)) {
                        this.setTarget(player);
                        break;
                    }
                }
                if (this.getTarget() == null) {
                    for (Mob mob : this.level().getEntitiesOfClass(Mob.class,
                            this.getBoundingBox().inflate(this.getAttributeValue(Attributes.FOLLOW_RANGE)),
                            EntitySelector.LIVING_ENTITY_STILL_ALIVE)) {
                        if (mob.getTarget() == this) {
                            this.setTarget(mob);
                            break;
                        }
                    }
                }
            }
        } else {
            if (this.killedPlayer) {
                this.killedPlayer = false;
                this.lastKilledPlayer = 200;
            }
            if (this.tickCount % 100 == 0 && !this.isSettingUpSecond()) {
                if (!this.level().isClientSide) {
                    if (this.isInNether()) {
                        for (ZombifiedPiglin zombifiedPiglin : this.level().getEntitiesOfClass(
                                ZombifiedPiglin.class, this.getBoundingBox().inflate(16))) {
                            if (zombifiedPiglin.getTarget() != target) {
                                zombifiedPiglin.setTarget(target);
                            }
                        }
                    }
                }
            }
            if (this.isSecondPhase()) {
                if (this.getHealth() <= this.getMaxHealth() / 8) {
                    if (this.tickCount % 100 == 0) {
                        this.apostleTeleport();
                    }
                }
                if (MobUtil.isInRain(target)) {
                    int count = 100 * (this.random.nextInt(5) + 1);
                    if (this.tickCount % count == 0) {
                        BlockPos.MutableBlockPos blockpos = new BlockPos.MutableBlockPos(
                                target.getX(), target.getY(), target.getZ());
                        while (blockpos.getY() > this.level().getMinBuildHeight()
                                && !this.level().getBlockState(blockpos).blocksMotion()) {
                            blockpos.move(Direction.DOWN);
                        }
                        LightningTrap lightningTrap = new LightningTrap(this.level(),
                                blockpos.getX(), blockpos.getY() + 1, blockpos.getZ());
                        lightningTrap.setOwner(this);
                        lightningTrap.setDuration(50);
                        this.level().addFreshEntity(lightningTrap);
                    }
                }
            }
            if ((target.distanceToSqr(this) > 1024 || !this.getSensing().hasLineOfSight(target))
                    && target.onGround() && !this.isSettingUpSecond()) {
                this.apostleTeleportTowards(target);
            }
        }
        if (this.isInWater() || this.isInLava() || this.isInFluidType() || this.isInWall()) {
            this.apostleTeleport();
        }
        if (this.isInNether()) {
            if (target != null) {
                target.addEffect(new MobEffectInstance(GoetyEffects.BURN_HEX, 100));
            }
        }
    }

    /** 客户端被动复刻：二阶段仪式时自身冒烟(替代 SpellCastingCultist/Apostle 客户端分支) */
    protected void clientApostlePassive() {
        if (this.isSettingUpSecond()) {
            for (int i = 0; i < 40; ++i) {
                double d0 = this.random.nextGaussian() * 0.2D;
                double d1 = this.random.nextGaussian() * 0.2D;
                double d2 = this.random.nextGaussian() * 0.2D;
                this.level().addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE,
                        this.getX(), this.getY() + 0.5, this.getZ(), d0, d1, d2);
            }
        }
        this.oClientShootIndicatorProgress = this.clientShootIndicatorProgress;
        this.oClientShootIndicatorEnd = this.clientShootIndicatorEnd;
        this.clientShootIndicatorProgress = this.entityData.get(SHOOT_INDICATOR_PROGRESS);
        this.clientShootIndicatorEnd = new Vec3(this.entityData.get(SHOOT_INDICATOR_END));
    }

    /** 第二阶段的流星轰炸：与 Apostle.getNetherMeteor 相同口径 */
    public NetherMeteor getNetherMeteor() {
        int range = this.getHealth() < this.getMaxHealth() / 2 ? 450 : 900;
        int trueRange = this.getHealth() < this.getMaxHealth() / 4 ? range / 2 : range;
        RandomSource random = this.level().random;
        double d = (random.nextBoolean() ? 1 : -1);
        double e = (random.nextBoolean() ? 1 : -1);
        double d2 = (random.nextInt(trueRange) * d);
        double d3 = -900.0D;
        double d4 = (random.nextInt(trueRange) * e);
        return new NetherMeteor(this.level(), this, d2, d3, d4);
    }

    /** 仪式推人：Apostle.barrier 同款 */
    public void barrier(Entity entity, LivingEntity livingEntity) {
        double d0 = entity.getX() - livingEntity.getX();
        double d1 = entity.getZ() - livingEntity.getZ();
        double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
        MobUtil.forcePush(entity, d0 / d2 * 2.0D, 0.1D, d1 / d2 * 2.0D);
    }

    // ================= 施法框架本地基类(SpellCastingCultist.UseSpellGoal / CastingASpellGoal 移植) =================

    public abstract class CatUseSpellGoal extends Goal {
        protected int spellWarmup;
        protected int spellCooldown;

        protected CatUseSpellGoal() {
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = ApollyonCatFightEntity.this.getTarget();
            if (livingentity != null && livingentity.isAlive()) {
                if (ApollyonCatFightEntity.this.isSpellcasting()) {
                    return false;
                } else {
                    return ApollyonCatFightEntity.this.tickCount >= this.spellCooldown;
                }
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity livingentity = ApollyonCatFightEntity.this.getTarget();
            return livingentity != null && livingentity.isAlive() && this.spellWarmup > 0;
        }

        @Override
        public void start() {
            this.spellWarmup = this.getCastWarmupTime();
            ApollyonCatFightEntity.this.spellTicks = this.getCastingTime();
            this.spellCooldown = ApollyonCatFightEntity.this.tickCount + this.getCastingInterval();
            SoundEvent soundevent = this.getSpellPrepareSound();
            if (soundevent != null) {
                ApollyonCatFightEntity.this.playSound(soundevent, this.castingVolume(), 1.0F);
            }
            ApollyonCatFightEntity.this.setSpellType(this.getSpellType());
        }

        @Override
        public void tick() {
            --this.spellWarmup;
            if (this.spellWarmup == 0) {
                this.castSpell();
                ApollyonCatFightEntity.this.setSpellType(SpellCastingCultist.SpellType.NONE);
                ApollyonCatFightEntity.this.playSound(
                        ApollyonCatFightEntity.this.getCastingSoundEvent(), 1.0F, 1.0F);
            }
        }

        protected float castingVolume() {
            return 1.0F;
        }

        protected abstract void castSpell();

        protected int getCastWarmupTime() {
            return 20;
        }

        protected abstract int getCastingTime();

        protected abstract int getCastingInterval();

        @Nullable
        protected abstract SoundEvent getSpellPrepareSound();

        protected abstract SpellCastingCultist.SpellType getSpellType();
    }

    public class CatCastingPoseBaseGoal extends Goal {
        public CatCastingPoseBaseGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return ApollyonCatFightEntity.this.getSpellTicks() > 0;
        }

        @Override
        public void start() {
            super.start();
            ApollyonCatFightEntity.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            super.stop();
            ApollyonCatFightEntity.this.setSpellType(SpellCastingCultist.SpellType.NONE);
        }

        @Override
        public void tick() {
            if (ApollyonCatFightEntity.this.getTarget() != null) {
                ApollyonCatFightEntity.this.getLookControl().setLookAt(ApollyonCatFightEntity.this.getTarget(),
                        (float) ApollyonCatFightEntity.this.getMaxHeadYRot(),
                        (float) ApollyonCatFightEntity.this.getMaxHeadXRot());
            }
        }
    }

    // ================= 岩浆漂浮(Apostle.floatApostle 等价，保持 Boss 一致) =================
    public void floatLikeApostle() {
        if (this.isInLava()) {
            CollisionContext collisioncontext = CollisionContext.of(this);
            if (collisioncontext.isAbove(LiquidBlock.STABLE_SHAPE, this.blockPosition(), true)
                    && !this.level().getFluidState(this.blockPosition().above()).is(FluidTags.LAVA)) {
                this.setOnGround(true);
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(0.0D, 0.05D, 0.0D));
            }
        }
    }

    // ================= 岩浆寻路(复刻 Apostle.ApostlePathNavigation：能走岩浆/火，可过门) =================
    @Override
    protected PathNavigation createNavigation(Level pLevel) {
        return new ApollyonCatGroundNavigation(this, pLevel);
    }

    public static class ApollyonCatGroundNavigation extends GroundPathNavigation {
        public ApollyonCatGroundNavigation(Mob mob, Level level) {
            super(mob, level);
        }

        @Override
        protected PathFinder createPathFinder(int pRange) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, pRange);
        }

        @Override
        protected boolean hasValidPathType(PathType pType) {
            return pType == PathType.LAVA || pType == PathType.DAMAGE_FIRE
                    || pType == PathType.DANGER_FIRE || super.hasValidPathType(pType);
        }

        @Override
        public boolean isStableDestination(BlockPos pPos) {
            return this.level.getBlockState(pPos).is(Blocks.LAVA) || super.isStableDestination(pPos);
        }
    }

    // ================= 野生(无主)亚小猫：Boss 式主动攻击玩家 ================

    class WildPlayerTargetGoal extends NearestAttackableTargetGoal<Player> {
        WildPlayerTargetGoal() {
            super(ApollyonCatFightEntity.this, Player.class, true);
        }

        @Override
        public boolean canUse() {
            if (ApollyonCatFightEntity.this.getTrueOwner() != null) {
                return false;   // 已绑定主人 → 绝不主动打玩家
            }
            return super.canUse();
        }
    }

}
