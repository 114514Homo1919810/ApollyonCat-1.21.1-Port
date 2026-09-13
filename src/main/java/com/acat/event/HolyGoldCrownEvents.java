package com.acat.event;

import com.Polarice3.Goety.api.entities.IOwned;
import com.Polarice3.Goety.init.ModAttributes;
import com.acat.AcatMod;
import com.acat.item.HolyGoldCrownItem;
import com.acat.network.client.CrownSelectPacket;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 圣金王冠 全部被动逻辑（服务端执行）
 * ─────────────────────────────────────────────────────────
 * 触发条件：王冠必须戴在 Curios 饰品槽（放进背包不生效）。
 *
 * ★ 唯一性：同一玩家同一时间最多只生效【一顶】圣金王冠 —— 物品 canEquip() 已拒绝
 *   佩戴第二顶；若旧档/其它模组多槽位等原因仍同时存在多顶，本类 equippedCrown()
 *   会扫描全部饰品槽并只取【等级最高】的那顶作为唯一生效源，其余王冠不产生任何效果。
 *
 * 等级数值一律读取 HolyGoldCrownItem 里的静态表，避免与 Tooltip 两处漂移。
 *
 *  ▸ 玩家属性（每 tick 同步，戴上才给，脱下即清）：
 *      goety:casting_speed / soul_discount / cooldown_discount
 *      —— 施法时间 1 级 -50% 由 MagicHatItem 底座(Goety ReduceCastTime)承担，
 *         差额 60/70/80% 由 casting_speed 属性补足。
 *  ▸ 玩家减伤 + 指定护卫分摊一半（LivingDamageEvent.Pre.HIGHEST，Lv2+ 减伤 / Lv3+ 分摊）。
 *  ▸ 仆从侧（服务端每秒全局扫描一次所有已加载仆从）：
 *      最大生命 / 伤害 属性修饰符 + 每秒回血（Lv3+）。
 *  ▸ 仆从伤害减免（Lv2+）。
 *  ▸ 仆从死亡不死图腾式复活（Lv5 900s / Lv6 600s，冷却记在仆从自身 NBT，每只独立）。
 *  ▸ Lv6：仆从命中时先附加一段 100% 全穿透伤害（无视无敌帧/护甲/附魔/抗性）。
 *  ▸ 护卫管理（controls v2，客户端在攻击管线前拦截左键经 C2S 包上报）：
 *      手持王冠 左键自己的仆从 → 指定/取消护卫（最多 9 个，名单存在王冠 NBT）；
 *      手持王冠 Shift＋左键(任意目标) → 一键清空全部护卫（护卫阵亡后也能清）。
 */
@EventBusSubscriber(modid = AcatMod.MOD_ID)
public class HolyGoldCrownEvents {

    /** 全穿透伤害类型（data/aaacat/damage_type/holy_pierce.json + 6 个 minecraft bypass 标签）。 */
    private static final ResourceKey<DamageType> PIERCE_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "holy_pierce"));

    /* 属性修饰符 id：1.21 起 AttributeModifier 用 ResourceLocation 标识（旧版为 UUID + 名称字符串）。 */
    private static final ResourceLocation CAST_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "holy_crown_casting");
    private static final ResourceLocation SOUL_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "holy_crown_soul");
    private static final ResourceLocation COOL_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "holy_crown_cooldown");
    private static final ResourceLocation SERVANT_HP_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "holy_crown_servant_hp");
    private static final ResourceLocation SERVANT_ATK_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "holy_crown_servant_atk");

    /** 服务端缓存：在线玩家 → 其当前佩戴王冠等级(0=没戴)。 */
    private static final Map<UUID, Integer> CROWN_LEVEL = new HashMap<>();

    /** 复活冷却记录键（存仆从自身持久 NBT，毫秒时间戳截止）。 */
    private static final String REVIVE_DEADLINE = "aaacat.holy_crown_revive_deadline";

    /* ===================================================================== */
    /* 玩家：每 tick 同步属性 + 刷新等级缓存                                    */
    /* ===================================================================== */

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        ItemStack crown = equippedCrown(player);
        int lv = crown.isEmpty() ? 0 : HolyGoldCrownItem.getLevel(crown);
        if (lv > 0) {
            CROWN_LEVEL.put(player.getUUID(), lv);
        } else {
            CROWN_LEVEL.remove(player.getUUID());
        }
        syncPlayerAttributes(player, lv);
    }

    /** 按等级写入玩家三项全法术属性（值=修饰符增量；无冠=0 即移除）。 */
    private static void syncPlayerAttributes(Player player, int lv) {
        double cast = lv > 0 ? HolyGoldCrownItem.castSpeedBonus(lv) : 0.0;
        double soul = lv > 0 ? HolyGoldCrownItem.soulDiscount(lv) : 0.0;
        double cool = lv > 0 ? HolyGoldCrownItem.coolDiscount(lv) : 0.0;
        setModifier(player, ModAttributes.CASTING_SPEED, CAST_ID, cast,
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, ModAttributes.SOUL_DISCOUNT, SOUL_ID, soul,
                AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, ModAttributes.COOLDOWN_DISCOUNT, COOL_ID, cool,
                AttributeModifier.Operation.ADD_VALUE);
    }

    /* ===================================================================== */
    /* 仆从：服务端每秒全局扫描（跨维度可用，主人离线自动清除）                  */
    /* ===================================================================== */

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null || server.getTickCount() % 20 != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof IOwned owned)) {
                    continue;
                }
                UUID ownerId = owned.getOwnerId();
                if (ownerId == null || !(entity instanceof LivingEntity servant)) {
                    continue;
                }
                int lv = CROWN_LEVEL.getOrDefault(ownerId, 0);
                applyServantTick(servant, lv);
            }
        }
    }

    /** 同步仆从属性修饰符 + 每秒回血。lv=0 → 全部移除。 */
    private static void applyServantTick(LivingEntity servant, int lv) {
        double hpMulti = lv > 0 ? HolyGoldCrownItem.servantHp(lv) / 100.0D : 0.0D;
        double atkMulti = lv > 0 ? HolyGoldCrownItem.servantDmg(lv) / 100.0D : 0.0D;

        setModifier(servant, Attributes.MAX_HEALTH, SERVANT_HP_ID, hpMulti,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(servant, Attributes.ATTACK_DAMAGE, SERVANT_ATK_ID, atkMulti,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (servant.getHealth() > servant.getMaxHealth()) {
            servant.setHealth(servant.getMaxHealth());
        }
        // 每秒回血（Lv3+）
        double regen = lv > 0 ? HolyGoldCrownItem.servantRegen(lv) : 0.0D;
        if (regen > 0 && servant.isAlive() && servant.getHealth() < servant.getMaxHealth()) {
            servant.heal((float) (servant.getMaxHealth() * regen / 100.0D));
        }
    }

    /* ===================================================================== */
    /* 仆从减伤                                                               */
    /* ===================================================================== */

    @SubscribeEvent
    public static void onServantHurt(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getSource().is(PIERCE_KEY)) {
            return; // 全穿透伤害不再吃减伤
        }
        if (!(target instanceof IOwned owned)) {
            return;
        }
        UUID ownerId = owned.getOwnerId();
        if (ownerId == null) {
            return;
        }
        int lv = CROWN_LEVEL.getOrDefault(ownerId, 0);
        int dr = lv > 0 ? HolyGoldCrownItem.servantDr(lv) : 0;
        if (dr > 0) {
            event.setNewDamage(event.getNewDamage() * (1.0F - dr / 100.0F));
        }
    }

    /* ===================================================================== */
    /* 玩家减伤 + 指定护卫分摊一半（可致死）                                    */
    /* ===================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide || event.getSource().is(PIERCE_KEY)) {
            return;
        }
        ItemStack crown = equippedCrown(player);
        if (crown.isEmpty()) {
            return;
        }
        int lv = HolyGoldCrownItem.getLevel(crown);
        int dr = HolyGoldCrownItem.playerDr(lv);
        float amount = event.getNewDamage();
        if (amount <= 0) {
            return;
        }

        if (HolyGoldCrownItem.hasShare(lv)) {
            List<LivingEntity> guards = aliveGuards(player, crown);
            if (!guards.isEmpty()) {
                float playerHalf = amount * 0.5F;
                // 玩家这一半，再吃玩家自己的减伤
                event.setNewDamage(playerHalf * (1.0F - dr / 100.0F));
                // 仆从那一半，由存活护卫平分（各自再吃自己的减伤/护甲，可致死）
                float each = playerHalf / guards.size();
                for (LivingEntity guard : guards) {
                    if (!guard.isAlive()) {
                        continue;
                    }
                    guard.invulnerableTime = 0; // 保证分摊伤害真实落地
                    guard.hurt(event.getSource(), each);
                }
                return;
            }
        }
        event.setNewDamage(amount * (1.0F - dr / 100.0F));
    }

    /**
     * 王冠名单里的存活护卫（跨维度搜索已加载实体，须仍归该玩家所有）。
     *
     * ★ 修复点：名单里存的是【仆从自身的 UUID】；过去误把它与 owned.getOwnerId()
     *   (主人的 UUID) 比较，导致永远匹配不上 → 护卫名单形同虚设、从不替玩家挡伤。
     *   现在只校验“找到的实体仍然归受伤玩家所有”：player.getUUID().equals(ownerId)。
     */
    private static List<LivingEntity> aliveGuards(Player player, ItemStack crown) {
        List<LivingEntity> guards = new ArrayList<>();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return guards;
        }
        for (UUID servantUuid : HolyGoldCrownItem.getServants(crown)) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(servantUuid);
                if (entity instanceof LivingEntity le && le.isAlive()
                        && le instanceof IOwned owned
                        && player.getUUID().equals(owned.getOwnerId())) {
                    guards.add(le);
                    break;
                }
            }
        }
        return guards;
    }

    /* ===================================================================== */
    /* 仆从死亡复活（不死图腾式，满血，每只独立冷却）                           */
    /* ===================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServantDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        if (!(mob instanceof IOwned owned)) {
            return;
        }
        UUID ownerId = owned.getOwnerId();
        if (ownerId == null) {
            return;
        }
        int lv = CROWN_LEVEL.getOrDefault(ownerId, 0);
        int cooldownSec = lv > 0 ? HolyGoldCrownItem.reviveCooldown(lv) : 0;
        if (cooldownSec <= 0) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        long now = System.currentTimeMillis();
        if (now < data.getLong(REVIVE_DEADLINE)) {
            return; // 该仆从冷却未好 → 正常死亡
        }
        event.setCanceled(true);
        data.putLong(REVIVE_DEADLINE, now + cooldownSec * 1000L);

        mob.setHealth(mob.getMaxHealth());
        mob.removeAllEffects();
        mob.invulnerableTime = 20;
        mob.hurtTime = 0;
        mob.setLastHurtByMob(null);
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                    32, 0.4D, 0.6D, 0.4D, 0.6D);
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (owned.getTrueOwner() instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "info.aaacat.holy_gold_crown.revive", mob.getDisplayName(), cooldownSec), true);
        }
    }

    /* ===================================================================== */
    /* Lv6：仆从命中 → 先打一段等量全穿透伤害，再走原本伤害                     */
    /* ===================================================================== */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServantAttackPierce(LivingIncomingDamageEvent event) {
        if (event.isCanceled()) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || event.getSource().is(PIERCE_KEY)) {
            return; // 穿透段自身不再触发
        }
        Entity causing = event.getSource().getEntity();
        if (!(causing instanceof LivingEntity attacker) || !(attacker instanceof IOwned owned)) {
            return;
        }
        UUID ownerId = owned.getOwnerId();
        if (ownerId == null) {
            return;
        }
        if (target.getUUID().equals(ownerId) || target == attacker) {
            return;
        }
        int lv = CROWN_LEVEL.getOrDefault(ownerId, 0);
        if (!HolyGoldCrownItem.hasPierce(lv)) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0) {
            return;
        }
        // ① 先打 100% 全穿透段（holy_pierce 带全部 bypass 标签，内层 hurt() 重进本事件时被上方
        //    is(PIERCE_KEY) 提前拦下 —— 这就是防递归/防无限叠加的天然闸门）。
        DamageSource pierce = pierceSource(target.level(), attacker);
        if (pierce != null) {
            target.hurt(pierce, amount);
        }
        // ② 穿透段未击杀 → 清掉目标无敌帧，让外层 LivingEntity.hurt() 的无敌帧分支(20>10 且
        //    amount<=lastHurt)不再吞掉原伤害 —— 顺序即"先穿透、后原伤害"(原伤害照常过护甲等结算)。
        //    穿透段已击杀 → 不清：外层 hurt() 会在无敌帧分支直接 return false，恰好不会走到收尾的
        //    isDeadOrDying() → die() 区块，避免对尸体二次触发死亡/掉落/二次事件。
        if (target.isAlive()) {
            target.invulnerableTime = 0;
        }
    }

    private static DamageSource pierceSource(Level level, Entity attacker) {
        try {
            Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(PIERCE_KEY);
            return new DamageSource(holder, attacker, attacker);
        } catch (Exception ignored) {
            return null;
        }
    }

    /* ===================================================================== */
    /* 护卫管理（controls v2）                                                 */
    /* --------------------------------------------------------------------- */
    /* 为什么不能再用攻击事件：Goety OwnerAttackCancel(默认 true) 会在服务端        */
    /* 取消“主人攻击自己仆从”的 AttackEntityEvent / LivingIncomingDamageEvent，   */
    /* 旧实现永远收不到命中。现在改为：客户端在攻击管线之前(InteractionKeyMappingTriggered) */
    /* 拦截左键，把意图经 C2S 包(CrownSelectPacket)发来，本方法做归属校验 + 名单修改： */
    /*   左键(不潜行) + 指向生物   → 指定/取消 该生物为护卫；                      */
    /*   Shift＋左键(任意目标)     → 一键清空全部护卫。                            */
    /* 服务端不依赖攻击事件、也不产生任何伤害。                                    */
    /* ===================================================================== */

    public static void handleCrownSelect(ServerPlayer player, int mode, int entityId) {
        if (player.level().isClientSide) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof HolyGoldCrownItem)) {
            return;
        }

        // ── 模式 1：Shift＋左键 一键清空全部护卫（无需瞄准仆从） ──
        if (mode == CrownSelectPacket.MODE_CLEAR_ALL) {
            if (!player.isShiftKeyDown()) {
                return; // 客户端会保证潜行；这里再兜底防伪造
            }
            int removed = HolyGoldCrownItem.clearServants(stack);
            if (removed > 0) {
                player.displayClientMessage(Component.translatable(
                        "info.aaacat.holy_gold_crown.cleared", removed), true);
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5F, 1.2F);
            } else {
                player.displayClientMessage(Component.translatable(
                        "info.aaacat.holy_gold_crown.noGuards"), true);
            }
            return;
        }

        // ── 模式 0：左键(不潜行) + 指向生物 → 指定/取消护卫 ──
        if (mode != CrownSelectPacket.MODE_SELECT || player.isShiftKeyDown()) {
            return;
        }
        Entity target = player.level().getEntity(entityId);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return; // 实体不在/已死：什么都不做
        }
        // 客户端准心选取本身已限定在实体交互距离内；这里再做一层距离兜底，
        // 防止伪造 C2S 包去远程指定/取消远处的实体（只影响名单，不产生伤害）。
        if (player.distanceToSqr(living) > 64.0D) {
            return;
        }
        if (living instanceof IOwned owned && player.getUUID().equals(owned.getOwnerId())) {
            UUID uuid = living.getUUID();
            if (HolyGoldCrownItem.isServantSelected(stack, uuid)) {
                HolyGoldCrownItem.toggleServant(stack, uuid);
                player.displayClientMessage(Component.translatable(
                        "info.aaacat.holy_gold_crown.unselect", living.getDisplayName()), true);
            } else if (HolyGoldCrownItem.getServantCount(stack) >= HolyGoldCrownItem.MAX_SERVANTS) {
                player.displayClientMessage(Component.translatable(
                        "info.aaacat.holy_gold_crown.full", HolyGoldCrownItem.MAX_SERVANTS), true);
            } else {
                HolyGoldCrownItem.toggleServant(stack, uuid);
                player.displayClientMessage(Component.translatable(
                        "info.aaacat.holy_gold_crown.select", living.getDisplayName(),
                        HolyGoldCrownItem.getServantCount(stack), HolyGoldCrownItem.MAX_SERVANTS), true);
            }
        } else {
            // 左键指向的不是“自己的仆从”：只提示，不产生攻击
            player.displayClientMessage(Component.translatable(
                    "info.aaacat.holy_gold_crown.notServant"), true);
        }
    }

    /* ===================================================================== */
    /* 工具                                                                   */
    /* ===================================================================== */

    /**
     * 读取玩家当前“唯一生效”的圣金王冠：扫描全部 Curios 饰品槽，只返回【等级最高】
     * 的那一顶。所有被动(属性/仆从增益/护卫分摊/减伤)都只认这一顶，保证
     * “只能生效一顶、优先最高级”，即使身上因旧档/多槽位存在多顶也不会叠加。
     */
    private static ItemStack equippedCrown(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        Optional<List<SlotResult>> equipped = CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(
                        s -> !s.isEmpty() && s.getItem() instanceof HolyGoldCrownItem));
        if (equipped.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack best = ItemStack.EMPTY;
        int bestLevel = 0;
        for (SlotResult result : equipped.get()) {
            ItemStack stack = result.stack();
            if (stack.isEmpty() || !(stack.getItem() instanceof HolyGoldCrownItem)) {
                continue;
            }
            int lv = HolyGoldCrownItem.getLevel(stack);
            if (lv > bestLevel) {
                bestLevel = lv;
                best = stack;
            }
        }
        return best;
    }

    /** 写入/移除单条属性修饰符：want==0 移除；数值变化才重写（避免每 tick 抖动）。 */
    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id,
                                    double want, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (want == 0.0D) {
            if (current != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (current == null || current.amount() != want) {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, want, operation));
        }
    }
}
