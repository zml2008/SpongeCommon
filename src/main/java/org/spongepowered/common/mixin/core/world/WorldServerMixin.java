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
package org.spongepowered.common.mixin.core.world;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEventData;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardSaveData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameType;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.ScheduledBlockUpdate;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.TileEntityType;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.CauseStackManager.StackFrame;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.action.LightningEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.world.ChangeWorldWeatherEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.api.world.GeneratorTypes;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.PortalAgentType;
import org.spongepowered.api.world.PortalAgentTypes;
import org.spongepowered.api.world.gen.BiomeGenerator;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.block.SpongeBlockSnapshotBuilder;
import org.spongepowered.common.bridge.TimingBridge;
import org.spongepowered.common.bridge.block.BlockBridge;
import org.spongepowered.common.bridge.block.BlockEventDataBridge;
import org.spongepowered.common.bridge.data.CustomDataHolderBridge;
import org.spongepowered.common.bridge.entity.EntityBridge;
import org.spongepowered.common.bridge.server.management.PlayerChunkMapBridge;
import org.spongepowered.common.bridge.tileentity.TileEntityBridge;
import org.spongepowered.common.bridge.world.ServerWorldBridge;
import org.spongepowered.common.bridge.world.WorldInfoBridge;
import org.spongepowered.common.bridge.world.WorldProviderBridge;
import org.spongepowered.common.bridge.world.chunk.ActiveChunkReferantBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkProviderBridge;
import org.spongepowered.common.bridge.world.chunk.ServerChunkProviderBridge;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.category.PhaseTrackerCategory;
import org.spongepowered.common.config.category.WorldCategory;
import org.spongepowered.common.config.type.WorldConfig;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.SpongeProxyBlockAccess;
import org.spongepowered.common.event.tracking.phase.block.BlockPhase;
import org.spongepowered.common.event.tracking.phase.general.GeneralPhase;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;
import org.spongepowered.common.event.tracking.phase.tick.TickPhase;
import org.spongepowered.common.bridge.world.NextTickListEntryBridge;
import org.spongepowered.common.interfaces.util.math.IMixinBlockPos;
import org.spongepowered.common.interfaces.world.gen.IPopulatorProvider;
import org.spongepowered.common.mixin.plugin.entityactivation.interfaces.ActivationCapability;
import org.spongepowered.common.mixin.plugin.entitycollisions.interfaces.CollisionsCapability;
import org.spongepowered.common.registry.provider.DirectionFacingProvider;
import org.spongepowered.common.registry.type.world.BlockChangeFlagRegistryModule;
import org.spongepowered.common.relocate.co.aikar.timings.TimingHistory;
import org.spongepowered.common.relocate.co.aikar.timings.WorldTimingsHandler;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.SpongeLocatableBlockBuilder;
import org.spongepowered.common.world.WorldManager;
import org.spongepowered.common.world.border.PlayerBorderListener;
import org.spongepowered.common.world.gen.SpongeChunkGenerator;
import org.spongepowered.common.world.gen.SpongeGenerationPopulator;
import org.spongepowered.common.world.gen.SpongeWorldGenerator;
import org.spongepowered.common.world.gen.WorldGenConstants;
import org.spongepowered.common.world.type.SpongeWorldType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

@Mixin(WorldServer.class)
public abstract class WorldServerMixin extends WorldMixin implements ServerWorldBridge {

    private final Map<net.minecraft.entity.Entity, Vector3d> rotationUpdates = new HashMap<>();
    private SpongeChunkGenerator spongegen;
    private long weatherStartTime;
    private Weather prevWeather;
    protected WorldTimingsHandler timings;
    private int chunkGCTickCount = 0;
    private int chunkGCLoadThreshold = 0;
    private int chunkGCTickInterval = Constants.World.CHUNK_GC_TICK_INTERVAL;
    private int chunkLoadCount = 0;
    private long chunkUnloadDelay = Constants.World.CHUNK_UNLOAD_DELAY;
    private boolean weatherThunderEnabled = true;
    private boolean weatherIceAndSnowEnabled = true;
    private int dimensionId;
    @Nullable private NextTickListEntry tmpScheduledObj;

    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private PlayerChunkMap playerChunkMap;
    @Shadow @Final @Mutable private Teleporter worldTeleporter;
    @Shadow @Final private WorldServer.ServerBlockEventList[] blockEventQueue;
    @Shadow private int blockEventCacheIndex;
    @Shadow private int updateEntityTick;

    @Shadow protected void saveLevel() throws MinecraftException { }
    @Shadow public abstract boolean fireBlockEvent(BlockEventData event);
    @Shadow protected abstract void createBonusChest();
    @Shadow public abstract PlayerChunkMap getPlayerChunkMap();
    @Shadow public abstract ChunkProviderServer getChunkProvider();
    @Shadow protected abstract void playerCheckLight();
    @Shadow protected abstract BlockPos adjustPosToNearbyEntity(BlockPos pos);
    @Shadow private boolean canAddEntity(final net.minecraft.entity.Entity entityIn) {
        return false; // Shadowed
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;setWorld(Lnet/minecraft/world/World;)V"))
    private void onSetWorld(final WorldProvider worldProvider, final World worldIn) {
        // Guarantees no mod has changed our worldInfo.
        // Mods such as FuturePack replace worldInfo with a custom one for separate world time.
        // This change is not needed as all worlds in Sponge use separate save handlers.
        final WorldInfo originalWorldInfo = worldIn.getWorldInfo();
        worldProvider.setWorld(worldIn);
        this.worldInfo = originalWorldInfo;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(final MinecraftServer server, final ISaveHandler saveHandlerIn, @Nullable WorldInfo info, final int dimensionId,
        final Profiler profilerIn, final CallbackInfo callbackInfo) {
        if (info == null) {
            SpongeImpl.getLogger().warn("World constructed without a WorldInfo! This will likely cause problems. Subsituting dummy info.",
                    new RuntimeException("Stack trace:"));
           info = new WorldInfo(new WorldSettings(0, GameType.NOT_SET, false, false, WorldType.DEFAULT),
                    "sponge$dummy_world");
        }
        // Checks to make sure no mod has changed our worldInfo and if so, reverts back to original.
        // Mods such as FuturePack replace worldInfo with a custom one for separate world time.
        // This change is not needed as all worlds use separate save handlers.
        this.worldInfo = info;
        this.timings = new WorldTimingsHandler((WorldServer) (Object) this);
        this.dimensionId = dimensionId;
        this.prevWeather = ((org.spongepowered.api.world.World) this).getWeather();
        this.weatherStartTime = this.worldInfo.getWorldTotalTime();
        this.getWorldBorder().addListener(new PlayerBorderListener(this.getMinecraftServer(), dimensionId));
        final PortalAgentType portalAgentType = ((WorldProperties) this.worldInfo).getPortalAgentType();
        if (!portalAgentType.equals(PortalAgentTypes.DEFAULT)) {
            try {
                this.worldTeleporter = (Teleporter) portalAgentType.getPortalAgentClass().getConstructor(new Class<?>[] {WorldServer.class})
                        .newInstance(new Object[] {this});
            } catch (Exception e) {
                SpongeImpl.getLogger().log(Level.ERROR, "Could not create PortalAgent of type " + portalAgentType.getId()
                                                        + " for world " + ((org.spongepowered.api.world.World) this).getName() + ": " + e.getMessage() + ". Falling back to default...");
            }
        }

        this.bridge$updateWorldGenerator();

        final WorldCategory worldCategory = ((WorldInfoBridge) this.getWorldInfo()).getConfigAdapter().getConfig().getWorld();
        this.chunkGCLoadThreshold = worldCategory.getChunkLoadThreshold();
        this.chunkGCTickInterval = worldCategory.getTickInterval();
        this.weatherIceAndSnowEnabled = worldCategory.getWeatherIceAndSnow();
        this.weatherThunderEnabled = worldCategory.getWeatherThunder();
        this.updateEntityTick = 0;
        this.setMemoryViewDistance(this.chooseViewDistanceValue(worldCategory.getViewDistance()));
    }

    @Redirect(method = "init", at = @At(value = "NEW", target = "net/minecraft/world/storage/MapStorage"))
    private MapStorage onCreateMapStorage(final ISaveHandler saveHandler) {
        final WorldServer overWorld = WorldManager.getWorldByDimensionId(0).orElse(null);
        // if overworld has loaded, use its mapstorage
        if (this.dimensionId != 0 && overWorld != null) {
            return overWorld.getMapStorage();
        }

        // if we are loading overworld, create a new mapstorage
        return new MapStorage(saveHandler);
    }

    // The following two redirects work around the fact that 'onCreateMapStorage' causes all worlds to share a single MapStorage.
    // Worlds other than the Overworld have scoreboard created, but they are never used. Therefore, we need to ensure that these unused scoreboards
    // are not saved into the global MapStorage when non-Overworld worlds are initialized.

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/MapStorage;setData(Ljava/lang/String;Lnet/minecraft/world/storage/WorldSavedData;)V"))
    private void onMapStorageSetData(final MapStorage storage, final String name, final WorldSavedData data) {
        if (name.equals("scoreboard") && this.dimensionId != 0) {
            return;
        }
        storage.setData(name, data);
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/ScoreboardSaveData;setScoreboard(Lnet/minecraft/scoreboard/Scoreboard;)V"))
    private void onSetSaveDataScoreboard(final ScoreboardSaveData scoreboardSaveData, final Scoreboard scoreboard) {
        if (this.dimensionId != 0) {
            return;
        }
        scoreboardSaveData.setScoreboard(scoreboard);
    }

    @Inject(method = "createSpawnPosition", at = @At(value = "HEAD"))
    private void onCreateBonusChest(final CallbackInfo ci) {
        GenerationPhase.State.TERRAIN_GENERATION.createPhaseContext()
                .source(this)
                .buildAndSwitch();
    }


    @Inject(method = "createSpawnPosition", at = @At(value = "RETURN"))
    private void onCreateBonusChestEnd(final CallbackInfo ci) {
        PhaseTracker.getInstance().getCurrentContext().close();
    }

    @Inject(method = "createSpawnPosition(Lnet/minecraft/world/WorldSettings;)V", at = @At("HEAD"), cancellable = true)
    private void onCreateSpawnPosition(final WorldSettings settings, final CallbackInfo ci) {
        final GeneratorType generatorType = (GeneratorType) settings.getTerrainType();

        // Allow bonus chest generation for non-Overworld worlds
        if (!this.provider.canRespawnHere() && ((org.spongepowered.api.world.World) this).getProperties().doesGenerateBonusChest()) {
            this.createBonusChest();
        }

        if ((generatorType != null && generatorType.equals(GeneratorTypes.THE_END)) || ((((WorldServer) (Object) this)).getChunkProvider().chunkGenerator instanceof ChunkGeneratorEnd)) {
            this.worldInfo.setSpawn(new BlockPos(100, 50, 0));
            ci.cancel();
        }
    }

    @Redirect(method = "createSpawnPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldSettings;isBonusChestEnabled()Z"))
    private boolean onIsBonusChestEnabled(final WorldSettings settings) {
        return ((org.spongepowered.api.world.World) this).getProperties().doesGenerateBonusChest();
    }

    @Override
    public boolean bridge$isMinecraftChunkLoaded(final int x, final int z, final boolean allowEmpty) {
        return this.isChunkLoaded(x, z, allowEmpty);
    }

    @Override
    public void bridge$updateConfigCache() {
        final SpongeConfig<WorldConfig> configAdapter = ((WorldInfoBridge) this.worldInfo).getConfigAdapter();

        // update cached settings
        final WorldCategory worldCategory = configAdapter.getConfig().getWorld();
        this.chunkGCLoadThreshold = worldCategory.getChunkLoadThreshold();
        this.chunkGCTickInterval = worldCategory.getTickInterval();
        this.weatherIceAndSnowEnabled = worldCategory.getWeatherIceAndSnow();
        this.weatherThunderEnabled = worldCategory.getWeatherThunder();
        this.chunkUnloadDelay = worldCategory.getChunkUnloadDelay() * 1000;
        if (this.getChunkProvider() != null) {
            final int maxChunkUnloads = worldCategory.getMaxChunkUnloads();
            ((ChunkProviderBridge) this.getChunkProvider()).bridge$setMaxChunkUnloads(maxChunkUnloads < 1 ? 1 : maxChunkUnloads);
            ((ServerChunkProviderBridge) this.getChunkProvider()).bridge$setDenyChunkRequests(worldCategory.getDenyChunkRequests());
            for (final net.minecraft.entity.Entity entity : this.loadedEntityList) {
                if (entity instanceof ActivationCapability) {
                    ((ActivationCapability) entity).activation$requiresActivationCacheRefresh(true);
                }
                if (entity instanceof CollisionsCapability) {
                    ((CollisionsCapability) entity).collision$requiresCollisionsCacheRefresh(true);
                }
            }
        }
    }

    @Override
    public void bridge$incrementChunkLoadCount() {
        if (this.chunkGCLoadThreshold > 0) {
            this.chunkLoadCount++;
        }
    }

    @Override
    public void bridge$updateWorldGenerator() {

        // Get the default generator for the world type
        final DataContainer generatorSettings = ((org.spongepowered.api.world.World) this).getProperties().getGeneratorSettings();

        final SpongeWorldGenerator newGenerator = this.bridge$createWorldGenerator(generatorSettings);
        // If the base generator is an IChunkProvider which implements
        // IPopulatorProvider we request that it add its populators not covered
        // by the base generation populator
        if (newGenerator.getBaseGenerationPopulator() instanceof IChunkGenerator) {
            // We check here to ensure that the IPopulatorProvider is one of our mixed in ones and not
            // from a mod chunk provider extending a provider that we mixed into
            if (WorldGenConstants.isValid((IChunkGenerator) newGenerator.getBaseGenerationPopulator(), IPopulatorProvider.class)) {
                ((IPopulatorProvider) newGenerator.getBaseGenerationPopulator()).addPopulators(newGenerator);
            }
        } else if (newGenerator.getBaseGenerationPopulator() instanceof IPopulatorProvider) {
            // If its not a chunk provider but is a populator provider then we call it as well
            ((IPopulatorProvider) newGenerator.getBaseGenerationPopulator()).addPopulators(newGenerator);
        }

        for (final WorldGeneratorModifier modifier : ((org.spongepowered.api.world.World) this).getProperties().getGeneratorModifiers()) {
            modifier.modifyWorldGenerator(((org.spongepowered.api.world.World) this).getProperties(), generatorSettings, newGenerator);
        }

        this.spongegen = this.bridge$createChunkGenerator(newGenerator);
        this.spongegen.setGenerationPopulators(newGenerator.getGenerationPopulators());
        this.spongegen.setPopulators(newGenerator.getPopulators());
        this.spongegen.setBiomeOverrides(newGenerator.getBiomeSettings());

        final ChunkProviderServer chunkProviderServer = this.getChunkProvider();
        chunkProviderServer.chunkGenerator = this.spongegen;
    }

    @Override
    public SpongeChunkGenerator bridge$createChunkGenerator(final SpongeWorldGenerator newGenerator) {
        return new SpongeChunkGenerator((net.minecraft.world.World) (Object) this, newGenerator.getBaseGenerationPopulator(),
                newGenerator.getBiomeGenerator());
    }

    @Override
    public SpongeWorldGenerator bridge$createWorldGenerator(final DataContainer settings) {
        // Minecraft uses a string for world generator settings
        // This string can be a JSON string, or be a string of a custom format

        // Try to convert to custom format
        final Optional<String> optCustomSettings = settings.getString(Constants.Sponge.World.WORLD_CUSTOM_SETTINGS);
        if (optCustomSettings.isPresent()) {
            return this.bridge$createWorldGenerator(optCustomSettings.get());
        }

        String jsonSettings = "";
        try {
            jsonSettings = DataFormats.JSON.write(settings);
        } catch (Exception e) {
            SpongeImpl.getLogger().warn("Failed to convert settings from [{}] for GeneratorType [{}] used by World [{}].", settings,
                    ((net.minecraft.world.World) (Object) this).getWorldType(), this, e);
        }

        return this.bridge$createWorldGenerator(jsonSettings);
    }

    @Override
    public SpongeWorldGenerator bridge$createWorldGenerator(final String settings) {
        final WorldServer worldServer = (WorldServer) (Object) this;
        final WorldType worldType = worldServer.getWorldType();
        final IChunkGenerator chunkGenerator;
        final BiomeProvider biomeProvider;
        if (worldType instanceof SpongeWorldType) {
            chunkGenerator = ((SpongeWorldType) worldType).getChunkGenerator(worldServer, settings);
            biomeProvider = ((SpongeWorldType) worldType).getBiomeProvider(worldServer);
        } else {
            final IChunkGenerator currentGenerator = this.getChunkProvider().chunkGenerator;
            if (currentGenerator != null) {
                chunkGenerator = currentGenerator;
            } else {
                final WorldProvider worldProvider = worldServer.provider;
                ((WorldProviderBridge) worldProvider).bridge$setGeneratorSettings(settings);
                chunkGenerator = worldProvider.createChunkGenerator();
            }
            biomeProvider = worldServer.provider.biomeProvider;
        }
        return new SpongeWorldGenerator(worldServer, (BiomeGenerator) biomeProvider, SpongeGenerationPopulator.of(worldServer, chunkGenerator));
    }
    /**
     * @author blood - February 20th, 2017
     * @reason Avoids loading unloaded chunk when checking for sky.
     *
     * @param pos The position to get the light for
     * @return Whether block position can see sky
     *
     * Technically an overwrite since it's overriding a method from {@link World#canSeeSky(BlockPos)}
     */
    @Override
    public boolean canSeeSky(BlockPos pos) {
        if (this.isFake()) {
            return super.canSeeSky(pos);
        }
        final net.minecraft.world.chunk.Chunk chunk = ((ChunkProviderBridge) this.getChunkProvider())
            .bridge$getLoadedChunkWithoutMarkingActive(pos.getX() >> 4, pos.getZ() >> 4);

        if (chunk == null || chunk.unloadQueued) {
            return false;
        }

        return chunk.canSeeSky(pos);
    }

    @Override
    protected void impl$getRawLightWithoutMarkingChunkActive(BlockPos pos, EnumSkyBlock enumSkyBlock, CallbackInfoReturnable<Integer> cir) {
        if (this.isFake()) {
            super.impl$getRawLightWithoutMarkingChunkActive(pos, enumSkyBlock, cir);
            return;
        }
        final Chunk chunk = ((ChunkProviderBridge) ((WorldServer) (Object) this).getChunkProvider())
            .bridge$getLoadedChunkWithoutMarkingActive(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || chunk.unloadQueued) {
            cir.setReturnValue(0);
        }
    }

    /**
     * @author blood - July 1st, 2016
     * @author gabizou - July 1st, 2016 - Update to 1.10 and cause tracking
     *
     * @reason Added chunk and block tick optimizations, timings, cause tracking, and pre-construction events.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Overwrite
    protected void updateBlocks() {
        this.playerCheckLight();

        if (this.worldInfo.getTerrainType() == WorldType.DEBUG_ALL_BLOCK_STATES)
        {
            final Iterator<net.minecraft.world.chunk.Chunk> iterator1 = this.playerChunkMap.getChunkIterator();

            while (iterator1.hasNext())
            {
                iterator1.next().onTick(false);
            }
            return; // Sponge: Add return
        }
        // else // Sponge - Remove unnecessary else
        // { //

        final int i = this.shadow$getGameRules().getInt("randomTickSpeed");
        final boolean flag = this.isRaining();
        final boolean flag1 = this.isThundering();
        this.profiler.startSection("pollingChunks");

        // Sponge: Use SpongeImplHooks for Forge
        for (final Iterator<net.minecraft.world.chunk.Chunk> iterator =
             SpongeImplHooks.getChunkIterator((WorldServer) (Object) this); iterator.hasNext(); ) // this.profiler.endSection()) // Sponge - don't use the profiler
        {
            this.profiler.startSection("getChunk");
            final net.minecraft.world.chunk.Chunk chunk = iterator.next();
            final net.minecraft.world.World world = chunk.getWorld();
            final int j = chunk.x * 16;
            final int k = chunk.z * 16;
            this.profiler.endStartSection("checkNextLight");
            this.timings.updateBlocksCheckNextLight.startTiming(); // Sponge - Timings
            chunk.enqueueRelightChecks();
            this.timings.updateBlocksCheckNextLight.stopTiming(); // Sponge - Timings
            this.profiler.endStartSection("tickChunk");
            this.timings.updateBlocksChunkTick.startTiming(); // Sponge - Timings
            chunk.onTick(false);
            this.timings.updateBlocksChunkTick.stopTiming(); // Sponge - Timings
            // Sponge start - if surrounding neighbors are not loaded, skip
            if (!((ChunkBridge) chunk).areNeighborsLoaded()) {
                continue;
            }
            // Sponge end
            this.profiler.endStartSection("thunder");
            // Sponge start
            this.timings.updateBlocksThunder.startTiming();

            // Sponge start - wrap call to canDoLightning in phase, since mods can run arbitrary code here

            try (final PhaseContext<?> context = TickPhase.Tick.WEATHER.createPhaseContext().source(this)) {
                context.buildAndSwitch();

                //if (this.provider.canDoLightning(chunk) && flag && flag1 && this.rand.nextInt(100000) == 0) // Sponge - Add SpongeImplHooks for forge
                if (this.weatherThunderEnabled && SpongeImplHooks.canDoLightning(this.provider, chunk) && flag && flag1
                        && this.rand.nextInt(100000) == 0) {

                    // Sponge end
                    this.updateLCG = this.updateLCG * 3 + 1013904223;
                    final int l = this.updateLCG >> 2;
                    final BlockPos blockpos = this.adjustPosToNearbyEntity(new BlockPos(j + (l & 15), 0, k + (l >> 8 & 15)));

                    if (this.isRainingAt(blockpos)) {
                        final DifficultyInstance difficultyinstance = this.getDifficultyForLocation(blockpos);

                        // Sponge - create a transform to be used for events
                        final Transform<org.spongepowered.api.world.World>
                                transform =
                                new Transform<>(((org.spongepowered.api.world.World) this), VecHelper.toVector3d(blockpos).toDouble());

                        if (world.getGameRules().getBoolean("doMobSpawning") && this.rand.nextDouble() < (double) difficultyinstance.getAdditionalDifficulty() * 0.01D) {
                            // Sponge Start - Throw construction events
                            try (final StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                                frame.pushCause(((org.spongepowered.api.world.World) this).getWeather());
                                frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.WEATHER);
                                final ConstructEntityEvent.Pre
                                        constructEntityEvent =
                                        SpongeEventFactory
                                                .createConstructEntityEventPre(frame.getCurrentCause(), EntityTypes.HORSE, transform);
                                SpongeImpl.postEvent(constructEntityEvent);
                                if (!constructEntityEvent.isCancelled()) {
                                    // Sponge End
                                    final EntitySkeletonHorse entityhorse = new EntitySkeletonHorse((WorldServer) (Object) this);
                                    entityhorse.setTrap(true);
                                    entityhorse.setGrowingAge(0);
                                    entityhorse.setPosition(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                                    this.spawnEntity(entityhorse);
                                    // Sponge Start - Throw a construct event for the lightning
                                }

                                final ConstructEntityEvent.Pre
                                        lightning =
                                        SpongeEventFactory
                                                .createConstructEntityEventPre(frame.getCurrentCause(), EntityTypes.LIGHTNING,
                                                        transform);
                                SpongeImpl.postEvent(lightning);
                                if (!lightning.isCancelled()) {
                                    final LightningEvent.Pre lightningPre = SpongeEventFactory.createLightningEventPre(frame.getCurrentCause());
                                    if (!SpongeImpl.postEvent(lightningPre)) {
                                        // Sponge End
                                        this.addWeatherEffect(new EntityLightningBolt(world, (double) blockpos.getX(), (double) blockpos.getY(),
                                                (double) blockpos.getZ(), true));
                                    }
                                } // Sponge - Brackets.
                            }
                        } else {
                            // Sponge start - Throw construction event for lightningbolts
                            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                                frame.pushCause(((org.spongepowered.api.world.World) this).getWeather());
                                frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.WEATHER);
                                final ConstructEntityEvent.Pre
                                        event =
                                        SpongeEventFactory.createConstructEntityEventPre(frame.getCurrentCause(),
                                                EntityTypes.LIGHTNING, transform);
                                SpongeImpl.postEvent(event);
                                if (!event.isCancelled()) {
                                    final LightningEvent.Pre lightningPre = SpongeEventFactory.createLightningEventPre(frame.getCurrentCause());
                                    if (!SpongeImpl.postEvent(lightningPre)) {
                                        // Sponge End
                                        this.addWeatherEffect(new EntityLightningBolt(world, (double) blockpos.getX(), (double) blockpos.getY(),
                                                (double) blockpos.getZ(), true));
                                    }
                                } // Sponge - Brackets.
                            }
                        }
                    }
                } // Sponge - brackets
                // Sponge End

                this.timings.updateBlocksThunder.stopTiming(); // Sponge - Stop thunder timing
                this.timings.updateBlocksIceAndSnow.startTiming(); // Sponge - Start thunder timing
                this.profiler.endStartSection("iceandsnow");

                // if (this.rand.nextInt(16) == 0) // Sponge - Rewrite to use our boolean, and forge hook
                if (this.weatherIceAndSnowEnabled && SpongeImplHooks.canDoRainSnowIce(this.provider, chunk) && this.rand.nextInt(16) == 0) {
                    // Sponge Start - Enter weather phase for snow and ice and flooding.
                    // Sponge End
                    this.updateLCG = this.updateLCG * 3 + 1013904223;
                    final int j2 = this.updateLCG >> 2;
                    final BlockPos blockpos1 = this.getPrecipitationHeight(new BlockPos(j + (j2 & 15), 0, k + (j2 >> 8 & 15)));
                    final BlockPos blockpos2 = blockpos1.down();

                    if (this.canBlockFreezeNoWater(blockpos2)) {
                        this.setBlockState(blockpos2, Blocks.ICE.getDefaultState());
                    }

                    if (flag && this.canSnowAt(blockpos1, true)) {
                        this.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState());
                    }

                    if (flag && this.getBiome(blockpos2).canRain()) {
                        this.getBlockState(blockpos2).getBlock().fillWithRain((WorldServer) (Object) this, blockpos2);
                    }
                }
            } // Sponge end phase - brackets

            this.timings.updateBlocksIceAndSnow.stopTiming(); // Sponge - Stop ice and snow timing
            this.timings.updateBlocksRandomTick.startTiming(); // Sponge - Start random block tick timing
            this.profiler.endStartSection("tickBlocks");

            if (i > 0)
            {
                for (final ExtendedBlockStorage extendedblockstorage : chunk.getBlockStorageArray())
                {
                    if (extendedblockstorage != net.minecraft.world.chunk.Chunk.NULL_BLOCK_STORAGE && extendedblockstorage.needsRandomTick())
                    {
                        for (int i1 = 0; i1 < i; ++i1)
                        {
                            this.updateLCG = this.updateLCG * 3 + 1013904223;
                            final int j1 = this.updateLCG >> 2;
                            final int k1 = j1 & 15;
                            final int l1 = j1 >> 8 & 15;
                            final int i2 = j1 >> 16 & 15;
                            final IBlockState iblockstate = extendedblockstorage.get(k1, i2, l1);
                            final Block block = iblockstate.getBlock();
                            this.profiler.startSection("randomTick");

                            if (block.getTickRandomly())
                            {
                                // Sponge start - capture random tick
                                // Remove the random tick for cause tracking
                                // block.randomTick(this, new BlockPos(k1 + j, i2 + extendedblockstorage.getYLocation(), l1 + k), iblockstate, this.rand);

                                final BlockPos pos = new BlockPos(k1 + j, i2 + extendedblockstorage.getYLocation(), l1 + k);
                                final BlockBridge spongeBlock = (BlockBridge) block;
                                spongeBlock.getTimingsHandler().startTiming();
                                final PhaseContext<?> context = PhaseTracker.getInstance().getCurrentContext();
                                final IPhaseState phaseState = context.state;
                                if (phaseState.alreadyCapturingBlockTicks(context)) {
                                    block.randomTick(world, pos, iblockstate, this.rand);
                                } else {
                                    TrackingUtil.randomTickBlock(this, block, pos, iblockstate, this.rand);
                                }
                                spongeBlock.getTimingsHandler().stopTiming();
                                // Sponge end
                            }

                            this.profiler.endSection();
                        }
                    }
                }
            }
        }

        this.timings.updateBlocksRandomTick.stopTiming(); // Sponge - Stop random block timing
         this.profiler.endSection();
        // } // Sponge- Remove unnecessary else
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/WorldInfo;setDifficulty(Lnet/minecraft/world/EnumDifficulty;)V"))
    private void syncDifficultyDueToHardcore(final WorldInfo worldInfo, final EnumDifficulty newDifficulty) {
        WorldManager.adjustWorldForDifficulty((WorldServer) (Object) this, newDifficulty, false);
    }

    @Redirect(method = "updateBlockTick", at = @At(value = "INVOKE", target= "Lnet/minecraft/world/WorldServer;isAreaLoaded(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean onBlockTickIsAreaLoaded(final WorldServer worldIn, final BlockPos fromPos, final BlockPos toPos) {
        int posX = fromPos.getX() + 8;
        int posZ = fromPos.getZ() + 8;
        // Forge passes the same block position for forced chunks
        if (fromPos.equals(toPos)) {
            posX = fromPos.getX();
            posZ = fromPos.getZ();
        }
        final net.minecraft.world.chunk.Chunk chunk = ((ChunkProviderBridge) this.getChunkProvider()).bridge$getLoadedChunkWithoutMarkingActive(posX >> 4, posZ >> 4);
        return chunk != null && ((ChunkBridge) chunk).areNeighborsLoaded();
    }

    /**
     * @author gabizou - July 8th, 2018
     * @reason Performs a check on the block update to take place whether it will be
     * immediately scheduled, and then whether we need to enter {@link TickPhase.Tick#BLOCK} for
     * the scheduled update. Likewise, this will check whether scheduled updates are immediate
     * for this method call and then flip the flag off to avoid nested recursion.
     *
     * @param block The block to update
     * @param worldIn The world server, otherwise known as "this" object
     * @param pos The position
     * @param state The block state
     * @param rand The random, otherwise known as "this.rand"
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(
        method = "updateBlockTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"
        )
    )
    private void spongeBlockUpdateTick(final Block block, final World worldIn, final BlockPos pos, final IBlockState state, final Random rand) {
        if (this.scheduledUpdatesAreImmediate) {
            /*
            The reason why we are first checking and then resetting the immediate updates flag is
            because Vanilla will allow block updates to be performed "immediately", but certain blocks
            will recursively update neighboring blocks/change neighboring blocks such that it can cause
            a near infinite recursion in a "blob" of re-entrance. This avoids nested immediate block updates
            within the same method call of the immediate block update.
            See: https://github.com/SpongePowered/SpongeForge/issues/2273 for further explanation
             */
            this.scheduledUpdatesAreImmediate = false;
        }
        final PhaseContext<?> context = PhaseTracker.getInstance().getCurrentContext();
        final IPhaseState<?> phaseState = context.state;
        if (((IPhaseState) phaseState).alreadyCapturingBlockTicks(context) || ((IPhaseState) phaseState).ignoresBlockUpdateTick(context)) {
            block.updateTick(worldIn, pos, state, rand);
            return;
        }
        TrackingUtil.updateTickBlock(this, block, pos, state, rand);

    }

    @Redirect(method = "updateBlockTick", // really scheduleUpdate
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/NextTickListEntry;setPriority(I)V"))
    private void onCreateScheduledBlockUpdate(final NextTickListEntry sbu, final int priority) {
        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        final IPhaseState<?> phaseState = phaseTracker.getCurrentState();

        if (phaseState.ignoresScheduledUpdates()) {
            this.tmpScheduledObj = sbu;
            return;
        }

        sbu.setPriority(priority);
        ((NextTickListEntryBridge) sbu).bridge$setWorld((WorldServer) (Object) this);
        this.tmpScheduledObj = sbu;
    }

    /**
     * @author blood - August 30th, 2016
     *
     * @reason Always allow entity cleanup to occur. This prevents issues such as a plugin
     *         generating chunks with no players causing entities not getting cleaned up.
     */
    @Override
    @Overwrite
    public void updateEntities() {
        // Sponge start
        /*
        if (this.playerEntities.isEmpty()) {
            if (this.updateEntityTick++ >= 300) {
                return;
            }
        } else {
            this.resetUpdateEntityTick();
        }*/

        TrackingUtil.tickWorldProvider(this);
        // Sponge end
        super.updateEntities();
    }

    // This ticks pending updates to blocks, Requires mixin for NextTickListEntry so we use the correct tracking
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(method = "tickUpdates",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;updateTick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Ljava/util/Random;)V"
        )
    )
    private void onUpdateTick(final Block block, final net.minecraft.world.World worldIn, final BlockPos pos, final IBlockState state, final Random rand) {
        final PhaseContext<?> context = PhaseTracker.getInstance().getCurrentContext();
        final IPhaseState phaseState = context.state;
        if (phaseState.alreadyCapturingBlockTicks(context) || phaseState.ignoresBlockUpdateTick(context)) {
            block.updateTick(worldIn, pos, state, rand);
            return;
        }
        TrackingUtil.updateTickBlock(this, block, pos, state, rand);
    }

    @Redirect(
        method = "tickUpdates",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/crash/CrashReportCategory;addBlockInfo(Lnet/minecraft/crash/CrashReportCategory;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)V"
        )
    )
    private void onBlockInfo(final CrashReportCategory category, final BlockPos pos, final IBlockState state) {
        try {
            CrashReportCategory.addBlockInfo(category, pos, state);
        } catch (NoClassDefFoundError e) {
            SpongeImpl.getLogger().error("An error occurred while adding crash report info!", e);
            SpongeImpl.getLogger().error("Original caught error:", category.crashReport.cause);
            throw new ReportedException(category.crashReport);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "addBlockEvent",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/WorldServer$ServerBlockEventList;add(Ljava/lang/Object;)Z",
            remap = false
        )
    )
    private boolean onAddBlockEvent(final WorldServer.ServerBlockEventList list, final Object obj, final BlockPos pos, final Block blockIn, final int eventId, final int eventParam) {
        final BlockEventData blockEventData = (BlockEventData) obj;
        final BlockEventDataBridge blockEvent = (BlockEventDataBridge) blockEventData;
        final PhaseContext<?> currentContext = PhaseTracker.getInstance().getCurrentContext();
        final IPhaseState phaseState = currentContext.state;
        // Short circuit phase states who do not track during block events
        if (phaseState.ignoresBlockEvent()) {
            return list.add(blockEventData);
        }

        if (((BlockBridge) blockIn).shouldFireBlockEvents()) {
            blockEvent.setBridge$sourceUser(currentContext.getActiveUser());
            if (SpongeImplHooks.hasBlockTileEntity(blockIn, getBlockState(pos))) {
                blockEvent.setBridge$TileEntity((TileEntity) getTileEntity(pos));
            }
            if (blockEvent.getBridge$TileEntity() == null) {
                final LocatableBlock locatable = new SpongeLocatableBlockBuilder()
                    .world(((org.spongepowered.api.world.World) this))
                    .position(pos.getX(), pos.getY(), pos.getZ())
                    .state((BlockState) this.getBlockState(pos))
                    .build();
                blockEvent.setBridge$TickingLocatable(locatable);
            }
        }

        // Short circuit any additional handling. We've associated enough with the BlockEvent to
        // allow tracking to take place for other/future phases
        if (!((BlockBridge) blockIn).shouldFireBlockEvents()) {
            return list.add((BlockEventData) obj);
        }
        // Occasionally, we have a phase state that will want to just capture the block events
        // and then decides to "add" them after the fact.
        if (phaseState.doesBulkBlockCapture(currentContext)) {
            if (currentContext.getCapturedBlockSupplier().trackEvent(pos, blockEventData)) {
                return true;
            }
        }
        try (final PhaseContext<?> context = BlockPhase.State.BLOCK_EVENT_QUEUE.createPhaseContext()
                .source(blockEvent)) {
            context.buildAndSwitch();
            phaseState.appendNotifierToBlockEvent(currentContext, context, this, pos, blockEvent);

            // We fire a Pre event to make sure our captures do not get stuck in a loop.
            // This is very common with pistons as they add block events while blocks are being notified.
            if (ShouldFire.CHANGE_BLOCK_EVENT_PRE) {
                if (blockIn instanceof BlockPistonBase) {
                    // We only fire pre events for pistons
                    if (SpongeCommonEventFactory.handlePistonEvent(this, list, obj, pos, blockIn, eventId, eventParam)) {
                        return false;
                    }

                } else {
                    BlockSnapshot notifySource = null;
                    if (!((BlockBridge) blockIn).isVanilla() && currentContext.getNeighborNotificationSource() != null) {
                        notifySource = currentContext.getNeighborNotificationSource();
                    }
                    final BlockPos notificationPos = notifySource != null ? VecHelper.toBlockPos(notifySource.getLocation().get()) : pos;
                    if (SpongeCommonEventFactory.callChangeBlockEventPre(this, notificationPos).isCancelled()) {
                        return false;
                    }
                }
            }

            // If we are capturing block positions, we need to check if the block position has any scheduled events
            // so they will be properly added after the fact.
            return list.add(blockEventData);
        }
    }

    @Redirect(method = "sendQueuedBlockEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/WorldServer;fireBlockEvent(Lnet/minecraft/block/BlockEventData;)Z"))
    private boolean onFireBlockEvent(final net.minecraft.world.WorldServer worldIn, final BlockEventData event) {
        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        final IPhaseState<?> phaseState = phaseTracker.getCurrentState();
        if (phaseState.ignoresBlockEvent()) {
            return fireBlockEvent(event);
        }
        return TrackingUtil.fireMinecraftBlockEvent(worldIn, event);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Nullable
    protected net.minecraft.tileentity.TileEntity getTileEntityForRemoval(final World thisWorld, final BlockPos pos) {
        if (this.isFake()) {
            return super.getTileEntityForRemoval(thisWorld, pos); // do nothing if we're not a sponge managed world
        }
        // We use the proxy access to proceed whether a TileEntity is marked as "there" or "not", because
        // transactions and processing neighbors/blockbreaks will end up causing chain reactions to
        // occur where we may end up losing a tile entity in place.
        // This has to occur before phase state checks because the tile entity could be unintentionally removed
        // from the world before we are able to capture it.
        net.minecraft.tileentity.TileEntity tileEntity =  this.proxyBlockAccess.getTileEntity(pos);
        if (tileEntity == null && !this.proxyBlockAccess.isTileEntityRemoved(pos)) {
            tileEntity = this.onChunkGetTileDuringRemoval(pos);
        }



        final PhaseTracker tracker = PhaseTracker.getInstance();
        final IPhaseState currentState = tracker.getCurrentState();
        final PhaseContext<?> currentContext = tracker.getCurrentContext();
        // More fast checks - bulk block capture is normally faster to be false than checking tile entity changes (certain block ticks don't capture changes)
        if (!ShouldFire.CHANGE_BLOCK_EVENT || !currentState.doesBulkBlockCapture(currentContext) || !currentState.tracksTileEntityChanges(currentContext)) {
            return tileEntity;
        }

        if (tileEntity != null) {
            ((TileEntityBridge) tileEntity).setCaptured(true);
            currentContext.getCapturedBlockSupplier().logTileChange(this, pos, tileEntity, null);
        }
        return tileEntity;
    }

    /**
     * @author gabizou - March 5th, 2019 - 1.12.2
     * @reason We can check the proxy block access whether the tile
     * entity is available and if it is available, if it's queued.
     * Note that we can take advantage of the fact that the tile
     * entity provided as a parameter will be possibly null, but
     * nonetheless, we'll be able to check for enqueuing at the
     * proxy level.
     *
     * @param pos The position
     * @param ci The cancellable
     * @param foundTile The found tile
     */
    @Override
    protected void onCheckTileEntityForRemoval(final BlockPos pos, final CallbackInfo ci, @Nullable final net.minecraft.tileentity.TileEntity foundTile, final net.minecraft.world.World thisWorld, final BlockPos samePos) {
        if (PhaseTracker.getInstance().getCurrentState().isRestoring()) {
            return;
        }
        if (foundTile != null) {
            // Easy short circuit.
            if (((TileEntityBridge) foundTile).isCaptured()) {
                ci.cancel();
            }
            if (this.proxyBlockAccess.isTileQueuedForRemoval(pos, foundTile)) {
                ci.cancel();
            }
        } else {
            if (!this.proxyBlockAccess.getQueuedTiles(pos).isEmpty()) {
                ci.cancel();
            } else if (this.proxyBlockAccess.isTileEntityRemoved(pos)) {
                ci.cancel();
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected boolean onSetTileEntityForCapture(final net.minecraft.tileentity.TileEntity newTile, final BlockPos pos, final net.minecraft.tileentity.TileEntity sameEntity) {
        if (this.isFake()) {
            // Short Circuit for fake worlds
            return newTile.isInvalid();
        }
        final PhaseTracker tracker = PhaseTracker.getInstance();
        final IPhaseState currentState = tracker.getCurrentState();
        final PhaseContext<?> currentContext = tracker.getCurrentContext();
        final TileEntityBridge mixinTile = (TileEntityBridge) newTile;
        if (mixinTile.isCaptured()) {
            // Don't do anything. If the tile entity is captured,
            // We return true to mark the tile entity as invalid, that way
            // no logic is performed until the transaction is processed.
            // Except in the circumstance that a tile entity is being re-set back onto
            // itself, at which point we'd want to double check that it's
            // not queued for removal. If it is queued for removal, then
            // we need to re-capture the tile entity replacement.
            if (this.proxyBlockAccess.hasTileEntity(pos, newTile)) {
                if (!this.proxyBlockAccess.isTileQueuedForRemoval(pos, newTile)) {
                    return true;
                }
            }
        } else if (this.proxyBlockAccess.hasTileEntity(pos, newTile)) {
            if (this.proxyBlockAccess.succeededInAdding(pos, newTile)) {
                this.addTileEntity(newTile);
            }
            // The chunk already has the tile entity at this point.
            return true;
        }
        // More fast checks - bulk block capture is normally faster to be false than checking tile entity changes (certain block ticks don't capture changes)
        if (!ShouldFire.CHANGE_BLOCK_EVENT || !currentState.doesBulkBlockCapture(currentContext) || !currentState.tracksTileEntityChanges(currentContext)) {
            return newTile.isInvalid();
        }
        if (!mixinTile.isCaptured()) {
            mixinTile.setCaptured(true);
        }

        // We cannot call getTileEntity because we run the risk of recursive re-entrance
        // when the chunk is being told to create the tile entity immediately, which calls
        // another set. So, this is done strictly to call getting the tile entity by proxy,
        // or calling to get the tile entity from the chunk as a CHECK
        net.minecraft.tileentity.TileEntity currentTile = this.getProcessingTileFromProxy(pos);

        if (currentTile == null && !this.proxyBlockAccess.isTileEntityRemoved(pos)) { // Make sure we're not getting a tile while it's being removed.
            currentTile = this.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        }
        newTile.setPos(pos);
        if (newTile.getWorld() != (WorldServer) (Object) this) {
            newTile.setWorld((WorldServer) (Object) this);
        }
        currentContext.getCapturedBlockSupplier().logTileChange(this, pos, currentTile, newTile);
        // We want to mark the tile as "invalid" only so that it does not get added to the world/chunk
        // just yet. Otherwise, some extra logic is involved in invalidating/revalidating the tile entity
        // after the fact.
        return true;
    }

    @Override
    protected boolean isTileMarkedForRemoval(final BlockPos pos) {
        return this.proxyBlockAccess.isTileEntityRemoved(pos);
    }

    @Override
    protected boolean isTileMarkedAsNull(final BlockPos pos, final net.minecraft.tileentity.TileEntity tileentity) {
        return this.proxyBlockAccess.hasTileEntity(pos) && this.proxyBlockAccess.getTileEntity(pos) == null;
    }

    @Override
    @Nullable
    protected net.minecraft.tileentity.TileEntity getProcessingTileFromProxy(final BlockPos pos) {
        return this.proxyBlockAccess.getTileEntity(pos);
    }

    @Nullable
    @Override
    protected net.minecraft.tileentity.TileEntity getQueuedRemovedTileFromProxy(final BlockPos pos) {
        return this.proxyBlockAccess.getQueuedTileForRemoval(pos);
    }

    // Chunk GC
    @Override
    public void doChunkGC() {
        this.chunkGCTickCount++;

        if (this.chunkLoadCount >= this.chunkGCLoadThreshold && this.chunkGCLoadThreshold > 0) {
            this.chunkLoadCount = 0;
        } else if (this.chunkGCTickCount >= this.chunkGCTickInterval && this.chunkGCTickInterval > 0) {
            this.chunkGCTickCount = 0;
        } else {
            return;
        }

        final ChunkProviderServer chunkProviderServer = this.getChunkProvider();
        for (final net.minecraft.world.chunk.Chunk chunk : chunkProviderServer.getLoadedChunks()) {
            final ChunkBridge spongeChunk = (ChunkBridge) chunk;
            if (chunk.unloadQueued || spongeChunk.isPersistedChunk() || !this.provider.canDropChunk(chunk.x, chunk.z)) {
                continue;
            }

            // If a player is currently using the chunk, skip it
            if (((PlayerChunkMapBridge) this.getPlayerChunkMap()).bridge$isChunkInUse(chunk.x, chunk.z)) {
                continue;
            }

            // If we reach this point the chunk leaked so queue for unload
            chunkProviderServer.queueUnload(chunk);
            SpongeHooks.logChunkGCQueueUnload(chunkProviderServer.world, chunk);
        }
    }


    @Inject(method = "saveLevel", at = @At("HEAD"))
    private void onSaveLevel(final CallbackInfo ci) {
        // Always call the provider's onWorldSave method as we do not use WorldServerMulti
        for (final WorldServer worldServer : this.server.worlds) {
            worldServer.provider.onWorldSave();
        }
    }

    /**
     * @author blood - July 20th, 2017
     * @reason This method is critical as it handles world saves and whether to queue chunks for unload if GC is enabled.
     * It has been overwritten to make it easier to manage for future updates.
     *
     * @param all Whether to save all chunks
     * @param progressCallback The save progress callback
     */
    @Overwrite
    public void saveAllChunks(final boolean all, @Nullable final IProgressUpdate progressCallback) throws MinecraftException
    {
        final ChunkProviderServer chunkproviderserver = this.getChunkProvider();

        if (chunkproviderserver.canSave())
        {
            final Cause currentCause = Sponge.getCauseStackManager().getCurrentCause();
            Sponge.getEventManager().post(SpongeEventFactory.createSaveWorldEventPre(currentCause, ((org.spongepowered.api.world.World) this)));
            if (progressCallback != null)
            {
                progressCallback.displaySavingString("Saving level");
            }

            this.saveLevel();

            if (progressCallback != null)
            {
                progressCallback.displayLoadingString("Saving chunks");
            }

            chunkproviderserver.saveChunks(all);
            Sponge.getEventManager().post(SpongeEventFactory.createSaveWorldEventPost(currentCause, ((org.spongepowered.api.world.World) this)));

            // The chunk GC handles all queuing for chunk unloads so we return here to avoid it during a save.
            if (this.chunkGCTickInterval > 0) {
                return;
            }

            for (final Chunk chunk : Lists.newArrayList(chunkproviderserver.getLoadedChunks()))
            {
                if (chunk != null && !this.playerChunkMap.contains(chunk.x, chunk.z))
                {
                    chunkproviderserver.queueUnload(chunk);
                }
            }
        }
    }

    @Redirect(method = "sendQueuedBlockEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/DimensionType;getId()I"), expect = 0, require = 0)
    private int onGetDimensionIdForBlockEvents(final DimensionType dimensionType) {
        return this.bridge$getDimensionId();
    }


    @Redirect(method = "updateAllPlayersSleepingFlag()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;isSpectator()Z"))
    private boolean isSpectatorOrIgnored(final EntityPlayer entityPlayer) {
        // spectators are excluded from the sleep tally in vanilla
        // this redirect expands that check to include sleep-ignored players as well
        final boolean ignore = entityPlayer instanceof Player && ((Player)entityPlayer).isSleepingIgnored();
        return ignore || entityPlayer.isSpectator();
    }

    @Redirect(method = "areAllPlayersAsleep()Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;isPlayerFullyAsleep()Z"))
    private boolean isPlayerFullyAsleep(final EntityPlayer entityPlayer) {
        // if isPlayerFullyAsleep() returns false areAllPlayerAsleep() breaks its loop and returns false
        // this redirect forces it to return true if the player is sleep-ignored even if they're not sleeping
        final boolean ignore = entityPlayer instanceof Player && ((Player)entityPlayer).isSleepingIgnored();
        return ignore || entityPlayer.isPlayerFullyAsleep();
    }

    @Redirect(method = "areAllPlayersAsleep()Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;isSpectator()Z"))
    private boolean isSpectatorAndNotIgnored(final EntityPlayer entityPlayer) {
        // if a player is marked as a spectator areAllPlayersAsleep() breaks its loop and returns false
        // this redirect forces it to return false if a player is sleep-ignored even if they're a spectator
        final boolean ignore = entityPlayer instanceof Player && ((Player)entityPlayer).isSleepingIgnored();
        return !ignore && entityPlayer.isSpectator();
    }



    /**
     * @author gabizou - April 24th, 2016
     * @reason Needs to redirect the dimension id for the packet being sent to players
     * so that the dimension is correctly adjusted
     *
     * @param id The world provider's dimension id
     * @return True if the spawn was successful and the effect is played.
     */
    // We expect 0 because forge patches it correctly
    @Redirect(method = "addWeatherEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/DimensionType;getId()I"), expect = 0, require = 0)
    private int getDimensionIdForWeatherEffect(final DimensionType id) {
        return this.bridge$getDimensionId();
    }

    /**
     * @author gabizou - February 7th, 2016
     * @author gabizou - September 3rd, 2016 - Moved from WorldMixin since WorldServer overrides the method.
     *
     * This will short circuit all other patches such that we control the
     * entities being loaded by chunkloading and can throw our bulk entity
     * event. This will bypass Forge's hook for individual entity events,
     * but the SpongeModEventManager will still successfully throw the
     * appropriate event and cancel the entities otherwise contained.
     *
     * @param entities The entities being loaded
     * @param callbackInfo The callback info
     */
    @Final
    @Inject(method = "loadEntities", at = @At("HEAD"), cancellable = true)
    private void spongeLoadEntities(final Collection<net.minecraft.entity.Entity> entities, final CallbackInfo callbackInfo) {
        if (entities.isEmpty()) {
            // just return, no entities to load!
            callbackInfo.cancel();
            return;
        }
        final List<Entity> entityList = new ArrayList<>();
        for (final net.minecraft.entity.Entity entity : entities) {
            // Make sure no entities load in invalid positions
            if (((IMixinBlockPos) entity.getPosition()).isInvalidYPosition()) {
                entity.setDead();
                continue;
            }
            if (this.canAddEntity(entity)) {
                entityList.add((Entity) entity);
            }
        }
        try (final StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.CHUNK_LOAD);
            frame.pushCause(this);
            final SpawnEntityEvent.ChunkLoad chunkLoad = SpongeEventFactory.createSpawnEntityEventChunkLoad(Sponge.getCauseStackManager().getCurrentCause(), Lists.newArrayList(entityList));
            SpongeImpl.postEvent(chunkLoad);
            if (!chunkLoad.isCancelled() && chunkLoad.getEntities().size() > 0) {
                for (final Entity successful : chunkLoad.getEntities()) {
                    this.loadedEntityList.add((net.minecraft.entity.Entity) successful);
                    this.onEntityAdded((net.minecraft.entity.Entity) successful);
                }
            }
            // Remove entities from chunk/world that were filtered in event
            // This prevents invisible entities from loading into the world and blocking the position.
            for (final Entity entity : entityList) {
                if (!chunkLoad.getEntities().contains(entity)) {
                    ((net.minecraft.world.World) (Object) this).removeEntityDangerously((net.minecraft.entity.Entity) entity);
                }
            }
            callbackInfo.cancel();
        }
    }

    /**
     * Based off {@link WorldServer#newExplosion(net.minecraft.entity.Entity, double, double, double, float, boolean, boolean)}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Explosion triggerInternalExplosion(org.spongepowered.api.world.explosion.Explosion explosion,
            final Function<Explosion, PhaseContext<?>> contextCreator) {
        // Sponge start
        final Explosion originalExplosion = (Explosion) explosion;
        // Set up the pre event
        final ExplosionEvent.Pre
                event =
                SpongeEventFactory.createExplosionEventPre(Sponge.getCauseStackManager().getCurrentCause(),
                        explosion, ((org.spongepowered.api.world.World) this));
        if (SpongeImpl.postEvent(event)) {
            return (Explosion) explosion;
        }
        explosion = event.getExplosion();
        final Explosion mcExplosion;
        try {
            // Since we already have the API created implementation Explosion, let's use it.
            mcExplosion = (Explosion) explosion;
        } catch (Exception e) {
            new PrettyPrinter(60).add("Explosion not compatible with this implementation").centre().hr()
                    .add("An explosion that was expected to be used for this implementation does not")
                    .add("originate from this implementation.")
                    .add(e)
                    .trace();
            return originalExplosion;
        }

        try (final PhaseContext<?> ignored = contextCreator.apply(mcExplosion)
                .source(((Optional) explosion.getSourceExplosive()).orElse(this))) {
            ignored.buildAndSwitch();
            final double x = mcExplosion.x;
            final double y = mcExplosion.y;
            final double z = mcExplosion.z;
            final boolean damagesTerrain = explosion.shouldBreakBlocks();
            final float strength = explosion.getRadius();
            // Sponge End

            mcExplosion.doExplosionA();
            mcExplosion.doExplosionB(false);

            if (!damagesTerrain) {
                mcExplosion.clearAffectedBlockPositions();
            }

            // Sponge Start - Don't send explosion packets, they're spammy, we can replicate it on the server entirely
            /*
            for (EntityPlayer entityplayer : this.playerEntities) {
                if (entityplayer.getDistanceSq(x, y, z) < 4096.0D) {
                    ((EntityPlayerMP) entityplayer).connection
                        .sendPacket(new SPacketExplosion(x, y, z, strength, mcExplosion.getAffectedBlockPositions(),
                            mcExplosion.getPlayerKnockbackMap().get(entityplayer)));
                }
            }

             */
            // Use the knockback map and set velocities, since the explosion packet isn't sent, we need to replicate
            // the players being moved.
            for (final EntityPlayer playerEntity : this.playerEntities) {
                final Vec3d knockback = mcExplosion.getPlayerKnockbackMap().get(playerEntity);
                if (knockback != null) {
                    // In Vanilla, doExplosionB always updates the 'motion[xyz]' fields for every entity in range.
                    // However, this field is completely ignored for players (since 'velocityChanged') is never set, and
                    // a completely different value is sent through 'SPacketExplosion'.

                    // To replicate this behavior, we manually send a velocity packet. It's critical that we don't simply
                    // add to the 'motion[xyz]' fields, as that will end up using the value set by 'doExplosionB', which must be
                    // ignored.
                    ((EntityPlayerMP) playerEntity).connection.sendPacket(new SPacketEntityVelocity(playerEntity.getEntityId(), knockback.x, knockback.y, knockback.z));
                }
            }
            // Sponge End

        }
        // Sponge End
        return mcExplosion;
    }

    // ------------------------- Start Cause Tracking overrides of Minecraft World methods ----------

    /**
     * @author gabizou March 11, 2016
     *
     * The train of thought for how spawning is handled:
     * 1) This method is called in implementation
     * 2) handleVanillaSpawnEntity is called to associate various contextual SpawnCauses
     * 3) {@link PhaseTracker#spawnEntity(WorldServer, net.minecraft.entity.Entity)} is called to
     *    check if the entity is to
     *    be "collected" or "captured" in the current {@link PhaseContext} of the current phase
     * 4) If the entity is forced or is captured, {@code true} is returned, otherwise, the entity is
     *    passed along normal spawning handling.
     */
    @Override
    public boolean spawnEntity(final net.minecraft.entity.Entity entity) {
        if (PhaseTracker.isEntitySpawnInvalid((Entity) entity)) {
            return true;
        }
        return canAddEntity(entity) && PhaseTracker.getInstance().spawnEntity((WorldServer) (Object) this, entity);
    }


    /**
     * @author gabizou, March 12th, 2016
     *
     * Move this into WorldServer as we should not be modifying the client world.
     *
     * Purpose: Rewritten to support capturing blocks
     */
    @Override
    public boolean setBlockState(final BlockPos pos, final IBlockState newState, final int flags) {
        if (!this.isValid(pos)) {
            return false;
        } else if (this.worldInfo.getTerrainType() == WorldType.DEBUG_ALL_BLOCK_STATES) { // isRemote is always false since this is WorldServer
            return false;
        } else {
            // Sponge - reroute to the PhaseTracker
            return PhaseTracker.getInstance().setBlockState(this, pos.toImmutable(), newState, BlockChangeFlagRegistryModule.fromNativeInt(flags));
        }
    }

    @Override
    public boolean bridge$setBlockState(final BlockPos pos, final IBlockState state, final BlockChangeFlag flag) {
        if (!this.isValid(pos)) {
            return false;
        } else if (this.worldInfo.getTerrainType() == WorldType.DEBUG_ALL_BLOCK_STATES) { // isRemote is always false since this is WorldServer
            return false;
        } else {
            // Sponge - reroute to the PhaseTracker
            return PhaseTracker.getInstance().setBlockState(this, pos.toImmutable(), state, flag);
        }
    }

    /**
     * @author gabizou - July 25th, 2018
     * @reason Technically an overwrite for {@link World#destroyBlock(BlockPos, boolean)}
     * so that we can artificially capture/associate entity spawns from the proposed block
     * destruction when the actual block event is thrown, whether captures are taking
     * place or not. In the context of "if block changes are not captured", we do still need
     * to associate the drops before the actual block is removed
     *
     * @param pos
     * @param dropBlock
     * @return
     */
    @Override
    public boolean destroyBlock(final BlockPos pos, final boolean dropBlock) {
        final IBlockState iblockstate = this.getBlockState(pos);
        final Block block = iblockstate.getBlock();

        if (iblockstate.getMaterial() == Material.AIR) {
            return false;
        }
        // Sponge Start - Fire the change block pre here, before we bother with drops. If the pre is cancelled, just don't bother.
        if (ShouldFire.CHANGE_BLOCK_EVENT_PRE) {
            if (SpongeCommonEventFactory.callChangeBlockEventPre(this, pos).isCancelled()) {
                return false;
            }
        }
        // Sponge End
        this.playEvent(2001, pos, Block.getStateId(iblockstate));

        if (dropBlock) {
            // Sponge Start - since we are going to perform block drops, we need
            // to notify the current phase state and find out if capture pos is to be used.
            final PhaseContext<?> context = PhaseTracker.getInstance().getCurrentContext();
            final IPhaseState<?> state = PhaseTracker.getInstance().getCurrentState();
            final boolean isCapturingBlockDrops = state.alreadyProcessingBlockItemDrops();
            final BlockPos previousPos;
            if (isCapturingBlockDrops) {
                previousPos = context.getCaptureBlockPos().getPos().orElse(null);
                context.getCaptureBlockPos().setPos(pos);
            } else {
                previousPos = null;
            }
            // Sponge End
            block.dropBlockAsItem((WorldServer) (Object) this, pos, iblockstate, 0);
            // Sponge Start
            if (isCapturingBlockDrops) {
                // we need to reset the capture pos because we've been capturing item and entity drops this way.
                context.getCaptureBlockPos().setPos(previousPos);
            }
            // Sponge End

        }

        // Sponge - reduce the call stack by calling the more direct method.
        return this.bridge$setBlockState(pos, Blocks.AIR.getDefaultState(), BlockChangeFlags.ALL);
    }

    private net.minecraft.tileentity.TileEntity onChunkGetTileDuringRemoval(final BlockPos pos) {

        if (this.isOutsideBuildHeight(pos)) {
            return null;
        } else {
            net.minecraft.tileentity.TileEntity tileentity2 = null;

            if (this.processingLoadedTiles) {
                tileentity2 = ((WorldAccessor) this).accessPendingTileEntityAt(pos);
            }

            if (tileentity2 == null) {
                // Sponge - Instead of creating the tile entity, just check if it's there. If the
                // tile entity doesn't exist, don't create it since we're about to just wholesale remove it...
                // tileentity2 = this.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.IMMEDIATE);
                tileentity2 = this.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
            }

            if (tileentity2 == null) {
                tileentity2 =  ((WorldAccessor) this).accessPendingTileEntityAt(pos);
            }

            return tileentity2;
        }
    }

    /**
     * @author gabizou - March 12th, 2016
     *
     * Technically an overwrite to properly track on *server* worlds.
     */
    @Override
    public void neighborChanged(final BlockPos pos, Block blockIn, final BlockPos otherPos) { // notifyBlockOfStateChange
        final Chunk chunk = ((ChunkProviderBridge) this.getChunkProvider())
            .bridge$getLoadedChunkWithoutMarkingActive(otherPos.getX() >> 4, otherPos.getZ() >> 4);

        // Don't let neighbor updates trigger a chunk load ever
        if (chunk == null) {
            return;
        }
        //noinspection ConstantConditions
        if (blockIn == null) {
            // If the block is null, check with the PhaseState to see if it can perform a safe way
            final PhaseContext<?> currentContext = PhaseTracker.getInstance().getCurrentContext();
            final PhaseTrackerCategory trackerConfig = SpongeImpl.getGlobalConfigAdapter().getConfig().getPhaseTracker();

            if (currentContext.state == TickPhase.Tick.TILE_ENTITY) {
                // Try to save ourselves
                final TileEntityType type = currentContext.getSource(TileEntity.class).map(TileEntity::getType).orElse(null);
                if (type != null) {
                    final Map<String, Boolean> autoFixedTiles = trackerConfig.getAutoFixedTiles();
                    final boolean contained = autoFixedTiles.containsKey(type.getId());
                    // If we didn't map the tile entity yet, we should apply the mapping
                    // based on whether the source is the same as the TileEntity.
                    if (!contained) {
                        if (pos.equals(currentContext.getSource(net.minecraft.tileentity.TileEntity.class).get().getPos())) {
                            autoFixedTiles.put(type.getId(), true);
                        } else {
                            autoFixedTiles.put(type.getId(), false);
                        }
                    }
                    final boolean useTile = contained && autoFixedTiles.get(type.getId());
                    if (useTile) {
                        blockIn = ((net.minecraft.tileentity.TileEntity) currentContext.getSource()).getBlockType();
                    } else {
                        blockIn = (pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z)
                                  ? chunk.getBlockState(pos).getBlock()
                                  : this.getBlockState(pos).getBlock();
                    }
                    if (!contained && trackerConfig.isReportNullSourceBlocks()) {
                        PhaseTracker.printNullSourceBlockWithTile(pos, blockIn, otherPos, type, useTile, new NullPointerException("Null Source Block For TileEntity Neighbor Notification"));
                    }
                } else {
                    blockIn = (pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z)
                              ? chunk.getBlockState(pos).getBlock()
                              : this.getBlockState(pos).getBlock();
                    if (trackerConfig.isReportNullSourceBlocks()) {
                        PhaseTracker.printNullSourceBlockNeighborNotificationWithNoTileSource(pos, blockIn, otherPos,
                            new NullPointerException("Null Source Block For Neighbor Notification"));
                    }
                }

            } else {
                blockIn = (pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z)
                          ? chunk.getBlockState(pos).getBlock()
                          : this.getBlockState(pos).getBlock();
                if (trackerConfig.isReportNullSourceBlocks()) {
                    PhaseTracker.printNullSourceForBlock(((WorldServer) (Object) this), pos, blockIn, otherPos, new NullPointerException("Null Source Block For Neighbor Notification"));
                }
            }
        }

        PhaseTracker.getInstance().notifyBlockOfStateChange(this, this.getBlockState(pos), pos, blockIn, otherPos);
    }

    /**
     * @author gabizou - March 12th, 2016
     *
     * Technically an overwrite to properly track on *server* worlds.
     */
    @Override
    public void notifyNeighborsOfStateExcept(final BlockPos pos, final Block blockType, final EnumFacing skipSide) {
        if (isChunkAvailable(pos)) {
            return;
        }

        // Check for listeners.
        if (ShouldFire.NOTIFY_NEIGHBOR_BLOCK_EVENT) {
            final EnumSet<EnumFacing> directions = EnumSet.of(EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH);
            directions.remove(skipSide);
            throwNotifyNeighborAndCall(pos, blockType, directions);
            return;
        }

        // Else, we just do vanilla. If there's no listeners, we don't want to spam the notification event
        for (final EnumFacing direction : Constants.World.NOTIFY_DIRECTIONS) { // ORDER MATTERS, we have to keep order in which directions are notified.
            if (direction == skipSide) {
                continue;
            }
            final BlockPos offset = pos.offset(direction);
            PhaseTracker.getInstance().notifyBlockOfStateChange(this, this.getBlockState(offset), offset, blockType, pos);
        }

    }

    /**
     * @author gabizou - March 12th, 2016
     *
     * Technically an overwrite to properly track on *server* worlds.
     */
    @Override
    public void notifyNeighborsOfStateChange(final BlockPos sourcePos, final Block sourceBlock, final boolean updateObserverBlocks) {
        if (isChunkAvailable(sourcePos)) {
            return;
        }

        if (ShouldFire.NOTIFY_NEIGHBOR_BLOCK_EVENT) {
            throwNotifyNeighborAndCall(sourcePos, sourceBlock, Constants.World.NOTIFY_DIRECTION_SET);
        } else {
            // Else, we just do vanilla. If there's no listeners, we don't want to spam the notification event
            for (final EnumFacing direction : Constants.World.NOTIFY_DIRECTIONS) {
                final BlockPos notifyPos = sourcePos.offset(direction);
                PhaseTracker.getInstance().notifyBlockOfStateChange(this, this.getBlockState(notifyPos), notifyPos, sourceBlock, sourcePos);
            }
        }

        // Copied over to ensure observers retain functionality.
        if (updateObserverBlocks) {
            this.updateObservingBlocksAt(sourcePos, sourceBlock);
        }
    }

    private void throwNotifyNeighborAndCall(final BlockPos sourcePos, final Block sourceBlock, final EnumSet<EnumFacing> notifyDirectionSet) {
        final NotifyNeighborBlockEvent event = SpongeCommonEventFactory.callNotifyNeighborEvent((org.spongepowered.api.world.World) this, sourcePos, notifyDirectionSet);
        if (event == null || !event.isCancelled()) {
            for (final EnumFacing facing : Constants.World.NOTIFY_DIRECTIONS) {
                if (event != null) {
                    final Direction direction = DirectionFacingProvider.getInstance().getKey(facing).get();
                    if (!event.getNeighbors().keySet().contains(direction)) {
                        continue;
                    }
                }

                final BlockPos notifyPos = sourcePos.offset(facing);
                PhaseTracker.getInstance().notifyBlockOfStateChange(this, this.getBlockState(notifyPos), notifyPos, sourceBlock, sourcePos);
            }
        }
    }

    private boolean isChunkAvailable(final BlockPos sourcePos) {
        if (!isValid(sourcePos)) {
            return true;
        }

        final Chunk chunk = ((ChunkProviderBridge) this.getChunkProvider())
            .bridge$getLoadedChunkWithoutMarkingActive(sourcePos.getX() >> 4, sourcePos.getZ() >> 4);

        // Don't let neighbor updates trigger a chunk load ever
        return chunk == null;
    }

    @Override
    public void onDestroyBlock(final BlockPos pos, final boolean dropBlock, final CallbackInfoReturnable<Boolean> cir) {
        if (SpongeCommonEventFactory.callChangeBlockEventPre(this, pos).isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Override
    protected void onUpdateWeatherEffect(final net.minecraft.entity.Entity entityIn) {
        onCallEntityUpdate(entityIn); // maybe we should combine these injections/redirects?
    }

    @Override
    protected void onUpdateTileEntities(final ITickable tile) {
        this.updateTileEntity(tile);
    }

    // separated from onUpdateEntities for TileEntityActivation mixin
    private void updateTileEntity(final ITickable tile) {
        if (!SpongeImplHooks.shouldTickTile(tile)) {
            return;
        }
        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        final IPhaseState<?> state = phaseTracker.getCurrentState();

        if (state.alreadyCapturingTileTicks()) {
            tile.update();
            return;
        }

        TrackingUtil.tickTileEntity(this, tile);
    }

    @Override
    protected void onCallEntityUpdate(final net.minecraft.entity.Entity entity) {
        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        final IPhaseState<?> state = phaseTracker.getCurrentState();
        if (state.alreadyCapturingEntityTicks()) {
            entity.onUpdate();
            return;
        }

        TrackingUtil.tickEntity(entity);
        bridge$updateRotation(entity);
    }

    @Override
    protected void onCallEntityRidingUpdate(final net.minecraft.entity.Entity entity) {
        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        final IPhaseState<?> state = phaseTracker.getCurrentState();
        if (state.alreadyCapturingEntityTicks()) {
            entity.updateRidden();
            return;
        }

        TrackingUtil.tickRidingEntity(entity);
        bridge$updateRotation(entity);
    }

    /**
     * @author gabizou - May 11th, 2018
     * @reason Due to mods attempting to retrieve spawned entity drops in the world,
     * we occasionally have to accomodate those mods by providing insight into the
     * entities that are being captured by the {@link PhaseTracker} in the instance
     * we have an {@link IPhaseState} that is capturing entities. This is only to
     * allow the mod to still retrieve said entities in the "world" that would otherwise
     * be spawned.
     *
     * <p>Note that the entities are also filtered on whether they are being removed
     * during the {@link IPhaseState#unwind(PhaseContext)} process to avoid duplicate
     * entity spawns.</p>
     *
     * @param clazz The entity class
     * @param aabb The axis aligned bounding box
     * @param filter The filter predicate
     * @param <T> The type of entity list
     * @return The list of entities found
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T extends net.minecraft.entity.Entity> List<T> getEntitiesWithinAABB(final Class<? extends T> clazz, final AxisAlignedBB aabb,
        @Nullable final Predicate<? super T> filter) {
        // Sponge start - get the max entity radius variable from forge
        final double maxEntityRadius = SpongeImplHooks.getWorldMaxEntityRadius((WorldServer) (Object) this);
        // j2 == minChunkX
        // k2 == maxChunkX
        // l2 == minChunkZ
        // i3 == maxChunkZ
        final int minChunkX = MathHelper.floor((aabb.minX - maxEntityRadius) / 16.0D);
        final int maxChunkX = MathHelper.ceil((aabb.maxX + maxEntityRadius) / 16.0D);
        final int minChunkZ = MathHelper.floor((aabb.minZ - maxEntityRadius) / 16.0D);
        final int maxChunkZ = MathHelper.ceil((aabb.maxZ + maxEntityRadius) / 16.0D);
        // Sponge End
        final List<T> list = Lists.newArrayList();

        for (int currentX = minChunkX; currentX < maxChunkX; ++currentX) {
            for (int currentZ = minChunkZ; currentZ < maxChunkZ; ++currentZ) {
                if (this.isChunkLoaded(currentX, currentZ, true)) {
                    this.getChunk(currentX, currentZ).getEntitiesOfTypeWithinAABB(clazz, aabb, list, filter);
                }
            }
        }
        // Sponge Start - check the phase tracker
        final boolean isMainThread = Sponge.isServerAvailable() && Sponge.getServer().isMainThread();
        if (!isMainThread) {
            // Short circuit here if we're not on the main thread. Don't bother with the PhaseTracker off thread.
            return list;
        }
        final PhaseContext<?> context = PhaseTracker.getInstance().getCurrentContext();
        final IPhaseState<?> state = context.state;
        if (((IPhaseState) state).doesCaptureEntityDrops(context) || state.doesAllowEntitySpawns()) {
            // We need to check for entity spawns and entity drops. If either are used, we need to offer them up in the lsit, provided
            // they pass the predicate check
            if (((IPhaseState) state).doesCaptureEntityDrops(context)) {
                for (final EntityItem entity : context.getCapturedItems()) {
                    // We can ignore the type check because we're already checking the instance class of the entity.
                    if (clazz.isInstance(entity) && entity.getEntityBoundingBox().intersects(aabb) && (filter == null || filter.apply((T) entity))) {
                        list.add((T) entity);
                    }
                }
            }
            if (state.doesCaptureEntitySpawns()) {
                for (final Entity entity : context.getCapturedEntities()) {
                    // We can ignore the type check because we're already checking the instance class of the entity.
                    if (clazz.isInstance(entity) && ((net.minecraft.entity.Entity) entity).getEntityBoundingBox().intersects(aabb) && (filter == null || filter.apply((T) entity))) {
                        list.add((T) entity);
                    }
                }
                if (((IPhaseState) state).doesBulkBlockCapture(context)) {
                    for (final net.minecraft.entity.Entity entity : context.getPerBlockEntitySpawnSuppplier().get().values()) {
                        // We can ignore the type check because we're already checking the instance class of the entity.
                        if (clazz.isInstance(entity) && entity.getEntityBoundingBox().intersects(aabb) && (filter == null || filter.apply((T) entity))) {
                            list.add((T) entity);
                        }
                    }
                }
            }

        }
        // Sponge End
        return list;
    }

    // ------------------------ End of Cause Tracking ------------------------------------

    // WorldBridge method
    @Override
    public void bridge$NotifyNeighborsPostBlockChange(final BlockPos pos, final IBlockState newState, final BlockChangeFlag flags) {
        if (flags.updateNeighbors()) {
            this.notifyNeighborsRespectDebug(pos, newState.getBlock(), flags.notifyObservers());

            if (newState.hasComparatorInputOverride()) {
                this.updateComparatorOutputLevel(pos, newState.getBlock());
            }
        }
    }

    // WorldBridge method
    @Override
    public void bridge$addEntityRotationUpdate(final net.minecraft.entity.Entity entity, final Vector3d rotation) {
        this.rotationUpdates.put(entity, rotation);
    }

    // WorldBridge method
    @Override
    public void bridge$updateRotation(final net.minecraft.entity.Entity entityIn) {
        final Vector3d rotationUpdate = this.rotationUpdates.get(entityIn);
        if (rotationUpdate != null) {
            entityIn.rotationPitch = (float) rotationUpdate.getX();
            entityIn.rotationYaw = (float) rotationUpdate.getY();
        }
        this.rotationUpdates.remove(entityIn);
    }

    @Override
    public void bridge$onSpongeEntityAdded(final net.minecraft.entity.Entity entity) {
        this.onEntityAdded(entity);
        ((EntityBridge) entity).onJoinWorld();
    }

    @Override
    public boolean bridge$forceSpawnEntity(final Entity entity) {
        final net.minecraft.entity.Entity minecraftEntity = (net.minecraft.entity.Entity) entity;
        final int x = minecraftEntity.getPosition().getX();
        final int z = minecraftEntity.getPosition().getZ();
        return forceSpawnEntity(minecraftEntity, x >> 4, z >> 4);
    }

    private boolean forceSpawnEntity(final net.minecraft.entity.Entity entity, final int chunkX, final int chunkZ) {
        if (!this.isFake() && SpongeImplHooks.isMainThread()) {
            SpongeHooks.logEntitySpawn(entity);
        }
        if (entity instanceof EntityPlayer) {
            final EntityPlayer entityplayer = (EntityPlayer) entity;
            this.playerEntities.add(entityplayer);
            this.updateAllPlayersSleepingFlag();
        }

        if (entity instanceof EntityLightningBolt) {
            this.addWeatherEffect(entity);
            return true;
        }

        this.getChunk(chunkX, chunkZ).addEntity(entity);
        this.loadedEntityList.add(entity);
        this.bridge$onSpongeEntityAdded(entity);
        return true;
    }


    @Override
    public SpongeBlockSnapshot bridge$createSnapshot(final IBlockState state, final IBlockState extended, final BlockPos pos, final BlockChangeFlag updateFlag) {
        final SpongeBlockSnapshotBuilder builder = new SpongeBlockSnapshotBuilder();
        builder.reset();
        builder.blockState(state)
                .extendedState(extended)
                .worldId(((org.spongepowered.api.world.World) this).getUniqueId())
                .position(VecHelper.toVector3i(pos));
        final Optional<UUID> creator = ((org.spongepowered.api.world.World) this).getCreator(pos.getX(), pos.getY(), pos.getZ());
        final Optional<UUID> notifier = ((org.spongepowered.api.world.World) this).getNotifier(pos.getX(), pos.getY(), pos.getZ());
        if (creator.isPresent()) {
            builder.creator(creator.get());
        }
        if (notifier.isPresent()) {
            builder.notifier(notifier.get());
        }
        final boolean hasTileEntity = SpongeImplHooks.hasBlockTileEntity(state.getBlock(), state);
        final net.minecraft.tileentity.TileEntity tileEntity = this.getChunk(pos).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        if (hasTileEntity || tileEntity != null) {
            // We MUST only check to see if a TE exists to avoid creating a new one.
            if (tileEntity != null) {
                final TileEntity tile = (TileEntity) tileEntity;
                for (final DataManipulator<?, ?> manipulator : ((CustomDataHolderBridge) tile).getCustomManipulators()) {
                    builder.add(manipulator);
                }
                final NBTTagCompound nbt = new NBTTagCompound();
                // Some mods like OpenComputers assert if attempting to save robot while moving
                try {
                    tileEntity.writeToNBT(nbt);
                    builder.unsafeNbt(nbt);
                }
                catch(Throwable t) {
                    // ignore
                }
            }
        }
        builder.flag(updateFlag);
        return builder.build();
    }

    @Override
    public SpongeBlockSnapshot bridge$createSnapshotWithEntity(final IBlockState state, final BlockPos pos, final BlockChangeFlag updateFlag,
        @Nullable final net.minecraft.tileentity.TileEntity tileEntity) {
        final SpongeBlockSnapshotBuilder builder = new SpongeBlockSnapshotBuilder();
        builder.reset();
        builder.blockState(state)
            .extendedState(state)
            .worldId(((org.spongepowered.api.world.World) this).getUniqueId())
            .position(VecHelper.toVector3i(pos));
        if (tileEntity != null) { // Store the information of the tile entity onto the snapshot
            final NBTTagCompound nbt = new NBTTagCompound();
            // Some mods like OpenComputers assert if attempting to save robot while moving
            try {
                tileEntity.writeToNBT(nbt);
                builder.unsafeNbt(nbt);
            }
            catch(Throwable t) {
                // ignore
            }
        }
        builder.flag(updateFlag);
        return builder.build();
    }

    /**
     * @author gabizou - September 10th, 2016
     * @author gabizou - September 21st, 2017 - Update for PhaseContext refactor.
     * @reason Due to the amount of changes, and to ensure that Forge's events are being properly
     * thrown, we must overwrite to have our hooks in place where we need them to be and when.
     * Likewise, since the event context is very ambiguously created, we may have an entity
     * coming in, or no entity, the explosion must always have a "source" in some context.
     *
     * @param entityIn The entity that caused the explosion
     * @param x The x position
     * @param y The y position
     * @param z The z position
     * @param strength The strength of the explosion, determines what blocks can be destroyed
     * @param isFlaming Whether fire will be caused from the explosion
     * @param isSmoking Whether blocks will break
     * @return The explosion
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    public Explosion newExplosion(@Nullable final net.minecraft.entity.Entity entityIn, final double x, final double y, final double z, final float strength, final boolean isFlaming,
            final boolean isSmoking) {
        Explosion explosion = new Explosion((WorldServer) (Object) this, entityIn, x, y, z, strength, isFlaming, isSmoking);

        // Sponge Start - all the remaining behavior is in triggerInternalExplosion().
        explosion = triggerInternalExplosion((org.spongepowered.api.world.explosion.Explosion) explosion, e -> GeneralPhase.State.EXPLOSION
                .createPhaseContext()
                .explosion(e)
                .potentialExplosionSource((WorldServer) (Object) this, entityIn));
        // Sponge End
        return explosion;
    }

    private SpongeProxyBlockAccess proxyBlockAccess = new SpongeProxyBlockAccess(this);

    @Override
    public SpongeProxyBlockAccess bridge$getProxyAccess() {
        return this.proxyBlockAccess;
    }

    /**
     * @author gabizou - August 4th, 2016
     * @author blood - May 11th, 2017 - Forces chunk requests if TE is ticking.
     * @reason Rewrites the check to be inlined to {@link IMixinBlockPos}.
     *
     * @param pos The position
     * @return The block state at the desired position
     */
    @Override
    public IBlockState getBlockState(final BlockPos pos) {
        // Sponge - Replace with inlined method
        // if (this.isOutsideBuildHeight(pos)) // Vanilla
        if (((IMixinBlockPos) pos).isInvalidYPosition()) {
            // Sponge end
            return Blocks.AIR.getDefaultState();
        } else {
            // ExtraUtilities 2 expects to get the proper chunk while mining or it gets stuck in infinite loop
            // TODO add TE config to disable/enable chunk loads
            final boolean forceChunkRequests = ((ServerChunkProviderBridge) this.getChunkProvider()).bridge$getForceChunkRequests();
            final PhaseTracker phaseTracker = PhaseTracker.getInstance();
            final IPhaseState<?> currentState = phaseTracker.getCurrentState();
            final boolean entered = currentState == TickPhase.Tick.TILE_ENTITY;
            if (entered) {
                ((ServerChunkProviderBridge) this.getChunkProvider()).bridge$setForceChunkRequests(true);
            }
            try {
                // Proxies have block changes for bulk special captures
                final IBlockState blockState = this.proxyBlockAccess.getBlockState(pos);
                if (blockState != null) {
                    return blockState;
                }
                final net.minecraft.world.chunk.Chunk chunk = this.getChunk(pos);
                return chunk.getBlockState(pos);
            } finally {
                if (entered) {
                    ((ServerChunkProviderBridge) this.getChunkProvider()).bridge$setForceChunkRequests(forceChunkRequests);
                }
            }
        }
    }

    /**
     * @author gabizou - August 4th, 2016
     * Overrides the same method from MixinWorld_Lighting that redirects
     * {@link #isAreaLoaded(BlockPos, int, boolean)} to simplify the check to
     * whether the chunk's neighbors are loaded. Since the passed radius is always
     * 17, the check is simply checking for whether neighboring chunks are loaded
     * properly.
     *
     * @param thisWorld This world
     * @param pos The block position to check light for
     * @param radius The radius, always 17
     * @param allowEmtpy Whether to allow empty chunks, always false
     * @param lightType The light type
     * @param samePosition The block position to check light for, again.
     * @return True if the chunk is loaded and neighbors are loaded
     */
    @Override
    public boolean spongeIsAreaLoadedForCheckingLight(
        final World thisWorld, final BlockPos pos, final int radius, final boolean allowEmtpy, final EnumSkyBlock lightType,
            final BlockPos samePosition) {
        final Chunk chunk = ((ChunkProviderBridge) this.getChunkProvider())
            .bridge$getLoadedChunkWithoutMarkingActive(pos.getX() >> 4, pos.getZ() >> 4);
        return !(chunk == null || !((ChunkBridge) chunk).areNeighborsLoaded());
    }

    /**
     * @author gabizou - April 8th, 2016
     *
     * Instead of providing chunks which has potential chunk loads,
     * we simply get the chunk directly from the chunk provider, if it is loaded
     * and return the light value accordingly.
     *
     * @param pos The block position
     * @return The light at the desired block position
     */
    @Override
    public int getLight(BlockPos pos) {
        if (pos.getY() < 0) {
            return 0;
        } else {
            if (pos.getY() >= 256) {
                pos = new BlockPos(pos.getX(), 255, pos.getZ());
            }
            // Sponge Start - Use our hook to get the chunk only if it is loaded
            // return this.getChunk(pos).getLightSubtracted(pos, 0);
            final Chunk chunk = ((ChunkProviderBridge) this.getChunkProvider())
                .bridge$getLoadedChunkWithoutMarkingActive(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? 0 : chunk.getLightSubtracted(pos, 0);
            // Sponge End
        }
    }

    /**
     * @author gabizou - April 8th, 2016
     *
     * @reason Rewrites the chunk accessor to get only a chunk if it is loaded.
     * This avoids loading chunks from file or generating new chunks
     * if the chunk didn't exist, when the only function of this method is
     * to get the light for the given block position.
     *
     * @param pos The block position
     * @param checkNeighbors Whether to check neighboring block lighting
     * @return The light value at the block position, if the chunk is loaded
     */
    @Override
    public int getLight(BlockPos pos, final boolean checkNeighbors) {
        if (((IMixinBlockPos) pos).isValidXZPosition()) { // Sponge - Replace with inlined method
            if (checkNeighbors && this.getBlockState(pos).useNeighborBrightness()) {
                int i1 = this.getLight(pos.up(), false);
                final int i = this.getLight(pos.east(), false);
                final int j = this.getLight(pos.west(), false);
                final int k = this.getLight(pos.south(), false);
                final int l = this.getLight(pos.north(), false);

                if (i > i1) {
                    i1 = i;
                }

                if (j > i1) {
                    i1 = j;
                }

                if (k > i1) {
                    i1 = k;
                }

                if (l > i1) {
                    i1 = l;
                }

                return i1;
            } else if (pos.getY() < 0) {
                return 0;
            } else {
                if (pos.getY() >= 256) {
                    pos = new BlockPos(pos.getX(), 255, pos.getZ());
                }

                // Sponge - Gets only loaded chunks, unloaded chunks will not get loaded to check lighting
                // Chunk chunk = this.getChunk(pos);
                // return chunk.getLightSubtracted(pos, this.skylightSubtracted);
                final Chunk chunk = ((ChunkProviderBridge) this.getChunkProvider()).bridge$getLoadedChunkWithoutMarkingActive(pos.getX() >> 4, pos.getZ() >> 4);
                return chunk == null ? 0 : chunk.getLightSubtracted(pos, this.getSkylightSubtracted());
                // Sponge End
            }
        } else {
            return 15;
        }
    }

    /**
     * @author gabizou - August 4th, 2016
     * @reason Rewrites the check to be inlined to {@link IMixinBlockPos}.
     *
     * @param type The type of sky lighting
     * @param pos The position
     * @return The light for the defined sky type and block position
     */
    @Override
    public int getLightFor(final EnumSkyBlock type, BlockPos pos) {
        if (pos.getY() < 0) {
            pos = new BlockPos(pos.getX(), 0, pos.getZ());
        }

        // Sponge Start - Replace with inlined method to check
        // if (!this.isValid(pos)) // vanilla
        if (!((IMixinBlockPos) pos).isValidPosition()) {
            // Sponge End
            return type.defaultLightValue;
        } else {
            // Do not get a chunk that potentially marks it as an active chunk
            final Chunk chunk = ((ChunkProviderBridge) this.getChunkProvider())
                .bridge$getLoadedChunkWithoutMarkingActive(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null) {
                return type.defaultLightValue;
            }
            return chunk.getLightFor(type, pos);
        }
    }

    @Override
    public boolean bridge$isLightLevel(final Chunk chunk, BlockPos pos, final int level) {
        if (((IMixinBlockPos) pos).isValidPosition()) {
            if (this.getBlockState(pos).useNeighborBrightness()) {
                if (this.getLight(pos.up(), false) >= level) {
                    return true;
                }
                if (this.getLight(pos.east(), false) >= level) {
                    return true;
                }
                if (this.getLight(pos.west(), false) >= level) {
                    return true;
                }
                if (this.getLight(pos.south(), false) >= level) {
                    return true;
                }
                if (this.getLight(pos.north(), false) >= level) {
                    return true;
                }
                return false;
            } else {
                if (pos.getY() >= 256) {
                    pos = new BlockPos(pos.getX(), 255, pos.getZ());
                }

                return chunk.getLightSubtracted(pos, this.getSkylightSubtracted()) >= level;
            }
        } else {
            return true;
        }
    }

    /**
     * @author amaranth - April 25th, 2016
     * @reason Avoid 25 chunk map lookups per entity per tick by using neighbor pointers
     *
     * @param xStart X block start coordinate
     * @param yStart Y block start coordinate
     * @param zStart Z block start coordinate
     * @param xEnd X block end coordinate
     * @param yEnd Y block end coordinate
     * @param zEnd Z block end coordinate
     * @param allowEmpty Whether empty chunks should be accepted
     * @return If the chunks for the area are loaded
     */
    @Override
    public boolean isAreaLoaded(int xStart, final int yStart, int zStart, int xEnd, final int yEnd, int zEnd, final boolean allowEmpty) {
        if (yEnd < 0 || yStart > 255) {
            return false;
        }

        xStart = xStart >> 4;
        zStart = zStart >> 4;
        xEnd = xEnd >> 4;
        zEnd = zEnd >> 4;

        final net.minecraft.world.chunk.Chunk base = ((ChunkProviderBridge) this.getChunkProvider()).bridge$getLoadedChunkWithoutMarkingActive(xStart, zStart);
        if (base == null) {
            return false;
        }

        ChunkBridge currentColumn = (ChunkBridge) base;
        for (int i = xStart; i <= xEnd; i++) {
            if (currentColumn == null) {
                return false;
            }

            ChunkBridge currentRow = currentColumn;
            for (int j = zStart; j <= zEnd; j++) {
                if (currentRow == null) {
                    return false;
                }

                if (!allowEmpty && ((net.minecraft.world.chunk.Chunk) currentRow).isEmpty()) {
                    return false;
                }

                currentRow = (ChunkBridge) currentRow.getNeighborChunk(1);
            }

            currentColumn = (ChunkBridge) currentColumn.getNeighborChunk(2);
        }

        return true;
    }

    @Redirect(method = "canAddEntity", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private void onCanAddEntityLogWarn(final Logger logger, final String message, final Object param1, final Object param2) {
        // don't log anything to avoid useless spam
    }

    /**
     * @author blood - October 3rd, 2017
     * @reason Rewrites the check to avoid loading chunks.
     *
     * @param x The chunk x position
     * @param z The chunk z position
     * @param allowEmpty Whether empty chunks are allowed
     * @return If chunk is loaded
     */
    @Overwrite
    protected boolean isChunkLoaded(final int x, final int z, final boolean allowEmpty) {
        final ChunkBridge spongeChunk = (ChunkBridge) ((ChunkProviderBridge) this.getChunkProvider()).bridge$getLoadedChunkWithoutMarkingActive(x, z);
        return spongeChunk != null && (!spongeChunk.isQueuedForUnload() || spongeChunk.isPersistedChunk());
    }

    @Override
    public void markChunkDirty(final BlockPos pos, final net.minecraft.tileentity.TileEntity unusedTileEntity)
    {
        if (unusedTileEntity == null) {
            super.markChunkDirty(pos, unusedTileEntity);
            return;
        }
        final ChunkBridge chunk = ((ActiveChunkReferantBridge) unusedTileEntity).bridge$getActiveChunk();
        if (chunk != null) {
            chunk.markChunkDirty();
        }
    }

    /**************************** TIMINGS ***************************************/
    /*
    The remaining of these overridden methods are all injectors into World#updateEntities() to where
    the exact fine tuning of where the methods are invoked, the call stack is precisely emulated as
    if this were an overwrite. The injections themselves are sensitive in some regards, but mostly
    will remain just fine.
     */


    @Override
    public void startEntityGlobalTimings() {
        this.timings.entityTick.startTiming();
        TimingHistory.entityTicks += this.loadedEntityList.size();
    }

    @Override
    public void stopTimingForWeatherEntityTickCrash(final net.minecraft.entity.Entity updatingEntity) {
        ((TimingBridge) updatingEntity).bridge$getTimingsHandler().stopTiming();
    }

    @Override
    public void stopEntityTickTimingStartEntityRemovalTiming() {
        this.timings.entityTick.stopTiming();
        this.timings.entityRemoval.startTiming();
    }

    @Override
    public void stopEntityRemovalTiming() {
        this.timings.entityRemoval.stopTiming();
    }

    @Override
    public void startEntityTickTiming() {
        this.timings.entityTick.startTiming();
    }

    @Override
    public void stopTimingTickEntityCrash(final net.minecraft.entity.Entity updatingEntity) {
        ((TimingBridge) updatingEntity).bridge$getTimingsHandler().stopTiming();
    }

    @Override
    public void stopEntityTickSectionBeforeRemove() {
       this.timings.entityTick.stopTiming();
    }

    @Override
    public void startEntityRemovalTick() {
        this.timings.entityRemoval.startTiming();
    }

    @Override
    public void startTileTickTimer() {
        this.timings.tileEntityTick.startTiming();
    }

    @Override
    public void stopTimingTickTileEntityCrash(final net.minecraft.tileentity.TileEntity updatingTileEntity) {
        ((TimingBridge) updatingTileEntity).bridge$getTimingsHandler().stopTiming();
    }

    @Override
    public void stopTileEntityAndStartRemoval() {
        this.timings.tileEntityTick.stopTiming();
        this.timings.tileEntityRemoval.startTiming();
    }

    @Override
    public void stopTileEntityRemovelInWhile() {
        this.timings.tileEntityRemoval.stopTiming();
    }

    @Override
    public void startPendingTileEntityTimings() {
        this.timings.tileEntityPending.startTiming();
    }

    @Override
    public void endPendingTileEntities() {
        this.timings.tileEntityPending.stopTiming();
        TimingHistory.tileEntityTicks += this.loadedTileEntityList.size();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=tickPending") )
    private void onBeginTickBlockUpdate(final CallbackInfo ci) {
        this.timings.scheduledBlocks.startTiming();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=tickBlocks") )
    private void onAfterTickBlockUpdate(final CallbackInfo ci) {
        this.timings.scheduledBlocks.stopTiming();
        this.timings.updateBlocks.startTiming();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=chunkMap") )
    private void onBeginUpdateBlocks(final CallbackInfo ci) {
        this.timings.updateBlocks.stopTiming();
        this.timings.doChunkMap.startTiming();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=village") )
    private void onBeginUpdateVillage(final CallbackInfo ci) {
        this.timings.doChunkMap.stopTiming();
        this.timings.doVillages.startTiming();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=portalForcer"))
    private void onBeginUpdatePortal(final CallbackInfo ci) {
        this.timings.doVillages.stopTiming();
        this.timings.doPortalForcer.startTiming();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V"))
    private void onEndUpdatePortal(final CallbackInfo ci) {
        this.timings.doPortalForcer.stopTiming();
    }

    /**
     * Seriously, this was stupid.
     */
    @Redirect(method = "tickUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos redirectNeedlessBlockPosObjectCreation(final BlockPos pos, final int x, final int y, final int z) {
        return pos;
    }

    @Redirect(method = "tickUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;scheduleUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;I)V"))
    private void redirectDontRescheduleBlockUpdates(final WorldServer worldServer, final BlockPos pos, final Block blockIn, final int delay) {
    }

    // TIMINGS
    @Inject(method = "tickUpdates", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", args = "ldc=cleaning"))
    private void onTickUpdatesCleanup(final boolean flag, final CallbackInfoReturnable<Boolean> cir) {
        this.timings.scheduledBlocksCleanup.startTiming();
    }

    @Inject(method = "tickUpdates", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", args = "ldc=ticking"))
    private void onTickUpdatesTickingStart(final boolean flag, final CallbackInfoReturnable<Boolean> cir) {
        this.timings.scheduledBlocksCleanup.stopTiming();
        this.timings.scheduledBlocksTicking.startTiming();
    }

    @Inject(method = "tickUpdates", at = @At("RETURN"))
    private void onTickUpdatesTickingEnd(final CallbackInfoReturnable<Boolean> cir) {
        this.timings.scheduledBlocksTicking.stopTiming();
    }

    @Override
    public WorldTimingsHandler bridge$getTimingsHandler() {
        return this.timings;
    }

    @Inject(method = "updateWeather", at = @At(value = "FIELD", target = "Lnet/minecraft/world/WorldServer;prevRainingStrength:F"), cancellable = true)
    private void onAccessPreviousRain(final CallbackInfo ci) {
        final Weather weather = ((org.spongepowered.api.world.World) this).getWeather();
        final int duration = (int) ((org.spongepowered.api.world.World) this).getRemainingDuration();
        if (!weather.equals(this.prevWeather) && duration > 0) {
            try (final StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(this);
                final ChangeWorldWeatherEvent event = SpongeEventFactory.createChangeWorldWeatherEvent(frame.getCurrentCause(), duration, duration,
                        weather, weather, this.prevWeather, ((org.spongepowered.api.world.World) this));
                if (Sponge.getEventManager().post(event)) {
                    ((org.spongepowered.api.world.World) this).setWeather(this.prevWeather);
                    this.prevWeather = ((org.spongepowered.api.world.World) this).getWeather();
                    ci.cancel();
                } else {
                    if (!weather.equals(event.getWeather()) || duration != event.getDuration()) {
                        ((org.spongepowered.api.world.World) this).setWeather(event.getWeather(), event.getDuration());
                        this.weatherStartTime = this.worldInfo.getWorldTotalTime();
                    } else {
                        this.prevWeather = event.getWeather();
                    }
                }
            }
        }
    }

    @Override
    public Weather bridge$getPreviousWeather() {
        return this.prevWeather;
    }

    @Override
    public void bridge$setPreviousWeather(final Weather weather) {
        this.prevWeather = weather;
    }

    @Override
    public SpongeChunkGenerator bridge$getSpongeGenerator() {
        return this.spongegen;
    }

    @Nullable
    @Override
    public ScheduledBlockUpdate bridge$getScheduledBlockUpdate() {
        return (ScheduledBlockUpdate) this.tmpScheduledObj;
    }

    @Override
    public void bridge$setScheduledBlockUpdate(@Nullable final ScheduledBlockUpdate sbu) {
        this.tmpScheduledObj = (NextTickListEntry) sbu;
    }

    @Override
    public int getChunkGCTickInterval() {
        return this.chunkGCTickInterval;
    }

    @Override
    public long getChunkUnloadDelay() {
        return this.chunkUnloadDelay;
    }

    private void setMemoryViewDistance(final int viewDistance) {
        this.playerChunkMap.setPlayerViewRadius(viewDistance);
    }

    private int chooseViewDistanceValue(final int value) {
        if (value == WorldCategory.USE_SERVER_VIEW_DISTANCE) {
            return this.server.getPlayerList().getViewDistance();
        }
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Name", this.worldInfo.getWorldName())
                .add("DimensionId", this.bridge$getDimensionId())
                .add("DimensionType", ((org.spongepowered.api.world.DimensionType) (Object) this.provider.getDimensionType()).getId())
                .toString();
    }
}
