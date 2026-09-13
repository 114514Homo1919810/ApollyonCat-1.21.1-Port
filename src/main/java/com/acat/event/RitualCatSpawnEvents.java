package com.acat.event;

import com.Polarice3.Goety.common.entities.boss.Apostle;
import com.Polarice3.Goety.common.entities.util.SummonApostle;
import com.acat.AcatMod;
import com.acat.entity.ApollyonCatEntity;
import com.acat.registry.AcatEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 亚小猫仪式替换：
 * 玩家用仪式召唤 Apostle、且法阵(SummonApostle)消失生成使徒的瞬间——
 * 若身处下界且高度 >127、附近玩家手持生鳕鱼或生鲑鱼，则有 25% 概率
 * 把普通 Apostle 替换为亚小猫。
 */
@EventBusSubscriber(modid = AcatMod.MOD_ID)
public class RitualCatSpawnEvents {

    private static final float REPLACE_CHANCE = 0.25F;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Apostle apostle) || entity instanceof ApollyonCatEntity) {
            return;
        }
        Level level = event.getLevel();
        // 只在仪式法阵(SummonApostle)正要生成使徒的这一刻替换
        if (level.dimension() != Level.NETHER) {
            return;
        }
        if (entity.getY() <= 127.0D) {
            return;
        }
        boolean ritual = !level.getEntitiesOfClass(SummonApostle.class,
                entity.getBoundingBox().inflate(8.0D)).isEmpty();
        if (!ritual) {
            return;
        }
        // 附近玩家手持生鱼（主手或副手，不消耗）
        boolean holdingFish = false;
        for (Player player : level.getEntitiesOfClass(Player.class,
                entity.getBoundingBox().inflate(16.0D))) {
            if (player.isSpectator()) {
                continue;
            }
            if (player.getMainHandItem().is(Items.COD) || player.getOffhandItem().is(Items.COD)
                    || player.getMainHandItem().is(Items.SALMON) || player.getOffhandItem().is(Items.SALMON)) {
                holdingFish = true;
                break;
            }
        }
        if (!holdingFish) {
            return;
        }
        if (level.random.nextFloat() >= REPLACE_CHANCE) {
            return;
        }
        // 替换：生成亚小猫，取消原使徒
        if (level instanceof ServerLevel serverLevel) {
            ApollyonCatEntity cat = new ApollyonCatEntity(AcatEntities.APOLLYON_CAT.get(), level);
            cat.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
            // 1.21 起 Mob#finalizeSpawn 去掉了 CompoundTag 参数（4 参重载）
            cat.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(entity.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(cat);
            event.setCanceled(true);
            apostle.discard();
        }
    }
}
