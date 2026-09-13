package com.acat.entity;

import com.Polarice3.Goety.common.entities.projectiles.MagicFire;
import com.Polarice3.Goety.common.entities.projectiles.VoidShockBomb;
import com.Polarice3.Goety.utils.BlockFinder;
import com.Polarice3.Goety.utils.SpellExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 亚小猫(Boss)三阶段专属虚空震荡弹：
 * 继承 Goety 原版 VoidShockBomb，落地/命中时【保留原版“虚空火(魔焰 MagicFire)”十字扩散】，
 * 但【不生成 8 个追踪小震荡弹(虚空震荡波 VoidShock)】。
 *
 * 用法与原弹完全一致：new YaVoidShockBomb(this, level)。
 * 注意：父类 VoidShockBomb(LivingEntity, Level) 构造器内部固定使用
 * ModEntityType.VOID_SHOCK_BOMB 的 EntityType —— 所以本类不需要额外注册
 * 实体类型/渲染器（服务端运行时对象是本子类，onHit 走这里的逻辑，
 * 客户端仍按原弹渲染），存档重载后即便退化为父类也只会短暂存在。
 */
public class YaVoidShockBomb extends VoidShockBomb {

    public YaVoidShockBomb(EntityType<? extends YaVoidShockBomb> type, Level world) {
        super(type, world);
    }

    public YaVoidShockBomb(LivingEntity thrower, Level world) {
        super(thrower, world);
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide || this.growTick > 0) {
            return;
        }
        // 命中实体：保留父类「直接命中伤害」段（不经过会分裂的父类 onHit）
        if (result instanceof EntityHitResult entityHitResult) {
            this.onHitEntity(entityHitResult);
        }
        // —— 继承“虚空火(魔焰)”：以落点为中心 + 水平四方向扩散 MagicFire（不生成追踪小震荡弹 VoidShock）——
        LivingEntity livingOwner = this.getOwner();
        if (livingOwner != null) {
            Vec3 center = Vec3.atCenterOf(this.blockPosition());
            if (result instanceof BlockHitResult blockHitResult) {
                BlockPos blockpos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                if (BlockFinder.canBeReplaced(this.level(), blockpos)) {
                    center = Vec3.atCenterOf(blockpos);
                }
            } else if (result instanceof EntityHitResult entityHitResult) {
                center = Vec3.atCenterOf(entityHitResult.getEntity().blockPosition());
            }
            MagicFire magicFire = new MagicFire(this.level(), center, livingOwner);
            if (this.level().addFreshEntity(magicFire)) {
                for (Direction direction : Direction.values()) {
                    if (direction.getAxis().isHorizontal()) {
                        MagicFire magicFire1 = new MagicFire(this.level(),
                                Vec3.atCenterOf(magicFire.blockPosition().relative(direction)), livingOwner);
                        this.level().addFreshEntity(magicFire1);
                    }
                }
            }
        }
        this.growTick = 1;
        this.playSound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.5F, 0.75F);
        float damage = this.baseDamage + this.getExtraDamage();
        new SpellExplosion(this.level(), this,
                this.damageSources().indirectMagic(this, this.getOwner()),
                this.getX(), this.getY(), this.getZ(), 2.0F, damage);
        this.level().broadcastEntityEvent(this, (byte) 6);
    }
}
