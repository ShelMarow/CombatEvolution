package net.shelmarow.combat_evolution.execution;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.shelmarow.combat_evolution.gameassets.ExecutionSkillAnimations;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionAttackAnimation;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionHitAnimation;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.item.EpicFightItems;

import java.util.HashMap;
import java.util.Map;

public class ExecutionTypeManager {

    public static final Type DEFAULT_TYPE =
            new Type(
                    ExecutionSkillAnimations.EXECUTION_SWORD, ExecutionSkillAnimations.EXECUTED_FULL,
                    new Vec3(1.35,0,0),-10,100
            );

    public static final Type DAGGER_TYPE =
            new Type(
                    ExecutionSkillAnimations.EXECUTION_DAGGER, ExecutionSkillAnimations.EXECUTED_FULL,
                    new Vec3(0.65,0,0),-30,100
            );

    public static final Type TACHI_TYPE =
            new Type(
                    ExecutionSkillAnimations.EXECUTION_TACHI, ExecutionSkillAnimations.EXECUTED_FULL,
                    new Vec3(1.35,0,0),-30,100
            );

    public static final Type GREATSWORD_TYPE =
            new Type(
                    ExecutionSkillAnimations.EXECUTION_GREATSWORD, ExecutionSkillAnimations.EXECUTED_FULL,
                    new Vec3(1.35,0,-0.4),-23F,100
            );

    //最高优先级为自定义实体的动画
    //其次是自定义武器的动画
    //最后是默认的类型动画

    //自定义物品处决动画
    private static final Map<ResourceLocation, Type> CUSTOM_ITEM_EXECUTION_MAP = new HashMap<>();

    //武器类型处决动画
    //自定义类型
    private static final Map<WeaponCategory, Type> CUSTOM_CATEGORY_EXECUTION_MAP = new HashMap<>();

    //默认类型
    private static final Map<WeaponCategory, Type> CATEGORY_EXECUTION_MAP = new HashMap<>(Map.of(
            CapabilityItem.WeaponCategories.DAGGER, DAGGER_TYPE,
            CapabilityItem.WeaponCategories.SWORD, DEFAULT_TYPE,
            CapabilityItem.WeaponCategories.LONGSWORD, DEFAULT_TYPE,
            CapabilityItem.WeaponCategories.UCHIGATANA, DEFAULT_TYPE,
            CapabilityItem.WeaponCategories.AXE, TACHI_TYPE,
            CapabilityItem.WeaponCategories.TACHI, TACHI_TYPE,
            CapabilityItem.WeaponCategories.SPEAR, GREATSWORD_TYPE,
            CapabilityItem.WeaponCategories.TRIDENT, GREATSWORD_TYPE,
            CapabilityItem.WeaponCategories.GREATSWORD, GREATSWORD_TYPE
    ));

    public static void registerByItem(ResourceLocation resourceLocation, ExecutionTypeManager.Type type) {
        CUSTOM_ITEM_EXECUTION_MAP.put(resourceLocation, type);
    }

    public static void registerByCategory(WeaponCategory weaponCategory, ExecutionTypeManager.Type type) {
        CUSTOM_CATEGORY_EXECUTION_MAP.put(weaponCategory,type);
    }

    public static Type getExecutionTypeByItem(Item item){
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return CUSTOM_ITEM_EXECUTION_MAP.get(id);
    }

    public static Type getExecutionTypeByCategory(WeaponCategory weaponCategory){
        return CUSTOM_CATEGORY_EXECUTION_MAP.getOrDefault(weaponCategory, CATEGORY_EXECUTION_MAP.getOrDefault(weaponCategory,DEFAULT_TYPE));
    }

    public record Type(AnimationManager.AnimationAccessor<? extends ExecutionAttackAnimation> executionAnimation, AnimationManager.AnimationAccessor<? extends ExecutionHitAnimation> executedAnimation, Vec3 offset, float rotationOffset, int totalTick){
        /*
            参数说明
            executionAnimation：处决者使用的动画
            executedAnimation：被处决者播放的动画
            offset：处决位置的偏移
                其中x为前后偏移，正数远离
                y为高度偏移，一般不调整
                z为左右偏移，左负右正
            rotationOffset：在传送完之后的视线偏移角度
            totalTick：处决持续的总时长
         */
    }
}
