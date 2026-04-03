package net.shelmarow.combat_evolution.ai;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.shelmarow.combat_evolution.ai.event.*;
import net.shelmarow.combat_evolution.ai.network.SPCEDataPacket;
import net.shelmarow.combat_evolution.ai.params.AnimationParams;
import net.shelmarow.combat_evolution.ai.params.PhaseParams;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.EpicFightConditions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.model.armature.HumanoidArmature;
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
    private static final Map<EntityType<?>, CompoundTag> TAGMAP = Maps.newHashMap();
    private static final Map<EntityType<?>, MobPatchReloadListener.AbstractMobPatchProvider> MOB_PATCH_PROVIDERS = Maps.newHashMap();

    public CEPatchReloadListener() {
        super(GSON, DIRECTORY);
    }


    protected @NotNull Map<ResourceLocation, JsonElement> prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profileIn) {
        MOB_PATCH_PROVIDERS.clear();
        TAGMAP.clear();
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
            CompoundTag tag;
            try {
                tag = TagParser.parseTag(json.toString());
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }

            tag.putString("id", registryName.toString());


            CEDatapackMobPatchProvider provider = new CEDatapackMobPatchProvider();
            initProvider(provider, tag);

            MOB_PATCH_PROVIDERS.put(entityType, provider);
            EntityPatchProvider.putCustomEntityPatch(entityType,(entity) -> () -> MOB_PATCH_PROVIDERS.get(entity.getType()).get(entity));

            String armatureString = tag.getString("armature");
            boolean isHumanoid = tag.getBoolean("humanoid");
            ResourceLocation armatureLocation = ResourceLocation.parse(armatureString);
            AssetAccessor<? extends Armature> armature = Armatures.getOrCreate(armatureLocation, isHumanoid ? HumanoidArmature::new : Armature::new);
            Armatures.registerEntityTypeArmature(entityType, armature);

            TAGMAP.put(entityType, tag);

            if (EpicFightSharedConstants.isPhysicalClient()) {
                ClientEngine.getInstance().renderEngine.registerCustomEntityRenderer(entityType, tag.getString("renderer"), tag);
            }
        }
    }


    public static int getSize() {
        return TAGMAP.size();
    }

    public static CompoundTag[] getTags() {
        return TAGMAP.values().toArray(CompoundTag[]::new);
    }

    @OnlyIn(Dist.CLIENT)
    public static void processServerPacket(SPCEDataPacket packet) {
        for (CompoundTag tag : packet.getTags()) {

            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(tag.getString("id")));

            CEDatapackMobPatchProvider provider = new CEDatapackMobPatchProvider();
            initProvider(provider, tag);

            MOB_PATCH_PROVIDERS.put(entityType, provider);
            EntityPatchProvider.putCustomEntityPatch(entityType,(entity) -> () -> MOB_PATCH_PROVIDERS.get(entity.getType()).get(entity));

            String armatureString = tag.getString("armature");
            boolean isHumanoid = tag.getBoolean("humanoid");
            ResourceLocation armatureLocation = ResourceLocation.parse(armatureString);
            AssetAccessor<? extends Armature> armature = Armatures.getOrCreate(armatureLocation, isHumanoid ? HumanoidArmature::new : Armature::new);
            Armatures.registerEntityTypeArmature(entityType, armature);

            ClientEngine.getInstance().renderEngine.registerCustomEntityRenderer(entityType, tag.getString("renderer"), tag);
        }
    }

    private static void initProvider(CEDatapackMobPatchProvider provider, CompoundTag tag) {
        provider.weaponLivingMotions = getWeaponLivingMotions(tag);
        provider.weaponAttackMotions = getWeaponAttackMotions(tag);
        provider.guardHitMotions = getGuardHitMotions(tag);
        provider.stunAnimations = getStunAnimations(tag);
        provider.attributeMap = getAttributeMap(tag);
        provider.faction = getFaction(tag);
        provider.chasingSpeed = getChasingSpeed(tag);
        provider.scale = getScale(tag);
        provider.breakTime = getBreakTime(tag);
        provider.recoverTime = getRecoverTime(tag);
        provider.staminaRegenDelay = getStaminaRegenDelay(tag);
        provider.hurtImpact = getHurtImpact(tag);
        provider.guardHitImpact = getGuardHitImpact(tag);
        provider.beParriedDamage = getBeParriedDamage(tag);
        initBossBarSetting(provider, tag);
        initBossBGM(provider, tag);
    }

    private static float getChasingSpeed(CompoundTag tag) {
        if(tag.contains("chasingSpeed")) {
            return tag.getFloat("chasingSpeed");
        }
        return 1.25F;
    }

    public static void initBossBGM(CEDatapackMobPatchProvider provider, CompoundTag tag) {
        if(tag.contains("ceBossMusic")){
            CompoundTag ceBossBGM = tag.getCompound("ceBossMusic");

            provider.playBGM = ceBossBGM.getBoolean("playBGM");
            provider.bgm =  ResourceLocation.parse(ceBossBGM.getString("bgm"));
            provider.bgmLoop =  ceBossBGM.getBoolean("loop");
            provider.bgmDuration = ceBossBGM.getInt("duration");
            provider.bgmVolume = ceBossBGM.getInt("volume");
            provider.bgmFadeIn = ceBossBGM.getInt("fadeIn");
            provider.bgmFadeOut = ceBossBGM.getInt("fadeOut");
        }
    }

    public static void initBossBarSetting(CEDatapackMobPatchProvider provider, CompoundTag tag) {
        if(tag.contains("ceBossBar")){
            CompoundTag bossBar = tag.getCompound("ceBossBar");
            if(bossBar.contains("enableBossBar")) {
                provider.enableBossBar = bossBar.getBoolean("enableBossBar");
            }
            if(bossBar.contains("bossBarName")){
                provider.bossBarName = bossBar.getString("bossBarName");
            }
            if(bossBar.contains("bossBarTextures")) {
                provider.bossBarTexture = ResourceLocation.parse(bossBar.getString("bossBarTextures"));
            }
        }
    }

    public static float getBeParriedDamage(CompoundTag tag) {
        if(tag.contains("beParriedDamage")){
            return tag.getFloat("beParriedDamage");
        }
        return 1;
    }

    public static float getGuardHitImpact(CompoundTag tag) {
        if (tag.contains("guardHitImpact")) {
            return tag.getFloat("guardHitImpact");
        }
        return 1;
    }

    public static float getHurtImpact(CompoundTag tag) {
        if (tag.contains("hurtImpact")) {
            return tag.getFloat("hurtImpact");
        }
        return 0.35F;
    }

    public static int getStaminaRegenDelay(CompoundTag tag) {
        if (tag.contains("staminaRegenDelay")) {
            return tag.getInt("staminaRegenDelay");
        }
        return 60;
    }

    public static int getRecoverTime(CompoundTag tag) {
        if (tag.contains("recoverTime")) {
            return tag.getInt("recoverTime");
        }
        return 60;
    }

    public static int getBreakTime(CompoundTag tag) {
        if(tag.contains("breakTime")) {
            return tag.getInt("breakTime");
        }
        return 40;
    }

    public static float getScale(CompoundTag tag) {
        if(tag.contains("scale")) {
            return tag.getFloat("scale");
        }
        return 1;
    }

    public static Factions getFaction(CompoundTag tag) {
        if(tag.contains("faction")) {
            try{
                return Factions.valueOf(tag.getString("faction").toUpperCase());
            }
            catch(IllegalArgumentException e){
                throw new JsonSyntaxException("Invalid faction");
            }
        }
        return Factions.NEUTRAL;
    }

    public static Map<Attribute, Double> getAttributeMap(CompoundTag tag) {
        Map<Attribute, Double> attributes = new HashMap<>();
        if (tag.contains("attributes")){
            ListTag array = tag.getList("attributes", Tag.TAG_COMPOUND);
            for (int i = 0; i < array.size(); i++){
                CompoundTag attributeTag = array.getCompound(i);
                ResourceLocation id = ResourceLocation.parse(attributeTag.getString("attribute"));
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getDelegateOrThrow(id).get();
                double value = attributeTag.getDouble("value");
                attributes.put(attribute, value);
            }
        }
        return attributes;
    }

    public static Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> getStunAnimations(CompoundTag tag) {
        Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> animations = new HashMap<>();
        if(tag.contains("stunAnimations")){
            CompoundTag stunAnimations = tag.getCompound("stunAnimations");
            for (StunType stunType : StunType.values()) {
                String name = stunType.name().toLowerCase();
                if(stunAnimations.contains(name)){
                    String stunAnimationName = stunAnimations.getString(name);
                    animations.put(stunType, AnimationManager.byKey(stunAnimationName));
                }
            }
        }

        return animations;
    }

    public static Map<WeaponCategory, Map<Style, List<AnimationManager.AnimationAccessor<? extends StaticAnimation>>>> getGuardHitMotions(CompoundTag tag) {
        Map<WeaponCategory, Map<Style, List<AnimationManager.AnimationAccessor<? extends StaticAnimation>>>> guardHitMotions = new HashMap<>();

        if(tag.contains("guardHitAnimation")){
            ListTag array = tag.getList("guardHitAnimation", Tag.TAG_COMPOUND);
            for (int i = 0; i < array.size(); i++){
                CompoundTag animationTag = array.getCompound(i);

                Style style = Style.ENUM_MANAGER.getOrThrow(animationTag.getString("style"));
                List<AnimationManager.AnimationAccessor<? extends StaticAnimation>> animations = new ArrayList<>();

                ListTag animationList = animationTag.getList("animations",Tag.TAG_STRING);
                for (Tag animation : animationList){
                    animations.add(AnimationManager.byKey(animation.getAsString()));
                }

                ListTag categories = animationTag.getList("weaponCategories",Tag.TAG_STRING);
                for (Tag categoryTag : categories) {
                    String categoryName = categoryTag.getAsString();
                    guardHitMotions.computeIfAbsent(WeaponCategory.ENUM_MANAGER.getOrThrow(categoryName), k -> new HashMap<>()).put(style, animations);
                }
            }
        }

        return guardHitMotions;
    }

    public static Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>>>> getWeaponLivingMotions(CompoundTag tag) {
        Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>>>> weaponLivingMotions = new HashMap<>();

        if(tag.contains("weaponLivingMotions")){
            ListTag array = tag.getList("weaponLivingMotions",Tag.TAG_COMPOUND);
            for (int i = 0; i < array.size(); i++){
                CompoundTag obj = array.getCompound(i);

                Style style = Style.ENUM_MANAGER.getOrThrow(obj.getString("style"));
                Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>> motions = new HashSet<>();

                CompoundTag animationCompound = obj.getCompound("livingMotions");
                for (LivingMotion motion : LivingMotions.values()) {
                    String motionName = motion.toString().toLowerCase();
                    if(animationCompound.contains(motionName)){
                        motions.add(Pair.of(motion, AnimationManager.byKey(animationCompound.getString(motionName))));
                    }
                }

                ListTag categoriesTags = obj.getList("weaponCategories",Tag.TAG_STRING);
                for (Tag categoryTag : categoriesTags){
                    String categoryName = categoryTag.getAsString();
                    weaponLivingMotions.computeIfAbsent(WeaponCategory.ENUM_MANAGER.getOrThrow(categoryName), k -> new HashMap<>()).put(style, motions);
                }
            }
        }

        return weaponLivingMotions;
    }

    public static Map<WeaponCategory, Map<Style, Supplier<CECombatBehaviors.Builder<MobPatch<?>>>>> getWeaponAttackMotions(CompoundTag tag) {
        Map<WeaponCategory, Map<Style, Supplier<CECombatBehaviors.Builder<MobPatch<?>>>>> weaponAttackMotions = new HashMap<>();


        //战斗集
        if (tag.contains("combatBehaviors")) {
            ListTag combatBehaviors = tag.getList("combatBehaviors", Tag.TAG_COMPOUND);

            //遍历所有武器行为
            for (int i = 0; i < combatBehaviors.size(); i++) {
                CompoundTag combatTag = combatBehaviors.getCompound(i);

                Set<WeaponCategory> weaponCategories = new HashSet<>();
                Style style = CapabilityItem.Styles.COMMON;

                //获取该行为对应的武器种类和风格
                if (combatTag.contains("weaponCategories")) {
                    ListTag tagArray = combatTag.getList("weaponCategories", Tag.TAG_STRING);
                    for (Tag categoryId : tagArray) {
                        weaponCategories.add(WeaponCategory.ENUM_MANAGER.getOrThrow(categoryId.getAsString()));
                    }
                }
                //获取风格
                if (combatTag.contains("style")) {
                    style = Style.ENUM_MANAGER.getOrThrow(combatTag.getString("style").toUpperCase());
                }

                Supplier<CECombatBehaviors.Builder<MobPatch<?>>> supplier = () -> {
                    CECombatBehaviors.Builder<MobPatch<?>> behaviorBuilder = CECombatBehaviors.builder();
                    //全局事件
                    if(combatTag.contains("stunEvents")){
                        CompoundTag stunTag = combatTag.getCompound("stunEvents");
                        for (StunType stunType : StunType.values()) {
                            String name = stunType.name().toLowerCase();
                            if(stunTag.contains(name)){
                                ListTag array = stunTag.getList(name,Tag.TAG_COMPOUND);
                                for (int j = 0; j < array.size(); j++){
                                    CompoundTag evet = array.getCompound(j);
                                    String command =  evet.getString("command");
                                    boolean onTarget = evet.getBoolean("onTarget");
                                    behaviorBuilder.addStunEvent(stunType, creatCommandConsumer(onTarget, command));
                                }
                            }
                        }
                    }

                    //递归获取所有行为集
                    if (combatTag.contains("behaviorRoots")) {
                        ListTag behaviorRoots = combatTag.getList("behaviorRoots",Tag.TAG_COMPOUND);
                        for (int j = 0; j < behaviorRoots.size(); j++){
                            CompoundTag root = behaviorRoots.getCompound(j);
                            if(root.contains("isGlobal")){
                                boolean global = root.getBoolean("isGlobal");
                                if(global){
                                    behaviorBuilder.newGlobalBehavior(getBehaviorRootBuilder(root));
                                    continue;
                                }
                            }
                            behaviorBuilder.newBehaviorRoot(getBehaviorRootBuilder(root));
                        }
                    }
                    return behaviorBuilder;
                };

                for (WeaponCategory category : weaponCategories){
                    weaponAttackMotions.computeIfAbsent(category, k -> new HashMap<>()).put(style, supplier);
                }
            }
        }

        return weaponAttackMotions;
    }


    public static CECombatBehaviors.BehaviorRoot.Builder<MobPatch<?>> getBehaviorRootBuilder(CompoundTag tag) {
        CECombatBehaviors.BehaviorRoot.Builder<MobPatch<?>> builder = new CECombatBehaviors.BehaviorRoot.Builder<>();

        if (tag.contains("rootName")) {
            builder.rootName(tag.getString("rootName"));
        }
        if (tag.contains("priority")) {
            builder.priority(tag.getDouble("priority"));
        }
        if (tag.contains("weight")) {
            builder.weight(tag.getDouble("weight"));
        }
        if (tag.contains("maxCooldown")) {
            builder.maxCooldown(tag.getInt("maxCooldown"));
        }
        if (tag.contains("cooldown")) {
            builder.cooldown(tag.getInt("cooldown"));
        }
        if (tag.contains("isGlobal")) {
            builder.isGlobal(tag.getBoolean("isGlobal"));
        }
        if (tag.contains("backAfterFinished")) {
            builder.backAfterFinished(tag.getBoolean("backAfterFinished"));
        }
        if(tag.contains("firstBehaviors")){
            ListTag firstBehaviors = tag.getList("firstBehaviors",Tag.TAG_COMPOUND);
            for (int i = 0; i < firstBehaviors.size(); i++) {
                CompoundTag firstBehavior = firstBehaviors.getCompound(i);
                builder.addFirstBehavior(getBehaviorBuilder(firstBehavior));
            }
        }
        return builder;
    }

    public static CECombatBehaviors.Behavior.Builder<MobPatch<?>> getBehaviorBuilder(CompoundTag behaviors) {
        CECombatBehaviors.Behavior.Builder<MobPatch<?>> builder = CECombatBehaviors.Behavior.builder();

        if(behaviors.contains("behaviorName")){
            builder.name(behaviors.getString("behaviorName"));
        }

        if (behaviors.contains("priority")) {
            builder.priority(behaviors.getDouble("priority"));
        }

        if (behaviors.contains("weight")) {
            builder.weight(behaviors.getDouble("weight"));
        }

        if (behaviors.contains( "stopByStun")) {
            builder.stopByStun(behaviors.getInt("stopByStun"));
        }

        if (behaviors.contains("interruptType")) {
            CECombatBehaviors.InterruptType type;
            List<Float> interruptedWindow = new ArrayList<>();
            try{
                type = CECombatBehaviors.InterruptType.valueOf(behaviors.getString("interruptType").toUpperCase());
            }
            catch(IllegalArgumentException e){
                throw new RuntimeException("Invalid interruptType");
            }

            if(behaviors.contains("interruptedWindow")) {
                ListTag doubleListTag = behaviors.getList("interruptedWindow", Tag.TAG_DOUBLE);
                ListTag intListTag = behaviors.getList("interruptedWindow", Tag.TAG_INT);
                if(!doubleListTag.isEmpty()) {
                    for (int j = 0; j < doubleListTag.size(); j++) {
                        float time = (float) doubleListTag.getDouble(j);
                        interruptedWindow.add(time);
                    }
                }
                else if(!intListTag.isEmpty()) {
                    for (int j = 0; j < intListTag.size(); j++) {
                        float time = intListTag.getInt(j);
                        interruptedWindow.add(time);
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

        if(behaviors.contains("canInterruptParent")){
            builder.canInterruptParent(behaviors.getBoolean("canInterruptParent"));
        }

        if(behaviors.contains("canInsertGlobalBehavior")){
            boolean canInsertGlobalBehavior = behaviors.getBoolean("canInsertGlobalBehavior");
            List<String> names = new ArrayList<>();
            if(canInsertGlobalBehavior){
                if(behaviors.contains("allowedGlobalNameList")){
                    ListTag array = behaviors.getList("allowedGlobalNameList",Tag.TAG_STRING);
                    for (int i = 0; i < array.size(); i++) {
                        names.add(array.getString(i));
                    }
                }
            }
            builder.canInsertGlobalBehavior(canInsertGlobalBehavior, names.toArray(String[]::new));
        }

        if(behaviors.contains("addCoolDown")){
            builder.addCooldown(behaviors.getInt("addCoolDown"));
        }

        if(behaviors.contains("setCoolDown")){
            builder.setCooldown(behaviors.getInt("setCoolDown"));
        }

        if(behaviors.contains("waitTime")){
            builder.waitTime(behaviors.getInt("waitTime"));
        }

        if(behaviors.contains("addStamina")){
            builder.addStamina(behaviors.getFloat("addStamina"));
        }

        if(behaviors.contains("setStamina")){
            builder.setStamina(behaviors.getFloat("setStamina"));
        }

        if(behaviors.contains("setPhase")){
            builder.setPhase(behaviors.getInt("setPhase"));
        }

        if(behaviors.contains("addPhase")){
            builder.addPhase(behaviors.getInt("addPhase"));
        }

        if(behaviors.contains("conditions")){
            ListTag array = behaviors.getList("conditions",Tag.TAG_COMPOUND);
            for (int i = 0; i < array.size(); i++) {
                CompoundTag conditionTag = array.getCompound(i);
                builder.condition(deserializeBehaviorCondition(conditionTag));
            }
        }

        if(behaviors.contains("animation")){
            String path = behaviors.getString("animation");
            AnimationManager.AnimationAccessor<? extends StaticAnimation> animation = AnimationManager.byKey(path);
            AnimationParams animationParams = new AnimationParams();

            if(behaviors.contains("animationParams")) {
                animationParams = getAnimationParams(behaviors.getCompound("animationParams"));
            }

            builder.animationBehavior(animation, animationParams);

            //动画专用事件
            if(behaviors.contains("timeEvents")){
                ListTag array = behaviors.getList("timeEvents",Tag.TAG_COMPOUND);
                for(int i = 0; i < array.size(); i++) {
                    CompoundTag timeEvent = array.getCompound(i);
                    try{
                        TimeEvent.EventType eventType = TimeEvent.EventType.valueOf(timeEvent.getString("type").toUpperCase());
                        boolean onTarget = timeEvent.getBoolean("onTarget");
                        String command = timeEvent.getString("command");
                        switch(eventType){
                            case TICK -> {
                                builder.addTimeEvent(new TimeEvent(creatCommandConsumer(onTarget, command)));
                            }
                            case IN_TIME -> {
                                float timeStart = timeEvent.getFloat("timeStart");
                                builder.addTimeEvent(new TimeEvent(timeStart, creatCommandConsumer(onTarget, command)));
                            }
                            case BETWEEN_TIMES -> {
                                float timeStart = timeEvent.getFloat("timeStart");
                                float timeEnd = timeEvent.getFloat("timeEnd");
                                builder.addTimeEvent(new TimeEvent(timeStart, timeEnd, creatCommandConsumer(onTarget, command)));
                            }
                        }
                    } catch (IllegalArgumentException e){
                        throw new RuntimeException("Invalid time event");
                    }
                }
            }

            if(behaviors.contains("hitEvents")) {
                ListTag array = behaviors.getList("hitEvents",Tag.TAG_COMPOUND);
                for(int i = 0; i < array.size(); i++) {
                    CompoundTag hitEvent = array.getCompound(i);
                    try{
                        AttackResult.ResultType resultType = AttackResult.ResultType.valueOf(hitEvent.getString("type").toUpperCase());
                        int phase = hitEvent.getInt("phase");
                        boolean onTarget = hitEvent.getBoolean("onTarget");
                        String command = hitEvent.getString("command");
                        builder.addHitEvent(new HitEvent(phase, resultType, creatCommandConsumer2(onTarget, command)));
                    } catch (IllegalArgumentException e){
                        throw new RuntimeException("Invalid hitEvent");
                    }
                }
            }

            if(behaviors.contains("blockedEvents")){
                ListTag array = behaviors.getList("blockedEvents",Tag.TAG_COMPOUND);
                for(int i = 0; i < array.size(); i++) {
                    CompoundTag blockedEvent = array.getCompound(i);
                    int phase = blockedEvent.getInt("phase");
                    boolean parried = blockedEvent.getBoolean("parried");
                    boolean onTarget = blockedEvent.getBoolean("onTarget");
                    String command = blockedEvent.getString("command");
                    builder.addBlockedEvent(new BlockedEvent(phase, parried, creatCommandConsumer3(onTarget, command)));
                }
            }
        }
        else{
            AnimationManager.AnimationAccessor<? extends StaticAnimation> counter = Animations.SWEEPING_EDGE;
            AnimationParams animationParams = new AnimationParams();
            if (behaviors.contains("counterAnimation")) {
                String path = behaviors.getString("counterAnimation");
                counter = AnimationManager.byKey(path);
                if(behaviors.contains("animationParams")) {
                    animationParams = getAnimationParams(behaviors.getCompound("animationParams"));
                }
            }
            builder.counterAnimation(counter, animationParams);

            if (behaviors.contains("resetGuardTime")){
                builder.resetGuardTime(behaviors.getBoolean("resetGuardTime"));
            }

            if(behaviors.contains("counterType")){
                try{
                    CECombatBehaviors.CounterType counterType = CECombatBehaviors.CounterType.valueOf(behaviors.getString("counterType").toUpperCase());
                    builder.counterType(counterType);
                } catch (IllegalArgumentException e){
                    throw new RuntimeException("Invalid counterType");
                }
            }

            if(behaviors.contains("counterChance")){
                builder.counterChance(behaviors.getFloat("counterChance"));
            }

            if(behaviors.contains("maxGuardHit")){
                builder.maxGuardHit(behaviors.getInt("maxGuardHit"));
            }

            if(behaviors.contains("onGuardHit")){
                ListTag array = behaviors.getList("onGuardHit",Tag.TAG_COMPOUND);
                for(int i = 0; i < array.size(); i++) {
                    CompoundTag onGuardHit = array.getCompound(i);
                    boolean onTarget = onGuardHit.getBoolean("onTarget");
                    String command = onGuardHit.getString("command");
                    builder.addGuardHitEvent(new GuardHitEvent(creatCommandConsumer4(onTarget, command)));
                }
            }

            if(behaviors.contains("beforeCounter")){
                CompoundTag beforeCounter = behaviors.getCompound("beforeCounter");
                boolean cancelHitAnimation = beforeCounter.getBoolean("cancelHitAnimation");
                boolean onTarget = beforeCounter.getBoolean("onTarget");
                String command = beforeCounter.getString("command");

                builder.setBeforeCounterEvent(new BeforeCounterEvent(creatFunctionCommand(cancelHitAnimation, onTarget, command)));
            }

            if(behaviors.contains("onCounterStart")){
                ListTag array = behaviors.getList("onCounterStart", Tag.TAG_COMPOUND);
                for(int i = 0; i < array.size(); i++) {
                    CompoundTag onCounterStart = array.getCompound(i);
                    boolean onTarget = onCounterStart.getBoolean("onTarget");
                    String command = onCounterStart.getString("command");
                    builder.onCounterStart(creatCommandConsumer(onTarget, command));
                }
            }

            float forward = 0;
            float strafe = 0;
            if(behaviors.contains("forward")){
                forward = behaviors.getFloat("forward");
            }
            if(behaviors.contains("strafe")){
                strafe = behaviors.getFloat("strafe");
            }

            if(behaviors.contains("guardWithWander")){
                int guardWithWander = behaviors.getInt("guardWithWander");
                boolean guardAnimation = false;
                if(behaviors.contains("guardAnimation")){
                    guardAnimation = behaviors.getBoolean("guardAnimation");
                }
                builder.guardWithWander(guardWithWander, forward, strafe, guardAnimation);
            }
            if(behaviors.contains("guardTime")){
                int guardTime = behaviors.getInt("guardTime");
                builder.guard(guardTime);
            }


            if(behaviors.contains("wanderTime")){
                int wanderTime =  behaviors.getInt("wanderTime");
                builder.wander(wanderTime, forward, strafe);
            }

        }

        //通用事件
        if (behaviors.contains("exBehavior")){
            ListTag array = behaviors.getList("exBehavior",Tag.TAG_COMPOUND);
            for (int i = 0; i < array.size(); i++) {
                CompoundTag exBehavior = array.getCompound(i);
                boolean onTarget = exBehavior.getBoolean("onTarget");
                String command = exBehavior.getString("command");

                builder.addExBehavior(creatCommandConsumer(onTarget, command));
            }
        }

        if(behaviors.contains("onHurtEvent")){
            CompoundTag hurtEvent = behaviors.getCompound("onHurtEvent");
            try{
                String returnResult = hurtEvent.getString("returnResult").toUpperCase();
                AttackResult.ResultType resultType = null;
                if(!returnResult.equals("DEFAULT")){
                    resultType = AttackResult.ResultType.valueOf(returnResult);
                }
                float damage = hurtEvent.getFloat("damage");
                boolean onTarget = hurtEvent.getBoolean("onTarget");
                String command = hurtEvent.getString("command");

                builder.setOnHurtEvent(new OnHurtEvent(creatTriFunctionCommand(resultType, damage, onTarget, command)));
            } catch (IllegalArgumentException e){
                throw new RuntimeException("Invalid onHurtEvent");
            }
        }

        //子行为
        if(behaviors.contains("nextBehaviors")){
            ListTag nextBehavior = behaviors.getList("nextBehaviors",Tag.TAG_COMPOUND);
            for (int i = 0; i < nextBehavior.size(); i++) {
                CompoundTag nextBehaviorTag = nextBehavior.getCompound(i);
                builder.addNextBehavior(getBehaviorBuilder(nextBehaviorTag));
            }
        }

        return builder;
    }

    public static Function<MobPatch<?>, Boolean> creatFunctionCommand(boolean cancelHitAnimation, boolean onTarget, String command) {
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

    public static @NotNull TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> creatTriFunctionCommand(AttackResult.ResultType resultType, float damage, boolean onTarget, String command) {
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

    public static @NotNull Consumer<MobPatch<?>> creatCommandConsumer(boolean onTarget, String command) {
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

    public static @NotNull BiConsumer<MobPatch<?>, Entity> creatCommandConsumer2(boolean onTarget, String command) {
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

    public static @NotNull BiConsumer<MobPatch<?>, LivingEntityPatch<?>> creatCommandConsumer3(boolean onTarget, String command) {
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


    public static @NotNull BiConsumer<MobPatch<?>, DamageSource> creatCommandConsumer4(boolean onTarget, String command) {
        return (mobPatch, damageSource) -> {
            if (mobPatch.isLogicalClient()) return;

            LivingEntity original = mobPatch.getOriginal();
            LivingEntity executor = onTarget ? (damageSource.getEntity() instanceof LivingEntity livingTarget ? livingTarget : null) : original;

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

    public static AnimationParams getAnimationParams(CompoundTag paramsTag) {
        AnimationParams animationParams = new AnimationParams();

        if(paramsTag.contains("transitionTime")){
            animationParams.transitionTime(paramsTag.getFloat("transitionTime"));
        }
        if(paramsTag.contains("playSpeed")){
            animationParams.playSpeed(paramsTag.getFloat("playSpeed"));
        }
        if (paramsTag.contains("phaseParams")){
            ListTag array = paramsTag.getList("phaseParams",Tag.TAG_COMPOUND);
            for(int i = 0; i < array.size(); i++){
                CompoundTag phaseTag = array.getCompound(i);
                int phaseIndex = -1;
                PhaseParams phaseParams = new PhaseParams();
                if (phaseTag.contains("phase")){
                    phaseIndex = phaseTag.getInt("phase");
                }
                if(phaseTag.contains("damageMultiplier")){
                    phaseParams.damageMultiplier(phaseTag.getFloat("damageMultiplier"));
                }
                if(phaseTag.contains("impactMultiplier")){
                    phaseParams.impactMultiplier(phaseTag.getFloat("impactMultiplier"));
                }
                if(phaseTag.contains("armorNegationMultiplier")){
                    phaseParams.armorNegationMultiplier(phaseTag.getFloat("armorNegationMultiplier"));
                }
                if(phaseTag.contains("stunType")){
                    String stunName = phaseTag.getString("stunType").toUpperCase();
                    if(!stunName.equals("DEFAULT")){
                        try {
                            StunType stun = StunType.valueOf(stunName);
                            phaseParams.stunType(stun);
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Invalid stunType");
                        }
                    }
                }
                if(phaseTag.contains("damage_tags")){
                    ListTag tags =  phaseTag.getList("damage_tags",Tag.TAG_STRING);
                    Set<TagKey<DamageType>> damageTags = new HashSet<>();
                    for (Tag tagElement : tags) {
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

    public static <T extends MobPatch<?>> Condition<T> deserializeBehaviorCondition(CompoundTag conditionTag) {
        String type = conditionTag.getString("type");

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
        condition.read(conditionTag);
        return condition;

    }

    public static class CEDatapackMobPatchProvider extends MobPatchReloadListener.AbstractMobPatchProvider {

        public Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>>>> weaponLivingMotions = new HashMap<>();
        public Map<WeaponCategory, Map<Style, List<AnimationManager.AnimationAccessor<? extends StaticAnimation>>>> guardHitMotions = new HashMap<>();
        public Map<WeaponCategory, Map<Style, Supplier<CECombatBehaviors.Builder<MobPatch<?>>>>> weaponAttackMotions = new HashMap<>();
        public Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> stunAnimations = new HashMap<>();
        public Map<Attribute, Double> attributeMap = new HashMap<>();
        public Factions faction;
        public int breakTime = 40;
        public int recoverTime = 60;
        public int staminaRegenDelay = 60;
        public float chasingSpeed = 1.25F;
        public float scale = 1;
        public float guardHitImpact = 1F;
        public float hurtImpact = 0.35F;
        public float beParriedDamage = 1F;
        public boolean enableBossBar = false;
        public String bossBarName = "[CE:EMPTY_NAME]";
        public ResourceLocation bossBarTexture = null;
        public boolean playBGM = false;
        public ResourceLocation bgm = null;
        public boolean bgmLoop = true;
        public int bgmDuration = Integer.MAX_VALUE;
        public int bgmFadeIn = 0;
        public int bgmFadeOut = 0;
        public float bgmVolume = 1;


        @Override
        public EntityPatch<?> get(Entity entity) {
            return new CEDatapackMobPatch(this);
        }

    }
}
