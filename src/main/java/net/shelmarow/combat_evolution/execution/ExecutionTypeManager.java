package net.shelmarow.combat_evolution.execution;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.shelmarow.combat_evolution.gameassets.ExecutionSkillAnimations;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.HashMap;
import java.util.Map;

public class ExecutionTypeManager {

    public static final Type DEFAULT_TYPE = new Type(ExecutionSkillAnimations.EXECUTION,ExecutionSkillAnimations.EXECUTED_FULL,new Vec3(1.35F,0,0));

    //最高优先级为自定义实体的动画
    //其次是自定义武器的动画
    //最后是默认的类型动画

    //自定义物品处决动画
    private static final Map<ItemStack, Type> CUSTOM_ITEM_EXECUTION_MAP = new HashMap<>(
            Map.of()
    );

    //武器类型处决动画
    private static final Map<WeaponCategory, Type> CATEGORY_EXECUTION_MAP = new HashMap<>(
            Map.of()
    );

    public static Type getExecutionType(ItemStack itemStack){
        return CUSTOM_ITEM_EXECUTION_MAP.get(itemStack);
    }

    public static Type getExecutionType(WeaponCategory weaponCategory){
        return CATEGORY_EXECUTION_MAP.get(weaponCategory);
    }

    public static class Type{
        private final AnimationManager.AnimationAccessor<? extends StaticAnimation> executionAnimation;
        private final AnimationManager.AnimationAccessor<? extends StaticAnimation> executedAnimation;
        private final Vec3 offset;

        public Type(AnimationManager.AnimationAccessor<? extends StaticAnimation> executionAnimation, AnimationManager.AnimationAccessor<? extends StaticAnimation> executedAnimation, Vec3 offset) {
            this.executionAnimation = executionAnimation;
            this.executedAnimation = executedAnimation;
            this.offset = offset;
        }

        public AnimationManager.AnimationAccessor<? extends StaticAnimation> getExecutionAnimation() {
            return executionAnimation;
        }

        public AnimationManager.AnimationAccessor<? extends StaticAnimation> getExecutedAnimation() {
            return executedAnimation;
        }

        public Vec3 getOffset() {
            return offset;
        }
    }
}
