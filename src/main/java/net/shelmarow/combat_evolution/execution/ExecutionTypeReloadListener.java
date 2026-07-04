package net.shelmarow.combat_evolution.execution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;

import java.util.Map;

public class ExecutionTypeReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "ce_execution/type";
    private static final Gson GSON = new GsonBuilder().create();

    public ExecutionTypeReloadListener() {
        super(GSON, DIRECTORY);
    }


    protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profileIn) {
        ExecutionTypeManager.clearDatapackExecutionType();
        return super.prepare(resourceManager, profileIn);
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> jsonMap, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            ResourceLocation rl = entry.getKey();
            String pathString = rl.getPath();
            ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), pathString);

            JsonObject json = entry.getValue().getAsJsonObject();
            String executionAttackAnimPath = json.get("executionAttackAnimation").getAsString();
            String executionHitAnimPath = json.get("executionHitAnimation").getAsString();

            ResourceLocation attackAnimId = ResourceLocation.parse(executionAttackAnimPath);
            ResourceLocation hitAnimId = ResourceLocation.parse(executionHitAnimPath);

            AnimationManager.AnimationAccessor<StaticAnimation> attackAnim = AnimationManager.byKey(attackAnimId);
            AnimationManager.AnimationAccessor<StaticAnimation> hitAnim = AnimationManager.byKey(hitAnimId);

            Vec3 posOffset = Vec3.ZERO;
            if (json.has("posOffset")) {
                JsonObject offsetJson = json.getAsJsonObject("posOffset");
                double x = offsetJson.has("x") ? offsetJson.get("x").getAsDouble() : 0.0;
                double y = offsetJson.has("y") ? offsetJson.get("y").getAsDouble() : 0.0;
                double z = offsetJson.has("z") ? offsetJson.get("z").getAsDouble() : 0.0;
                posOffset = new Vec3(x, y, z);
            }

            float rotationOffset = json.has("rotationOffset") ? json.get("rotationOffset").getAsFloat() : 0.0f;
            int totalTick = json.has("totalTick") ? json.get("totalTick").getAsInt() : 100;

            // 注册处决类型
            ExecutionTypeManager.creatDatapackExecutionType(registryName, attackAnim, hitAnim, posOffset, rotationOffset, totalTick);
        }
    }


}
