package com.acat.event;

import com.Polarice3.Goety.common.blocks.entities.ArcaBlockEntity;
import com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy;
import com.Polarice3.Goety.common.events.ArcaTeleporter;
import com.Polarice3.Goety.config.MainConfig;
import com.Polarice3.Goety.utils.SEHelper;
import com.acat.AcatMod;
import com.acat.config.AcatConfig;
import com.acat.item.HolySoulArkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * 圣金魂匣 核心逻辑（全部在服务端执行）
 * ─────────────────────────────────────────────────────────
 * 【设计 v3：魂匣在背包 = 魂匣接管死亡，灵魂方舟被完全拦截】
 * 旧实现(v1)劫持 Goety Arca 指针 → 绑定点无方舟导致 SEActive 被 Goety 反复关闭 → 灵魂饥饿；
 * v2 修复饥饿，但“未绑定+有方舟”仍交还 Goety 原生复活、且“魂量 < MaxSouls”会直接死亡，
 * 均与本需求冲突。v3 规则：
 *
 *  1. 绑定坐标只存在魂匣 NBT，绝不写入 se.arcaBlock；
 *     · 有真实方舟 → 指针与 SEActive 交给 Goety 维护(校验通过即不饥饿)；
 *     · 无方舟 → se.arcaBlock 保持 null，由我们维持 SEActive=true(灵魂入账，无饥饿)；
 *     · 同维已失效方舟指针 → 顺手清空，避免 Goety 空转。
 *  2. 死亡(LivingDeathEvent.HIGHEST，先于 Goety)：
 *     · 背包里有【属于该玩家的魂匣】(无主则当场归属) → 魂匣接管死亡：
 *         已绑定 → 传送回绑定坐标复活；未绑定 → 原地复活；
 *       与灵魂方舟一致：灵魂能量 ≥ MaxSouls(10000) 才触发复活，一次性扣 10000；
 *       灵魂能量不足 10000 → 不复活、不取消死亡，正常死亡。
 *       复活成功后再临时关 SEActive 拦截 Goety 方舟/图腾二次复活(下一 tick 自动恢复)。
 *     · 完全没有魂匣 → 完全交还 Goety 原生(方舟照常)。
 *  3. 全部功能仅对“终身持有者(Owner UUID 匹配)”生效；死亡瞬间背包里的无主魂匣自动归属死者。
 *
 * 【持有加成】(持有者，背包任意槽/副手)
 *   · 每秒 +333 灵魂能量
 *   · 每 10000 灵魂能量 → +2 最大生命、+1 攻击(AttributeModifier 动态档位)
 *   · 每秒消耗 100 灵魂能量 → 恢复 1% 最大生命(血量不满且魂量足够时)
 *
 * 【Shift+左键】取消绑定坐标：点方块(服务端 LeftClickBlock) / 点实体(服务端 LivingAttackEvent) 均可；点【空气】由客户端 InteractionKeyMappingTriggered 截获后经 C2S 包请求解绑
 */
@EventBusSubscriber(modid = AcatMod.MOD_ID)
public class HolySoulArkEvents {

    /* ================= 常量 ================= */

    // ★ 数值均读取 config/aaacat.toml -> holySoulArk.*（见 AcatConfig），此处不再写死。
    //   复活扣费仍取 Goety MainConfig.MaxSouls（默认 10000），与灵魂方舟口径一致。

    private static final ResourceLocation HP_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "holy_soul_ark.max_health");
    private static final ResourceLocation ATK_MOD_ID =
            ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "holy_soul_ark.attack_damage");

    /* ================= 每 tick：归属 / 维持 SE / 加成 ================= */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.isAlive()) {
            return;
        }
        ISoulEnergy se = SEHelper.getCapability(player);

        // 无主魂匣 → 首次携带即终身归属(不可更换)
        claimUnownedInInventory(player);

        ItemStack box = HolySoulArkItem.findUsableInInventory(player);
        boolean usable = !box.isEmpty();

        if (usable) {
            // 维持灵魂容器开启(Goety 饥饿分支要求 !SEActive 才触发，这里始终为 true)
            boolean dirty = false;
            if (!se.getSEActive()) {
                se.setSEActive(true);
                dirty = true;
            }
            // 清理“同维度已失效”的方舟指针，避免 Goety 校验每 tick 关 SEActive
            if (cleanupStaleArcaPointer(player, se)) {
                dirty = true;
            }
            if (dirty) {
                SEHelper.sendSEUpdatePacket(player);
            }

            if (player.tickCount % 20 == 0) {
                // ── 每秒 +333 灵魂能量(封顶由 Goety MaxArcaSouls 决定) ──
                int before = se.getSoulEnergy();
                boolean increased = se.increaseSE(AcatConfig.ARK_SOUL_PER_SECOND.get());
                if (increased || se.getSoulEnergy() != before) {
                    SEHelper.sendSEUpdatePacket(player);
                }
                // ── 每秒消耗 100 灵魂 → 恢复 1% 最大生命(含属性加成) ──
                if (se.getSoulEnergy() >= AcatConfig.ARK_HEAL_COST_PER_SECOND.get()
                        && player.getHealth() < player.getMaxHealth()) {
                    se.decreaseSE(AcatConfig.ARK_HEAL_COST_PER_SECOND.get());
                    player.heal(player.getMaxHealth() * (float) (AcatConfig.ARK_HEAL_PERCENT.get() / 100.0D));
                    SEHelper.sendSEUpdatePacket(player);
                }
            }

            // ── 每 10000 灵魂 → +2 最大生命 / +1 攻击(按档位动态增减) ──
            int tier = se.getSoulEnergy() / AcatConfig.ARK_TIER_SOUL_DIV.get();
            syncAttribute(player, Attributes.MAX_HEALTH, HP_MOD_ID,
                    tier * (double) AcatConfig.ARK_MAX_HEALTH_PER_TIER.get());
            syncAttribute(player, Attributes.ATTACK_DAMAGE, ATK_MOD_ID,
                    tier * (double) AcatConfig.ARK_ATTACK_PER_TIER.get());
        } else {
            // 无本玩家魂匣 → 移除全部加成(档位归零)
            syncAttribute(player, Attributes.MAX_HEALTH, HP_MOD_ID, 0.0D);
            syncAttribute(player, Attributes.ATTACK_DAMAGE, ATK_MOD_ID, 0.0D);
            // 无真实方舟 → SEActive 交还 Goety 默认(关闭)，与原生行为一致
            if (!isValidOwnedArca(player, se) && se.getSEActive()) {
                se.setSEActive(false);
                SEHelper.sendSEUpdatePacket(player);
            }
        }
    }

    /** 背包(36 格)+副手中的无主魂匣 → 终身归属当前玩家。 */
    private static void claimUnownedInInventory(Player player) {
        boolean claimed = false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof HolySoulArkItem
                    && !HolySoulArkItem.hasOwner(stack)) {
                HolySoulArkItem.setOwner(stack, player.getUUID(), player.getName().getString());
                claimed = true;
            }
        }
        ItemStack offhand = player.getInventory().offhand.get(0);
        if (!offhand.isEmpty() && offhand.getItem() instanceof HolySoulArkItem
                && !HolySoulArkItem.hasOwner(offhand)) {
            HolySoulArkItem.setOwner(offhand, player.getUUID(), player.getName().getString());
            claimed = true;
        }
        if (claimed) {
            player.displayClientMessage(Component.translatable("info.aaacat.holy_soul_ark.claim"), true);
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.6F);
        }
    }

    /** 清空“同维度但已失效(被拆/易主)”的方舟指针；跨维度指针不动(可能仍有效)。 */
    private static boolean cleanupStaleArcaPointer(Player player, ISoulEnergy se) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        BlockPos pos = se.getArcaBlock();
        ResourceKey<Level> dim = se.getArcaBlockDimension();
        if (pos == null || dim == null) {
            return false;
        }
        if (!dim.equals(player.level().dimension())) {
            return false;
        }
        ServerLevel level = serverPlayer.getServer().getLevel(dim);
        if (level == null) {
            return false;
        }
        BlockEntity tile = level.getBlockEntity(pos);
        if (!(tile instanceof ArcaBlockEntity arcaTile && arcaTile.getPlayer() == player)) {
            se.setArcaBlock(null);
            se.setArcaBlockDimension(null);
            return true;
        }
        return false;
    }

    /** 当前 capability 的 arca 是否指向一台真实存在且属于该玩家的方舟方块。 */
    private static boolean isValidOwnedArca(Player player, ISoulEnergy se) {
        BlockPos pos = se.getArcaBlock();
        ResourceKey<Level> dim = se.getArcaBlockDimension();
        if (pos == null || dim == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        ServerLevel level = serverPlayer.getServer().getLevel(dim);
        if (level == null) {
            return false;
        }
        BlockEntity tile = level.getBlockEntity(pos);
        return tile instanceof ArcaBlockEntity arcaTile && arcaTile.getPlayer() == player;
    }

    /**
     * 同步单条属性加成：目标值 want 与当前修饰符不一致时才重写(避免每 tick 抖动)；
     * want == 0 时移除。修饰符为 transient，死亡重生/登出后会丢失，
     * 因此每 tick 对照“是否存在/数值是否一致”即可自动恢复，无需持久化档位。
     */
    private static void syncAttribute(Player player, Holder<Attribute> attribute,
                                     ResourceLocation modifierId, double want) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(modifierId);
        if (want == 0.0D) {
            if (current != null) {
                instance.removeModifier(modifierId);
                clampHealth(player);
            }
            return;
        }
        if (current == null || current.amount() != want) {
            instance.removeModifier(modifierId);
            instance.addTransientModifier(new AttributeModifier(modifierId, want,
                    AttributeModifier.Operation.ADD_VALUE));
            clampHealth(player);
        }
    }

    /** 加成下调后，把当前血量压回新上限，避免出现“超上限”的血条。 */
    private static void clampHealth(Player player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /* ================= Shift + 左键：取消绑定坐标 ================= */

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        handleShiftLeftClick(event.getEntity(), event.getHand(), event);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        handleShiftLeftClick(event.getEntity(), event.getHand(), event);
    }

    private static void handleShiftLeftClick(Player player, InteractionHand hand, PlayerInteractEvent event) {
        if (player.level().isClientSide || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof HolySoulArkItem)) {
            return;
        }
        if (!HolySoulArkItem.isOwner(stack, player)) {
            return; // 非持有者：不产生任何效果，也不拦截普通交互
        }
        if (!HolySoulArkItem.isBound(stack)) {
            return; // 未绑定：无需取消
        }
        if (event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true); // 取消拆方块/挥空，仅执行取消绑定
        }
        doUnbind(player, stack);
    }

    /** Shift + 左键点实体(攻击)时同样取消绑定。 */
    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof HolySoulArkItem)) {
            return;
        }
        if (!HolySoulArkItem.isOwner(stack, player) || !HolySoulArkItem.isBound(stack)) {
            return;
        }
        event.setCanceled(true);
        doUnbind(player, stack);
    }

    private static void doUnbind(Player player, ItemStack stack) {
        HolySoulArkItem.clearBound(stack);
        player.displayClientMessage(Component.translatable("info.aaacat.holy_soul_ark.unbind"), true);
        player.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8F, 0.5F);
    }

    /** 客户端「shift+左键空气」发来的解绑请求（C2S 包 ArkUnbindPacket 处理）。 */
    public static void handleArkUnbind(ServerPlayer player) {
        if (player.level().isClientSide || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof HolySoulArkItem)) {
            return;
        }
        if (!HolySoulArkItem.isOwner(stack, player) || !HolySoulArkItem.isBound(stack)) {
            return; // 非持有者 / 未绑定：不产生任何效果
        }
        doUnbind(player, stack);
    }

    /* ================= 死亡复活(魂匣接管 + 拦截方舟) ================= */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        // 死亡瞬间兜底：背包里的无主魂匣归属死者(防归属 tick 尚未执行 / 旧档无主)
        claimUnownedInInventory(player);

        ISoulEnergy se = SEHelper.getCapability(player);
        int cost = MainConfig.MaxSouls.get();

        ItemStack boundBox = HolySoulArkItem.findBoundUsableInInventory(player);
        ItemStack anyBox = HolySoulArkItem.findUsableInInventory(player);

        // 背包里没有任何【属于该玩家】的魂匣 → 完全交还 Goety 原生(方舟/图腾照常)
        if (anyBox.isEmpty()) {
            return;
        }

        // ── 有魂匣：魂匣接管死亡 ──
        //    与灵魂方舟一致：灵魂能量 ≥ MaxSouls(10000) 才触发复活，一次性扣 10000。
        //    能量不足 → 不复活、不取消死亡，正常死亡(Goety 方舟同样要求 ≥ MaxSouls)。
        if (se.getSoulEnergy() < cost) {
            return;
        }
        //    已绑定 → 传送回绑定坐标；未绑定 → 原地复活。
        //    复活成功后临时关 SEActive 拦截 Goety 方舟二次复活(下一 tick 自动恢复)。
        event.setCanceled(true);

        BlockPos boxPos = null;
        ResourceKey<Level> boxDim = null;
        boolean teleport = false;
        if (!boundBox.isEmpty()) {
            boxPos = HolySoulArkItem.getBoundPos(boundBox);
            boxDim = HolySoulArkItem.getBoundDim(boundBox);
            teleport = boxPos != null && boxDim != null;
        }
        AcatMod.LOGGER.info("[aaacat] HolySoulArk revives {} ({}): {} at {}",
                player.getName().getString(), player.getUUID(),
                teleport ? "teleport-revive" : "in-place revive",
                teleport ? boxDim.location().toString() + " " + boxPos : "death spot");
        doRevive(player, se, cost, teleport ? boxPos : null, teleport ? boxDim : null);
        blockGoetyRevive(player, se);
    }

    /**
     * 执行魂匣复活(机制与灵魂方舟一致)：
     *  传送到目标点(重生锚站立点 → 向上找空位 → 强制落点)，失败也至少原地保命；
     *  回 1 血、清效果、再生/吸收/防火、Wither 音效；
     *  固定扣 MaxSouls(10000) 灵魂(与方舟一致；不足时调用方已放行死亡，不会走到这里)。
     * 全程 try/catch：任何一步异常都不会让“取消死亡”失效。
     */
    private static void doRevive(ServerPlayer player, ISoulEnergy se, int cost,
                                 @Nullable BlockPos targetPos, @Nullable ResourceKey<Level> targetDim) {
        try {
            if (targetPos != null && targetDim != null) {
                teleportPlayerTo(player, targetPos, targetDim);
            }
        } catch (Exception ignored) {
            // 传送失败 → 原地保命(死亡已取消)
        }
        try {
            player.setHealth(1.0F);
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_DEATH, player.getSoundSource(), 1.0F, 1.0F);
            se.decreaseSE(cost); // 固定扣 MaxSouls(10000)，与灵魂方舟完全一致
            SEHelper.sendSEUpdatePacket(player);
        } catch (Exception ignored) {
        }
    }

    /**
     * 拦截 Goety 的方舟/图腾二次复活：Goety 的死亡处理器不检查事件是否已取消，
     * 只要 SEActive=true 且其条件满足就会再救一次。这里临时把 SEActive 置 false，
     * 使其方舟分支短路(魂匣复活没有图腾，图腾分支不会触发)。
     * 下一 tick 的 HIGHEST 处理器会因背包魂匣而重新置回 true。
     */
    private static void blockGoetyRevive(ServerPlayer player, ISoulEnergy se) {
        se.setSEActive(false);
        SEHelper.sendSEUpdatePacket(player);
    }

    /**
     * 传送到目标点(与 SEHelper.teleportToArca 同思路，更强兜底)：
     * 优先重生锚站立点；找不到则在目标列向上逐格找空位(最多 8 格)；再找不到放目标上方。
     */
    private static void teleportPlayerTo(ServerPlayer player, BlockPos pos, ResourceKey<Level> dim) {
        BlockPos center = BlockPos.containing(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);

        if (dim == player.level().dimension()) {
            ServerLevel level = (ServerLevel) player.level();
            Vec3 target = findStandTarget(level, center);
            if (level.getWorldBorder().isWithinBounds(target.x, target.y, target.z)) {
                player.teleportTo(target.x, target.y, target.z);
            } else {
                BlockPos spawn = level.getSharedSpawnPos();
                player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
            }
        } else {
            ServerLevel dest = player.getServer() != null ? player.getServer().getLevel(dim) : null;
            if (dest == null) {
                return; // 目标维度不存在 → 原地(死亡已取消，玩家保命)
            }
            Vec3 target = findStandTarget(dest, center);
            if (dest.getWorldBorder().isWithinBounds(target.x, target.y, target.z)) {
                player.changeDimension(ArcaTeleporter.transition(dest, player, target));
                player.teleportTo(target.x, target.y, target.z);
            } else {
                BlockPos spawn = dest.getSharedSpawnPos();
                player.changeDimension(ArcaTeleporter.transition(dest, player,
                        Vec3.atCenterOf(spawn).subtract(0, 0.5, 0)));
                player.teleportTo(spawn.getX(), spawn.getY(), spawn.getZ());
            }
        }
    }

    /** 优先重生锚站立点；失败则在目标列向上找空位；再失败放目标上方。 */
    private static Vec3 findStandTarget(ServerLevel level, BlockPos center) {
        Optional<Vec3> optional = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, level, center);
        if (optional.isPresent()) {
            return optional.get();
        }
        double x = center.getX() + 0.5D;
        double z = center.getZ() + 0.5D;
        EntityDimensions dims = EntityType.PLAYER.getDimensions();
        int top = center.getY() + 8;
        for (int y = center.getY(); y <= top; ++y) {
            AABB box = dims.makeBoundingBox(x, y, z);
            if (level.noCollision(box)) {
                return new Vec3(x, y, z);
            }
        }
        return new Vec3(x, center.getY() + 0.5D, z);
    }
}
