package com.acat.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import java.util.UUID;

/**
 * 三阶段腐化光束锚点（aaacat:beam_anchor）
 * ─────────────────────────────────────────────
 * 隐形、无敌、无 AI、无碰撞的 Mob：只给 Goety 的 CorruptedBeam 当 owner。
 * 亚小猫每 tick 驱动它的位置(钉在自身 xz、y 追踪锁定目标脚底)与朝向(60° 间隔、60°/s 旋转)，
 * CorruptedBeam 的伤害结算(服务端取 owner 视线)与渲染(客户端取 owner 位置/朝向)都会自动跟随，
 * 因此 6 条腐化射线能干净地"绕小猫旋转扫射"，无需改 Goety 任何代码。
 *
 * 持久化 ownerUuid：世界存档重载(玩家退世界重进)后，亚小猫能识别"旧锚点是自己的"并统一清理，
 * 不会补生成一套新的导致旧光束实体永久残留。
 *
 * 1.21 起眼高改为由 EntityType.Builder#eyeHeight 定义（EntityDimensions.eyeHeight()），
 * 因此"锚点眼高归零"在 AcatEntities 注册时用 .eyeHeight(0.0F) 声明。
 */
public class BeamAnchorEntity extends Mob {

    /** 所属亚小猫(服务端)：存档重载后用于归属识别；主人消失时锚点自动销毁防残留 */
    private UUID ownerUuid;

    public BeamAnchorEntity(EntityType<? extends BeamAnchorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;      // 穿墙：玩家站在高处/坑里也能让射线保持在其高度
        this.noCulling = true;
        this.setNoGravity(true);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // 无 AI
    }

    @Override
    public void tick() {
        super.tick();
        // 主人(亚小猫)死亡/被移除/已卸载 → 锚点自毁，随附光束(owner=锚点)下一 tick 也会消失
        if (!this.level().isClientSide && this.ownerUuid != null
                && this.level() instanceof ServerLevel serverLevel) {
            Entity owner = serverLevel.getEntity(this.ownerUuid);
            if (owner == null || !owner.isAlive() || owner.isRemoved()) {
                this.discard();
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    /** 绑定所属亚小猫(服务端调用，生成时即绑定) */
    public void bindOwner(UUID owner) {
        this.ownerUuid = owner;
    }

    /** 归属的亚小猫 UUID(无则为 null) */
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected float getSoundVolume() {
        return 0.0F;
    }
}
