package com.acat.client;

import com.acat.AcatMod;
import com.acat.item.HolyGoldCrownItem;
import com.acat.item.HolySoulArkItem;
import com.acat.network.client.ArkUnbindPacket;
import com.acat.network.client.CrownSelectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端专用：把「手持 aaacat 道具 + 左键」从普攻语义改为「护卫管理/解绑」语义。
 * ─────────────────────────────────────────────────────────
 * NeoForge 在每次左键点击进入攻击管线之前(Minecraft.startAttack → ClientHooks.onClickInput)
 * 都会派发 InputEvent.InteractionKeyMappingTriggered；此刻 Minecraft.hitResult 就是
 * 准心指向的第一个命中物(实体/方块/空气)，且事件可取消 —— 取消后不会走
 * gameMode.attack(实体) / startDestroyBlock(方块) / onEmptyLeftClick(空气)。
 *
 *  ▸ 圣金王冠：
 *      · 左键(不潜行) + 指向【生物】→ 取消本次攻击并 C2S 上报该实体 ID
 *        (服务端校验是不是你自己的仆从：是 → 指定/取消护卫)。
 *      · Shift＋左键【任意目标：空气/方块/实体都行】→ 取消本次交互并 C2S 请求
 *        一键清空这顶王冠的全部护卫(不再需要逐只瞄准，护卫阵亡后也能清)。
 *      · 其余左键(点方块/空气但不潜行)保持原交互，不影响其它功能。
 *
 *  ▸ 圣金魂匣 + 潜行 + 左键 空气：
 *      取消本次空挥并 C2S 请求解绑坐标（服务端校验持有者/已绑定后执行）。
 *      点方块/点实体的解绑仍走原服务端事件路径，这里不拦截。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = AcatMod.MOD_ID, value = Dist.CLIENT)
public class AcatClientEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return; // 只处理左键攻击键
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        boolean sneak = player.isShiftKeyDown();

        if (stack.getItem() instanceof HolyGoldCrownItem) {
            if (sneak) {
                // Shift＋左键：无论指向空气/方块/实体，一律清空全部护卫（防止护卫死了不能移除）
                event.setCanceled(true);
                PacketDistributor.sendToServer(CrownSelectPacket.clearAll());
            } else {
                // 左键 + 指向生物：取消普攻，尝试 指定/取消 护卫
                HitResult hit = mc.hitResult;
                if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity) {
                    event.setCanceled(true);
                    PacketDistributor.sendToServer(CrownSelectPacket.select(entityHit.getEntity().getId()));
                }
                // 其余(点方块/空气)保持原交互，不影响其它功能
            }
        } else if (stack.getItem() instanceof HolySoulArkItem) {
            // 仅拦截“点空气”：点方块 / 点实体仍走原服务端解绑路径
            if (!sneak) {
                return;
            }
            HitResult hit = mc.hitResult;
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                event.setCanceled(true); // 取消空挥(不再触发仅客户端可用的 LeftClickEmpty)
                PacketDistributor.sendToServer(new ArkUnbindPacket());
            }
        }
    }
}
