package com.acat.item;

import com.acat.AcatMod;
import com.acat.config.AcatConfig;
import com.acat.registry.AcatDataComponents;
import com.Polarice3.Goety.common.items.curios.MagicHatItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 圣金王冠（aaacat:holy_gold_crown）
 * ─────────────────────────────────────────────────────────
 * 头饰饰品（Curios head 槽），右键直接可戴，戴上后渲染 3D 模型于头顶。
 *
 * 【等级系统】等级写在数据组件 aaacat:crown_level(1~6，缺省=1)，物品描述第一行显示当前等级，
 *   防止玩家查错合成表。锻造仪式合成/升级产出固定等级：
 *     - 初始配方(风暴王冠+祭品) → 1 级
 *     - 1~5 级各自有对应升级配方(锻造仪式) → 升到 2~6 级
 *
 * 【施法加速】本类继承 Goety 的 MagicHatItem(黑暗帽子同款底座)：
 *   Goety 的 ISpell.ReduceCastTime() 检测 Curio 槽物品 instanceof MagicHatItem
 *   即可让全法术施法时间减半(1 级 50% 的来源)，更高等级的差额由
 *   goety:casting_speed 属性修饰符在 HolyGoldCrownEvents 中按等级补足。
 *
 * 【指定护卫】(controls v2)
 *   - 左键(不潜行) 自己的仆从 → 加入/移出护卫名单(最多 9 个)；
 *   - Shift＋左键(无需瞄准仆从，点空气/方块/实体均可) → 一键清空全部护卫
 *     (防止护卫阵亡后无法用左键移除)。
 *   名单 UUID 存在这顶王冠自己的 NBT（换一顶冠 = 换一份名单）。
 *   佩戴者受伤时，Lv3+ 会由存活护卫平分承担一半伤害(可致死)。
 *
 * 【唯一性】每位玩家只能佩戴一顶圣金王冠：canEquip() 会在“身上已戴有另一顶”时
 *   拒绝再戴上新的；若因旧档/其它模组多槽位等原因同时有多顶，事件逻辑只取
 *   等级最高的那一顶作为唯一生效源(属性/仆从增益/护卫分摊都只看它)。
 *
 * 【Tooltip】第一行 = 等级；未按 Shift 只显示提示；按 Shift 展开全部属性/功能。
 */
public class HolyGoldCrownItem extends MagicHatItem {

    public static final int MAX_LEVEL = 6;
    public static final int MAX_SERVANTS = 9;

    /** NBT 键：1.20.1 旧版的等级键(1~6)。1.21 起等级存数据组件 aaacat:crown_level；此键仅用于读旧档迁移。 */
    public static final String TAG_LEVEL = "level";
    /** NBT 键：指定护卫 UUID 字符串列表(ListTag<String>，最多 9)。 */
    public static final String TAG_SERVANTS = "holy_crown_servants";

    public HolyGoldCrownItem() {
        super();
    }

    /* ---------- 掉落物防护：防火 / 防爆炸 / 防仙人掌（圣金魂匣同款） ----------
     * 注册走 SingleStackItem 无参构造(Properties 拿不到 fireResistant)，故用 canBeHurtBy
     * 兜住爆炸/仙人掌；「防火」组件由 AcatItemDefaults（ModifyDefaultComponentsEvent）补上
     * —— 1.21 起 Item#isFireResistant() 已删除，防火 = 物品默认组件 minecraft:fire_resistant。 */
    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.CACTUS)) {
            return false;
        }
        return super.canBeHurtBy(stack, source);
    }

    /* ==================== 唯一性：只允许“身上戴一顶” ==================== */

    /**
     * Curios 在装备/校验槽位时都会调用本方法。
     * 规则：如果佩戴者已经在任一 Curios 槽里戴了【另一顶】圣金王冠，则拒绝再戴上新的
     * (只能佩戴一顶)。同一顶已经戴在身上的王冠在重载/换槽校验时放行，不会被误踢。
     */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        if (slotContext == null || slotContext.entity() == null) {
            return true;
        }
        LivingEntity wearer = slotContext.entity();
        Optional<List<SlotResult>> equipped = CuriosApi.getCuriosInventory(wearer)
                .map(inventory -> inventory.findCurios(
                        s -> !s.isEmpty() && s.getItem() instanceof HolyGoldCrownItem));
        if (equipped.isEmpty()) {
            return true;
        }
        boolean sameStackWorn = false;
        int wornCrowns = 0;
        for (SlotResult result : equipped.get()) {
            wornCrowns++;
            if (result.stack() == stack) {
                sameStackWorn = true; // 这顶本身已戴在身上(重载/槽位刷新校验) → 放行
            }
        }
        if (sameStackWorn) {
            return true;
        }
        return wornCrowns == 0; // 身上还戴着别的王冠 → 不允许再戴第二顶
    }

    /* ==================== 等级 ==================== */

    public static int getLevel(ItemStack stack) {
        if (stack != null) {
            int level = stack.getOrDefault(AcatDataComponents.CROWN_LEVEL.get(), 0);
            if (level == 0) {
                // 兼容 1.20.1 旧存档：原版物品数据升级会把旧根 NBT 搬进 minecraft:custom_data，
                // 旧版的等级键「level」仍在那里，读出来当作组件缺省值(一次性迁移语义，不写回)。
                level = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(TAG_LEVEL);
            }
            if (level >= 1 && level <= MAX_LEVEL) {
                return level;
            }
        }
        return 1;
    }

    public static void setLevel(ItemStack stack, int level) {
        stack.set(AcatDataComponents.CROWN_LEVEL.get(), Math.max(1, Math.min(MAX_LEVEL, level)));
    }

    /* ==================== 指定护卫名单(存于王冠 NBT) ==================== */

    /** 当前名单（可能含已失效 UUID，逻辑处会过滤）。 */
    public static List<UUID> getServants(ItemStack stack) {
        List<UUID> list = new ArrayList<>();
        if (stack != null) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains(TAG_SERVANTS, Tag.TAG_LIST)) {
                ListTag listTag = tag.getList(TAG_SERVANTS, Tag.TAG_STRING);
                for (int i = 0; i < listTag.size() && i < MAX_SERVANTS; i++) {
                    String s = listTag.getString(i);
                    try {
                        list.add(UUID.fromString(s));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return list;
    }

    public static int getServantCount(ItemStack stack) {
        return getServants(stack).size();
    }

    public static boolean isServantSelected(ItemStack stack, UUID uuid) {
        return getServants(stack).contains(uuid);
    }

    /** 切换指定状态；返回切换后是否处于“已指定”。 */
    public static boolean toggleServant(ItemStack stack, UUID uuid) {
        List<UUID> list = getServants(stack);
        if (list.contains(uuid)) {
            list.remove(uuid);
            writeServants(stack, list);
            return false;
        }
        if (list.size() >= MAX_SERVANTS) {
            return false;
        }
        list.add(uuid);
        writeServants(stack, list);
        return true;
    }

    /** 一键清空全部护卫；返回被移除的数量。 */
    public static int clearServants(ItemStack stack) {
        int count = getServants(stack).size();
        if (count > 0) {
            writeServants(stack, new ArrayList<>());
        }
        return count;
    }

    private static void writeServants(ItemStack stack, List<UUID> list) {
        ListTag listTag = new ListTag();
        for (UUID uuid : list) {
            listTag.add(StringTag.valueOf(uuid.toString()));
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(TAG_SERVANTS, listTag));
    }

    /* ==================== 显示 ==================== */

    /** 物品名：金色（传说级，不依赖稀有度着色）。 */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.GOLD);
    }

    /** 允许从使用中右键直接装备（同 Goety 帽子；SingleStackItem 本身也允许）。 */
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int lv = getLevel(stack);

        // 第一行：等级（防止玩家查错合成表）
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.level", lv, MAX_LEVEL)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.shiftHint")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        ChatFormatting main = ChatFormatting.DARK_AQUA;
        ChatFormatting sub = ChatFormatting.GRAY;

        // ── 玩家侧效果 ──
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.head.player").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.cast", castPercent(lv)).withStyle(main));
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.soul", soulPercent(lv)).withStyle(main));
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.cool", coolPercent(lv)).withStyle(main));
        if (playerDr(lv) > 0) {
            tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.playerDr", playerDr(lv)).withStyle(main));
        }
        if (hasShare(lv)) {
            tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.share").withStyle(main));
        }

        // ── 指定护卫 ──
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.head.guards").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.selected", getServantCount(stack), MAX_SERVANTS)
                .withStyle(sub));
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.selectHint").withStyle(sub));

        // ── 仆从侧效果 ──
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.head.servant").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.servantHp", servantHp(lv)).withStyle(main));
        if (servantDr(lv) > 0) {
            tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.servantDr", servantDr(lv)).withStyle(main));
        }
        if (servantDmg(lv) > 0) {
            tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.servantDmg", servantDmg(lv)).withStyle(main));
        }
        if (servantRegen(lv) > 0) {
            tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.servantRegen", servantRegenDisplay(lv)).withStyle(main));
        }
        if (reviveCooldown(lv) > 0) {
            tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.servantRevive", reviveCooldown(lv)).withStyle(main));
        }
        if (hasPierce(lv)) {
            tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.feat.servantPierce")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.onlyWorn")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.aaacat.holy_gold_crown.onlyOne")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /* ==================== 各等级数值表（供 Tooltip 与事件共用） ==================== */

    /** 施法时间总减少 %（1~6 级；显示用：1 级 -50% 来自 MagicHatItem 底座，差额见 castSpeedBonus）。 */
    public static int castPercent(int lv) {
        return (int) Math.round(AcatConfig.crownValue(AcatConfig.CROWN_CAST_PERCENT, lv));
    }

    /** goety:casting_speed 属性实际补足增量（1~6 级，读取配置 holyGoldCrown.castSpeedBonus）。 */
    public static double castSpeedBonus(int lv) {
        return AcatConfig.crownValue(AcatConfig.CROWN_CAST_SPEED_BONUS, lv);
    }

    /** 灵魂消耗降低 %（显示用；实际增量见 soulDiscount）。 */
    public static int soulPercent(int lv) {
        return (int) Math.round(AcatConfig.crownValue(AcatConfig.CROWN_SOUL_PERCENT, lv));
    }

    /** goety:soul_discount 属性实际增量（1~6 级）。 */
    public static double soulDiscount(int lv) {
        return AcatConfig.crownValue(AcatConfig.CROWN_SOUL_DISCOUNT, lv);
    }

    /** 冷却降低 %（显示用；实际增量见 coolDiscount）。 */
    public static int coolPercent(int lv) {
        return (int) Math.round(AcatConfig.crownValue(AcatConfig.CROWN_COOL_PERCENT, lv));
    }

    /** goety:cooldown_discount 属性实际增量（1~6 级）。 */
    public static double coolDiscount(int lv) {
        return AcatConfig.crownValue(AcatConfig.CROWN_COOL_DISCOUNT, lv);
    }

    /** 玩家伤害减免 %。 */
    public static int playerDr(int lv) {
        return (int) Math.round(AcatConfig.crownValue(AcatConfig.CROWN_PLAYER_DR, lv));
    }

    /** 仆从最大生命加成 %。 */
    public static int servantHp(int lv) {
        return (int) Math.round(AcatConfig.crownValue(AcatConfig.CROWN_SERVANT_HP, lv));
    }

    /** 仆从伤害减免 %。 */
    public static int servantDr(int lv) {
        return (int) Math.round(AcatConfig.crownValue(AcatConfig.CROWN_SERVANT_DR, lv));
    }

    /** 仆从伤害提升 %。 */
    public static int servantDmg(int lv) {
        return (int) Math.round(AcatConfig.crownValue(AcatConfig.CROWN_SERVANT_DMG, lv));
    }

    /** 仆从每秒回血 %（如 0.5 / 1 / 2）。 */
    public static double servantRegen(int lv) {
        return AcatConfig.crownValue(AcatConfig.CROWN_SERVANT_REGEN, lv);
    }

    private static String servantRegenDisplay(int lv) {
        double v = servantRegen(lv);
        return v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** 仆从复活冷却（秒），0 = 本等级无复活。 */
    public static int reviveCooldown(int lv) {
        return (int) Math.round(AcatConfig.crownValue(AcatConfig.CROWN_REVIVE_COOLDOWN, lv));
    }

    /** 是否需要 指定护卫分摊一半伤害。 */
    public static boolean hasShare(int lv) {
        return lv >= 3;
    }

    /** 是否需要 仆从额外 100% 穿透伤害。 */
    public static boolean hasPierce(int lv) {
        return lv >= MAX_LEVEL;
    }
}
