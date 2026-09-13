package com.acat.entity;

import com.Polarice3.Goety.common.entities.boss.Apostle;
import com.Polarice3.Goety.common.items.ModItems;
import com.Polarice3.Goety.utils.MobUtil;
import com.acat.effect.ApollyonCatBlessingEffect;
import com.acat.registry.AcatEffects;
import com.acat.registry.AcatItems;
import com.acat.util.ApollyonCatKillAura;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import com.acat.config.AcatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * 亚小猫 · 仆从版（aaacat:aaacat_servant）
 * ─────────────────────────────────────────────────────────────
 * 继承链：ApollyonCatServantEntity → ApollyonCatFightEntity → Summoned → Owned → PathfinderMob
 *   - 战斗(哈气/风筝/二阶段/三阶段领域/全部技能)与 Boss 版行为一致：
 *     ApollyonCatFightEntity 内自建 Apostle 被动底座等价实现；
 *     仆从版与 Boss 版是两套各自独立维护的实现（不共享，改动需两侧同步）。
 *   - Goety 仆从系统(主人 UUID/跟随/还击/索敌联动/待命守卫指令/区块加载/装备升级)
 *     由 Summoned(Owned) 原生提供，无需手写。
 *
 * 仆从版差异(相对 Boss 版)：
 *  1. 无 Boss 血条 \/ 无 apostle_theme BGM(本就无 Apostle 血条；应同步位关掉)；
 *  2. 无 Boss 战利品 \/ 无经验 —— 但有仆从专属硬编码掉落：1 不洁圣冠 + 1 至纯喵环（死亡即掉）；
 *  3. 三阶段领域只困敌人，绝不动主人与同盟玩家(任何玩家都不被吸入\/困禁)；
 *  4. 锁定敌人后每 3 秒自动 +1 哈气值(不放风爆\/地震)，累计 3\/6\/9\/12 照常释放对应技能；
 *  5. 主人误伤不记仇；野生(未绑定)才主动攻击玩家；
 *  6. ★ 和平难度下不消失(仆从不会被和平模式清除)；
 *  7. ★ 自动秒杀半径 64 格内 5 种天敌(幻翼\/苦力怕\/深渊蚊\/海鸥\/仙子)，击杀算仆从；
 *  8. ★ 玩家用生鳕鱼\/生鲑鱼右键可喂食：回 10 血 + 冒爱心 + 玩家获得 30 分钟「亚小猫的祝福」。
 *
 * 获取方式：aaacat:aaacat_servant_spawn_egg
 *  —— 潜行右键使用 = 绑定为你自己的仆从(ServantSpawnEggItem 的 IOwned 绑定逻辑)；
 *  —— 不潜行使用 = 生成无主(野生)版，保留 Boss 式主动攻击玩家的行为。
 * 另：亚小猫的安息仪式(下界 Y>127) 核心=至纯喵环 可召唤仆从版(见 data\/aaacat\/recipes\/summon_apollyon_cat_servant.json)
 *     ritual_type = goety:summon_tamed_no_variant（仪式完成即认主施法玩家，与潜行右键刷怪蛋绑定同款）；
 */
public class ApollyonCatServantEntity extends ApollyonCatFightEntity {

    /* ==================== 主动哈气：锁定敌人每 3 秒 +1 ==================== */
    private static final int AUTO_HISS_INTERVAL = 60;      // 3 秒 = 60 tick
    private int autoHissTimer = AUTO_HISS_INTERVAL;        // 剩余 tick（有目标才递减）
    private int pendingAutoHiss;                           // 施法中积压的哈气，空闲后逐次结算

    /* ==================== ★ 睡觉(陪主人上床)：状态 + 动画渐变量 ==================== */
    // 复刻原版 Cat 的 IS_LYING / RELAX_STATE_ONE 同步位；卧巨量在 aiStep 里逐帧插值，供客户端模型读取。
    private static final EntityDataAccessor<Boolean> IS_LYING =
            SynchedEntityData.defineId(ApollyonCatServantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RELAX_STATE_ONE =
            SynchedEntityData.defineId(ApollyonCatServantEntity.class, EntityDataSerializers.BOOLEAN);
    private float lieDownAmount;
    private float lieDownAmountO;
    private float lieDownAmountTail;
    private float lieDownAmountOTail;
    private float relaxStateOneAmount;
    private float relaxStateOneAmountO;
    private boolean sleepingGoalActive;   // 是否正在执行上床睡觉 goal(整个入睡过程/躺床上都不战斗)

    public ApollyonCatServantEntity(EntityType<? extends ApollyonCatServantEntity> type, Level level) {
        super(type, level);
        // 仆从版名牌：红色加粗「亚小猫 / aaacat」(仆从文案)
        this.setCustomName(this.servantName());
        this.setCustomNameVisible(true);
    }

    /** 仆从版名牌文案(红色加粗)，二阶段\/三阶段\/读档统一走这里 */
    private Component servantName() {
        return Component.translatable("entity.aaacat.aaacat_servant")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
    }

    /* ==================== 无 Boss BGM：不写 mob-target cap ==================== */
    @Override
    protected boolean shouldSyncBossMusicTarget() {
        return false;
    }

    /* ==================== ★ 和平模式不清除 ==================== */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        // 原(Owned)：isHostile()==true 时和平难度会被 checkDespawn 直接 discard；
        // 亚小猫仆从即使被判定为敌对(野生主动攻击玩家)也不应在和平模式被清除。
        return false;
    }

    /* ==================== 主人误伤不写入 lastHurtByMob → 还击目标永远不会指向主人 ==================== */
    @Override
    public void setLastHurtByMob(@Nullable LivingEntity livingEntity) {
        if (livingEntity != null && livingEntity == this.getTrueOwner()) {
            return;
        }
        super.setLastHurtByMob(livingEntity);
    }

    /* ==================== 仆从版差异：无 Boss 战利品表 / 无经验 ==================== */
    // 1.21：Mob#getLootTable() 变为 final，覆写点改为 getDefaultLootTable()，
    //       返回类型由 ResourceLocation 变为 ResourceKey<LootTable>
    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    /* ==================== ★ 死亡即掉：不依赖使徒「二段延迟死亡」流程 ====================
     * 使徒基座把真正执行 vanilla 死亡(唯一会触发原版掉落链的点)推迟到 tickDeath 二段：
     *   普通 = 死后第 1 tick 补调 die()；下界花式(仆从召唤仪式在下界、该配置默认开) = 浮空 200 tick 落地才死。
     * 仆从若在二段「真正死亡」前被移除/卸载/复活事件取消，dropCustomDeathLoot 根本不会被调用，
     * 两件套就被整个吞掉 —— 认主仆从死亡不掉至纯喵环/不洁圣冠即源于此。
     * 仆从版自管掉落：首次 die()(血量归零那一刻)只登记待发，随后每 tick 确认真正死亡
     * (生命值<=0 即算，花式流程走没走完都不影响)才发两件套，整只仆从一生只发一次；
     * 被复活类事件(圣金王冠 Lv5+ 不死图腾式复活)取消的死亡不发放。 */
    private boolean servantDropPending;      // 已发生致死 die()，等待确认真正死亡
    private boolean servantDropsSpawned;     // 两件套已发放(一生只发一次)

    @Override
    public void die(DamageSource pCause) {
        if (!this.level().isClientSide && !this.servantDropsSpawned) {
            this.servantDropPending = true;
        }
        super.die(pCause);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.servantDropPending) {
            if (this.isDeadOrDying()) {
                this.servantDropPending = false;
                this.servantDropsSpawned = true;
                this.spawnServantDrops();
            } else if (this.getHealth() > 0.0F) {
                this.servantDropPending = false;   // 死亡被取消/被复活 → 撤销待发，等下次真正死亡
            }
        }
    }

    /**
     * 仆从专属硬编码掉落（死亡即掉，与 Boss 掉落一样不走战利品表）：
     *   1 不洁圣冠(goety:unholy_hat) + 1 至纯喵环(aaacat:pure_cat_ring)
     */
    private void spawnServantDrops() {
        this.spawnServantDrop(new ItemStack(ModItems.UNHOLY_HAT.get()));          // 1 不洁圣冠
        this.spawnServantDrop(new ItemStack(AcatItems.PURE_CAT_RING.get()));      // 1 至纯喵环
    }

    private void spawnServantDrop(ItemStack stack) {
        ItemEntity itemEntity = this.spawnAtLocation(stack);
        if (itemEntity != null) {
            itemEntity.setExtendedLifetime();
        }
    }

    // 1.21：LivingEntity#getExperienceReward() 变为 final 且签名改为 (ServerLevel, Entity)，
    //       可覆写的经验产出口改为 getBaseExperienceReward()
    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    /* ==================== 名牌保持仆从版文案(二阶段\/读档回填) ==================== */
    @Override
    public void setSecondPhase(boolean secondPhase) {
        super.setSecondPhase(secondPhase);
        if (secondPhase && !this.level().isClientSide) {
            this.setCustomName(this.servantName());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (this.getTrueOwner() instanceof Player) {
            this.setCustomName(this.servantName());
        }
    }

    /* ==================== 三阶段领域：只困敌人，绝不波及主人\/同盟玩家 ==================== */
    @Override
    protected boolean isPhase3PlayerEnemy(Player player) {
        return false;   // 仆从版：任何玩家都不受领域吸入\/阻力带\/困禁影响
    }

    @Override
    protected boolean isPhase3Targetable(LivingEntity le) {
        if (le == this) {
            return false;
        }
        if (le instanceof BeamAnchorEntity) {
            return false;
        }
        if (le instanceof IApollyonCat) {
            return false;               // 别的亚小猫(Boss\/仆从)不互拉
        }
        LivingEntity master = this.getTrueOwner();
        if (master != null) {
            if (le == master) {
                return false;
            }
            if (MobUtil.areAllies(master, le)) {
                return false;           // 主人及主人的其它仆从\/宠物\/同盟
            }
        }
        if (le instanceof Player) {
            return false;               // 仆从版不把玩家当三阶段目标
        }
        if (le instanceof Apostle) {
            return true;                // 其它使徒类 Boss 照常拉入困住(与 Boss 版一致)
        }
        if (le instanceof Enemy) {
            return true;                // 敌对生物照常拉入困住
        }
        return !MobUtil.areAllies(this, le);
    }

    /* ==================== 主动哈气：锁定敌人每 3 秒 +1（不放风爆\/地震，里程碑照常释放） ==================== */
    @Override
    protected void tickServantAutoHiss() {
        if (this.level().isClientSide || this.phase3Active || this.isNoAi() || !this.isAlive()) {
            return;
        }
        LivingEntity target = this.getTarget();
        boolean locked = target != null && target.isAlive() && !target.isRemoved() && !target.isSpectator();
        if (!locked) {
            this.autoHissTimer = AUTO_HISS_INTERVAL;
            this.pendingAutoHiss = 0;
            return;
        }
        // 锁定中：每 3 秒获得 1 点哈气值
        if (--this.autoHissTimer <= 0) {
            this.autoHissTimer = AUTO_HISS_INTERVAL;
            ++this.pendingAutoHiss;
        }
        // 空闲时逐次结算（施法中\/二阶段仪式中先攒着），同 tick 只结算 1 点，避免技能叠放
        if (this.pendingAutoHiss > 0
                && !this.isCasting() && !this.isSettingUpSecond()) {
            --this.pendingAutoHiss;
            this.addAutoHiss();
        }
    }

    /* ==================== ★ 天敌猎杀：半径 64 格自动秒杀 5 种天敌，击杀归属仆从 ==================== */
    @Override
    protected void tickServantKillAura() {
        if (this.level().isClientSide || this.phase3Active || this.isNoAi() || !this.isAlive()) {
            return;
        }
        // 错开不同仆从的扫描 tick（每 ~1 秒扫一次，避免同 tick 全体扫描）
        if ((this.tickCount + this.getId()) % 20 == 0) {
            ApollyonCatKillAura.killTargetsInRange(this.level(), this, this, 64);
        }
    }

    /* ==================== ★ 喂食：生鳕鱼\/生鲑鱼 → 回 10 血 + 爱心 + 玩家 30 分钟祝福 ==================== */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // ★ 不洁之血：二阶段(含转阶段仪式中)右键 -> 消耗 1 个恢复第一阶段(回满血+重置锁血)；
        //   三阶段领域或本就在一阶段 -> 不消耗。天气不做任何改动。
        if (stack.is(ModItems.UNHOLY_BLOOD.get())) {
            if (!this.level().isClientSide && this.forceFirstPhase()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.playSound(SoundEvents.CAT_PURR, 1.2F, 1.0F);
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            this.getX(), this.getY() + this.getBbHeight() * 0.75D, this.getZ(),
                            10, 0.5D, 0.4D, 0.5D, 0.02D);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (stack.is(Items.COD) || stack.is(Items.SALMON)) {
            if (!this.level().isClientSide) {
                this.heal(10.0F);   // 恢复 10 点生命值(不超过上限)
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                // 爱心粒子（服务端直发，任何客户端都能看到）
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            this.getX(), this.getY() + this.getBbHeight() * 0.75D, this.getZ(),
                            8, 0.4D, 0.3D, 0.4D, 0.02D);
                }
                this.playSound(SoundEvents.GENERIC_EAT, 0.8F, 0.9F + this.random.nextFloat() * 0.2F);
                // 30 分钟「亚小猫的祝福」；重复喂食先移除再添加 = 刷新回满 30 分钟
                player.removeEffect(AcatEffects.APOLLYON_CAT_BLESSING);
                player.addEffect(new MobEffectInstance(AcatEffects.APOLLYON_CAT_BLESSING,
                        ApollyonCatBlessingEffect.DURATION, 0));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }


    /* ==================== ★ 睡觉(陪主人上床)：同步位 / 访问器 / 动画插值 ==================== */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_LYING, false);
        builder.define(RELAX_STATE_ONE, false);
    }

    public void setLying(boolean lying) {
        this.entityData.set(IS_LYING, lying);
    }

    public boolean isLying() {
        return this.entityData.get(IS_LYING);
    }

    public void setRelaxStateOne(boolean relax) {
        this.entityData.set(RELAX_STATE_ONE, relax);
    }

    public boolean isRelaxStateOne() {
        return this.entityData.get(RELAX_STATE_ONE);
    }

    /* 供客户端模型读取：增量插值(partialTicks) */
    public float getLieDownAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.lieDownAmountO, this.lieDownAmount);
    }

    public float getLieDownAmountTail(float partialTicks) {
        return Mth.lerp(partialTicks, this.lieDownAmountOTail, this.lieDownAmountTail);
    }

    public float getRelaxStateOneAmount(float partialTicks) {
        return Mth.lerp(partialTicks, this.relaxStateOneAmountO, this.relaxStateOneAmount);
    }

    /** 逐帧被基类 aiStep 的 servantTickSleep() 调用：更新卧巨量 + 打呼噜音效(与原版 Cat.handleLieDown 一致)。 */
    @Override
    protected void servantTickSleep() {
        if ((this.isLying() || this.isRelaxStateOne()) && this.tickCount % 5 == 0) {
            this.playSound(SoundEvents.CAT_PURR, 0.6F + 0.4F * (this.random.nextFloat() - this.random.nextFloat()), 1.0F);
        }
        this.updateLieDownAmount();
        this.updateRelaxStateOneAmount();
    }

    private void updateLieDownAmount() {
        this.lieDownAmountO = this.lieDownAmount;
        this.lieDownAmountOTail = this.lieDownAmountTail;
        if (this.isLying()) {
            this.lieDownAmount = Math.min(1.0F, this.lieDownAmount + 0.15F);
            this.lieDownAmountTail = Math.min(1.0F, this.lieDownAmountTail + 0.08F);
        } else {
            this.lieDownAmount = Math.max(0.0F, this.lieDownAmount - 0.22F);
            this.lieDownAmountTail = Math.max(0.0F, this.lieDownAmountTail - 0.13F);
        }
    }

    private void updateRelaxStateOneAmount() {
        this.relaxStateOneAmountO = this.relaxStateOneAmount;
        if (this.isRelaxStateOne()) {
            this.relaxStateOneAmount = Math.min(1.0F, this.relaxStateOneAmount + 0.1F);
        } else {
            this.relaxStateOneAmount = Math.max(0.0F, this.relaxStateOneAmount - 0.13F);
        }
    }

    /* ==================== ★ 睡觉时不战斗 ==================== */
    /** 仆从在主人床上睡觉/放松 → 和平模式(不战斗)。 */
    @Override
    protected boolean isServantPeaceful() {
        return this.sleepingGoalActive || this.isLying() || this.isRelaxStateOne();
    }

    /** 睡觉时任何 setTarget 都被强制拉回 null(杜绝 Summoned 目标联动把沉睡的猫重新拉入战斗)。 */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.sleepingGoalActive || this.isLying() || this.isRelaxStateOne()) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    /* ==================== ★ 睡觉 goal 注册 ==================== */
    @Override
    protected void registerGoals() {
        super.registerGoals();   // 基类战斗 + Summoned 仆从 goal
        this.goalSelector.addGoal(2, new CatSleepOnOwnerBedGoal());
    }

    /* ====================================================================
     * ★ CatSleepOnOwnerBedGoal —— 复刻原版 CatRelaxOnOwnerGoal：主人睡 → 猫走到床边躺下；
     *     主人起床(睡够且天亮、70% 概率) → 送一份礼物(从 config gifts 列表随机取一)。
     *     仅当仆从处于「跟随/闲逛」(!isStaying) 且不在战斗中才启动；睡下后 via isServantPeaceful 停战。
     * ==================================================================== */
    class CatSleepOnOwnerBedGoal extends Goal {
        @Nullable
        private Player ownerPlayer;
        @Nullable
        private BlockPos goalPos;
        private int onBedTicks;

        @Override
        public boolean canUse() {
            if (ApollyonCatServantEntity.this.getTrueOwner() instanceof Player owner) {
                if (ApollyonCatServantEntity.this.isStaying()) {
                    return false;   // 待命/守卫命令：不睡
                }
                this.ownerPlayer = owner;
                if (!owner.isSleeping()) {
                    return false;
                }
                if (ApollyonCatServantEntity.this.distanceToSqr(owner) > 100.0D) {
                    return false;
                }
                if (ApollyonCatServantEntity.this.getTarget() != null) {
                    return false;   // 战斗中不睡
                }
                BlockPos blockpos = owner.blockPosition();
                BlockState blockstate = ApollyonCatServantEntity.this.level().getBlockState(blockpos);
                if (blockstate.is(BlockTags.BEDS)) {
                    this.goalPos = blockstate.getOptionalValue(BedBlock.FACING).map((dir) -> blockpos.relative(dir.getOpposite())).orElseGet(() -> new BlockPos(blockpos));
                    return !this.spaceIsOccupied();
                }
            }
            return false;
        }

        private boolean spaceIsOccupied() {
            for (ApollyonCatServantEntity other : ApollyonCatServantEntity.this.level().getEntitiesOfClass(ApollyonCatServantEntity.class, (new AABB(this.goalPos)).inflate(2.0D))) {
                if (other != ApollyonCatServantEntity.this && (other.isLying() || other.isRelaxStateOne())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ownerPlayer != null && this.ownerPlayer.isSleeping()
                    && this.goalPos != null && !this.spaceIsOccupied()
                    && !ApollyonCatServantEntity.this.isStaying();
        }

        @Override
        public void start() {
            ApollyonCatServantEntity.this.sleepingGoalActive = true;
            if (this.goalPos != null) {
                ApollyonCatServantEntity.this.getNavigation().moveTo((double) this.goalPos.getX(), (double) this.goalPos.getY(), (double) this.goalPos.getZ(), 1.1D);
            }
        }

        @Override
        public void stop() {
            ApollyonCatServantEntity.this.sleepingGoalActive = false;
            ApollyonCatServantEntity.this.setLying(false);
            float f = ApollyonCatServantEntity.this.level().getTimeOfDay(1.0F);
            if (this.ownerPlayer != null && this.ownerPlayer.getSleepTimer() >= 100 && (double) f > 0.77D && (double) f < 0.8D && (double) ApollyonCatServantEntity.this.level().getRandom().nextFloat() < 0.7D) {
                this.giveMorningGift();
            }
            this.onBedTicks = 0;
            ApollyonCatServantEntity.this.setRelaxStateOne(false);
            ApollyonCatServantEntity.this.getNavigation().stop();
        }

        private void giveMorningGift() {
            if (ApollyonCatServantEntity.this.level().isClientSide) {
                return;
            }
            List<? extends String> ids = AcatConfig.GIFT_ITEMS.get();
            if (ids == null || ids.isEmpty()) {
                return;
            }
            String chosen = ids.get(ApollyonCatServantEntity.this.random.nextInt(ids.size()));
            ResourceLocation rl = ResourceLocation.tryParse(chosen);
            if (rl == null) {
                return;
            }
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                return;
            }
            ApollyonCatServantEntity.this.spawnAtLocation(new ItemStack(item));
        }

        @Override
        public void tick() {
            if (this.ownerPlayer != null && this.goalPos != null) {
                ApollyonCatServantEntity.this.getNavigation().moveTo((double) this.goalPos.getX(), (double) this.goalPos.getY(), (double) this.goalPos.getZ(), 1.1D);
                if (ApollyonCatServantEntity.this.distanceToSqr(this.ownerPlayer) < 2.5D) {
                    ++this.onBedTicks;
                    if (this.onBedTicks > this.adjustedTickDelay(16)) {
                        ApollyonCatServantEntity.this.setLying(true);
                        ApollyonCatServantEntity.this.setRelaxStateOne(false);
                    } else {
                        ApollyonCatServantEntity.this.lookAt(this.ownerPlayer, 45.0F, 45.0F);
                        ApollyonCatServantEntity.this.setRelaxStateOne(true);
                    }
                } else {
                    ApollyonCatServantEntity.this.setLying(false);
                }
            }
        }
    }

}
