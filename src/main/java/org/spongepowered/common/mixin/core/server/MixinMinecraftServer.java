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
package org.spongepowered.common.mixin.core.server;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.command.TabCompleteEvent;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.SerializationBehaviors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.bridge.CommandSourceBridge;
import org.spongepowered.common.bridge.SubjectBridge;
import org.spongepowered.common.bridge.command.CommandSenderBridge;
import org.spongepowered.common.bridge.server.MinecraftServerBridge;
import org.spongepowered.common.bridge.world.WorldInfoBridge;
import org.spongepowered.common.bridge.world.WorldProviderBridge;
import org.spongepowered.common.command.SpongeCommandManager;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.type.WorldConfig;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.CauseTrackerCrashHandler;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.general.GeneralPhase;
import org.spongepowered.common.event.tracking.phase.general.MapConversionContext;
import org.spongepowered.common.event.tracking.phase.generation.GenerationContext;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;
import org.spongepowered.common.event.tracking.phase.generation.GenericGenerationContext;
import org.spongepowered.common.event.tracking.phase.plugin.BasicPluginContext;
import org.spongepowered.common.event.tracking.phase.plugin.PluginPhase;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.bridge.world.ServerWorldBridge;
import org.spongepowered.common.bridge.world.chunk.ServerChunkProviderBridge;
import org.spongepowered.common.mixin.core.world.storage.MixinWorldInfo;
import org.spongepowered.common.relocate.co.aikar.timings.TimingsManager;
import org.spongepowered.common.resourcepack.SpongeResourcePack;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.WorldManager;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.annotation.Nullable;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements MinecraftServerBridge, SubjectBridge, CommandSourceBridge, CommandSenderBridge {

    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final public Profiler profiler;
    @Shadow private boolean serverStopped;
    @Shadow private int tickCounter;
    @Shadow public WorldServer[] worlds;
    @Shadow public abstract void sendMessage(ITextComponent message);
    @Shadow public abstract boolean isServerRunning();
    @Shadow public abstract PlayerList getPlayerList();
    @Shadow public abstract EnumDifficulty getDifficulty();
    @Shadow public abstract GameType getGameType();
    @Shadow protected abstract void setUserMessage(String message);
    @Shadow protected abstract void outputPercentRemaining(String message, int percent);
    @Shadow protected abstract void clearCurrentTask();
    @Shadow protected abstract void convertMapIfNeeded(String worldNameIn);
    @Shadow public abstract boolean isDedicatedServer();
    @Shadow public abstract boolean allowSpawnMonsters();
    @Shadow public abstract boolean getCanSpawnAnimals();

    @Nullable private List<String> currentTabCompletionOptions;
    @Nullable private ResourcePack resourcePack;
    @Nullable private Integer dimensionId;
    private boolean isSavingEnabled = true;

    /**
     * @author blood - December 23rd, 2015
     * @author Zidane - March 13th, 2016
     * @author gabizou - April 22nd, 2019 - Minecraft 1.12.2
     *
     * @reason Sponge rewrites the method to use the Sponge {@link WorldManager} to load worlds,
     * migrating old worlds, upgrading worlds to our standard, and configuration loading. Also
     * validates that the {@link MixinWorldInfo onConstruction} will not be doing anything
     * silly during map conversions.
     */
    @Overwrite
    public void loadAllWorlds(String overworldFolder, String worldName, long seed, WorldType type, String generatorOptions) {
        try (MapConversionContext context = GeneralPhase.State.MAP_CONVERSION.createPhaseContext()
            .source(this)
            .world(overworldFolder)) {
            context.buildAndSwitch();
            this.convertMapIfNeeded(overworldFolder);
        }
        this.setUserMessage("menu.loadingLevel");

        //WorldManager.loadAllWorlds(seed, type, generatorOptions);

        this.getPlayerList().setPlayerManager(this.worlds);
        this.setDifficultyForAllWorlds(this.getDifficulty());
    }

    /**
     * @author Zidane
     * @reason Separate out spawn generation to a bridge method to be called later.
     */
    @Overwrite
    public void initialWorldChunkLoad() {
        for (WorldServer worldServer: this.worlds) {
            this.bridge$prepareSpawnArea(worldServer);
        }
        this.clearCurrentTask();
    }

    /**
     * @author blood - June 2nd, 2016
     *
     * @reason To allow per-world auto-save tick intervals or disable auto-saving entirely
     *
     * @param dontLog Whether to log during saving
     */
    @Overwrite
    public void saveAllWorlds(boolean dontLog) {
        if (!this.isSavingEnabled) {
            return;
        }
        for (WorldServer world : this.worlds) {
            final boolean save = world.getChunkProvider().canSave() && ((WorldProperties) world.getWorldInfo()).getSerializationBehavior() != SerializationBehaviors.NONE;
            boolean log = !dontLog;

            if (save) {
                // Sponge start - check auto save interval in world config
                if (this.isDedicatedServer() && this.isServerRunning()) {
                    final SpongeConfig<WorldConfig> configAdapter = ((WorldInfoBridge) world.getWorldInfo()).getConfigAdapter();
                    final int autoSaveInterval = configAdapter.getConfig().getWorld().getAutoSaveInterval();
                    if (log) {
                        log = configAdapter.getConfig().getLogging().logWorldAutomaticSaving();
                    }
                    if (autoSaveInterval <= 0
                        || ((WorldProperties) world.getWorldInfo()).getSerializationBehavior() != SerializationBehaviors.AUTOMATIC) {
                        if (log) {
                            LOGGER.warn("Auto-saving has been disabled for level \'" + world.getWorldInfo().getWorldName() + "\'/"
                                + world.provider.getDimensionType().getName() + ". "
                                + "No chunk data will be auto-saved - to re-enable auto-saving set 'auto-save-interval' to a value greater than"
                                + " zero in the corresponding world config.");
                        }
                        continue;
                    }
                    if (this.tickCounter % autoSaveInterval != 0) {
                        continue;
                    }
                    if (log) {
                        LOGGER.info("Auto-saving chunks for level \'" + world.getWorldInfo().getWorldName() + "\'/"
                            + world.provider.getDimensionType().getId());
                    }
                } else if (log) {
                    LOGGER.info("Saving chunks for level \'" + world.getWorldInfo().getWorldName() + "\'/"
                        + world.provider.getDimensionType().getId());
                }

                // Sponge end
                try {
                    ((WorldBridge) world).bridge$save();
                } catch (MinecraftException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * @author Zidane
     * @reason Re-route through {@link WorldManager}.
     */
    @Overwrite
    public WorldServer getWorld(int dimensionId) {
        WorldServer worldServer = SpongeImpl.getWorldManager().getWorld(dimensionId);
        if (worldServer == null) {
            worldServer = SpongeImpl.getWorldManager().getDefaultWorld();
        }

        if (worldServer == null) {
            throw new RuntimeException("Attempted to lookup world with id " + dimensionId + " but overworld is not loaded yet!");
        }

        return worldServer;
    }

    /**
     * @author Zidane
     * @reason Support per-world difficulties and inherited difficulty.
     */
    @Overwrite
    public void setDifficultyForAllWorlds(EnumDifficulty difficulty) {
        final EnumDifficulty serverDifficulty = SpongeImpl.getServer().getDifficulty();

        for (WorldServer worldServer : SpongeImpl.getWorldManager().getWorlds()) {
            final boolean alreadySet = ((WorldInfoBridge) worldServer.getWorldInfo()).hasCustomDifficulty();
            this.bridge$updateWorldForDifficulty(worldServer, alreadySet ? worldServer.getWorldInfo().getDifficulty() : serverDifficulty, false);
        }
    }

    @Redirect(method = "getTabCompletions", at = @At(value = "INVOKE",
        target = "Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap = false))
    private ArrayList<String> onGetTabCompletionCreateList() {
        ArrayList<String> list = new ArrayList<>();
        this.currentTabCompletionOptions = list;
        return list;
    }

    @Inject(method = "getTabCompletions", at = @At(value = "RETURN", ordinal = 0))
    private void onTabCompleteChat(ICommandSender sender, String input, BlockPos pos, boolean usingBlock,
        CallbackInfoReturnable<List<String>> cir) {

        final List<String> completions = checkNotNull(this.currentTabCompletionOptions);
        this.currentTabCompletionOptions = null;

        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(sender);

            final Optional<Location<World>> target = Optional.of(new Location<>((World) sender.getEntityWorld(), VecHelper.toVector3i(pos)));
            final TabCompleteEvent.Chat event = SpongeEventFactory.createTabCompleteEventChat(Sponge.getCauseStackManager().getCurrentCause(),
                ImmutableList.copyOf(completions), completions, input, target, usingBlock);
            if (Sponge.getEventManager().post(event)) {
                completions.clear();
            }
        }
    }

    @Redirect(method = "getTabCompletions", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/ICommandManager;getTabCompletions"
        + "(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;Lnet/minecraft/util/math/BlockPos;)Ljava/util/List;"))
    private List<String> onGetTabCompletionOptions(ICommandManager manager, ICommandSender sender, String input, @Nullable BlockPos pos,
        ICommandSender sender_, String input_, BlockPos pos_, boolean hasTargetBlock) {

        final Location<World> target;
        if (pos == null) {
            target = null;
        } else {
            target = new Location<>((World) sender.getEntityWorld(), VecHelper.toVector3i(pos));
        }

        return ((SpongeCommandManager) SpongeImpl.getGame().getCommandManager()).getSuggestions((CommandSource) sender, input, target, hasTargetBlock);
    }

    @Inject(method = "setResourcePack(Ljava/lang/String;Ljava/lang/String;)V", at = @At("HEAD") )
    private void onSetResourcePack(String url, String hash, CallbackInfo ci) {
        if (url.length() == 0) {
            this.resourcePack = null;
        } else {
            try {
                this.resourcePack = SpongeResourcePack.create(url, hash);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onServerTickStart(CallbackInfo ci) {
        TimingsManager.FULL_SERVER_TICK.startTiming();
    }

    @Inject(method = "stopServer", at = @At(value = "HEAD"), cancellable = true) private void onStopServer(CallbackInfo ci) {
        // If the server is already stopping, don't allow stopServer to be called off the main thread
        // (from the shutdown handler thread in MinecraftServer)
        if ((Sponge.isServerAvailable() && !((MinecraftServer) Sponge.getServer()).isServerRunning() && !Sponge.getServer().isMainThread())) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onServerTickEnd(CallbackInfo ci) {
        int lastAnimTick = SpongeCommonEventFactory.lastAnimationPacketTick;
        int lastPrimaryTick = SpongeCommonEventFactory.lastPrimaryPacketTick;
        int lastSecondaryTick = SpongeCommonEventFactory.lastSecondaryPacketTick;
        if (SpongeCommonEventFactory.lastAnimationPlayer != null) {
            EntityPlayerMP player = SpongeCommonEventFactory.lastAnimationPlayer.get();
            if (player != null && lastAnimTick != lastPrimaryTick && lastAnimTick != lastSecondaryTick && lastAnimTick != 0 && lastAnimTick - lastPrimaryTick > 3 && lastAnimTick - lastSecondaryTick > 3) {
                BlockSnapshot blockSnapshot = BlockSnapshot.NONE;

                final RayTraceResult result = SpongeImplHooks.rayTraceEyes(player, SpongeImplHooks.getBlockReachDistance(player) + 1);
                // Hit non-air block
                if (result != null && result.getBlockPos() != null) {
                    return;
                }

                if (!player.getHeldItemMainhand().isEmpty() && SpongeCommonEventFactory.callInteractItemEventPrimary(player, player.getHeldItemMainhand(), EnumHand.MAIN_HAND, null, blockSnapshot).isCancelled()) {
                    SpongeCommonEventFactory.lastAnimationPacketTick = 0;
                    SpongeCommonEventFactory.lastAnimationPlayer = null;
                    return;
                }

                SpongeCommonEventFactory.callInteractBlockEventPrimary(player, player.getHeldItemMainhand(), EnumHand.MAIN_HAND, null);
            }
            SpongeCommonEventFactory.lastAnimationPlayer = null;
        }
        SpongeCommonEventFactory.lastAnimationPacketTick = 0;

        PhaseTracker.getInstance().ensureEmpty();

        TimingsManager.FULL_SERVER_TICK.stopTiming();
    }

    @Redirect(method = "addServerStatsToSnooper", at = @At(value = "FIELD", target = "Lnet/minecraft/world/WorldServer;provider:Lnet/minecraft/world/WorldProvider;", opcode = Opcodes.GETFIELD))
    private WorldProvider onGetWorldProviderForSnooper(WorldServer world) {
        if (((WorldBridge) world).isFake()) {
            return SpongeImpl.getWorldManager().getDefaultWorld().provider;
        }

        this.dimensionId = ((WorldProviderBridge) world.provider).bridge$getDimensionId();
        return world.provider;
    }

    @Redirect(method = "addServerStatsToSnooper", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;valueOf(I)Ljava/lang/Integer;", ordinal = 5))
    @Nullable
    private Integer onValueOfInteger(int original) {
        return this.dimensionId;
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 900))
    private int getSaveTickInterval(int tickInterval) {
        if (!isDedicatedServer()) {
            return tickInterval;
        } else if (!this.isServerRunning()) {
            // Don't auto-save while server is stopping
            return this.tickCounter + 1;
        }

        final int autoPlayerSaveInterval = SpongeImpl.getGlobalConfigAdapter().getConfig().getWorld().getAutoPlayerSaveInterval();
        if (autoPlayerSaveInterval > 0 && (this.tickCounter % autoPlayerSaveInterval == 0)) {
            this.getPlayerList().saveAllPlayerData();
        }

        this.saveAllWorlds(true);
        // force check to fail as we handle everything above
        return this.tickCounter + 1;
    }

    @Redirect(method = "callFromMainThread", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/Callable;call()Ljava/lang/Object;", remap = false))
    private Object onCall(Callable<?> callable) throws Exception {
        // This method can be called async while server is stopping
        if (this.serverStopped && !SpongeImplHooks.isMainThread()) {
            return callable.call();
        }

        Object value;
        try (BasicPluginContext context = PluginPhase.State.SCHEDULED_TASK.createPhaseContext()
            .source(callable)) {
            context.buildAndSwitch();
            value = callable.call();
        }
        return value;
    }

    @Redirect(method = "updateTimeLightAndEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;runTask(Ljava/util/concurrent/FutureTask;Lorg/apache/logging/log4j/Logger;)Ljava/lang/Object;"))
    private Object onRun(FutureTask<?> task, Logger logger) {
        return SpongeImplHooks.onUtilRunTask(task, logger);
    }

    @Inject(method = "addServerInfoToCrashReport", at = @At("RETURN"), cancellable = true)
    private void onCrashReport(CrashReport report, CallbackInfoReturnable<CrashReport> cir) {
        report.makeCategory("Sponge PhaseTracker").addDetail("Phase Stack", CauseTrackerCrashHandler.INSTANCE);
        cir.setReturnValue(report);
    }

    @Override
    public String bridge$getSubjectCollectionIdentifier() {
        return PermissionService.SUBJECTS_SYSTEM;
    }

    @Override
    public Tristate bridge$getPermissionDefault(String permission) {
        return Tristate.TRUE;
    }

    @Override
    public ICommandSender bridge$asMinecraftCommandSender() {
        return (MinecraftServer) (Object) this;
    }

    @Override
    public CommandSource bridge$asCommandSource() {
        return (CommandSource) this;
    }

    @Override
    public void bridge$prepareSpawnArea(WorldServer worldServer) {
        final WorldProperties worldProperties = (WorldProperties) worldServer.getWorldInfo();
        if (!((WorldInfoBridge) worldProperties).isValid() || !worldProperties.doesGenerateSpawnOnLoad()) {
            return;
        }

        final ServerChunkProviderBridge chunkProvider = (ServerChunkProviderBridge) worldServer.getChunkProvider();
        chunkProvider.bridge$setForceChunkRequests(true);

        try (GenerationContext<GenericGenerationContext> context = GenerationPhase.State.TERRAIN_GENERATION.createPhaseContext()
            .source(worldServer)
            .world( worldServer)) {
            context.buildAndSwitch();
            int i = 0;
            this.setUserMessage("menu.generatingTerrain");
            LOGGER.info("Preparing start region for world {} ({}/{})", worldServer.getWorldInfo().getWorldName(),
                ((DimensionType) (Object) worldServer.provider.getDimensionType()).getId(),
                ((WorldProviderBridge) worldServer.provider).bridge$getDimensionId());
            BlockPos blockpos = worldServer.getSpawnPoint();
            long j = MinecraftServer.getCurrentTimeMillis();
            for (int k = -192; k <= 192 && this.isServerRunning(); k += 16) {
                for (int l = -192; l <= 192 && this.isServerRunning(); l += 16) {
                    long i1 = MinecraftServer.getCurrentTimeMillis();

                    if (i1 - j > 1000L) {
                        this.outputPercentRemaining("Preparing spawn area", i * 100 / 625);
                        j = i1;
                    }

                    ++i;
                    worldServer.getChunkProvider().provideChunk(blockpos.getX() + k >> 4, blockpos.getZ() + l >> 4);
                }
            }
            this.clearCurrentTask();
        }
        chunkProvider.bridge$setForceChunkRequests(false);
    }

    @Override
    public void bridge$setSaveEnabled(boolean enabled) {
        this.isSavingEnabled = enabled;
    }

    @Override
    public void bridge$updateWorldForDifficulty(WorldServer worldServer, EnumDifficulty difficulty, boolean isCustom) {
        final boolean alreadySet = ((WorldInfoBridge) worldServer.getWorldInfo()).hasCustomDifficulty();

        if (worldServer.getWorldInfo().isHardcoreModeEnabled()) {
            difficulty = EnumDifficulty.HARD;
            worldServer.setAllowedSpawnTypes(true, true);
        } else if (SpongeImpl.getServer().isSinglePlayer()) {
            worldServer.setAllowedSpawnTypes(worldServer.getDifficulty() != EnumDifficulty.PEACEFUL, true);
        } else {
            worldServer.setAllowedSpawnTypes(this.allowSpawnMonsters(), this.getCanSpawnAnimals());
        }

        if (!alreadySet) {
            if (!isCustom) {
                ((WorldInfoBridge) worldServer.getWorldInfo()).forceSetDifficulty(difficulty);
            } else {
                worldServer.getWorldInfo().setDifficulty(difficulty);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
