package com.acat.network;

import com.acat.network.client.ArkUnbindPacket;
import com.acat.network.client.CrownSelectPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * aaacat 网络通道（全部为 C2S：客户端操作意图 → 服务端校验执行）。
 * 目前用于：
 *  - CrownSelectPacket：圣金王冠 左键操作（controls v2）——
 *      MODE_SELECT：左键(不潜行)仆从 → 指定/取消护卫；
 *      MODE_CLEAR_ALL：Shift＋左键(任意目标) → 一键清空全部护卫。
 *    客户端在攻击管线之前(InteractionKeyMappingTriggered)用准心选取生物，
 *    只发模式+实体 ID 过来，服务端做归属校验后修改名单（与攻击事件完全解耦，
 *    规避 Goety OwnerAttackCancel 取消主人攻击自己仆从导致旧路径永不触发的问题）。
 *  - ArkUnbindPacket：圣金魂匣「shift+左键空气」取消绑定坐标（点方块/实体仍走原事件路径）。
 *
 * NeoForge 1.21 起网络改为 CustomPacketPayload 载荷体系：注册在 mod 事件总线
 * (RegisterPayloadHandlersEvent)，发送走 PacketDistributor。
 */
public class AcatNetwork {

    private static final String PROTOCOL_VERSION = "1";

    /** 必须在客户端与服务端两侧各调用一次（AcatMod 构造器）。 */
    public static void init(IEventBus modBus) {
        modBus.addListener(AcatNetwork::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(CrownSelectPacket.TYPE, CrownSelectPacket.STREAM_CODEC, CrownSelectPacket::handle);
        registrar.playToServer(ArkUnbindPacket.TYPE, ArkUnbindPacket.STREAM_CODEC, ArkUnbindPacket::handle);
    }
}
