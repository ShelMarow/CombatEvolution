package net.shelmarow.combat_evolution.execution;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.iml.CustomExecuteEntity;
import net.shelmarow.combat_evolution.gameassets.ExecutionSkillAnimations;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionAttackAnimation;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionHitAnimation;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExecutionTypeManager {
    //处决类型表
    private static final Map<ResourceLocation, Type> EXECUTION_TYPES = new HashMap<>();
    private static final Map<ResourceLocation, Type> DATAPACK_EXECUTION_TYPES = new HashMap<>();

    private static final Map<ResourceLocation, Type> ASSASSINATION_TYPES = new HashMap<>();

    private static final Map<ResourceLocation, Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type>>> CUSTOM_ENTITY_ASSASSINATION_MAP = new HashMap<>();
    private static final Map<ResourceLocation, Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type>>> CUSTOM_ITEM_ASSASSINATION_MAP = new HashMap<>();
    private static final Map<WeaponCategory, Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type>>> CUSTOM_CATEGORY_ASSASSINATION_MAP = new HashMap<>();


    public static final Type SWORD_TYPE = creatExecutionType(
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "sword"),
            ExecutionSkillAnimations.EXECUTION_SWORD, ExecutionSkillAnimations.EXECUTED_FULL,
            new Vec3(1.35,0,0),-10,100
    );

    public static final Type DAGGER_TYPE = creatExecutionType(
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "dagger"),
            ExecutionSkillAnimations.EXECUTION_DAGGER, ExecutionSkillAnimations.EXECUTED_FULL,
            new Vec3(0.65,0,0),-30,100
    );

    public static final Type TACHI_TYPE = creatExecutionType(
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "tachi"),
            ExecutionSkillAnimations.EXECUTION_TACHI, ExecutionSkillAnimations.EXECUTED_FULL2,
            new Vec3(1.35,0,0.2),0,100
    );

    public static final Type SPEAR_TYPE = creatExecutionType(
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "spear"),
            ExecutionSkillAnimations.EXECUTION_COLOSSALSWORD, ExecutionSkillAnimations.EXECUTED_FULL2,
            new Vec3(1.5,0,0),0F,100
    );

    public static final Type GREATSWORD_TYPE = creatExecutionType(
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "greatsword"),
            ExecutionSkillAnimations.EXECUTION_GREATSWORD, ExecutionSkillAnimations.EXECUTED_FULL,
            new Vec3(1.35,0,-0.4),-23F,100
    );

    public static final Type COLOSSALSWORD_TYPE = creatExecutionType(
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "colossalsword"),
            ExecutionSkillAnimations.EXECUTION_COLOSSALSWORD, ExecutionSkillAnimations.EXECUTED_FULL2,
            new Vec3(1.35,0,0),0F,100
    );


    //最高优先级为自定义实体的动画
    //其次是自定义武器的动画
    //最后是默认的类型动画

    //自定义实体处决动画
    private static final Map<ResourceLocation, Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>>> CUSTOM_ENTITY_EXECUTION_MAP = new HashMap<>();
    private static final Map<ResourceLocation, Type> CUSTOM_DATAPACK_ENTITY_EXECUTION_MAP = new HashMap<>();

    //自定义物品处决动画
    private static final Map<ResourceLocation, Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>>> CUSTOM_ITEM_EXECUTION_MAP = new HashMap<>();

    //武器类型处决动画
    //自定义类型
    private static final Map<WeaponCategory, Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>>> CUSTOM_CATEGORY_EXECUTION_MAP = new HashMap<>();

    //默认类型
    private static final Map<WeaponCategory, Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>>> CATEGORY_EXECUTION_MAP = Map.of(
            CapabilityItem.WeaponCategories.DAGGER, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> DAGGER_TYPE),
            CapabilityItem.WeaponCategories.SWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> SWORD_TYPE),
            CapabilityItem.WeaponCategories.LONGSWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> SWORD_TYPE),
            CapabilityItem.WeaponCategories.AXE, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> COLOSSALSWORD_TYPE),
            CapabilityItem.WeaponCategories.UCHIGATANA, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> TACHI_TYPE),
            CapabilityItem.WeaponCategories.TACHI, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> TACHI_TYPE),
            CapabilityItem.WeaponCategories.SPEAR, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> SPEAR_TYPE),
            CapabilityItem.WeaponCategories.TRIDENT, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> GREATSWORD_TYPE),
            CapabilityItem.WeaponCategories.GREATSWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> {
                return entityPatch.getOriginal().level().getRandom().nextDouble() >= 0.5 ? GREATSWORD_TYPE : COLOSSALSWORD_TYPE;
            })
    );









    public static final Type ASSASSINATION_COLOSSALSWORD_TYPE = creatAssassinationType(
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "assassination_colossalsword"),
            ExecutionSkillAnimations.EXECUTION_COLOSSALSWORD, ExecutionSkillAnimations.ASSASSINATION,
            new Vec3(-1.0,0,0.2),0,100
    );


    public static final Type ASSASSINATION_DAGGER_TYPE = creatExecutionType(
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "assassination_dagger"),
            ExecutionSkillAnimations.EXECUTION_DAGGER, ExecutionSkillAnimations.ASSASSINATION,
            new Vec3(-0.65,0,0),0,100
    );

    //暗杀的处决类型
    private static final Map<WeaponCategory, Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>>> CATEGORY_ASSASSINATION_MAP = Map.of(
            CapabilityItem.WeaponCategories.DAGGER, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_DAGGER_TYPE),
            CapabilityItem.WeaponCategories.SWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_DAGGER_TYPE),
            CapabilityItem.WeaponCategories.LONGSWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_DAGGER_TYPE),
            CapabilityItem.WeaponCategories.AXE, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_COLOSSALSWORD_TYPE),
            CapabilityItem.WeaponCategories.UCHIGATANA, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_COLOSSALSWORD_TYPE),
            CapabilityItem.WeaponCategories.TACHI, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_COLOSSALSWORD_TYPE),
            CapabilityItem.WeaponCategories.SPEAR, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_COLOSSALSWORD_TYPE),
            CapabilityItem.WeaponCategories.TRIDENT, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_COLOSSALSWORD_TYPE),
            CapabilityItem.WeaponCategories.GREATSWORD, Map.of(CapabilityItem.Styles.COMMON, (item, entityPatch, targetPatch) -> ASSASSINATION_COLOSSALSWORD_TYPE)
    );

    public static void registerAssassinationByEntity(ResourceLocation resourceLocation, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type> function) {
        CUSTOM_ENTITY_ASSASSINATION_MAP.computeIfAbsent(resourceLocation, key -> new HashMap<>()).put(style, function);
    }

    public static void registerAssassinationByItem(ResourceLocation resourceLocation, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type> function) {
        CUSTOM_ITEM_ASSASSINATION_MAP.computeIfAbsent(resourceLocation, key -> new HashMap<>()).put(style, function);
    }

    public static void registerAssassinationByCategory(WeaponCategory weaponCategory, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type> function) {
        CUSTOM_CATEGORY_ASSASSINATION_MAP.computeIfAbsent(weaponCategory, key -> new HashMap<>()).put(style, function);
    }

    public static void registerByEntity(ResourceLocation resourceLocation, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type> function) {
        CUSTOM_ENTITY_EXECUTION_MAP.computeIfAbsent(resourceLocation, c -> new HashMap<>()).put(style, function);
    }

    public static void registerByDatapackEntity(ResourceLocation resourceLocation, Type type) {
        CUSTOM_DATAPACK_ENTITY_EXECUTION_MAP.put(resourceLocation, type);
    }

    public static void registerByItem(ResourceLocation resourceLocation, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type> function) {
        CUSTOM_ITEM_EXECUTION_MAP.computeIfAbsent(resourceLocation, c -> new HashMap<>()).put(style, function);
    }

    public static void registerByCategory(WeaponCategory weaponCategory, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type> function) {
        CUSTOM_CATEGORY_EXECUTION_MAP.computeIfAbsent(weaponCategory, c-> new HashMap<>()).put(style, function);
    }

    public static Type getExecutionTypeByEntity(EntityType<?> entityType, Item item, Style style, LivingEntityPatch<?> entityPatch,  LivingEntityPatch<?> targetPatch) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>> stylesTypeMap = CUSTOM_ENTITY_EXECUTION_MAP.getOrDefault(id, new HashMap<>());
        TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type> type = stylesTypeMap.containsKey(style) ? stylesTypeMap.get(style) : stylesTypeMap.get(CapabilityItem.Styles.COMMON);
        return type == null ? null : type.apply(item, entityPatch,targetPatch);
    }

    public static Type getExecutionTypeByDatapackEntity(EntityType<?> entityType) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        return CUSTOM_DATAPACK_ENTITY_EXECUTION_MAP.get(id);
    }

    public static Type getExecutionTypeByItem(Item item, Style style, LivingEntityPatch<?> entityPatch,  LivingEntityPatch<?> targetPatch) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>> stylesTypeMap = CUSTOM_ITEM_EXECUTION_MAP.getOrDefault(id, new HashMap<>());
        TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type> type = stylesTypeMap.containsKey(style) ? stylesTypeMap.get(style) : stylesTypeMap.get(CapabilityItem.Styles.COMMON);
        return type == null ? null : type.apply(item, entityPatch, targetPatch);
    }

    public static Type getExecutionTypeByCategory(WeaponCategory weaponCategory, Style style, Item item, LivingEntityPatch<?> entityPatch,  LivingEntityPatch<?> targetPatch) {
        Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>> custom = CUSTOM_CATEGORY_EXECUTION_MAP.getOrDefault(weaponCategory, new HashMap<>());
        TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type> executionFunction  = custom.containsKey(style) ? custom.get(style) : custom.get(CapabilityItem.Styles.COMMON);
        if(executionFunction != null){
            return executionFunction.apply(item, entityPatch, targetPatch);
        }
        else {
            Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>,Type>> orDefault = CATEGORY_EXECUTION_MAP.getOrDefault(weaponCategory, new HashMap<>());
            return orDefault.containsKey(style) ? orDefault.get(style).apply(item, entityPatch, targetPatch) : orDefault.getOrDefault(CapabilityItem.Styles.COMMON, (i, e, t)-> SWORD_TYPE).apply(item,entityPatch, targetPatch);
        }

    }

    public static Type getAssassinationTypeByEntity(EntityType<?> entityType, Item item, Style style, LivingEntityPatch<?> executorPatch, LivingEntityPatch<?> targetPatch) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type>> styles = CUSTOM_ENTITY_ASSASSINATION_MAP.getOrDefault(id, Map.of());
        TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type> function = styles.getOrDefault(style, styles.get(CapabilityItem.Styles.COMMON));
        return function == null ? null : function.apply(item, executorPatch, targetPatch);
    }

    public static Type getAssassinationTypeByItem(Item item, Style style, LivingEntityPatch<?> executorPatch, LivingEntityPatch<?> targetPatch) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type>> styles = CUSTOM_ITEM_ASSASSINATION_MAP.getOrDefault(id, Map.of());
        TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type> function = styles.getOrDefault(style, styles.get(CapabilityItem.Styles.COMMON));
        return function == null ? null : function.apply(item, executorPatch, targetPatch);
    }

    public static Type getAssassinationTypeByCategory(WeaponCategory weaponCategory, Style style, Item item, LivingEntityPatch<?> executorPatch, LivingEntityPatch<?> targetPatch) {
        Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type>> custom = CUSTOM_CATEGORY_ASSASSINATION_MAP.getOrDefault(weaponCategory, Map.of());
        TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type> function = custom.getOrDefault(style, custom.get(CapabilityItem.Styles.COMMON));
        if (function != null) {
            return function.apply(item, executorPatch, targetPatch);
        }

        Map<Style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, Type>> defaults = CATEGORY_ASSASSINATION_MAP.getOrDefault(weaponCategory, Map.of());
        function = defaults.getOrDefault(style, defaults.get(CapabilityItem.Styles.COMMON));
        return function == null ? ASSASSINATION_COLOSSALSWORD_TYPE : function.apply(item, executorPatch, targetPatch);
    }

    @Deprecated
    public static Type getAssassinationType(WeaponCategory weaponCategory, Style style, Item item, LivingEntityPatch<?> executorPatch, LivingEntityPatch<?> targetPatch) {
        return getAssassinationTypeByCategory(weaponCategory, style, item, executorPatch, targetPatch);
    }

    public static Type getAssassinationType(LivingEntityPatch<?> executorPatch, LivingEntityPatch<?> targetPatch) {
        CapabilityItem capabilityItem = executorPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        Item item = executorPatch.getOriginal().getItemInHand(InteractionHand.MAIN_HAND).getItem();
        WeaponCategory weaponCategory = capabilityItem.getWeaponCategory();
        Style style = capabilityItem.getStyle(executorPatch);

        Type assassinationType = getAssassinationTypeByItem(item, style, executorPatch, targetPatch);
        if (assassinationType == null) {
            assassinationType = getAssassinationTypeByCategory(weaponCategory, style, item, executorPatch, targetPatch);
        }

        Type entityType = getAssassinationTypeByEntity(targetPatch.getOriginal().getType(), item, style, executorPatch, targetPatch);
        if (entityType != null) {
            return entityType;
        }

        if (targetPatch instanceof CustomExecuteEntity customExecuteEntity
                && customExecuteEntity.canUseCustomType(executorPatch, assassinationType)) {
            Type customType = customExecuteEntity.getExecutionType(executorPatch, assassinationType);
            if (customType != null) {
                return customType;
            }
        }

        return assassinationType;
    }

    public static Type creatExecutionType(ResourceLocation id, AnimationManager.AnimationAccessor<? extends ExecutionAttackAnimation> executionAnimation, AnimationManager.AnimationAccessor<? extends ExecutionHitAnimation> executedAnimation, Vec3 offset, float rotationOffset, int totalTick){
        Type type = new Type(executionAnimation, executedAnimation, offset, rotationOffset, totalTick);
        EXECUTION_TYPES.put(id, type);
        return type;
    }

    public static Type creatAssassinationType(ResourceLocation id, AnimationManager.AnimationAccessor<? extends ExecutionAttackAnimation> executionAnimation, AnimationManager.AnimationAccessor<? extends ExecutionHitAnimation> executedAnimation, Vec3 offset, float rotationOffset, int totalTick) {
        Type type = new Type(executionAnimation, executedAnimation, offset, rotationOffset, totalTick);
        ASSASSINATION_TYPES.put(id, type);
        return type;
    }

    public static void creatDatapackExecutionType(ResourceLocation id, AnimationManager.AnimationAccessor<? extends StaticAnimation> executionAnimation, AnimationManager.AnimationAccessor<? extends StaticAnimation> executedAnimation, Vec3 offset, float rotationOffset, int totalTick){
        Type type = new Type(executionAnimation, executedAnimation, offset, rotationOffset, totalTick);
        DATAPACK_EXECUTION_TYPES.put(id, type);
    }


    public static Type getAssassinationType(ResourceLocation id) {
        return ASSASSINATION_TYPES.get(id);
    }

    public static Set<ResourceLocation> getAssassinationTypeKeys() {
        return new HashSet<>(ASSASSINATION_TYPES.keySet());
    }

    public static void clearDatapackExecutionType(){
        DATAPACK_EXECUTION_TYPES.clear();
    }

    public static void clearDatapackExecutionMob(){
       CUSTOM_DATAPACK_ENTITY_EXECUTION_MAP.clear();
    }


    public static @Nullable Type getExecutionType(ResourceLocation id){
        Type type = DATAPACK_EXECUTION_TYPES.get(id);
        if (type == null) {
            type = EXECUTION_TYPES.get(id);
        }
        return type == null ? ASSASSINATION_TYPES.get(id) : type;
    }

    public static Set<ResourceLocation> getExecutionTypeKeys() {
        Set<ResourceLocation> keys = new HashSet<>();
        keys.addAll(EXECUTION_TYPES.keySet());
        keys.addAll(DATAPACK_EXECUTION_TYPES.keySet());
        keys.addAll(ASSASSINATION_TYPES.keySet());
        return keys;
    }


    public static class Type{
        private final AnimationManager.AnimationAccessor<? extends StaticAnimation> executionAnimation;
        private final AnimationManager.AnimationAccessor<? extends StaticAnimation> executedAnimation;
        private final Vec3 offset;
        private final float rotationOffset;
        private final int totalTick;
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

        @Deprecated
        public Type(AnimationManager.AnimationAccessor<? extends StaticAnimation> executionAnimation, AnimationManager.AnimationAccessor<? extends StaticAnimation> executedAnimation, Vec3 offset, float rotationOffset, int totalTick) {
            this.executionAnimation = executionAnimation;
            this.executedAnimation = executedAnimation;
            this.offset = offset;
            this.rotationOffset = rotationOffset;
            this.totalTick = totalTick;
        }

        public AnimationManager.AnimationAccessor<? extends StaticAnimation> executionAnimation(){
            return executionAnimation;
        }

        public AnimationManager.AnimationAccessor<? extends StaticAnimation> executedAnimation(){
            return executedAnimation;
        }

        public Vec3 offset(){
            return offset;
        }

        public float rotationOffset(){
            return rotationOffset;
        }

        public int totalTick(){
            return totalTick;
        }
    }
}
