package com.acat.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.Arrays;
import java.util.List;

/**
 * 亚小猫 (aaacat) 配置文件 —— 自动生成于 config/aaacat.toml（文件名与 mod id 一致）。
 * Ya Cat (aaacat) config; type COMMON, changes require a full game restart.
 *
 * 可配置项 Configurable options:
 *  1. entities      Boss/仆从 最大生命            Boss & servant max health
 *  2. phase3        三阶段每秒扣血                Phase-3 drain
 *  3. holySoulArk   圣金魂匣                      Holy Soul Ark
 *  4. holyGoldCrown 圣金王冠 1~6 级               Holy Gold Crown (levels 1-6)
 *  5. killAura      天敌秒杀列表                  Natural-enemy kill list
 *  6. skillDamage   技能伤害                      Skill damage
 */
public class AcatConfig {

    public static final ModConfigSpec SPEC;

    /* ==================== Boss / 仆从 ==================== */
    public static final ModConfigSpec.DoubleValue BOSS_MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue SERVANT_MAX_HEALTH;

    /* ==================== 三阶段(领域) ==================== */
    public static final ModConfigSpec.DoubleValue P3_DRAIN_PERCENT;

    /* ==================== 圣金魂匣(holy_soul_ark) ==================== */
    public static final ModConfigSpec.IntValue ARK_SOUL_PER_SECOND;       // 每秒回复灵魂能量
    public static final ModConfigSpec.IntValue ARK_HEAL_COST_PER_SECOND;  // 每秒治疗消耗灵魂
    public static final ModConfigSpec.DoubleValue ARK_HEAL_PERCENT;       // 每秒回复 % 最大生命
    public static final ModConfigSpec.IntValue ARK_TIER_SOUL_DIV;         // 每 N 灵魂 = 1 档
    public static final ModConfigSpec.IntValue ARK_MAX_HEALTH_PER_TIER;   // 每档 + 最大生命
    public static final ModConfigSpec.IntValue ARK_ATTACK_PER_TIER;       // 每档 + 攻击力

    /* ==================== 圣金王冠(holy_gold_crown) 等级 1~6 ==================== */
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_CAST_PERCENT;      // 施法时间总减少 %（显示）
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_CAST_SPEED_BONUS;  // goety:casting_speed 实际增量
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_SOUL_PERCENT;      // 灵魂消耗减少 %（显示）
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_SOUL_DISCOUNT;     // goety:soul_discount 实际增量
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_COOL_PERCENT;      // 冷却减少 %（显示）
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_COOL_DISCOUNT;     // goety:cooldown_discount 实际增量
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_PLAYER_DR;         // 玩家减伤 %
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_SERVANT_HP;        // 仆从最大生命加成 %
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_SERVANT_DR;        // 仆从减伤 %
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_SERVANT_DMG;       // 仆从伤害提升 %
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_SERVANT_REGEN;     // 仆从每秒回血 %
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> CROWN_REVIVE_COOLDOWN;   // 仆从复活冷却(秒)，0=无复活

    /* ==================== 仆从/祝福 天敌猎杀 ==================== */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> KILL_AURA_IDS;

    /* ==================== 仆从 起床礼物 ==================== */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> GIFT_ITEMS;

    /* ==================== 技能伤害（Boss 与 仆从共用同一套） ==================== */
    public static final ModConfigSpec.DoubleValue SKILL_SONIC_BOOM;        // 音爆
    public static final ModConfigSpec.DoubleValue SKILL_QUAKE;             // 地震
    public static final ModConfigSpec.DoubleValue SKILL_WIND_BLADE;        // 风刃
    public static final ModConfigSpec.DoubleValue SKILL_RUPTURE_TICK;      // 虚空裂隙(每tick)
    public static final ModConfigSpec.DoubleValue SKILL_RUPTURE_BURST;     // 虚空裂隙(收束爆炸)
    public static final ModConfigSpec.DoubleValue SKILL_CORRUPT_BEAM;      // 腐化光束
    public static final ModConfigSpec.DoubleValue SKILL_KILLING_PERCENT;   // 索命(目标当前生命 %)
    public static final ModConfigSpec.DoubleValue SKILL_FIREBALL;          // 一阶段地狱爆裂弹(直接命中)
    public static final ModConfigSpec.DoubleValue SKILL_P3_BOLT;           // 三阶段蒸汽弹
    public static final ModConfigSpec.DoubleValue SKILL_P3_METEOR;         // 三阶段下界流星(直接命中)
    public static final ModConfigSpec.DoubleValue SKILL_P3_FIRE_RING;      // 三阶段火圈
    public static final ModConfigSpec.DoubleValue SKILL_P3_TORNADO;        // 三阶段火焰龙卷
    public static final ModConfigSpec.DoubleValue SKILL_P3_VOID_BOMB;      // 三阶段虚空震荡弹(爆炸)
    public static final ModConfigSpec.DoubleValue CAT_HEAD_RED_PER_HIT;       // 二阶段红猫头(狱火)直击
    public static final ModConfigSpec.DoubleValue CAT_HEAD_BLUE_PER_HIT;      // 二阶段蓝猫头(冰火)直击
    public static final ModConfigSpec.DoubleValue CAT_HEAD_YELLOW_PER_HIT;    // 二阶段黄猫头(终末火)直击
    public static final ModConfigSpec.IntValue CAT_HEAD_FREEZE_TICKS;         // 二阶段蓝猫头冰冻 tick 数

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        /* ---------- Boss / 仆从 ---------- */
        builder.comment("亚小猫 Boss / 仆从 数值。 Ya Cat boss & servant stats.")
                .push("entities");
        BOSS_MAX_HEALTH = builder.comment("亚小猫 Boss 最大生命值。 Boss max health.")
                .defineInRange("bossMaxHealth", 333.0, 1.0, 1.0E7);
        SERVANT_MAX_HEALTH = builder.comment("亚小猫 仆从 最大生命值。 Servant max health.")
                .defineInRange("servantMaxHealth", 333.0, 1.0, 1.0E7);
        builder.pop();

        /* ---------- 三阶段(领域) ---------- */
        builder.comment("三阶段(领域)。 Phase 3 domain.")
                .push("phase3");
        P3_DRAIN_PERCENT = builder.comment("三阶段每秒直扣最大生命的百分比(默认 2 = 2%/秒)。 Phase-3 % max health drained per second (default 2 = 2%/s).")
                .defineInRange("drainPercentPerSecond", 2.0, 0.0, 100.0);
        builder.pop();

        /* ---------- 圣金魂匣 ---------- */
        builder.comment("圣金魂匣(holy_soul_ark) 强化数值。 Holy Soul Ark boosts.")
                .push("holySoulArk");
        ARK_SOUL_PER_SECOND = builder.comment("持有者每秒回复灵魂能量。 Soul energy gained per second.")
                .defineInRange("soulPerSecond", 333, 0, Integer.MAX_VALUE);
        ARK_HEAL_COST_PER_SECOND = builder.comment("每秒治疗消耗灵魂能量。 Soul cost per second to heal.")
                .defineInRange("healCostPerSecond", 100, 0, Integer.MAX_VALUE);
        ARK_HEAL_PERCENT = builder.comment("每秒回复最大生命百分比(默认 1)。 Max health % healed per second (default 1).")
                .defineInRange("healPercentPerSecond", 1.0, 0.0, 100.0);
        ARK_TIER_SOUL_DIV = builder.comment("每多少灵魂能量记为 1 档属性加成。 Soul energy per tier.")
                .defineInRange("soulPerTier", 10000, 1, Integer.MAX_VALUE);
        ARK_MAX_HEALTH_PER_TIER = builder.comment("每档 +最大生命。 Max health added per tier.")
                .defineInRange("maxHealthPerTier", 2, 0, Integer.MAX_VALUE);
        ARK_ATTACK_PER_TIER = builder.comment("每档 +攻击力。 Attack added per tier.")
                .defineInRange("attackPerTier", 1, 0, Integer.MAX_VALUE);
        builder.pop();

        /* ---------- 圣金王冠 ---------- */
        builder.comment("圣金王冠(holy_gold_crown) 各等级数值。",
                "除 hasShare(Lv>=3 护卫分摊)/hasPierce(Lv6 全穿透) 等开关外，每项列表共 6 个值，依次对应 1~6 级。",
                "Holy Gold Crown per-level values (6 entries = levels 1-6).")
                .push("holyGoldCrown");
        CROWN_CAST_PERCENT = crownList(builder, "castPercent",
                "施法时间总减少 %(显示；实际增量见 castSpeedBonus)。 Cast time reduction % (display; real bonus: castSpeedBonus).",
                50.0, 60.0, 70.0, 80.0, 80.0, 80.0);
        CROWN_CAST_SPEED_BONUS = crownList(builder, "castSpeedBonus",
                "goety:casting_speed 实际增量。 Actual goety:casting_speed bonus.",
                0.0, 0.2, 0.4, 0.6, 0.6, 0.6);
        CROWN_SOUL_PERCENT = crownList(builder, "soulPercent",
                "灵魂消耗减少 %(显示；实际增量见 soulDiscount)。 Soul cost reduction % (display; real bonus: soulDiscount).",
                50.0, 60.0, 70.0, 80.0, 80.0, 80.0);
        CROWN_SOUL_DISCOUNT = crownList(builder, "soulDiscount",
                "goety:soul_discount 实际增量。 Actual goety:soul_discount bonus.",
                0.5, 0.6, 0.7, 0.8, 0.8, 0.8);
        CROWN_COOL_PERCENT = crownList(builder, "coolPercent",
                "冷却减少 %(显示；实际增量见 coolDiscount)。 Cooldown reduction % (display; real bonus: coolDiscount).",
                20.0, 25.0, 30.0, 40.0, 50.0, 50.0);
        CROWN_COOL_DISCOUNT = crownList(builder, "coolDiscount",
                "goety:cooldown_discount 实际增量。 Actual goety:cooldown_discount bonus.",
                0.2, 0.25, 0.3, 0.4, 0.5, 0.5);
        CROWN_PLAYER_DR = crownList(builder, "playerDr",
                "玩家伤害减免 %。 Player damage reduction %.",
                0.0, 10.0, 20.0, 30.0, 40.0, 60.0);
        CROWN_SERVANT_HP = crownList(builder, "servantHp",
                "仆从最大生命加成 %。 Servant max-health bonus %.",
                10.0, 20.0, 30.0, 30.0, 30.0, 30.0);
        CROWN_SERVANT_DR = crownList(builder, "servantDr",
                "仆从伤害减免 %。 Servant damage reduction %.",
                0.0, 10.0, 20.0, 30.0, 30.0, 30.0);
        CROWN_SERVANT_DMG = crownList(builder, "servantDmg",
                "仆从伤害提升 %。 Servant damage bonus %.",
                0.0, 0.0, 10.0, 20.0, 30.0, 50.0);
        CROWN_SERVANT_REGEN = crownList(builder, "servantRegen",
                "仆从每秒回血 %。 Servant regen per second %.",
                0.0, 0.0, 0.5, 1.0, 1.0, 2.0);
        CROWN_REVIVE_COOLDOWN = crownList(builder, "reviveCooldown",
                "仆从死亡复活冷却(秒)，0 = 本等级无复活。 Servant revive cooldown (s), 0 = no revive.",
                0.0, 0.0, 0.0, 0.0, 900.0, 600.0);
        builder.pop();

        /* ---------- 天敌猎杀 ---------- */
        builder.comment("亚小猫仆从本体 / 亚小猫的祝福(玩家) 的自动天敌猎杀。",
                "Natural-enemy kill aura of the servant and of the player carrying the blessing.")
                .push("killAura");
        KILL_AURA_IDS = builder.comment("自动秒杀的天敌实体注册名列表(命名空间:路径)。",
                "Entity ids auto-killed as natural enemies (namespace:path).")
                .defineList("targetEntityIds",
                        Arrays.asList(
                                "minecraft:phantom",
                                "minecraft:creeper",
                                "alexsmobs:crimson_mosquito",
                                "alexsmobs:seagull",
                                "iceandfire:if_pixie"),
                        o -> o instanceof String);
        builder.pop();

        /* ---------- 技能伤害 ---------- */
        builder.comment("亚小猫 技能伤害(Boss 与仆从共用一套；无任何加成时的最终伤害，护甲/魔抗减免之前)。",
                "Skill damage, one shared set for boss & servant; final raw damage before armor/resist.",
                "只含 aaacat 代码直接控制伤害的技能；Goety 内部结算的伤害(爆裂弹爆炸/狱火等)不在此列。",
                "Only skills whose damage is controlled by aaacat are listed; Goety-internal damage keeps its defaults.",
                "修改后需重启游戏生效。 Restart the game to apply.")
                .push("skillDamage");
        SKILL_SONIC_BOOM = builder.comment("音爆 每次伤害(默认 15)。 Sonic boom damage per hit (default 15).")
                .defineInRange("sonicBoom", 15.0, 0.0, 1.0E7);
        SKILL_QUAKE = builder.comment("地震 每段伤害(默认 6)。 Quake damage per segment (default 6).")
                .defineInRange("quakePerHit", 6.0, 0.0, 1.0E7);
        SKILL_WIND_BLADE = builder.comment("风刃 每发伤害(默认 16)。 Wind blade damage per hit (default 16).")
                .defineInRange("windBladePerHit", 16.0, 0.0, 1.0E7);
        SKILL_RUPTURE_TICK = builder.comment("虚空裂隙 每 tick 伤害(默认 3)。 Void rift damage per tick (default 3).")
                .defineInRange("rupturePerTick", 3.0, 0.0, 1.0E7);
        SKILL_RUPTURE_BURST = builder.comment("虚空裂隙 收束爆炸伤害(默认 20)。 Void rift closing burst damage (default 20).")
                .defineInRange("ruptureBurst", 20.0, 0.0, 1.0E7);
        SKILL_CORRUPT_BEAM = builder.comment("腐化光束 每次命中伤害(默认 10)。 Corrupted beam damage per hit (default 10).")
                .defineInRange("corruptBeamPerTick", 10.0, 0.0, 1.0E7);
        SKILL_KILLING_PERCENT = builder.comment("索命：造成目标当前生命值的百分比(默认 100 = 直接击杀)。",
                "Killing: percent of target current health dealt (default 100 = instant kill).")
                .defineInRange("killingPercentOfCurrentHealth", 100.0, 0.0, 100.0);
        SKILL_FIREBALL = builder.comment("一阶段地狱爆裂弹 直接命中伤害(默认 6)。 Phase-1 Hell Blast direct-hit damage (default 6).")
                .defineInRange("fireballPerHit", 6.0, 0.0, 1.0E7);
        SKILL_P3_BOLT = builder.comment("三阶段蒸汽弹 每颗命中伤害(默认 5)。 Phase-3 steam missile damage per hit (default 5).")
                .defineInRange("phase3BoltPerHit", 5.0, 0.0, 1.0E7);
        SKILL_P3_METEOR = builder.comment("三阶段下界流星 直接命中伤害(默认 10)。 Phase-3 meteor direct-hit damage (default 10).")
                .defineInRange("phase3MeteorPerHit", 10.0, 0.0, 1.0E7);
        SKILL_P3_FIRE_RING = builder.comment("三阶段火圈 每次伤害(默认 10)。 Phase-3 fire ring damage per tick (default 10).")
                .defineInRange("phase3FireRingPerTick", 10.0, 0.0, 1.0E7);
        SKILL_P3_TORNADO = builder.comment("三阶段火焰龙卷 每次伤害(默认 6)。 Phase-3 fire tornado damage per tick (default 6).")
                .defineInRange("phase3TornadoPerTick", 6.0, 0.0, 1.0E7);
        SKILL_P3_VOID_BOMB = builder.comment("三阶段虚空震荡弹 爆炸伤害(不含魔焰持续伤害)(默认 7)。",
                "Phase-3 void bomb explosion damage, excluding MagicFire lingering (default 7).")
                .defineInRange("phase3VoidBombPerBoom", 7.0, 0.0, 1.0E7);
        CAT_HEAD_RED_PER_HIT = builder.comment("二阶段红猫头(狱火) 直接命中伤害(默认 8)。",
                "Phase-2 red cat-head (Hellfire) direct-hit damage (default 8).")
                .defineInRange("catHeadRedPerHit", 8.0, 0.0, 1.0E7);
        CAT_HEAD_BLUE_PER_HIT = builder.comment("二阶段蓝猫头(冰火) 直接命中伤害(默认 8)。",
                "Phase-2 blue cat-head (Ice) direct-hit damage (default 8).")
                .defineInRange("catHeadBluePerHit", 8.0, 0.0, 1.0E7);
        CAT_HEAD_YELLOW_PER_HIT = builder.comment("二阶段黄猫头(终末火) 直接命中伤害(默认 10，魔法伤害)。",
                "Phase-2 yellow cat-head (Void/MagicFire) direct-hit damage (default 10, magic)")
                .defineInRange("catHeadYellowPerHit", 10.0, 0.0, 1.0E7);
        CAT_HEAD_FREEZE_TICKS = builder.comment("蓝猫头冰冻 tick 数(默认 200)。 Freeze ticks of blue cat-head (default 200).")
                .defineInRange("catHeadFreezeTicks", 200, 1, 100000);
        builder.pop();


        /* ---------- 亚小猫仆从 起床礼物 ---------- */
        builder.comment("亚小猫仆从 陪主人睡觉后起床赠送的礼物(随机取其一)。",
                "Wake-up gift list given by the cat servant after sleeping with its owner (one chosen at random).")
                .push("gifts");
        GIFT_ITEMS = builder.comment("礼物物品注册名列表。",
                "Gift item registry-name list (namespace:path).")
                .defineList("giftItemIds",
                        Arrays.asList(
                                "goety:shadow_essence",
                                "goety:forbidden_fragment",
                                "goety:forbidden_piece",
                                "goety:treasure_pouch",
                                "goety:ominous_shard",
                                "minecraft:echo_shard",
                                "minecraft:honey_bottle",
                                "minecraft:golden_apple"),
                        o -> o instanceof String);
        builder.pop();

        SPEC = builder.build();
    }

    /** 王冠数值列表：定义 1~6 级默认值。 Crown per-level value list (defaults for levels 1-6). */
    private static ModConfigSpec.ConfigValue<List<? extends Double>> crownList(
            ModConfigSpec.Builder builder, String name, String comment, Double... defaults) {
        builder.comment(comment);
        return builder.defineList(name, Arrays.asList(defaults), o -> o instanceof Double);
    }

    /**
     * 读取配置值；若配置尚未加载则回退到定义时的默认值。
     * ─────────────────────────────────────────────────────────
     * 1.21 里实体属性表（EntityAttributeCreationEvent）是在 common 配置绑定**之前**构建的，
     * 此时直接 {@code ConfigValue#get()} 会抛 "Cannot get config value before config is loaded."。
     * 属性数量级的默认值只需保证出生时正确，实体进入世界时还会由
     * {@code setConfigurableAttributes()}（Goety 的 ICustomAttributes 机制）按真实配置刷新，
     * 因此这里与 Goety 的 {@code AttributesConfig.get(...)} 用同一套兜底逻辑。
     */
    public static <T> T get(ModConfigSpec.ConfigValue<T> configValue) {
        try {
            return configValue.get();
        } catch (IllegalStateException exception) {
            if (!"Cannot get config value before config is loaded.".equals(exception.getMessage())) {
                throw exception;
            }
            return configValue.getDefault();
        }
    }

    /**
     * 读取王冠某等级数值（按列表长度截断：列表不足 6 个时超出部分取末位，空表返回 0）。
     * Read a crown value for a given level (clamped to the list; 0 if empty).
     */
    public static double crownValue(ModConfigSpec.ConfigValue<List<? extends Double>> key, int level) {
        List<? extends Double> list = key.get();
        if (list == null || list.isEmpty()) {
            return 0.0D;
        }
        int idx = Math.max(0, Math.min(level - 1, list.size() - 1));
        Double v = list.get(idx);
        return v == null ? 0.0D : v;
    }
}
