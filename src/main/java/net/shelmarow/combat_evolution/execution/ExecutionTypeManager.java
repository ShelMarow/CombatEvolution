package net.shelmarow.combat_evolution.execution;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.shelmarow.combat_evolution.gameassets.ExecutionSkillAnimations;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionAttackAnimation;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionHitAnimation;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ExecutionTypeManager {

    public static final Type DEFAULT_TYPE = new Type(
            ExecutionSkillAnimations.EXECUTION_SWORD, ExecutionSkillAnimations.EXECUTED_FULL,
            new Vec3(1.35,0,0),-10,100
    );

    public static final Type DAGGER_TYPE = new Type(
            ExecutionSkillAnimations.EXECUTION_DAGGER, ExecutionSkillAnimations.EXECUTED_FULL,
            new Vec3(0.65,0,0),-30,100
    );

    public static final Type TACHI_TYPE = new Type(
            ExecutionSkillAnimations.EXECUTION_TACHI, ExecutionSkillAnimations.EXECUTED_FULL,
            new Vec3(1.35,0,0),-30,100
    );

    public static final Type GREATSWORD_TYPE = new Type(
            ExecutionSkillAnimations.EXECUTION_GREATSWORD, ExecutionSkillAnimations.EXECUTED_FULL,
            new Vec3(1.35,0,-0.4),-23F,100
    );

    //最高优先级为自定义实体的动画
    //其次是自定义武器的动画
    //最后是默认的类型动画

    //自定义物品处决动画
    private static final Map<ResourceLocation, Map<Style, BiFunction<Item, LivingEntityPatch<?>,Type>>> CUSTOM_ITEM_EXECUTION_MAP = new HashMap<>();

    //武器类型处决动画
    //自定义类型
    private static final Map<WeaponCategory, Map<Style, BiFunction<Item, LivingEntityPatch<?>,Type>>> CUSTOM_CATEGORY_EXECUTION_MAP = new HashMap<>();

    //默认类型
    private static final Map<WeaponCategory, Map<Style, BiFunction<Item, LivingEntityPatch<?>,Type>>> CATEGORY_EXECUTION_MAP = Map.of(
            CapabilityItem.WeaponCategories.DAGGER, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> DAGGER_TYPE),
            CapabilityItem.WeaponCategories.SWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> DEFAULT_TYPE),
            CapabilityItem.WeaponCategories.LONGSWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> DEFAULT_TYPE),
            CapabilityItem.WeaponCategories.UCHIGATANA, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> TACHI_TYPE),
            CapabilityItem.WeaponCategories.AXE, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> TACHI_TYPE),
            CapabilityItem.WeaponCategories.TACHI, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> TACHI_TYPE),
            CapabilityItem.WeaponCategories.SPEAR, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> GREATSWORD_TYPE),
            CapabilityItem.WeaponCategories.TRIDENT, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> GREATSWORD_TYPE),
            CapabilityItem.WeaponCategories.GREATSWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch) -> GREATSWORD_TYPE)
    );

    public static void registerByItem(ResourceLocation resourceLocation, Style style, BiFunction<Item, LivingEntityPatch<?>,Type> biFunction) {
        CUSTOM_ITEM_EXECUTION_MAP.computeIfAbsent(resourceLocation, c -> new HashMap<>()).put(style, biFunction);
    }

    public static void registerByCategory(WeaponCategory weaponCategory, Style style, BiFunction<Item, LivingEntityPatch<?>,Type> biFunction) {
        CUSTOM_CATEGORY_EXECUTION_MAP.computeIfAbsent(weaponCategory, c-> new HashMap<>()).put(style, biFunction);
    }

    public static Type getExecutionTypeByItem(Item item, Style style, LivingEntityPatch<?> entityPatch) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        Map<Style, BiFunction<Item, LivingEntityPatch<?>, Type>> stylesTypeMap = CUSTOM_ITEM_EXECUTION_MAP.getOrDefault(id, new HashMap<>());
        BiFunction<Item, LivingEntityPatch<?>, Type> type = stylesTypeMap.containsKey(style) ? stylesTypeMap.get(style) : stylesTypeMap.get(CapabilityItem.Styles.COMMON);
        return type == null ? null : type.apply(item, entityPatch);
    }

    public static Type getExecutionTypeByCategory(WeaponCategory weaponCategory, Style style, Item item, LivingEntityPatch<?> entityPatch) {
        Map<Style, BiFunction<Item, LivingEntityPatch<?>, Type>> custom = CUSTOM_CATEGORY_EXECUTION_MAP.getOrDefault(weaponCategory, new HashMap<>());
        BiFunction<Item, LivingEntityPatch<?>, Type> executionFunction  = custom.containsKey(style) ? custom.get(style) : custom.get(CapabilityItem.Styles.COMMON);
        if(executionFunction != null){
            return executionFunction.apply(item, entityPatch);
        }
        else {
            Map<Style, BiFunction<Item, LivingEntityPatch<?>, Type>> orDefault = CATEGORY_EXECUTION_MAP.getOrDefault(weaponCategory, new HashMap<>());
            return orDefault.containsKey(style) ? orDefault.get(style).apply(item, entityPatch) : orDefault.getOrDefault(CapabilityItem.Styles.COMMON, (i, e)-> DEFAULT_TYPE).apply(item,entityPatch);
        }

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
