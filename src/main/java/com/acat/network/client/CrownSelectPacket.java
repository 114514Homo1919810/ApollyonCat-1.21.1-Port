package com.acat.network.client;

import com.acat.AcatMod;
import com.acat.event.HolyGoldCrownEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：圣金王冠 左键操作。
 * ─────────────────────────────────────────────────────────
 * 与攻击事件完全无关：客户端在攻击管线之前(InteractionKeyMappingTriggered)拦截左键并发送。
 * 服务端 HolyGoldCrownEvents.handleCrownSelect 做归属校验与名单修改。
 *
 *  mode = MODE_SELECT(0)：携带被指向实体的网络 ID(entityId)，用于 左键仆从 指定/取消 护卫；
 *  mode = MODE_CLEAR_ALL(1)：Shift＋左键，无需瞄准仆从(点空气/方块/实体均可)，一键清空全部护卫。
 */
public class CrownSelectPacket implements CustomPacketPayload {

    /** 左键点实体：指定/取消 该生物为护卫。 */
    public static final int MODE_SELECT = 0;
    /** Shift＋左键(任意目标)：清空这顶王冠的全部护卫。 */
    public static final int MODE_CLEAR_ALL = 1;

    public static final CustomPacketPayload.Type<CrownSelectPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "crown_select"));

    public static final StreamCodec<FriendlyByteBuf, CrownSelectPacket> STREAM_CODEC =
            StreamCodec.of(CrownSelectPacket::encode, CrownSelectPacket::decode);

    private final int mode;
    private final int entityId;

    public CrownSelectPacket(int mode, int entityId) {
        this.mode = mode;
        this.entityId = entityId;
    }

    /** 左键指向某生物 → 尝试指定/取消护卫。 */
    public static CrownSelectPacket select(int entityId) {
        return new CrownSelectPacket(MODE_SELECT, entityId);
    }

    /** Shift＋左键(无需瞄准仆从) → 清空全部护卫。 */
    public static CrownSelectPacket clearAll() {
        return new CrownSelectPacket(MODE_CLEAR_ALL, 0);
    }

    public static void encode(FriendlyByteBuf buffer, CrownSelectPacket packet) {
        buffer.writeInt(packet.mode);
        buffer.writeInt(packet.entityId);
    }

    public static CrownSelectPacket decode(FriendlyByteBuf buffer) {
        return new CrownSelectPacket(buffer.readInt(), buffer.readInt());
    }

    public static void handle(CrownSelectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                HolyGoldCrownEvents.handleCrownSelect(player, packet.mode, packet.entityId);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
