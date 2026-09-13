package com.acat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 圣金魂匣（aaacat:holy_soul_ark）
 * ─────────────────────────────────────────────────────────
 * 「便携版灵魂方舟」：放入背包任意位置(含副手)即生效，无需先绑定；
 * 潜行右键把当前坐标写入 NBT 作为「复活归位点」(优先级高于真实方舟)。
 *
 * 持有者(终身绑定，不可更换)：
 *   首次放入某玩家背包(或首次被该玩家使用时)自动写入持有者 UUID，
 *   此后只有持有者本人能使用(绑定/取消绑定/回魂/属性/治疗/死亡复活)。
 *
 * 防护：
 *   注册时 .fireResistant()(火焰/岩浆不燃) +
 *   canBeHurtBy 覆写(掉落物形态免疫 火焰/岩浆/爆炸/仙人掌)。
 *
 * NBT：
 *   Bound    : 1b                 —— 是否已绑定坐标
 *   BoundPos : {X,Y,Z}            —— 绑定坐标
 *   BoundDim : "minecraft:overworld" 等 —— 绑定维度
 *   Owner    : UUID               —— 终身持有者
 *   OwnerName: String             —— 持有者显示名(仅用于 tooltip)
 */
public class HolySoulArkItem extends Item {

    public static final String TAG_BOUND = "Bound";
    public static final String TAG_BOUND_POS = "BoundPos";
    public static final String TAG_BOUND_DIM = "BoundDim";
    public static final String TAG_OWNER = "Owner";
    public static final String TAG_OWNER_NAME = "OwnerName";

    public HolySoulArkItem(Properties properties) {
        super(properties);
    }

    /** 物品名显示为金色(传说级)。 */
    @Override
    public Component getName(ItemStack pStack) {
        return Component.translatable(this.getDescriptionId(pStack)).withStyle(ChatFormatting.GOLD);
    }

    /* ---------- 掉落物防护：防火 / 防爆炸 / 防仙人掌 ---------- */
    /*
     * ItemEntity.hurt() 会先调用 Item.canBeHurtBy(DamageSource) 做物品级判定：
     * 返回 false 则该伤害源对物品实体完全无效(不会被点燃/烧毁/炸毁/被仙人掌销毁)。
     * 火焰与岩浆同属 minecraft:is_fire 标签；爆炸伤害(is_explosion)与
     * 仙人掌(minecraft:cactus) 一并豁免。
     */
    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.CACTUS)) {
            return false;
        }
        return super.canBeHurtBy(stack, source);
    }

    /* ---------- 持有者(UUID 终身绑定，不可更换) ---------- */

    public static boolean hasOwner(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().hasUUID(TAG_OWNER);
    }

    @Nullable
    public static UUID getOwner(ItemStack stack) {
        if (hasOwner(stack)) {
            return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getUUID(TAG_OWNER);
        }
        return null;
    }

    public static String getOwnerName(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!stack.isEmpty() && tag.contains(TAG_OWNER_NAME, Tag.TAG_STRING)) {
            return tag.getString(TAG_OWNER_NAME);
        }
        return "";
    }

    /** 写入持有者(仅在无主时调用，一次写入后终身有效)。 */
    public static void setOwner(ItemStack stack, UUID uuid, String name) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID(TAG_OWNER, uuid);
            tag.putString(TAG_OWNER_NAME, name == null ? "" : name);
        });
    }

    /** 该玩家是否为这把魂匣的终身持有者。 */
    public static boolean isOwner(ItemStack stack, Player player) {
        UUID owner = getOwner(stack);
        return owner != null && owner.equals(player.getUUID());
    }

    /* ---------- NBT 工具（供事件类复用） ---------- */

    /** 该物品是否已绑定过坐标。 */
    public static boolean isBound(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(TAG_BOUND);
    }

    /** 把绑定坐标写入物品 NBT。 */
    public static void setBound(ItemStack stack, BlockPos pos, ResourceKey<Level> dim) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(TAG_BOUND, true);
            tag.put(TAG_BOUND_POS, NbtUtils.writeBlockPos(pos));
            tag.putString(TAG_BOUND_DIM, dim.location().toString());
        });
    }

    /** 取消绑定坐标(仅清坐标，不影响持有者)。 */
    public static void clearBound(ItemStack stack) {
        if (stack.isEmpty() || stack.get(DataComponents.CUSTOM_DATA) == null) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(TAG_BOUND);
            tag.remove(TAG_BOUND_POS);
            tag.remove(TAG_BOUND_DIM);
        });
    }

    @Nullable
    public static BlockPos getBoundPos(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (isBound(stack) && tag.contains(TAG_BOUND_POS)) {
            return NbtUtils.readBlockPos(tag, TAG_BOUND_POS).orElse(null);
        }
        return null;
    }

    @Nullable
    public static ResourceKey<Level> getBoundDim(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (isBound(stack) && tag.contains(TAG_BOUND_DIM)) {
            ResourceLocation rl = ResourceLocation.tryParse(tag.getString(TAG_BOUND_DIM));
            if (rl != null) {
                return ResourceKey.create(Registries.DIMENSION, rl);
            }
        }
        return null;
    }

    /* ---------- 背包检索(36 格 + 副手，均要求是“该玩家自己的”魂匣) ---------- */

    /** 背包(36 格)+副手中是否存在属于该玩家的圣金魂匣(无论是否绑定)。 */
    public static boolean hasUsableInInventory(Player player) {
        return !findUsableInInventory(player).isEmpty();
    }

    /** 背包(36 格)+副手中的第一把属于该玩家的圣金魂匣。 */
    public static ItemStack findUsableInInventory(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof HolySoulArkItem && isOwner(stack, player)) {
                return stack;
            }
        }
        ItemStack offhand = player.getInventory().offhand.get(0);
        if (!offhand.isEmpty() && offhand.getItem() instanceof HolySoulArkItem && isOwner(offhand, player)) {
            return offhand;
        }
        return ItemStack.EMPTY;
    }

    /** 背包(36 格)+副手中是否存在属于该玩家的“已绑定坐标”魂匣。 */
    public static boolean hasBoundUsableInInventory(Player player) {
        return !findBoundUsableInInventory(player).isEmpty();
    }

    /** 背包(36 格)+副手中的第一把属于该玩家的“已绑定坐标”魂匣。 */
    public static ItemStack findBoundUsableInInventory(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof HolySoulArkItem && isBound(stack) && isOwner(stack, player)) {
                return stack;
            }
        }
        ItemStack offhand = player.getInventory().offhand.get(0);
        if (!offhand.isEmpty() && offhand.getItem() instanceof HolySoulArkItem && isBound(offhand) && isOwner(offhand, player)) {
            return offhand;
        }
        return ItemStack.EMPTY;
    }

    /** 无主魂匣 → 归属当前玩家(终身绑定)。返回 true 表示本次完成归属。 */
    public static boolean claimFor(ItemStack stack, Player player) {
        if (!stack.isEmpty() && stack.getItem() instanceof HolySoulArkItem && !hasOwner(stack)) {
            setOwner(stack, player.getUUID(), player.getName().getString());
            return true;
        }
        return false;
    }

    /* ---------- 右键行为 ---------- */

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // 无主 → 首次使用视为归属(正常情况下放入背包时已被 tick 归属)
            if (claimFor(stack, player)) {
                player.displayClientMessage(Component.translatable("info.aaacat.holy_soul_ark.claim"), true);
                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.6F);
            }
            if (!isOwner(stack, player)) {
                player.displayClientMessage(Component.translatable("info.aaacat.holy_soul_ark.notOwner"), true);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            if (player.isShiftKeyDown()) {
                // 潜行右键：绑定 / 重新绑定当前位置(仅持有者)
                BlockPos pos = player.blockPosition();
                setBound(stack, pos, level.dimension());
                player.displayClientMessage(Component.translatable("info.aaacat.holy_soul_ark.bind",
                        level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ()), true);
                player.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8F, 0.9F);
            } else {
                // 普通右键：查询当前绑定信息(仅持有者)
                BlockPos pos = getBoundPos(stack);
                ResourceKey<Level> dim = getBoundDim(stack);
                if (pos != null && dim != null) {
                    player.displayClientMessage(Component.translatable("info.aaacat.holy_soul_ark.boundAt",
                            dim.location().toString(), pos.getX(), pos.getY(), pos.getZ()), true);
                } else {
                    player.displayClientMessage(Component.translatable("info.aaacat.holy_soul_ark.notBound"), true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.descUnbind").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.descBind").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.descEnhance").withStyle(ChatFormatting.GRAY));

        if (hasOwner(stack)) {
            String ownerName = getOwnerName(stack);
            if (!ownerName.isEmpty()) {
                tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.owner", ownerName).withStyle(ChatFormatting.DARK_AQUA));
            } else {
                tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.ownerUuid").withStyle(ChatFormatting.DARK_AQUA));
            }
        } else {
            tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.noOwner").withStyle(ChatFormatting.RED));
        }

        BlockPos pos = getBoundPos(stack);
        ResourceKey<Level> dim = getBoundDim(stack);
        if (pos != null && dim != null) {
            tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.bound",
                    dim.location().toString(), pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            tooltip.add(Component.translatable("item.aaacat.holy_soul_ark.notBoundTooltip").withStyle(ChatFormatting.RED));
        }
    }
}
