package net.shelmarow.combat_evolution.ai;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import net.shelmarow.combat_evolution.ai.event.*;
import net.shelmarow.combat_evolution.ai.params.AnimationParams;
import net.shelmarow.combat_evolution.ai.params.PhaseParams;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.EpicFightConditions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.capabilities.provider.EntityPatchProvider;
import yesman.epicfight.world.damagesource.StunType;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CEPatchReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "ce_mobpatch";
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<EntityType<?>, MobPatchReloadListener.AbstractMobPatchProvider> MOB_PATCH_PROVIDERS = Maps.newHashMap();

    public CEPatchReloadListener() {
        super(GSON, DIRECTORY);
    }

    protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profileIn) {
        MOB_PATCH_PROVIDERS.clear();
        return super.prepare(resourceManager, profileIn);
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> jsonMap, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            ResourceLocation rl = entry.getKey();
            String pathString = rl.getPath();

            ResourceLocation registryName = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), pathString);
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(registryName)) {
                EpicFightMod.LOGGER.warn("Mob Patch Exception: No Entity named {}", registryName);
                continue;
            }
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(registryName);

            //json文件内容
            JsonObject json = entry.getValue().getAsJsonObject();

            CEDatapackMobPatchProvider provider = new CEDatapackMobPatchProvider();
            initProvider(provider, json);

            MOB_PATCH_PROVIDERS.put(entityType, provider);
            EntityPatchProvider.putCustomEntityPatch(entityType,(entity) -> () -> MOB_PATCH_PROVIDERS.get(entity.getType()).get(entity));


            if (EpicFightSharedConstants.isPhysicalClient()) {
                CompoundTag tag = new CompoundTag();
                if(json.has("renderer")){
                    tag.putString("renderer", json.get("renderer").getAsString());
                }
                if (json.has("model")) {
                    tag.putString("model", json.get("model").getAsString());
                }

                if (json.has("humanoid")) {
                    tag.putBoolean("humanoid", json.get("humanoid").getAsBoolean());
                }
                ClientEngine.getInstance().renderEngine.registerCustomEntityRenderer(entityType, tag.getString("renderer"), tag);
            }
        }
    }

    private void initProvider(CEDatapackMobPatchProvider provider, JsonObject json) {

        provider.weaponLivingMotions = getWeaponLivingMotions(json);
        provider.weaponAttackMotions = getWeaponAttackMotions(json);
        provider.guardHitMotions = getGuardHitMotions(json);
        provider.stunAnimations = getStunAnimations(json);
        provider.attributeMap = getAttributeMap(json);
        provider.faction = getFaction(json);
        provider.scale = getScale(json);
        provider.breakTime = getBreakTime(json);
        provider.recoverTime = getRecoverTime(json);
        provider.staminaRegenDelay = getStaminaRegenDelay(json);
        provider.hurtImpact = getHurtImpact(json);
        provider.guardHitImpact = getGuardHitImpact(json);
        provider.beParriedDamage = getBeParriedDamage(json);
    }

    private float getBeParriedDamage(JsonObject json) {
        if(json.has("beParriedDamage")){
            return json.get("beParriedDamage").getAsFloat();
        }
        return 1;
    }

    private float getGuardHitImpact(JsonObject json) {
        if (json.has("guardHitImpact")) {
            return json.get("guardHitImpact").getAsFloat();
        }
        return 1;
    }

    private float getHurtImpact(JsonObject json) {
        if (json.has("hurtImpact")) {
            return json.get("hurtImpact").getAsFloat();
        }
        return 0.35F;
    }

    private int getStaminaRegenDelay(JsonObject json) {
        if (json.has("staminaRegenDelay")) {
            return json.get("staminaRegenDelay").getAsInt();
        }
        return 60;
    }

    private int getRecoverTime(JsonObject json) {
        if (json.has("recoverTime")) {
            return json.get("recoverTime").getAsInt();
        }
        return 60;
    }

    private int getBreakTime(JsonObject json) {
        if(json.has("breakTime")) {
            return json.get("breakTime").getAsInt();
        }
        return 40;
    }

    private float getScale(JsonObject json) {
        if(json.has("scale")) {
            return json.get("scale").getAsFloat();
        }
        return 1;
    }

    private Factions getFaction(JsonObject json) {
        if(json.has("faction")) {
            try{
                return Factions.valueOf(json.get("faction").getAsString().toUpperCase());
            }
            catch(IllegalArgumentException e){
                throw new JsonSyntaxException("Invalid faction");
            }
        }
        return Factions.NEUTRAL;
    }

    private Map<Attribute, Double> getAttributeMap(JsonObject json) {
        Map<Attribute, Double> attributes = new HashMap<>();
        if (json.has("attributes")){
            JsonArray array = json.getAsJsonArray("attributes");
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                ResourceLocation id = ResourceLocation.parse(obj.get("attribute").getAsString());
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getDelegateOrThrow(id).get();
                double value = obj.get("value").getAsDouble();
                attributes.put(attribute, value);
            }
        }
        return attributes;
    }

    private Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> getStunAnimations(JsonObject json) {
        Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> animations = new HashMap<>();
        if(json.has("stunAnimations")){
            JsonObject stunObj = json.getAsJsonObject("stunAnimations");
            for (StunType stunType : StunType.values()) {
                String name = stunType.name().toLowerCase();
                if(stunObj.has(name)){
                    String stunAnimationName = stunObj.get(name).getAsString();
                    animations.put(stunType, AnimationManager.byKey(stunAnimationName));
                }
            }
        }

        return animations;
    }

    private Map<WeaponCategory, Map<Style, List<AnimationManager.AnimationAccessor<? extends StaticAnimation>>>> getGuardHitMotions(JsonObject json) {
        Map<WeaponCategory, Map<Style, List<AnimationManager.AnimationAccessor<? extends StaticAnimation>>>> guardHitMotions = new HashMap<>();

        if(json.has("guardHitAnimation")){
            JsonArray array = json.getAsJsonArray("guardHitAnimation");
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();

                Style style = Style.ENUM_MANAGER.getOrThrow(obj.get("style").getAsString());
                List<AnimationManager.AnimationAccessor<? extends StaticAnimation>> animations = new ArrayList<>();

                JsonArray animationJson = obj.get("animations").getAsJsonArray();
                for (JsonElement animationJsonElement : animationJson) {
                    animations.add(AnimationManager.byKey(animationJsonElement.getAsString()));
                }

                JsonArray categoriesJson = obj.get("weaponCategories").getAsJsonArray();
                for (JsonElement categoryJson : categoriesJson) {
                    String categoryName = categoryJson.getAsString();
                    guardHitMotions.computeIfAbsent(WeaponCategory.ENUM_MANAGER.getOrThrow(categoryName), k -> new HashMap<>()).put(style, animations);
                }
            }
        }

        return guardHitMotions;
    }

    private Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>>>> getWeaponLivingMotions(JsonObject json) {
        Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>>>> weaponLivingMotions = new HashMap<>();

        if(json.has("weaponLivingMotions")){
            JsonArray array = json.getAsJsonArray("weaponLivingMotions");
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();

                Style style = Style.ENUM_MANAGER.getOrThrow(obj.get("style").getAsString());
                Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>> motions = new HashSet<>();

                JsonObject animationJson = obj.get("livingMotions").getAsJsonObject();
                for (LivingMotion motion : LivingMotions.values()) {
                    String motionName = motion.toString().toLowerCase();
                    if(animationJson.has(motionName)){
                        motions.add(Pair.of(motion, AnimationManager.byKey(animationJson.get(motionName).getAsString())));
                    }
                }

                JsonArray categoriesJson = obj.get("weaponCategories").getAsJsonArray();
                for (JsonElement categoryJson : categoriesJson) {
                    String categoryName = categoryJson.getAsString();
                    weaponLivingMotions.computeIfAbsent(WeaponCategory.ENUM_MANAGER.getOrThrow(categoryName), k -> new HashMap<>()).put(style, motions);
                }
            }
        }

        return weaponLivingMotions;
    }

    private Map<WeaponCategory, Map<Style, CECombatBehaviors.Builder<MobPatch<?>>>> getWeaponAttackMotions(JsonObject json) {
        Map<WeaponCategory, Map<Style, CECombatBehaviors.Builder<MobPatch<?>>>> weaponAttackMotions = new HashMap<>();


        //战斗集
        if (json.has("combatBehaviors")) {
            JsonArray combatBehaviors = json.getAsJsonArray("combatBehaviors");

            //遍历所有武器行为
            for (JsonElement combatBehavior : combatBehaviors) {
                JsonObject combatJson = combatBehavior.getAsJsonObject();

                Set<WeaponCategory> weaponCategories = new HashSet<>();
                Style style = CapabilityItem.Styles.COMMON;
                CECombatBehaviors.Builder<MobPatch<?>> behaviorBuilder = CECombatBehaviors.builder();

                //全局事件
                if(combatJson.has("stunEvents")){
                    JsonObject stunObj = combatJson.getAsJsonObject("stunEvents");
                    for (StunType stunType : StunType.values()) {
                        String name = stunType.name().toLowerCase();
                        if(stunObj.has(name)){
                            JsonArray array = stunObj.getAsJsonArray(name);
                            for (JsonElement element : array) {
                                JsonObject obj = element.getAsJsonObject();
                                String command =  obj.get("command").getAsString();
                                boolean onTarget = obj.get("onTarget").getAsBoolean();
                                behaviorBuilder.addStunEvent(stunType, creatCommandConsumer(onTarget, command));
                            }
                        }
                    }
                }

                //获取该行为对应的武器种类和风格
                if (combatJson.has("weaponCategories")) {
                    JsonArray jsonArray = combatJson.getAsJsonArray("weaponCategories");
                    for (JsonElement categoryId : jsonArray) {
                        weaponCategories.add(WeaponCategory.ENUM_MANAGER.getOrThrow(categoryId.getAsString()));
                    }
                }
                //获取风格
                if (combatJson.has("style")) {
                    style = Style.ENUM_MANAGER.getOrThrow(combatJson.get("style").getAsString().toUpperCase());
                }

                //递归获取所有行为集
                if (combatJson.has("behaviorRoots")) {
                    JsonArray behaviorRoots = combatJson.getAsJsonArray("behaviorRoots");
                    for (JsonElement behaviorRoot : behaviorRoots) {
                        JsonObject root = behaviorRoot.getAsJsonObject();
                        if(root.has("isGlobal")){
                            boolean global = root.get("isGlobal").getAsBoolean();
                            if(global){
                                behaviorBuilder.newGlobalBehavior(getBehaviorRootBuilder(root));
                                continue;
                            }
                        }
                        behaviorBuilder.newBehaviorRoot(getBehaviorRootBuilder(root));
                    }
                }

                for (WeaponCategory category : weaponCategories){
                    weaponAttackMotions.computeIfAbsent(category, k -> new HashMap<>()).put(style, behaviorBuilder);
                }
            }
        }

        return weaponAttackMotions;
    }

    public CECombatBehaviors.BehaviorRoot.Builder<MobPatch<?>> getBehaviorRootBuilder(JsonObject json) {

        CECombatBehaviors.BehaviorRoot.Builder<MobPatch<?>> builder = new CECombatBehaviors.BehaviorRoot.Builder<>();

        if (json.has("rootName")) {
            builder.rootName(json.get("rootName").getAsString());
        }
        if (json.has("priority")) {
            builder.priority(json.get("priority").getAsDouble());
        }
        if (json.has("weight")) {
            builder.weight(json.get("weight").getAsDouble());
        }
        if (json.has("maxCooldown")) {
            builder.maxCooldown(json.get("maxCooldown").getAsInt());
        }
        if (json.has("cooldown")) {
            builder.cooldown(json.get("cooldown").getAsInt());
        }
        if (json.has("isGlobal")) {
            builder.isGlobal(json.get("isGlobal").getAsBoolean());
        }
        if (json.has("backAfterFinished")) {
            builder.backAfterFinished(json.get("backAfterFinished").getAsBoolean());
        }
        if(json.has("firstBehaviors")){
            JsonArray firstBehaviors = json.getAsJsonArray("firstBehaviors");
            for (JsonElement firstBehavior : firstBehaviors) {
                JsonObject firstBehaviorJson = firstBehavior.getAsJsonObject();
                builder.addFirstBehavior(getBehaviorBuilder(firstBehaviorJson));
            }
        }

        return builder;
    }

    private CECombatBehaviors.Behavior.Builder<MobPatch<?>> getBehaviorBuilder(JsonObject behaviors) {
        CECombatBehaviors.Behavior.Builder<MobPatch<?>> builder = CECombatBehaviors.Behavior.builder();

        if(behaviors.has("behaviorName")){
            builder.name(behaviors.get("behaviorName").getAsString());
        }

        if (behaviors.has("priority")) {
            builder.priority(behaviors.get("priority").getAsDouble());
        }

        if (behaviors.has("weight")) {
            builder.weight(behaviors.get("weight").getAsDouble());
        }

        if (behaviors.has( "stopByStun")) {
            builder.stopByStun(behaviors.get("stopByStun").getAsInt());
        }

        if (behaviors.has("interruptType")) {
            CECombatBehaviors.InterruptType type;
            List<Float> interruptedWindow = new ArrayList<>();
            try{
                type = CECombatBehaviors.InterruptType.valueOf(behaviors.get("interruptType").getAsString().toUpperCase());
            }
            catch(IllegalArgumentException e){
                throw new RuntimeException("Invalid interruptType");
            }

            if(behaviors.has("interruptedWindow")) {
                JsonArray array = behaviors.getAsJsonArray("interruptedWindow");
                if(!array.isEmpty()) {
                    for (JsonElement element : array) {
                        interruptedWindow.add(element.getAsFloat());
                    }
                }
                else {
                    throw new RuntimeException("No interruptedWindow");
                }
            }
            else {
                throw new RuntimeException("No interruptedWindow");
            }

            switch(type) {
                case TIME->{
                    builder.interruptedByTime(interruptedWindow.get(0), interruptedWindow.get(1));
                }
                case LEVEL -> {
                    builder.interruptedByLevel(interruptedWindow.stream().map(Float::intValue).toArray(Integer[]::new));
                }
            }
        }

        if(behaviors.has("canInterruptParent")){
            builder.canInterruptParent(behaviors.get("canInterruptParent").getAsBoolean());
        }

        if(behaviors.has("canInsertGlobalBehavior")){
            boolean canInsertGlobalBehavior = behaviors.get("canInsertGlobalBehavior").getAsBoolean();
            List<String> names = new ArrayList<>();
            if(canInsertGlobalBehavior){
                if(behaviors.has("allowedGlobalNameList")){
                    JsonArray array = behaviors.getAsJsonArray("allowedGlobalNameList");
                    for(JsonElement element : array){
                        names.add(element.getAsString());
                    }
                }
            }
            builder.canInsertGlobalBehavior(canInsertGlobalBehavior, names.toArray(String[]::new));
        }

        if(behaviors.has("addCoolDown")){
            builder.addCooldown(behaviors.get("addCoolDown").getAsInt());
        }

        if(behaviors.has("setCoolDown")){
            builder.setCooldown(behaviors.get("setCoolDown").getAsInt());
        }

        if(behaviors.has("waitTime")){
            builder.waitTime(behaviors.get("waitTime").getAsInt());
        }

        if(behaviors.has("addStamina")){
            builder.addStamina(behaviors.get("addStamina").getAsFloat());
        }

        if(behaviors.has("setStamina")){
            builder.setStamina(behaviors.get("setStamina").getAsFloat());
        }

        if(behaviors.has("setPhase")){
            builder.setPhase(behaviors.get("setPhase").getAsInt());
        }

        if(behaviors.has("addPhase")){
            builder.addPhase(behaviors.get("addPhase").getAsInt());
        }

        if(behaviors.has("conditions")){
            JsonArray array = behaviors.getAsJsonArray("conditions");
            for(JsonElement element : array){
                JsonObject conditionJson = element.getAsJsonObject();
                builder.condition(deserializeBehaviorCondition(conditionJson));
            }
        }

        if(behaviors.has("animation")){
            String path = behaviors.get("animation").getAsString();
            AnimationManager.AnimationAccessor<? extends StaticAnimation> animation = AnimationManager.byKey(path);
            AnimationParams animationParams = new AnimationParams();

            if(behaviors.has("animationParams")) {
                animationParams = getAnimationParams(behaviors.get("animationParams").getAsJsonObject());
            }

            builder.animationBehavior(animation, animationParams);

            //动画专用事件
            if(behaviors.has("timeEvents")){
                JsonArray array = behaviors.getAsJsonArray("timeEvents");
                for(JsonElement element : array){
                    JsonObject timeEventJson = element.getAsJsonObject();
                    try{
                        TimeEvent.EventType eventType = TimeEvent.EventType.valueOf(timeEventJson.get("type").getAsString().toUpperCase());
                        boolean onTarget = timeEventJson.get("onTarget").getAsBoolean();
                        String command = timeEventJson.get("command").getAsString();
                        switch(eventType){
                            case TICK -> {
                                builder.addTimeEvent(new TimeEvent(creatCommandConsumer(onTarget, command)));
                            }
                            case IN_TIME -> {
                                float timeStart = timeEventJson.get("timeStart").getAsFloat();
                                builder.addTimeEvent(new TimeEvent(timeStart, creatCommandConsumer(onTarget, command)));
                            }
                            case BETWEEN_TIMES -> {
                                float timeStart = timeEventJson.get("timeStart").getAsFloat();
                                float timeEnd = timeEventJson.get("timeEnd").getAsFloat();
                                builder.addTimeEvent(new TimeEvent(timeStart, timeEnd, creatCommandConsumer(onTarget, command)));
                            }
                        }
                    } catch (IllegalArgumentException e){
                        throw new RuntimeException("Invalid time event");
                    }
                }
            }

            if(behaviors.has("hitEvents")) {
                JsonArray array = behaviors.getAsJsonArray("hitEvents");
                for(JsonElement element : array){
                    JsonObject hitEventJson = element.getAsJsonObject();
                    try{
                        AttackResult.ResultType resultType = AttackResult.ResultType.valueOf(hitEventJson.get("type").getAsString().toUpperCase());
                        int phase = hitEventJson.get("phase").getAsInt();
                        boolean onTarget = hitEventJson.get("onTarget").getAsBoolean();
                        String command = hitEventJson.get("command").getAsString();
                        builder.addHitEvent(new HitEvent(phase, resultType, creatCommandConsumer2(onTarget, command)));
                    } catch (IllegalArgumentException e){
                        throw new RuntimeException("Invalid hitEvent");
                    }
                }
            }

            if(behaviors.has("blockedEvents")){
                JsonArray array = behaviors.getAsJsonArray("blockedEvents");
                for(JsonElement element : array){
                    JsonObject blockedEventJson = element.getAsJsonObject();
                    int phase = blockedEventJson.get("phase").getAsInt();
                    boolean parried = blockedEventJson.get("parried").getAsBoolean();
                    boolean onTarget = blockedEventJson.get("onTarget").getAsBoolean();
                    String command = blockedEventJson.get("command").getAsString();
                    builder.addBlockedEvent(new BlockedEvent(phase, parried, creatCommandConsumer3(onTarget, command)));
                }
            }
        }
        else{
            AnimationManager.AnimationAccessor<? extends StaticAnimation> counter = Animations.SWEEPING_EDGE;
            AnimationParams animationParams = new AnimationParams();
            if (behaviors.has("counterAnimation")) {
                String path = behaviors.get("counterAnimation").getAsString();
                counter = AnimationManager.byKey(path);
                if(behaviors.has("animationParams")) {
                    animationParams = getAnimationParams(behaviors.get("animationParams").getAsJsonObject());
                }
            }
            builder.counterAnimation(counter, animationParams);

            if (behaviors.has("resetGuardTime")){
                builder.resetGuardTime(behaviors.get("resetGuardTime").getAsBoolean());
            }

            if(behaviors.has("counterType")){
                try{
                    CECombatBehaviors.CounterType counterType = CECombatBehaviors.CounterType.valueOf(behaviors.get("counterType").getAsString().toUpperCase());
                    builder.counterType(counterType);
                } catch (IllegalArgumentException e){
                    throw new RuntimeException("Invalid counterType");
                }
            }

            if(behaviors.has("counterChance")){
                builder.counterChance(behaviors.get("counterChance").getAsFloat());
            }

            if(behaviors.has("maxGuardHit")){
                builder.maxGuardHit(behaviors.get("maxGuardHit").getAsInt());
            }

            if(behaviors.has("beforeCounter")){
                JsonObject beforeCounter = behaviors.getAsJsonObject("beforeCounter");
                boolean cancelHitAnimation = beforeCounter.get("cancelHitAnimation").getAsBoolean();
                boolean onTarget = beforeCounter.get("onTarget").getAsBoolean();
                String command = beforeCounter.get("command").getAsString();

                builder.setBeforeCounterEvent(new BeforeCounterEvent(creatFunctionCommand(cancelHitAnimation, onTarget, command)));
            }


            float forward = 0;
            float strafe = 0;
            if(behaviors.has("forward")){
                forward = behaviors.get("forward").getAsFloat();
            }
            if(behaviors.has("strafe")){
                strafe = behaviors.get("strafe").getAsFloat();
            }

            if(behaviors.has("guardWithWander")){
                int guardWithWander = behaviors.get("guardWithWander").getAsInt();
                boolean guardAnimation = false;
                if(behaviors.has("guardAnimation")){
                    guardAnimation = behaviors.get("guardAnimation").getAsBoolean();
                }
                builder.guardWithWander(guardWithWander, forward, strafe, guardAnimation);
            }
            if(behaviors.has("guardTime")){
                int guardTime = behaviors.get("guardTime").getAsInt();
                builder.guard(guardTime);
            }


            if(behaviors.has("wanderTime")){
                int wanderTime =  behaviors.get("wanderTime").getAsInt();
                builder.wander(wanderTime, forward, strafe);
            }

        }

        //通用事件
        if (behaviors.has("exBehavior")){
            JsonArray array = behaviors.getAsJsonArray("exBehavior");
            for (JsonElement element : array) {
                JsonObject exBehaviorJson = element.getAsJsonObject();
                boolean onTarget = exBehaviorJson.has("onTarget") && exBehaviorJson.get("onTarget").getAsBoolean();
                String command = exBehaviorJson.get("command").getAsString();

                builder.addExBehavior(creatCommandConsumer(onTarget, command));
            }
        }

        if(behaviors.has("onHurtEvent")){
            JsonObject hurtEventJson = behaviors.getAsJsonObject("onHurtEvent");
            try{
                String returnResult = hurtEventJson.get("returnResult").getAsString().toUpperCase();
                AttackResult.ResultType resultType = null;
                if(!returnResult.equals("DEFAULT")){
                    resultType = AttackResult.ResultType.valueOf(returnResult);
                }
                float damage = hurtEventJson.get("damage").getAsFloat();
                boolean onTarget = hurtEventJson.get("onTarget").getAsBoolean();
                String command = hurtEventJson.get("command").getAsString();

                builder.setOnHurtEvent(new OnHurtEvent(creatTriFunctionCommand(resultType, damage, onTarget, command)));
            } catch (IllegalArgumentException e){
                throw new RuntimeException("Invalid onHurtEvent");
            }
        }

        //子行为
        if(behaviors.has("nextBehaviors")){
            JsonArray nextBehaviorJson = behaviors.getAsJsonArray("nextBehaviors");
            for (JsonElement element : nextBehaviorJson) {
                JsonObject nextBehaviorJsonObject = element.getAsJsonObject();
                builder.addNextBehavior(getBehaviorBuilder(nextBehaviorJsonObject));
            }
        }

        return builder;
    }

    private Function<MobPatch<?>, Boolean> creatFunctionCommand(boolean cancelHitAnimation, boolean onTarget, String command) {
        return mobPatch -> {
            if (!mobPatch.isLogicalClient()){
                LivingEntity target = mobPatch.getTarget();
                LivingEntity original = mobPatch.getOriginal();
                LivingEntity executor = onTarget ? target : original;

                if (executor != null) {
                    MinecraftServer server = executor.getServer();
                    if (server != null) {
                        CommandSourceStack source =
                                executor.createCommandSourceStack()
                                        .withPermission(2)
                                        .withSuppressedOutput();

                        server.getCommands().performPrefixedCommand(source, command);
                    }
                }
            }
            return cancelHitAnimation;
        };
    }

    private @NotNull TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> creatTriFunctionCommand(AttackResult.ResultType resultType, float damage, boolean onTarget, String command) {
        return (mobPatch, damageSource, attackResult) -> {
            if (!mobPatch.isLogicalClient()){
                LivingEntity target = mobPatch.getTarget();
                LivingEntity original = mobPatch.getOriginal();
                LivingEntity executor = onTarget ? target : original;

                if (executor != null) {
                    MinecraftServer server = executor.getServer();
                    if (server != null) {
                        CommandSourceStack source =
                                executor.createCommandSourceStack()
                                        .withPermission(2)
                                        .withSuppressedOutput();

                        server.getCommands().performPrefixedCommand(source, command);
                    }
                }
            }

            if(resultType != null){
                return new AttackResult(resultType, damage == -1 ? attackResult.damage : damage);
            }
            return attackResult;
        };
    }

    private static @NotNull Consumer<MobPatch<?>> creatCommandConsumer(boolean onTarget, String command) {
        return mobPatch -> {
            if (mobPatch.isLogicalClient()) return;

            LivingEntity target = mobPatch.getTarget();
            LivingEntity original = mobPatch.getOriginal();
            LivingEntity executor = onTarget ? target : original;

            if (executor != null) {
                MinecraftServer server = executor.getServer();
                if (server != null) {
                    CommandSourceStack source =
                            executor.createCommandSourceStack()
                                    .withPermission(2)
                                    .withSuppressedOutput();

                    server.getCommands().performPrefixedCommand(source, command);
                }
            }

        };
    }

    private static @NotNull BiConsumer<MobPatch<?>, Entity> creatCommandConsumer2(boolean onTarget, String command) {
        return (mobPatch, target) -> {
            if (mobPatch.isLogicalClient()) return;

            LivingEntity original = mobPatch.getOriginal();
            LivingEntity executor = onTarget ? (target instanceof LivingEntity livingTarget ? livingTarget : null) : original;

            if (executor != null) {
                MinecraftServer server = executor.getServer();
                if (server != null) {
                    CommandSourceStack source =
                            executor.createCommandSourceStack()
                                    .withPermission(2)
                                    .withSuppressedOutput();

                    server.getCommands().performPrefixedCommand(source, command);
                }
            }

        };
    }

    private static @NotNull BiConsumer<MobPatch<?>, LivingEntityPatch<?>> creatCommandConsumer3(boolean onTarget, String command) {
        return (mobPatch, targetPatch) -> {
            if (mobPatch.isLogicalClient()) return;

            LivingEntity original = mobPatch.getOriginal();
            LivingEntity executor = onTarget ? targetPatch.getOriginal() : original;

            if (executor != null) {
                MinecraftServer server = executor.getServer();
                if (server != null) {
                    CommandSourceStack source =
                            executor.createCommandSourceStack()
                                    .withPermission(2)
                                    .withSuppressedOutput();

                    server.getCommands().performPrefixedCommand(source, command);
                }
            }

        };
    }

    private static AnimationParams getAnimationParams(JsonObject jsonParams) {
        AnimationParams animationParams = new AnimationParams();

        if(jsonParams.has("transitionTime")){
            animationParams.transitionTime(jsonParams.get("transitionTime").getAsFloat());
        }
        if(jsonParams.has("playSpeed")){
            animationParams.playSpeed(jsonParams.get("playSpeed").getAsFloat());
        }
        if (jsonParams.has("phaseParams")){
            JsonArray array = jsonParams.getAsJsonArray("phaseParams");
            for(JsonElement phaseElement : array){
                JsonObject phaseJson = phaseElement.getAsJsonObject();
                int phaseIndex = -1;
                PhaseParams phaseParams = new PhaseParams();
                if (phaseJson.has("phase")){
                    phaseIndex = phaseJson.get("phase").getAsInt();
                }
                if(phaseJson.has("damageMultiplier")){
                    phaseParams.damageMultiplier(phaseJson.get("damageMultiplier").getAsFloat());
                }
                if(phaseJson.has("impactMultiplier")){
                    phaseParams.impactMultiplier(phaseJson.get("impactMultiplier").getAsFloat());
                }
                if(phaseJson.has("armorNegationMultiplier")){
                    phaseParams.armorNegationMultiplier(phaseJson.get("armorNegationMultiplier").getAsFloat());
                }
                if(phaseJson.has("stunType")){
                    String stunName = phaseJson.get("stunType").getAsString().toUpperCase();
                    if(!stunName.equals("DEFAULT")){
                        try {
                            StunType stun = StunType.valueOf(stunName);
                            phaseParams.stunType(stun);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Invalid stunType");
                        }
                    }
                }
                if(phaseJson.has("damage_tags")){
                    JsonArray tags =  phaseJson.getAsJsonArray("damage_tags");
                    Set<TagKey<DamageType>> damageTags = new HashSet<>();
                    for (JsonElement tagElement : tags) {
                        ResourceLocation id = ResourceLocation.parse(tagElement.getAsString());
                        try{
                            TagKey<DamageType> tag = TagKey.create(Registries.DAMAGE_TYPE, id);
                            damageTags.add(tag);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Invalid damageType");
                        }
                    }
                    phaseParams.damageSource(damageTags);
                }

                animationParams.addPhase(phaseIndex, phaseParams);
            }
        }
        return animationParams;
    }

    public static <T extends MobPatch<?>> Condition<T> deserializeBehaviorCondition(JsonObject conditionJson) {
        try {
            String type = conditionJson.get("type").getAsString();

            ResourceLocation resourceLocation;
            if (type.contains(":")) {
                resourceLocation = ResourceLocation.parse(type);
            } else {
                resourceLocation = EpicFightMod.identifier(type);
            }

            Supplier<Condition<T>> provider = EpicFightConditions.getConditionOrNull(resourceLocation);

            if (provider == null) {
                throw new IllegalArgumentException("Unknown condition: " + resourceLocation);
            }

            Condition<T> condition = provider.get();
            CompoundTag tag = TagParser.parseTag(conditionJson.toString());
            condition.read(tag);
            return condition;

        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Failed to parse condition: " + conditionJson, e);
        }
    }

    public static class CEDatapackMobPatchProvider extends MobPatchReloadListener.AbstractMobPatchProvider {

        public Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>>>> weaponLivingMotions = new HashMap<>();
        public Map<WeaponCategory, Map<Style, List<AnimationManager.AnimationAccessor<? extends StaticAnimation>>>> guardHitMotions = new HashMap<>();
        public Map<WeaponCategory, Map<Style, CECombatBehaviors.Builder<MobPatch<?>>>> weaponAttackMotions = new HashMap<>();
        public Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> stunAnimations = new HashMap<>();
        public Map<Attribute, Double> attributeMap = new HashMap<>();
        public Factions faction;
        public int breakTime = 40;
        public int recoverTime = 60;
        public int staminaRegenDelay = 60;
        public float scale = 1;
        public float guardHitImpact = 1F;
        public float hurtImpact = 0.35F;
        public float beParriedDamage = 1F;


        @Override
        public EntityPatch<?> get(Entity entity) {
            return new CEDatapackMobPatch(this);
        }

    }
}
