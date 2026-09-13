package com.acat.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 三阶段领域光环（aaacat:domain_ring）
 * ─────────────────────────────────────────────
 * 纯装饰实体：挂在领域中心(地面)，客户端由 ApollyonCatDomainRingRenderer 绘制
 * 半径 16 / 20 两条圆形轮廓 + 各自环绕运动的"长条"（参考炼狱魔典圆形火焰边界）。
 * 前 60 tick(三阶段展开的 3 秒)半径从 0 生长到目标值；亚小猫死亡时由服务端销毁。
 */
public class ApollyonCatDomainRingEntity extends Entity {

    /** 展开动画时长(tick)，与服务端三阶段展开期一致 */
    public static final int SETUP_TICKS = 60;

    public ApollyonCatDomainRingEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /** 1.21 起生成包改为按 ServerEntity 构造（原 NetworkHooks.getEntitySpawningPacket 已移除）。 */
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /** 供渲染器使用：当前半径生长比例(0~1)，超过展开期恒为 1 */
    public float growth(float partialTick) {
        return Math.min(1.0F, (this.tickCount + partialTick) / (float) SETUP_TICKS);
    }
}
