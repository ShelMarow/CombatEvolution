package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class CurrentAngle implements Condition<LivingEntityPatch<?>> {
    private static final double EPS = 1e-6;
    private TargetSide side;
    private double degreeFirst;
    private double degreeSecond;

    public CurrentAngle(TargetSide side, double degreeFirst, double degreeSecond) {
        this.side = side;
        this.degreeFirst = degreeFirst;
        this.degreeSecond = degreeSecond;

    }

    public CurrentAngle() {
    }

    @Override
    public Condition<LivingEntityPatch<?>> read(CompoundTag tag){
        this.side = TargetSide.valueOf(tag.getString("side").toUpperCase());
        this.degreeFirst = tag.getDouble("first");
        this.degreeSecond = tag.getDouble("second");
        return this;
    }

    @Override
    public CompoundTag serializePredicate(){
        CompoundTag tag = new CompoundTag();
        tag.putString("side", this.side.toString().toUpperCase());
        tag.putDouble("first", this.degreeFirst);
        tag.putDouble("second", this.degreeSecond);
        return tag;
    }

    @Override
    public boolean predicate(LivingEntityPatch<?> livingEntityPatch){
        if(livingEntityPatch.getTarget() != null){
            LivingEntity target = livingEntityPatch.getTarget();
            return switch (side) {
                case LEFT,RIGHT ->
                        isInSideAngle(livingEntityPatch.getOriginal(), target, side, degreeFirst, degreeSecond);
                case ROUND ->
                        isInWrappedAngle(livingEntityPatch.getOriginal(), target, degreeFirst, degreeSecond);
            };
        }
        return false;
    }

    @Override
    public List<ParameterEditor> getAcceptingParameters(Screen screen){
        return null;
    }

    /**
     * 单侧检查（LEFT / RIGHT + 0~180）
     */
    private static boolean isInSideAngle(LivingEntity observer, LivingEntity target, TargetSide side, double start, double end) {
        if (side == null) return false;
        if (start < 0 || end > 180 || start > end) return false;

        double angle = getSignedAngle(observer, target);
        double abs = Math.abs(angle);

        return switch (side) {
            case LEFT  -> angle < 0 && abs >= start && abs <= end;
            case RIGHT -> angle > 0 && abs >= start && abs <= end;
            default -> false;
        };
    }

    /**
     * 跨 360° 扇形检查
     * 示例：330 ~ 30
     */
    private static boolean isInWrappedAngle(LivingEntity observer, LivingEntity target, double start, double end) {
        double angle = getWrappedAngle(observer, target);

        start = (start % 360 + 360) % 360;
        end   = (end   % 360 + 360) % 360;

        if (start <= end) {
            return angle >= start && angle <= end;
        }
        else {
            return angle >= start || angle <= end;
        }
    }


    /**
     * 相对角度（-180 ~ +180）
     * 左负右正，0 为正前，±180 为正后
     */
    private static double getSignedAngle(LivingEntity observer, LivingEntity target) {
        Vec3 forward = observer.getLookAngle();
        forward = new Vec3(forward.x, 0, forward.z);

        Vec3 toTarget = target.position().subtract(observer.position());
        toTarget = new Vec3(toTarget.x, 0, toTarget.z);

        if (forward.lengthSqr() < EPS || toTarget.lengthSqr() < EPS) {
            return 0;
        }

        forward = forward.normalize();
        toTarget = toTarget.normalize();

        double dot = Mth.clamp(forward.dot(toTarget), -1.0, 1.0);
        double angle = Math.toDegrees(Math.acos(dot)); // 0~180

        double crossY = forward.cross(toTarget).y;
        return crossY < 0 ? angle : -angle;
    }

    /**
     * 绝对角度（0 ~ 360）
     * 0 = 正前方，顺时针
     */
    private static double getWrappedAngle(LivingEntity observer, LivingEntity target) {
        double signed = getSignedAngle(observer, target);
        return (signed + 360) % 360;
    }

    public enum TargetSide{
        ROUND,
        LEFT,
        RIGHT;
    }

}
