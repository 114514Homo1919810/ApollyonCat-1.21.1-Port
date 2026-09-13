package com.acat.ritual;

import com.Polarice3.Goety.api.ritual.IRitualType;
import com.Polarice3.Goety.common.blocks.entities.DarkAltarBlockEntity;
import com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity;
import com.Polarice3.Goety.common.ritual.RitualRequirements;
import com.Polarice3.Goety.common.ritual.RitualTypes;
import com.Polarice3.Goety.utils.ColorUtil;
import com.Polarice3.Goety.utils.ServerParticleUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 亚小猫专用召唤仪式类型（craftType = "apollyon_cat_ritual"，注册进 Goety 的 RitualType 静态表）。
 *
 * 布置与 Apostle 的【安息仪式(sabbath)】完全一致，但在激活祭坛时额外卡两道门槛：
 *   1) 必须身处【下界】(Level.NETHER)；
 *   2) 祭坛高度必须 >127（即 y ≥ 128）；
 *   3) 祭坛周围还必须满足 sabbath 结构（8 哭泣的黑曜石 + 16 黑曜石 + 4 灵魂火，8 格范围内）。
 *
 * 任一条件不满足 → 对玩家弹提示并返回 false，仪式被阻止、材料不消耗。
 * 完成光柱与 Apostle 安息仪式一致（暗红天光）。
 */
public class ApollyonCatSabbathRitualType implements IRitualType {

    /** 与 data/aaacat/recipes/summon_apollyon_cat.json 的 "craftType" 字段保持一致 */
    public static final String NAME = "apollyon_cat_ritual";

    /** 允许召唤的最低祭坛 Y（>127 即 >=128） */
    public static final int MIN_ALTAR_Y = 128;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ItemStack getJeiIcon() {
        return new ItemStack(Items.CRYING_OBSIDIAN);
    }

    /**
     * 激活祭坛尝试开始仪式时的门槛检查（DarkAltarBlockEntity.activate → RitualRequirements.getProperStructure 调用）。
     */
    @Override
    public boolean getRequirement(RitualBlockEntity pTileEntity, @Nullable Player pPlayer, BlockPos pPos, Level pLevel) {
        if (pLevel.dimension() != Level.NETHER) {
            if (pPlayer != null) {
                pPlayer.displayClientMessage(Component.translatable("info.aaacat.ritual.requireNether"), true);
            }
            return false;
        }
        if (pPos.getY() < MIN_ALTAR_Y) {
            if (pPlayer != null) {
                pPlayer.displayClientMessage(Component.translatable("info.aaacat.ritual.requireHeight"), true);
            }
            return false;
        }
        // 与 Apostle 召唤相同的安息仪式(sabbath)结构要求
        return RitualRequirements.getStructures(RitualTypes.SABBATH, pPlayer, pPos, pLevel);
    }

    /** 完成时放出与 Apostle 安息仪式一致的暗红色天光柱 */
    @Override
    public void sendFinishRay(Level world, BlockPos darkAltarPos, DarkAltarBlockEntity tileEntity,
                              Player castingPlayer, ItemStack activationItem) {
        if (world instanceof ServerLevel serverLevel) {
            ColorUtil colorUtil = new ColorUtil(ChatFormatting.DARK_RED);
            Vec3 vec3 = darkAltarPos.getCenter();
            ServerParticleUtil.sendStretchedGodRay(serverLevel, vec3.x, vec3.y, vec3.z, colorUtil);
        }
    }
}
