package net.shelmarow.combat_evolution.ai.efcondition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class CurrentAngle implements Condition<LivingEntityPatch<?>> {
    private String side;
    private double degreeFirst;
    private double degreeSecond;

    public CurrentAngle(String side, double degreeFirst, double degreeSecond) {
        this.side = side;
        this.degreeFirst = degreeFirst;
        this.degreeSecond = degreeSecond;

    }

    public CurrentAngle() {
    }

    @Override
    public Condition<LivingEntityPatch<?>> read(CompoundTag tag){
        this.side = tag.getString("side");
        this.degreeFirst = tag.getDouble("first");
        this.degreeSecond = tag.getDouble("second");
        return this;
    }

    @Override
    public CompoundTag serializePredicate(){
        CompoundTag tag = new CompoundTag();
        tag.putString("side", this.side);
        tag.putDouble("first", this.degreeFirst);
        tag.putDouble("second", this.degreeSecond);
        return tag;
    }

    @Override
    public boolean predicate(LivingEntityPatch<?> livingEntityPatch){
        if(livingEntityPatch.getTarget() != null){
            LivingEntity target = livingEntityPatch.getTarget();
            return isInSideAngleRange(livingEntityPatch.getOriginal(),target, this.side, this.degreeFirst, this.degreeSecond);
        }
        return false;
    }

    @Override
    public List<ParameterEditor> getAcceptingParameters(Screen screen){
        return null;
    }

    /**
     * 计算目标相对于观察者的左右角度
     * 左侧返回负值(绝对值 0~180)，右侧返回正值(0~180)，0 表示正前方
     */
    public static double getSideAngle(LivingEntity observer, LivingEntity target) {
        Vec3 lookVec = observer.getLookAngle().normalize();
        lookVec = new Vec3(lookVec.x, 0, lookVec.z).normalize(); // 只取水平分量

        Vec3 toTarget = new Vec3(
                target.getX() - observer.getX(),
                0,
                target.getZ() - observer.getZ()
        ).normalize();

        // 计算夹角 (0~180)
        double dot = lookVec.dot(toTarget);
        dot = Math.max(-1.0, Math.min(1.0, dot)); // 防止浮点误差
        double angle = Math.toDegrees(Math.acos(dot));

        // 判断左右（叉积 y > 0 表示左侧）
        double crossY = lookVec.cross(toTarget).y;
        if (crossY > 0) {
            return -angle; // 左侧
        } else if (crossY < 0) {
            return angle; // 右侧
        } else {
            return 0; // 正前/正后
        }
    }

    /**
     * 检测目标是否在指定角度区间（可跨越区间起点）
     * @param observer 观察者
     * @param target 目标
     * @param side "left" 或 "right"
     * @param startAngle 区间起始角度（0~180）
     * @param endAngle 区间结束角度（0~180）
     */
    public static boolean isInSideAngleRange(LivingEntity observer, LivingEntity target,
                                             String side, double startAngle, double endAngle) {
        double angle = getSideAngle(observer, target);
        double absAngle = Math.abs(angle);

        if ("left".equalsIgnoreCase(side) && angle < 0) {
            return isAngleInRange(absAngle, startAngle, endAngle);
        } else if ("right".equalsIgnoreCase(side) && angle > 0) {
            return isAngleInRange(absAngle, startAngle, endAngle);
        }
        return false;
    }

    /**
     * 检查角度是否在给定范围（0~180，可跨越起点）
     */
    private static boolean isAngleInRange(double angle, double start, double end) {
        if (start <= end) {
            return angle >= start && angle <= end;
        } else {
            // 跨越的情况，例如 150~30（180→0→30）
            return angle >= start || angle <= end;
        }
    }

}
