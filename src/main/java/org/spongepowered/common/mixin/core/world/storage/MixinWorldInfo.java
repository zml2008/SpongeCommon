/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.world.storage;

import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.MoreObjects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketServerDifficulty;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.Level;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.PortalAgentType;
import org.spongepowered.api.world.PortalAgentTypes;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.SerializationBehaviors;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.difficulty.Difficulty;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.type.WorldConfig;
import org.spongepowered.common.data.persistence.NbtTranslator;
import org.spongepowered.common.data.util.DataQueries;
import org.spongepowered.common.data.util.DataUtil;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.interfaces.world.IMixinDerivedWorldInfo;
import org.spongepowered.common.interfaces.world.IMixinDimensionType;
import org.spongepowered.common.interfaces.world.IMixinGameRules;
import org.spongepowered.common.interfaces.world.IMixinWorldInfo;
import org.spongepowered.common.interfaces.world.IMixinWorldSettings;
import org.spongepowered.common.registry.type.world.DimensionTypeRegistryModule;
import org.spongepowered.common.registry.type.world.PortalAgentRegistryModule;
import org.spongepowered.common.registry.type.world.WorldGeneratorModifierRegistryModule;
import org.spongepowered.common.util.FunctionalUtil;
import org.spongepowered.common.world.WorldManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

@NonnullByDefault
@Mixin(WorldInfo.class)
@Implements(@Interface(iface = WorldProperties.class, prefix = "worldproperties$"))
public abstract class MixinWorldInfo implements WorldProperties, IMixinWorldInfo {

    @Shadow public long randomSeed;
    @Shadow private WorldType terrainType;
    @Shadow private String generatorOptions;
    @Shadow private int spawnX;
    @Shadow private int spawnY;
    @Shadow private int spawnZ;
    @Shadow private long totalTime;
    @Shadow private long worldTime;
    @Shadow private long lastTimePlayed;
    @Shadow private long sizeOnDisk;
    @Shadow private NBTTagCompound playerTag;
    @Shadow private String levelName;
    @Shadow private int saveVersion;
    @Shadow private int cleanWeatherTime;
    @Shadow private boolean raining;
    @Shadow private int rainTime;
    @Shadow private boolean thundering;
    @Shadow private int thunderTime;
    @Shadow private GameType gameType;
    @Shadow private boolean mapFeaturesEnabled;
    @Shadow private boolean hardcore;
    @Shadow private boolean allowCommands;
    @Shadow private boolean initialized;
    @Shadow public EnumDifficulty difficulty;
    @Shadow private boolean difficultyLocked;
    @Shadow private double borderCenterX;
    @Shadow private double borderCenterZ;
    @Shadow private double borderSize;
    @Shadow private long borderSizeLerpTime;
    @Shadow private double borderSizeLerpTarget;
    @Shadow private double borderSafeZone;
    @Shadow private double borderDamagePerBlock;
    @Shadow private int borderWarningDistance;
    @Shadow private int borderWarningTime;
    @Shadow private GameRules gameRules;
    private ServerScoreboard scoreboard;
    private boolean hasCustomDifficulty = false;

    @Shadow public abstract void setDifficulty(EnumDifficulty newDifficulty);
    @Shadow public abstract NBTTagCompound cloneNBTCompound(@Nullable NBTTagCompound nbt);

    @Nullable private UUID uuid;
    @Nullable private Integer dimensionId;
    private DimensionType dimensionType = DimensionTypes.OVERWORLD;
    private SerializationBehavior serializationBehavior = SerializationBehaviors.AUTOMATIC;
    private boolean isMod = false;
    private boolean generateBonusChest;
    private NBTTagCompound spongeRootLevelNbt = new NBTTagCompound(), spongeNbt = new NBTTagCompound();
    private final NBTTagList playerUniqueIdNbt = new NBTTagList();
    private final BiMap<Integer, UUID> playerUniqueIdMap = HashBiMap.create();
    private final List<UUID> pendingUniqueIds = new ArrayList<>();
    private int trackedUniqueIdCount = 0;
    @Nullable private SpongeConfig<WorldConfig> worldConfig;
    @Nullable private PortalAgentType portalAgentType;

    //     protected WorldInfo()
    @Inject(method = "<init>", at = @At("RETURN") )
    private void onConstruction(CallbackInfo ci) {
        this.onConstructionCommon();
    }

    //     public WorldInfo(NBTTagCompound nbt)
    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN") )
    private void onConstruction(NBTTagCompound nbt, CallbackInfo ci) {
        if (SpongeImplHooks.isMainThread() && !PhaseTracker.getInstance().getCurrentContext().state.isConvertingMaps()) {
            this.onConstructionCommon();
        }
    }

    //     public WorldInfo(WorldSettings settings, String name)
    @Inject(method = "<init>(Lnet/minecraft/world/WorldSettings;Ljava/lang/String;)V", at = @At("RETURN"))
    private void onConstruction(WorldSettings settings, String name, CallbackInfo ci) {
        if (!this.isValid()) {
            return;
        }

        this.onConstructionCommon();

        final WorldArchetype archetype = (WorldArchetype) (Object) settings;
        this.setDimensionType(archetype.getDimensionType());

        this.createWorldConfig();
        this.setEnabled(archetype.isEnabled());
        this.setLoadOnStartup(archetype.loadOnStartup());
        if (((IMixinWorldSettings)(Object) settings).internalKeepSpawnLoaded() != null) {
            this.setKeepSpawnLoaded(archetype.doesKeepSpawnLoaded());
        }
        this.setGenerateSpawnOnLoad(archetype.doesGenerateSpawnOnLoad());
        this.forceSetDifficulty((EnumDifficulty) (Object) archetype.getDifficulty());
        Collection<WorldGeneratorModifier> modifiers = this.getGeneratorModifiers();
        if (modifiers.isEmpty()) {
            this.setGeneratorModifiers(archetype.getGeneratorModifiers());
        } else {
            // use config modifiers
            this.setGeneratorModifiers(modifiers);
        }
        this.setSerializationBehavior(archetype.getSerializationBehavior());
        this.createWorldConfig();
    }

    //     public WorldInfo(WorldInfo worldInformation)
    @Inject(method = "<init>(Lnet/minecraft/world/storage/WorldInfo;)V", at = @At("RETURN") )
    private void onConstruction(WorldInfo worldInformation, CallbackInfo ci) {
        this.onConstructionCommon();

        MixinWorldInfo info = (MixinWorldInfo) (Object) worldInformation;
        this.getOrCreateWorldConfig(); // Create the config now if it has not yet been created.
        this.portalAgentType = info.portalAgentType;
        this.setDimensionType(info.dimensionType);
    }

    // used in all init methods
    private void onConstructionCommon() {
        this.spongeNbt.setTag(NbtDataUtil.SPONGE_PLAYER_UUID_TABLE, this.playerUniqueIdNbt);
        this.spongeRootLevelNbt.setTag(NbtDataUtil.SPONGE_DATA, this.spongeNbt);
    }

    @Inject(method = "updateTagCompound", at = @At("HEAD"))
    private void ensureLevelNameMatchesDirectory(NBTTagCompound compound, NBTTagCompound player, CallbackInfo ci) {
        if (this.dimensionId == null) {
            return;
        }

        final String name = WorldManager.getWorldFolderByDimensionId(this.dimensionId).orElse(this.levelName);
        if (!this.levelName.equalsIgnoreCase(name)) {
            this.levelName = name;
        }
    }

    @Override
    public boolean createWorldConfig() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().createWorldConfig();
        }

        if (this.worldConfig != null) {
             return false;
        }

        if (this.isValid()) {
            this.worldConfig =
                    new SpongeConfig<>(SpongeConfig.Type.WORLD, ((IMixinDimensionType) this.dimensionType).getConfigPath()
                            .resolve(this.levelName)
                            .resolve("world.conf"),
                            SpongeImpl.ECOSYSTEM_ID,
                            ((IMixinDimensionType) getDimensionType()).getDimensionConfig());
        } else {
            this.worldConfig = SpongeConfig.newDummyConfig(SpongeConfig.Type.WORLD);
        }

        return true;
    }

    @Override
    public boolean isValid() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().isValid();
        }

        return !(this.levelName == null || this.levelName.equals("") || this.levelName.equals("MpServer") || this.levelName.equals("sponge$dummy_world"));
    }

    @Override
    public Vector3i getSpawnPosition() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getSpawnPosition();
        }
        return new Vector3i(this.spawnX, this.spawnY, this.spawnZ);
    }

    @Override
    public void setSpawnPosition(Vector3i position) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setSpawnPosition(position);
            return;
        }

        checkNotNull(position);
        this.spawnX = position.getX();
        this.spawnY = position.getY();
        this.spawnZ = position.getZ();
    }

    @Override
    public GeneratorType getGeneratorType() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getGeneratorType();
        }
        return (GeneratorType) this.terrainType;
    }

    @Override
    public void setGeneratorType(GeneratorType type) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setGeneratorType(type);
            return;
        }
        this.terrainType = (WorldType) type;
    }

    @Intrinsic
    public long worldproperties$getSeed() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getSeed();
        }
        return this.randomSeed;
    }

    @Override
    public void setSeed(long seed) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setSeed(seed);
            return;
        }

        this.randomSeed = seed;
    }

    @Override
    public long getTotalTime() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getTotalTime();
        }
        return this.totalTime;
    }

    @Intrinsic
    public long worldproperties$getWorldTime() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldTime();
        }
        return this.worldTime;
    }

    @Override
    public void setWorldTime(long time) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldTime(time);
            return;
        }
        this.worldTime = time;
    }

    @Override
    public DimensionType getDimensionType() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getDimensionType();
        }
        return this.dimensionType;
    }

    @Override
    public void setDimensionType(DimensionType type) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().setDimensionType(type);
            return;
        }
        this.dimensionType = type;
        String modId = SpongeImplHooks.getModIdFromClass(this.dimensionType.getDimensionClass());
        if (!modId.equals("minecraft")) {
            this.isMod = true;
        }
    }

    @Override
    public PortalAgentType getPortalAgentType() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getPortalAgentType();
        }
        if (this.portalAgentType == null) {
            this.portalAgentType = PortalAgentTypes.DEFAULT;
        }
        return this.portalAgentType;
    }

    @Override
    public void setPortalAgentType(PortalAgentType type) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().setPortalAgentType(type);
            return;
        }
        this.portalAgentType = type;
    }

    @Intrinsic
    public boolean worldproperties$isRaining() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().isRaining();
        }
        return this.raining;
    }

    @Override
    public void setRaining(boolean state) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setRaining(state);
            return;
        }
        this.raining = state;
    }

    @Intrinsic
    public int worldproperties$getRainTime() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getRainTime();
        }
        return this.rainTime;
    }

    @Intrinsic
    public void worldproperties$setRainTime(int time) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setRainTime(time);
            return;
        }
        this.rainTime = time;
    }

    @Intrinsic
    public boolean worldproperties$isThundering() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().isThundering();
        }
        return this.thundering;
    }

    @Intrinsic
    public void worldproperties$setThundering(boolean state) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setThundering(state);
        }
        this.thundering = state;
    }

    @Override
    public int getThunderTime() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getThunderTime();
        }
        return this.thunderTime;
    }

    @Override
    public void setThunderTime(int time) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setThunderTime(time);
            return;
        }
        this.thunderTime = time;
    }

    @Override
    public GameMode getGameMode() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getGameMode();
        }
        return (GameMode) (Object) this.gameType;
    }

    @Override
    public void setGameMode(GameMode gamemode) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setGameMode(gamemode);
            return;
        }
        this.gameType = (GameType) (Object) gamemode;
    }

    @Override
    public boolean usesMapFeatures() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().usesMapFeatures();
        }
        return this.mapFeaturesEnabled;
    }

    @Override
    public void setMapFeaturesEnabled(boolean state) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setMapFeaturesEnabled(state);
            return;
        }
        this.mapFeaturesEnabled = state;
    }

    @Override
    public boolean isHardcore() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().isHardcore();
        }
        return this.hardcore;
    }

    @Override
    public void setHardcore(boolean state) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setHardcore(state);
            return;
        }
        this.hardcore = state;
    }

    @Override
    public boolean areCommandsAllowed() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().areCommandsAllowed();
        }
        return this.allowCommands;
    }

    @Override
    public void setCommandsAllowed(boolean state) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setCommandsAllowed(state);
            return;
        }
        this.allowCommands = state;
    }

    @Override
    public boolean isInitialized() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().isInitialized();
        }
        return this.initialized;
    }

    @Override
    public Difficulty getDifficulty() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getDifficulty();
        }
        return (Difficulty) (Object) this.difficulty;
    }

    @Inject(method = "setDifficulty", at = @At("HEAD"), cancellable = true)
    private void onSetDifficultyVanilla(EnumDifficulty newDifficulty, CallbackInfo ci) {
        this.hasCustomDifficulty = true;
        if (newDifficulty == null) {
            // This is an error from someone
            new PrettyPrinter(60).add("Null Difficulty being set!").centre().hr()
                .add("Someone (not Sponge) is attempting to set a null difficulty to this WorldInfo setup! Please report to the mod/plugin author!")
                .add()
                .addWrapped(60, " %s : %s", "WorldInfo", this)
                .add()
                .add(new Exception("Stacktrace"))
                .log(SpongeImpl.getLogger(), Level.ERROR);
            ci.cancel(); // We cannot let the null set the field.
            return;
        }

        // If the difficulty is set, we need to sync it to the players in the world attached to the worldinfo (if any)
        WorldManager.getWorlds()
          .stream()
          .filter(world -> world.getWorldInfo() == (WorldInfo) (Object) this)
          .flatMap(world -> world.playerEntities.stream())
          .filter(player -> player instanceof EntityPlayerMP)
          .map(player -> (EntityPlayerMP) player)
          .forEach(player -> player.connection.sendPacket(new SPacketServerDifficulty(newDifficulty, ((WorldInfo) (Object) this).isDifficultyLocked
            ())));
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setDifficulty(difficulty);
            return;
        }
        this.setDifficulty((EnumDifficulty) (Object) difficulty);
    }

    @Override
    public boolean hasCustomDifficulty() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().hasCustomDifficulty();
        }
        return this.hasCustomDifficulty;
    }

    @Override
    public void forceSetDifficulty(EnumDifficulty difficulty) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().forceSetDifficulty(difficulty);
            return;
        }
        this.difficulty = difficulty;
    }

    @Override
    public boolean doesGenerateBonusChest() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().doesGenerateBonusChest();
        }
        return this.generateBonusChest;
    }

    @Override
    public Vector3d getWorldBorderCenter() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldBorderCenter();
        }
        return new Vector3d(this.borderCenterX, 0, this.borderCenterZ);
    }

    @Override
    public void setWorldBorderCenter(double x, double z) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldBorderCenter(x, z);
            return;
        }
        this.borderCenterX = x;
        this.borderCenterZ = z;
    }

    @Override
    public double getWorldBorderDiameter() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldBorderDiameter();
        }
        return this.borderSize;
    }

    @Override
    public void setWorldBorderDiameter(double diameter) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldBorderDiameter(diameter);
            return;
        }
        this.borderSize = diameter;
    }

    @Override
    public double getWorldBorderTargetDiameter() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldBorderTargetDiameter();
        }
        return this.borderSizeLerpTarget;
    }

    @Override
    public void setWorldBorderTargetDiameter(double diameter) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldBorderTargetDiameter(diameter);
            return;
        }
        this.borderSizeLerpTarget = diameter;
    }

    @Override
    public double getWorldBorderDamageThreshold() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldBorderDamageThreshold();
        }
        return this.borderSafeZone;
    }

    @Override
    public void setWorldBorderDamageThreshold(double distance) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldBorderDamageThreshold(distance);
            return;
        }
        this.borderSafeZone = distance;
    }

    @Override
    public double getWorldBorderDamageAmount() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldBorderDamageAmount();
        }
        return this.borderDamagePerBlock;
    }

    @Override
    public void setWorldBorderDamageAmount(double damage) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldBorderDamageAmount(damage);
            return;
        }
        this.borderDamagePerBlock = damage;
    }

    @Override
    public int getWorldBorderWarningTime() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldBorderWarningTime();
        }
        return this.borderWarningTime;
    }

    @Override
    public void setWorldBorderWarningTime(int time) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldBorderWarningTime(time);
            return;
        }
        this.borderWarningTime = time;
    }

    @Override
    public int getWorldBorderWarningDistance() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldBorderWarningDistance();
        }
        return this.borderWarningDistance;
    }

    @Override
    public void setWorldBorderWarningDistance(int distance) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldBorderWarningDistance(distance);
            return;
        }
        this.borderWarningDistance = distance;
    }

    @Override
    public long getWorldBorderTimeRemaining() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getWorldBorderTimeRemaining();
        }
        return this.borderSizeLerpTime;
    }

    @Override
    public void setWorldBorderTimeRemaining(long time) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setWorldBorderTimeRemaining(time);
            return;
        }
        this.borderSizeLerpTime = time;
    }

    @Override
    public Optional<String> getGameRule(String gameRule) {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getGameRule(gameRule);
        }
        checkNotNull(gameRule, "The gamerule cannot be null!");
        if (this.gameRules.hasRule(gameRule)) {
            return Optional.of(this.gameRules.getString(gameRule));
        }
        return Optional.empty();
    }

    @Override
    public Map<String, String> getGameRules() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getGameRules();
        }
        ImmutableMap.Builder<String, String> ruleMap = ImmutableMap.builder();
        for (String rule : this.gameRules.getRules()) {
            ruleMap.put(rule, this.gameRules.getString(rule));
        }
        return ruleMap.build();
    }

    @Override
    public void setGameRule(String gameRule, String value) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setGameRule(gameRule, value);
            return;
        }
        checkNotNull(gameRule, "The gamerule cannot be null!");
        checkNotNull(value, "The gamerule value cannot be null!");
        this.gameRules.setOrCreateGameRule(gameRule, value);
    }

    @Override
    public boolean removeGameRule(String gameRule) {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().removeGameRule(gameRule);
        }
        checkNotNull(gameRule, "The gamerule cannot be null!");
        return ((IMixinGameRules) this.gameRules).removeGameRule(gameRule);
    }

    @Override
    public void setDimensionId(int id) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().setDimensionId(id);
            return;
        }
        this.dimensionId = id;
    }

    @Override
    public Integer getDimensionId() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().getDimensionId();
        }
        return this.dimensionId;
    }

    @Override
    public UUID getUniqueId() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getUniqueId();
        }
        return this.uuid;
    }

    @Override
    public int getContentVersion() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getContentVersion();
        }
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().toContainer();
        }
        return NbtTranslator.getInstance().translateFrom(this.cloneNBTCompound(null));
    }

    @Override
    public boolean isEnabled() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().isEnabled();
        }
        return this.getOrCreateWorldConfig().getConfig().getWorld().isWorldEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setEnabled(enabled);
            return;
        }
        this.getOrCreateWorldConfig().getConfig().getWorld().setWorldEnabled(enabled);
    }

    @Override
    public boolean loadOnStartup() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().loadOnStartup();
        }
        Boolean loadOnStartup = this.getOrCreateWorldConfig().getConfig().getWorld().loadOnStartup();
        if (loadOnStartup == null) {
           loadOnStartup = ((IMixinDimensionType) this.dimensionType).shouldGenerateSpawnOnLoad();
           this.setLoadOnStartup(loadOnStartup);
        }
        return loadOnStartup;
    }

    @Override
    public void setLoadOnStartup(boolean state) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setLoadOnStartup(state);
            return;
        }
        this.getOrCreateWorldConfig().getConfig().getWorld().setLoadOnStartup(state);
        this.saveConfig();
    }

    @Override
    public boolean doesKeepSpawnLoaded() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().doesKeepSpawnLoaded();
        }
        Boolean keepSpawnLoaded = this.getOrCreateWorldConfig().getConfig().getWorld().getKeepSpawnLoaded();
        if (keepSpawnLoaded == null) {
            keepSpawnLoaded = ((IMixinDimensionType) this.dimensionType).shouldLoadSpawn();
        } else if (this.isMod && !keepSpawnLoaded) { // If disabled and a mod dimension, validate
            if (this.dimensionId == ((net.minecraft.world.DimensionType)(Object) this.dimensionType).getId()) {
                keepSpawnLoaded = ((IMixinDimensionType) this.dimensionType).shouldKeepSpawnLoaded();
                this.setKeepSpawnLoaded(keepSpawnLoaded);
            }
        }
        return keepSpawnLoaded;
    }

    @Override
    public void setKeepSpawnLoaded(boolean loaded) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setKeepSpawnLoaded(loaded);
            return;
        }
        this.getOrCreateWorldConfig().getConfig().getWorld().setKeepSpawnLoaded(loaded);
        this.saveConfig();
    }

    @Override
    public boolean doesGenerateSpawnOnLoad() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().doesGenerateSpawnOnLoad();
        }
        Boolean shouldGenerateSpawn = this.getOrCreateWorldConfig().getConfig().getWorld().getGenerateSpawnOnLoad();
        if (shouldGenerateSpawn == null) {
            shouldGenerateSpawn = ((IMixinDimensionType) this.dimensionType).shouldGenerateSpawnOnLoad();
            this.setGenerateSpawnOnLoad(shouldGenerateSpawn);
        }
        return shouldGenerateSpawn;
    }

    @Override
    public void setGenerateSpawnOnLoad(boolean state) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setGenerateSpawnOnLoad(state);
            return;
        }
        this.getOrCreateWorldConfig().getConfig().getWorld().setGenerateSpawnOnLoad(state);
    }

    @Override
    public boolean isPVPEnabled() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().isPVPEnabled();
        }
        return this.getOrCreateWorldConfig().getConfig().getWorld().getPVPEnabled();
    }

    @Override
    public void setPVPEnabled(boolean enabled) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setPVPEnabled(enabled);
            return;
        }
        this.getOrCreateWorldConfig().getConfig().getWorld().setPVPEnabled(enabled);
    }

    @Override
    public void setUniqueId(UUID uniqueId) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().setUniqueId(uniqueId);
            return;
        }
        this.uuid = uniqueId;
    }

    @Override
    public void setIsMod(boolean flag) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().setIsMod(flag);
            return;
        }
        this.isMod = flag;
    }

    @Override
    public void setScoreboard(ServerScoreboard scoreboard) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().setScoreboard(scoreboard);
            return;
        }
        this.scoreboard = scoreboard;
    }

    @Override
    public boolean getIsMod() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().getIsMod();
        }
        return this.isMod;
    }

    @Override
    public SpongeConfig<WorldConfig> getOrCreateWorldConfig() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().getOrCreateWorldConfig();
        }
        if (this.worldConfig == null) {
            this.createWorldConfig();
        }
        return this.worldConfig;
    }

    @Override
    public SpongeConfig<WorldConfig> getWorldConfig() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().getOrCreateWorldConfig();
        }
        return this.getOrCreateWorldConfig();
    }

    @Override
    public Collection<WorldGeneratorModifier> getGeneratorModifiers() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getGeneratorModifiers();
        }
        return WorldGeneratorModifierRegistryModule.getInstance().toModifiers(this.getOrCreateWorldConfig().getConfig().getWorldGenModifiers());
    }

    @Override
    public void setGeneratorModifiers(Collection<WorldGeneratorModifier> modifiers) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setGeneratorModifiers(modifiers);
            return;
        }
        checkNotNull(modifiers, "modifiers");

        this.getOrCreateWorldConfig().getConfig().getWorldGenModifiers().clear();
        this.getOrCreateWorldConfig().getConfig().getWorldGenModifiers().addAll(WorldGeneratorModifierRegistryModule.getInstance().toIds(modifiers));
    }

    @Override
    public DataContainer getGeneratorSettings() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getGeneratorSettings();
        }
        // Minecraft uses a String, we want to return a fancy DataContainer
        // Parse the world generator settings as JSON
        try {
            return DataFormats.JSON.read(this.generatorOptions);
        } catch (JsonParseException | IOException ignored) {
        }
        return DataContainer.createNew().set(DataQueries.WORLD_CUSTOM_SETTINGS, this.generatorOptions);
    }

    @Override
    public SerializationBehavior getSerializationBehavior() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getSerializationBehavior();
        }
        return this.serializationBehavior;
    }

    @Override
    public void setSerializationBehavior(SerializationBehavior behavior) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setSerializationBehavior(behavior);
            return;
        }
        this.serializationBehavior = behavior;
    }

    @Override
    public Optional<DataView> getPropertySection(DataQuery path) {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getPropertySection(path);
        }
        if (this.spongeRootLevelNbt.hasKey(path.toString())) {
            return Optional
                    .<DataView>of(NbtTranslator.getInstance().translateFrom(this.spongeRootLevelNbt.getCompoundTag(path.toString())));
        }
        return Optional.empty();
    }

    @Override
    public void setPropertySection(DataQuery path, DataView data) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getAPIDelegate().setPropertySection(path, data);
            return;
        }
        NBTTagCompound nbt = NbtTranslator.getInstance().translateData(data);
        this.spongeRootLevelNbt.setTag(path.toString(), nbt);
    }

    @Override
    public int getIndexForUniqueId(UUID uuid) {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().getIndexForUniqueId(uuid);
        }
        final Integer index = this.playerUniqueIdMap.inverse().get(uuid);
        if (index != null) {
            return index;
        }

        this.playerUniqueIdMap.put(this.trackedUniqueIdCount, uuid);
        this.pendingUniqueIds.add(uuid);
        return this.trackedUniqueIdCount++;
    }

    @Override
    public Optional<UUID> getUniqueIdForIndex(int index) {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().getUniqueIdForIndex(index);
        }
        return Optional.ofNullable(this.playerUniqueIdMap.get(index));
    }

    @Override
    public NBTTagCompound getSpongeRootLevelNbt() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().getSpongeRootLevelNbt();
        }
        this.writeSpongeNbt();
        return this.spongeRootLevelNbt;
    }

    @Override
    public NBTTagCompound getSpongeNbt() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getMixinDelegate().getSpongeNbt();
        }
        this.writeSpongeNbt();
        return this.spongeNbt;
    }

    @Override
    public void setSpongeRootLevelNBT(NBTTagCompound nbt) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().setSpongeRootLevelNBT(nbt);
            return;
        }
        this.spongeRootLevelNbt = nbt;
        if (nbt.hasKey(NbtDataUtil.SPONGE_DATA)) {
            this.spongeNbt = nbt.getCompoundTag(NbtDataUtil.SPONGE_DATA);
        }
    }

    @Override
    public void readSpongeNbt(NBTTagCompound nbt) {
        if (this instanceof IMixinDerivedWorldInfo) {
            ((IMixinDerivedWorldInfo) this).getMixinDelegate().readSpongeNbt(nbt);
            return;
        }
        final UUID nbtUniqueId = nbt.getUniqueId(NbtDataUtil.UUID);
        if (UUID.fromString("00000000-0000-0000-0000-000000000000").equals(nbtUniqueId)) {
            return;
        }
        this.uuid = nbtUniqueId;
        this.dimensionId = nbt.getInteger(NbtDataUtil.DIMENSION_ID);
        final String dimensionTypeId = nbt.getString(NbtDataUtil.DIMENSION_TYPE);
        final DimensionType dimensionType = (org.spongepowered.api.world.DimensionType)(Object) WorldManager.getDimensionType(this.dimensionId).orElse(null);
        this.setDimensionType(dimensionType != null ? dimensionType : DimensionTypeRegistryModule.getInstance().getById(dimensionTypeId)
                .orElseThrow(FunctionalUtil.invalidArgument("Could not find a DimensionType registered for world '" + this.getWorldName() + "' with dim id: " + this.dimensionId)));
        this.generateBonusChest = nbt.getBoolean(NbtDataUtil.GENERATE_BONUS_CHEST);
        this.portalAgentType = PortalAgentRegistryModule.getInstance().validatePortalAgent(nbt.getString(NbtDataUtil.PORTAL_AGENT_TYPE), this.levelName);
        this.hasCustomDifficulty = nbt.getBoolean(NbtDataUtil.HAS_CUSTOM_DIFFICULTY);
        this.trackedUniqueIdCount = 0;
        if (nbt.hasKey(NbtDataUtil.WORLD_SERIALIZATION_BEHAVIOR)) {
            short saveBehavior = nbt.getShort(NbtDataUtil.WORLD_SERIALIZATION_BEHAVIOR);
            if (saveBehavior == 1) {
                this.serializationBehavior = SerializationBehaviors.AUTOMATIC;
            } else if (saveBehavior == 0) {
                this.serializationBehavior = SerializationBehaviors.MANUAL;
            } else {
                this.serializationBehavior = SerializationBehaviors.NONE;
            }
        }
        if (nbt.hasKey(NbtDataUtil.SPONGE_PLAYER_UUID_TABLE, NbtDataUtil.TAG_LIST)) {
            final NBTTagList playerIdList = nbt.getTagList(NbtDataUtil.SPONGE_PLAYER_UUID_TABLE, NbtDataUtil.TAG_COMPOUND);
            for (int i = 0; i < playerIdList.tagCount(); i++) {
                final NBTTagCompound playerId = playerIdList.getCompoundTagAt(i);
                final UUID playerUuid = playerId.getUniqueId(NbtDataUtil.UUID);
                final Integer playerIndex = this.playerUniqueIdMap.inverse().get(playerUuid);
                if (playerIndex == null) {
                    this.playerUniqueIdMap.put(this.trackedUniqueIdCount++, playerUuid);
                } else {
                    playerIdList.removeTag(i);
                }
            }

        }
    }

    private void writeSpongeNbt() {
        // Never save Sponge data if we have no UUID
        if (this.uuid != null && this.isValid()) {
            this.spongeNbt.setInteger(NbtDataUtil.DATA_VERSION, DataUtil.DATA_VERSION);
            this.spongeNbt.setUniqueId(NbtDataUtil.UUID, this.uuid);
            this.spongeNbt.setInteger(NbtDataUtil.DIMENSION_ID, this.dimensionId);
            this.spongeNbt.setString(NbtDataUtil.DIMENSION_TYPE, this.dimensionType.getId());
            this.spongeNbt.setBoolean(NbtDataUtil.GENERATE_BONUS_CHEST, this.generateBonusChest);
            if (this.portalAgentType == null) {
                this.portalAgentType = PortalAgentTypes.DEFAULT;
            }
            this.spongeNbt.setString(NbtDataUtil.PORTAL_AGENT_TYPE, this.portalAgentType.getPortalAgentClass().getName());
            short saveBehavior = 1;
            if (this.serializationBehavior == SerializationBehaviors.NONE) {
                saveBehavior = -1;
            } else if (this.serializationBehavior == SerializationBehaviors.MANUAL) {
                saveBehavior = 0;
            }
            this.spongeNbt.setShort(NbtDataUtil.WORLD_SERIALIZATION_BEHAVIOR, saveBehavior);
            this.spongeNbt.setBoolean(NbtDataUtil.HAS_CUSTOM_DIFFICULTY, this.hasCustomDifficulty);
            final Iterator<UUID> iterator = this.pendingUniqueIds.iterator();
            final NBTTagList playerIdList = this.spongeNbt.getTagList(NbtDataUtil.SPONGE_PLAYER_UUID_TABLE, NbtDataUtil.TAG_COMPOUND);
            while (iterator.hasNext()) {
                final NBTTagCompound compound = new NBTTagCompound();
                compound.setUniqueId(NbtDataUtil.UUID, iterator.next());
                playerIdList.appendTag(compound);
                iterator.remove();
            }
        }
    }

    // In development, this methos is replaced
    // by the overwrite below.
    // In production, this method calls the (re-obfuscated)
    // overwritten method below
    public String worldproperties$getWorldName() {
        return this.getWorldName();
    }

    /**
     * @reason Some mods set a null levelName, which is incompatible with Spongne's policy
     * of never returning null from our API. Since this method has the same deobfuscated
     * name as an API method, I'm choosing to overwrite it to keep development and production
     * consistent. If we end up breaking mods because of this, we'll probably need to switch
     * to using a wrapepr type for WorldInfo
     *
     * @author Aaron1011 - August 9th, 2018
     */
    @Override
    @Overwrite
    public String getWorldName() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getDelegate().getWorldName();
        }
        if (this.levelName == null) {
            this.levelName = "";
        }
        return this.levelName;
    }

    @Override
    public DataContainer getAdditionalProperties() {
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getAPIDelegate().getAdditionalProperties();
        }
        NBTTagCompound additionalProperties = this.spongeRootLevelNbt.copy();
        additionalProperties.removeTag(SpongeImpl.ECOSYSTEM_NAME);
        return NbtTranslator.getInstance().translateFrom(additionalProperties);
    }

    private void saveConfig() {
        this.getOrCreateWorldConfig().save();
    }

    @Override
    public String toString() {
        // TODO May not delegate this call out
        if (this instanceof IMixinDerivedWorldInfo) {
            return ((IMixinDerivedWorldInfo) this).getDelegate().toString();
        }

        return MoreObjects.toStringHelper(this)
            .add("levelName", this.levelName)
            .add("terrainType", this.terrainType)
            .add("uuid", this.uuid)
            .add("dimensionId", this.dimensionId)
            .add("dimensionType", this.dimensionType)
            .add("spawnX", this.spawnX)
            .add("spawnY", this.spawnY)
            .add("spawnZ", this.spawnZ)
            .add("gameType", this.gameType)
            .add("hardcore", this.hardcore)
            .add("difficulty", this.difficulty)
            .add("isMod", this.isMod)
            .toString();
    }
}
