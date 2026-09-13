package com.acat.network.client;

import com.acat.AcatMod;
import com.acat.event.HolySoulArkEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：圣金魂匣 shift+左键【空气】→ 取消绑定坐标。
 * （shift+左键方块 / 实体仍走原有服务端 PlayerInteractEvent.LeftClickBlock 与
 *   LivingAttackEvent 路径，本包只补上空击空气这一条，避免依赖仅客户端触发的
 *   PlayerInteractEvent.LeftClickEmpty。）
 */
public class ArkUnbindPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ArkUnbindPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AcatMod.MOD_ID, "ark_unbind"));

    public static final StreamCodec<FriendlyByteBuf, ArkUnbindPacket> STREAM_CODEC =
            StreamCodec.of(ArkUnbindPacket::encode, ArkUnbindPacket::decode);

    public static void encode(FriendlyByteBuf buffer, ArkUnbindPacket packet) {
    }

    public static ArkUnbindPacket decode(FriendlyByteBuf buffer) {
        return new ArkUnbindPacket();
    }

    public static void handle(ArkUnbindPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                HolySoulArkEvents.handleArkUnbind(player);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
