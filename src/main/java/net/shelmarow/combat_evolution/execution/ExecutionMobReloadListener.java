package net.shelmarow.combat_evolution.execution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ExecutionMobReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "ce_execution/mob";
    private static final Gson GSON = new GsonBuilder().create();

    public ExecutionMobReloadListener() {
        super(GSON, DIRECTORY);
    }

    protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profileIn) {
        ExecutionTypeManager.clearDatapackExecutionMob();
        return super.prepare(resourceManager, profileIn);
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> jsonMap, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            ResourceLocation rl = entry.getKey();
            String pathString = rl.getPath();
            ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), pathString);

            JsonObject json = entry.getValue().getAsJsonObject();
            String type = json.get("type").getAsString();
            ResourceLocation location = ResourceLocation.tryParse(type);

            ExecutionTypeManager.registerByDatapackEntity(registryName, ExecutionTypeManager.getExecutionType(location));
        }
    }


}
