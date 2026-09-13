package com.acat.entity;

import com.Polarice3.Goety.common.entities.hostile.servants.Damned;
import com.Polarice3.Goety.common.entities.projectiles.Hellfire;
import com.Polarice3.Goety.common.entities.projectiles.IceBouquet;
import com.Polarice3.Goety.common.entities.projectiles.MagicFire;
import com.Polarice3.Goety.init.ModSounds;
import com.Polarice3.Goety.utils.MobUtil;
import com.Polarice3.Goety.utils.ModDamageSource;
import com.Polarice3.Goety.utils.TrailEffect;
import com.acat.config.AcatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

/**
 * 亚小猫二阶段攻击召唤物 —— 「猫头魂体」(aaacat:cat_head)
 * ─────────────────────────────────────────────────────────────
 * 本类直接继承 Goety 使徒召唤的「狱魂」(hostile.servants.Damned)——狱魂本身就是一只活的
 * 飞行生物(Owned PathfinderMob + Enemy + ShootIndicatorOwner)，不是弹射物，因此把猫头做成
 * Damned 的子类后，整套「狱魂」骨架零成本继承(命中筛选、限时寿命、俯冲命中检测、无敌…)：
 *
 *   召唤 → 先升空(出生时给 0.3 上抛速度，无重力悬停)
 *        → 悬停约 2 秒，头始终转向锁定目标(低/抬头看敌人)
 *        → 到点猫 HISS 尖叫一声，朝敌人直线俯冲(速度约 1 格/tick)，俯冲全程带半透明拖尾
 *        → 俯冲中由 Damned 自带命中检测，撞实体/方块触发 onHit
 *
 * 不做任何「射击轨迹线」：父类 Damned.tick() 自带的金色轨迹线是私有时序 + 精确 ==tick 单次判定，
 * 既难改色又容易漏发(bug 多)。因此 override tick()：
 *   1) 在 super.tick() 期间用 getTarget() 遮蔽目标，让父类私有时序完全不触发，
 *      但移动/命中/寿命/友军判定等照常走 Owned/Damned 机制；
 *   2) 在 super 之后用自己的 aimTime 重放「悬停瞄准 → 尖叫俯冲」，并把目标同步写进
 *      父类可见的实体字段(yRot/yHeadRot/xRot/setCharging/setCharge)，供客户端渲染。
 *
 * 一个实体类型 + 同步 variant(NBT) 区分三种元素：
 *   variant 0 = 红 r：直击造成火焰伤害(hellfire)并点燃 5 秒；落地铺「5 朵狱火」十字(Hellfire，中心+东南西北)
 *   variant 1 = 蓝 b：直击造成冰冻伤害(iceBouquet)并给满原版霜冻状态；落地铺「十字冰之火」(IceBouquet ×4，±1.5 格)
 *   variant 2 = 黄 y：直击造成魔法伤害(indirectMagic)；落地铺「十字终末火」(MagicFire，中心+东南西北)
 *
 * 命中 / 未命中(超时耗尽寿命) / 撞墙都就地铺对应十字火后消散；限时寿命耗尽由 Damned/Owned 的
 * ownedTick 自动调用 lifeSpanDamage()。命中筛选沿用 Damned.canHitEntity 的友军豁免：不伤施法者、
 * 不伤施法者友方/召唤物(仆从版猫头的魂体绝不会咬主人)。
 */
public class ApollyonCatHeadEntity extends Damned {

    public static final int VARIANT_RED = 0;      // 红：狱火
    public static final int VARIANT_BLUE = 1;     // 蓝：冰之火(霜冻)
    public static final int VARIANT_YELLOW = 2;   // 黄：终末火(魔焰)

    /** 默认寿命 tick：80 = 4 秒。升空≈0.5s + 悬停瞄准≈2s + 俯冲≈1.5s，超时未命中则就地铺十字火 */
    public static final int DEFAULT_LIFETIME = 80;   // 4 秒：升空≈0.5s + 悬停瞄准≈2s + 俯冲≈1.5s，避免旧批未死新批已续(看着像没冷却)
    /** 目标丢失后(施法者目标已死)就近重选的搜索半径 */
    private static final int RETARGET_RADIUS = 48;
    /** 找地面最大下探深度 */
    private static final int GROUND_SCAN_DOWN = 48;

    /** 悬停瞄准时长(tick)：达到后尖叫俯冲 */
    private static final int AIM_TIME_TO_DIVE = 40;

    /** 拖尾(俯冲时更新)，渲染端由 CatHeadRenderer 画半透明飘带 */
    public final TrailEffect trail = new TrailEffect(0.4F, 5.0F);

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(ApollyonCatHeadEntity.class, EntityDataSerializers.INT);

    /** 调 super.tick() 时是否对父类遮蔽目标(避免父类私有时序跑) */
    private boolean suppressTarget;
    /** 我们自己的悬停瞄准计时(仅服务端推进) */
    private int aimTime;
    /** 是否已进入俯冲(只触发一次) */
    private boolean diveFired;
    /** 服务端当前锁定目标(不与 mob 内部 target 混淆) */
    @Nullable
    private LivingEntity lockedTarget;

    public ApollyonCatHeadEntity(EntityType<? extends ApollyonCatHeadEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Damned.setCustomAttributes();
    }

    /**
     * 施法点：本体周围 ±3 格随机选点，再向下找实心方块，返回其上方一格(可站立的出生格)。
     * 找不到地面(虚空)则退回原随机点。
     */
    public static BlockPos summonPositionNear(LivingEntity caster, BlockPos base) {
        Level level = caster.level();
        BlockPos.MutableBlockPos m = base.mutable();
        int minY = level.getMinBuildHeight();
        for (int i = 0; i < GROUND_SCAN_DOWN; ++i) {
            if (m.getY() <= minY) {
                break;
            }
            if (level.getBlockState(m).blocksMotion()) {
                return m.above().immutable();
            }
            m.move(Direction.DOWN);
        }
        return base;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, VARIANT_RED);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, variant);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            this.setVariant(tag.getInt("Variant"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
    }

    /** 遮蔽目标：仅在调 super.tick() 期间生效，让 Damned 私有时序永远看不到目标 */
    @Nullable
    @Override
    public LivingEntity getTarget() {
        return this.suppressTarget ? null : super.getTarget();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty,
                                        MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        pSpawnData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
        this.setHuman(false);   // 猫头没有“人形/非人形”之分，关掉狱魂的随机人形
        return pSpawnData;
    }

    /** 猫头是短命攻击召唤物：禁止被 Owned 的“距使徒主人>32 格自动拉回”打断直线俯冲 */
    @Override
    public void teleportTowards(Entity entity) {
        // no-op
    }

    @Override
    public void tick() {
        boolean wasCharging = this.isCharging();
        Vec3 before = this.position();

        // —— 父类 tick：遮蔽目标 → 私有瞄准/画线/尖叫不触发，其余(移动/俯冲命中/寿命)照常 ——
        this.suppressTarget = true;
        super.tick();
        this.suppressTarget = false;
        if (this.isRemoved()) {
            return;
        }
        this.setNoGravity(true);
        this.setYRot(this.getYHeadRot());
        this.yBodyRot = this.getYRot();

        // —— 俯冲拖尾(双端都记录，客户端用于渲染) ——
        if (wasCharging || this.isCharging()) {
            this.trail.update(before);
        }

        if (this.level().isClientSide) {
            return;   // 客户端只负责渲染：旋转/俯冲数据均由服务端同步
        }

        // ==================== 服务端逻辑 ====================
        if (this.isCharging()) {
            // 俯冲中：头转向实际飞行方向(直线俯冲，含俯仰角)
            Vec3 m = this.getDeltaMovement();
            if (m.lengthSqr() > 1.0E-8D) {
                double h = Math.sqrt(m.x * m.x + m.z * m.z);
                float yaw = (float) (Mth.atan2(-m.x, m.z) * (180.0D / Math.PI));
                float pitch = h < 1.0E-5D ? 0.0F : (float) (Mth.atan2(-m.y, h) * (180.0D / Math.PI));
                this.setYRot(yaw);
                this.setYHeadRot(yaw);
                this.yBodyRot = yaw;
                this.setXRot(pitch);
            }
            return;   // 命中检测已由 super 内的 Damned 俯冲块完成，无需再做
        }

        LivingEntity owner = this.getTrueOwner();
        if (owner == null || owner.isRemoved() || !owner.isAlive()) {
            this.discard();
            return;
        }

        // —— 重选锁定目标(悬停期)：自己的锁定 → 施法者当前目标 → 就近合法敌人 ——
        LivingEntity target = this.lockedTarget;
        if (!this.isValidEnemy(owner, target)) {
            target = owner instanceof Mob mob && this.isValidEnemy(owner, mob.getTarget()) ? mob.getTarget() : null;
        }
        if (target == null) {
            target = this.nearestEnemy(owner);
        }
        this.lockedTarget = target;

        if (target == null) {
            return;   // 无目标：原地悬停等寿命耗尽就地铺火
        }

        // —— 转向目标：yaw(整体朝向) + pitch(俯仰，模型据此抬头/低头) ——
        MobUtil.instaLook(this, target);
        Vec3 eye = this.getEyePosition();
        Vec3 tEye = target.getEyePosition();
        double dx = tEye.x - eye.x;
        double dy = tEye.y - eye.y;
        double dz = tEye.z - eye.z;
        double hh = Math.sqrt(dx * dx + dz * dz);
        if (hh > 1.0E-5D) {
            this.setXRot((float) (-(Math.atan2(dy, hh)) * (180.0D / Math.PI)));
        }

        // —— 悬停计时到点：猫叫一声，朝目标锁定方向直线俯冲 ——
        ++this.aimTime;
        if (this.aimTime >= AIM_TIME_TO_DIVE && !this.diveFired) {
            this.diveFired = true;
            this.playSound(SoundEvents.CAT_HISS, 3.0F, this.getVoicePitch());
            double ddx = this.getX() - target.getX();
            double ddy = this.getY() - target.getY();
            double ddz = this.getZ() - target.getZ();
            double d0 = Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
            if (d0 > 1.0E-4D) {
                double velocity = 5.0D;
                double xPower = -(ddx / d0 * velocity * 0.2D);
                double yPower = -(ddy / d0 * velocity * 0.2D);
                double zPower = -(ddz / d0 * velocity * 0.2D);
                this.setCharge(xPower, yPower, zPower);
            } else {
                this.setCharge(0.0D, -0.5D, 0.0D);
            }
            this.setCharging(true);
        }
    }

    /** 命中/落点判定：沿用狱魂的友军豁免规则，只补“存活/非旁观/非创造” */
    private boolean isValidEnemy(@Nullable LivingEntity owner, @Nullable Entity e) {
        if (!(e instanceof LivingEntity living)) {
            return false;
        }
        if (e == this || e == owner) {
            return false;
        }
        if (living.isRemoved() || !living.isAlive()) {
            return false;
        }
        if (living instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        return this.canHitEntity(living);
    }

    /** 就近重选：FOLLOW_RANGE(狱魂默认 48)内离自己最近的合法敌人 */
    private LivingEntity nearestEnemy(@Nullable LivingEntity owner) {
        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (range <= 0) {
            range = RETARGET_RADIUS;
        }
        double rangeSqr = range * range;
        LivingEntity best = null;
        double bestSqr = rangeSqr;
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range),
                e0 -> this.isValidEnemy(owner, e0) && this.distanceToSqr(e0) <= rangeSqr)) {
            double d = this.distanceToSqr(e);
            if (d < bestSqr) {
                bestSqr = d;
                best = e;
            }
        }
        return best;
    }

    /** 狱魂撞墙/撞实体后：先结算直击伤害(仅对实体)，再就地铺对应十字火并消散 */
    @Override
    protected void onHit(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            this.onHitEntity(entityHitResult);
        }
        if (this.level().isClientSide) {
            return;
        }
        Vec3 impact;
        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos pos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
            impact = Vec3.atCenterOf(pos);
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            impact = Vec3.atCenterOf(entityHitResult.getEntity().blockPosition());
        } else {
            impact = this.position();
        }
        this.spawnImpactEffects(impact);
        this.discard();
    }

    /** 直击伤害/状态：按变体结算，不改动“铺火与消散”(那在 onHit 里统一做) */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        Entity victim = result.getEntity();
        LivingEntity owner = this.getTrueOwner();
        float damage = switch (this.getVariant()) {
            case VARIANT_RED -> (float) (double) AcatConfig.CAT_HEAD_RED_PER_HIT.get();
            case VARIANT_BLUE -> (float) (double) AcatConfig.CAT_HEAD_BLUE_PER_HIT.get();
            default -> (float) (double) AcatConfig.CAT_HEAD_YELLOW_PER_HIT.get();
        };
        switch (this.getVariant()) {
            case VARIANT_RED -> {
                victim.hurt(ModDamageSource.hellfire(this, owner), damage);
                if (victim instanceof LivingEntity living) {
                    living.igniteForSeconds(5.0F);   // 狱火点燃 5 秒
                }
            }
            case VARIANT_BLUE -> {
                victim.hurt(ModDamageSource.iceBouquet(this, owner), damage);
                if (victim instanceof LivingEntity living) {
                    this.applyFrost(living);      // 原版霜冻状态(满冻)
                }
            }
            default -> victim.hurt(victim.damageSources().indirectMagic(this, owner), damage);
        }
    }

    /** 原版霜冻状态：在目标现有霜冻值上叠加 config 指定 tick 数(默认 200，直接到满冻线) */
    private void applyFrost(LivingEntity target) {
        int add = AcatConfig.CAT_HEAD_FREEZE_TICKS.get();
        int cap = target.getTicksRequiredToFreeze();
        int now = Math.max(0, target.getTicksFrozen());
        target.setTicksFrozen(Math.min(cap, now + add));
    }

    /** 寿命耗尽(未命中)：与命中一样就地铺对应十字火后消散 */
    @Override
    public void lifeSpanDamage() {
        if (!this.level().isClientSide) {
            this.spawnImpactEffects(this.position());
        }
        this.discard();
    }

    /** 只有红色(狱火)魂体在俯冲时身上带火(继承狱魂 isOnFire=isCharging 的观感)，蓝/黄不冒火 */
    @Override
    public boolean isOnFire() {
        return this.isCharging() && this.getVariant() == VARIANT_RED;
    }

    /** 把狱魂的尖叫声(DAMNED_SCREAM)换成猫的尖叫声(CAT_HISS) */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (sound == ModSounds.DAMNED_SCREAM.get()) {
            sound = SoundEvents.CAT_HISS;
        }
        super.playSound(sound, volume, pitch);
    }

    /** 就地铺对应变体的十字火 + 命中粒子 + 命中音效(红爆炸/蓝碎冰) */
    private void spawnImpactEffects(Vec3 at) {
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity owner = this.getTrueOwner();
        this.placeCrossFire(at, owner != null ? owner : this);
        if (this.level() instanceof ServerLevel serverLevel) {
            switch (this.getVariant()) {
                case VARIANT_RED -> serverLevel.sendParticles(ParticleTypes.FLAME,
                        at.x, at.y + 0.2D, at.z, 10, 0.4D, 0.3D, 0.4D, 0.05D);
                case VARIANT_BLUE -> serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        at.x, at.y + 0.2D, at.z, 10, 0.4D, 0.3D, 0.4D, 0.05D);
                default -> serverLevel.sendParticles(ParticleTypes.WITCH,
                        at.x, at.y + 0.2D, at.z, 10, 0.4D, 0.3D, 0.4D, 0.05D);
            }
        }
        switch (this.getVariant()) {
            case VARIANT_RED -> this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 1.0F);
            case VARIANT_BLUE -> this.playSound(SoundEvents.GLASS_BREAK, 2.0F, 1.0F);
            default -> { }
        }
    }

    /** 按变体铺地面十字火总入口 */
    private void placeCrossFire(Vec3 at, LivingEntity owner) {
        BlockPos seed = BlockPos.containing(at.x, at.y, at.z);
        if (!this.level().getBlockState(seed).blocksMotion()) {
            seed = seed.below();   // 落点格是空气就从下面一格开始向下找地面
        }
        BlockPos solid = this.groundBelow(seed);
        BlockPos anchor = solid != null ? solid.above() : seed;   // 放火的那个格子
        switch (this.getVariant()) {
            case VARIANT_RED -> this.spawnHellfireCross(anchor, owner);
            case VARIANT_BLUE -> this.spawnIceCross(solid != null ? solid : seed, owner);
            default -> this.spawnMagicFireCross(anchor, owner);
        }
    }

    /** 找 (x,z) 柱状向下最近的实心方块，找不到返回 null */
    private BlockPos groundBelow(BlockPos start) {
        BlockPos.MutableBlockPos m = start.mutable();
        int minY = this.level().getMinBuildHeight();
        for (int i = 0; i < GROUND_SCAN_DOWN; ++i) {
            if (m.getY() <= minY) {
                return null;
            }
            if (this.level().getBlockState(m).blocksMotion()) {
                return m.immutable();
            }
            m.move(Direction.DOWN);
        }
        return null;
    }

    /**
     * 红：5 朵狱火(Hellfire)十字 —— 中心 + 东南西北，同 Goety HellBlast/狱魂 onHit 口径。
     */
    private void spawnHellfireCross(BlockPos anchor, LivingEntity owner) {
        Vec3 center = Vec3.atCenterOf(anchor);
        Hellfire hellfire = new Hellfire(this.level(), center, owner);
        if (this.level().addFreshEntity(hellfire)) {
            for (Direction direction : Direction.values()) {
                if (direction.getAxis().isHorizontal()) {
                    Hellfire side = new Hellfire(this.level(),
                            Vec3.atCenterOf(hellfire.blockPosition().relative(direction)), owner);
                    this.level().addFreshEntity(side);
                }
            }
        }
    }

    /**
     * 蓝：十字冰之火(IceBouquet ×4) —— 中心不放，东西南北各 1.5 格各 1 朵，
     * 数值/寿命/center 口径与亚小猫三阶段 spawnSweepIceFire 一致。
     */
    private void spawnIceCross(BlockPos solid, LivingEntity owner) {
        double cx = solid.getX() + 0.5D;
        double cz = solid.getZ() + 0.5D;
        double gy = solid.getY() + 1.0D;
        this.spawnIceBouquetAt(cx, cz, gy, 1.5D, 0.0D, owner);
        this.spawnIceBouquetAt(cx, cz, gy, -1.5D, 0.0D, owner);
        this.spawnIceBouquetAt(cx, cz, gy, 0.0D, 1.5D, owner);
        this.spawnIceBouquetAt(cx, cz, gy, 0.0D, -1.5D, owner);
    }

    private void spawnIceBouquetAt(double cx, double cz, double gy, double xshift, double zshift, LivingEntity owner) {
        IceBouquet ice = new IceBouquet(this.level(), cx + xshift, gy + 0.5D, cz + zshift, owner);
        ice.setExtraDamage(5.0F);
        ice.addLifeSpan(120);
        ice.setCenter(false);
        MobUtil.moveDownToGround(ice);
        this.level().addFreshEntity(ice);
    }

    /**
     * 黄：5 朵终末火(MagicFire)十字 —— 中心 + 东南西北，同 YaVoidShockBomb/虚空震荡弹魔焰口径。
     */
    private void spawnMagicFireCross(BlockPos anchor, LivingEntity owner) {
        Vec3 center = Vec3.atCenterOf(anchor);
        MagicFire magicFire = new MagicFire(this.level(), center, owner);
        if (this.level().addFreshEntity(magicFire)) {
            for (Direction direction : Direction.values()) {
                if (direction.getAxis().isHorizontal()) {
                    MagicFire side = new MagicFire(this.level(),
                            Vec3.atCenterOf(magicFire.blockPosition().relative(direction)), owner);
                    this.level().addFreshEntity(side);
                }
            }
        }
    }
}
