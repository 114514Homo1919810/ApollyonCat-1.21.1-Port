package com.acat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * 至纯喵环（aaacat:pure_cat_ring）
 * ───────────────────────────────
 * 亚小猫的专属掉落物，也是「亚小猫仆从召唤仪式」的核心(黑暗祭坛激活物)。
 * 名称红色；描述：「显而易见，这东西太小了，你戴不上。」
 * 贴图直接复用亚小猫头顶光环 cat_hole.png（拷贝一份到 textures/item，模型 layer0 → aaacat:item/cat_hole）。
 */
public class PureCatRingItem extends Item {

    public PureCatRingItem(Properties properties) {
        super(properties);
    }

    /* ---------- 掉落物防护：防火 / 防爆炸 / 防仙人掌（圣金魂匣同款） ---------- */
    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.CACTUS)) {
            return false;
        }
        return super.canBeHurtBy(stack, source);
    }

    /** 物品名：红色（不依赖稀有度着色，直接压红） */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack))
                .withStyle(ChatFormatting.RED);
    }

    /** 描述行 */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.aaacat.pure_cat_ring.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
