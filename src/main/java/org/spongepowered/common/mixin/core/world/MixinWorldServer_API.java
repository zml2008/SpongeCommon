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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.SessionLockException;
import org.apache.logging.log4j.Level;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.weather.Weather;
import org.spongepowered.api.world.weather.Weathers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.block.SpongeBlockSnapshot;
import org.spongepowered.common.block.SpongeBlockSnapshotBuilder;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.category.WorldCategory;
import org.spongepowered.common.config.type.GeneralConfigBase;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseContext;
import org.spongepowered.common.event.tracking.PhaseData;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.general.GeneralPhase;
import org.spongepowered.common.event.tracking.phase.plugin.BasicPluginContext;
import org.spongepowered.common.event.tracking.phase.plugin.PluginPhase;
import org.spongepowered.common.interfaces.data.IMixinCustomDataHolder;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.util.NonNullArrayList;
import org.spongepowered.common.util.VecHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer_API extends MixinWorld_API {

    @Shadow public abstract PlayerChunkMap getPlayerChunkMap();

    @Shadow @Nullable public abstract net.minecraft.entity.Entity getEntityFromUuid(UUID uuid);



    @Override
    public boolean isLoaded() {
        return true; // TODO - Zidane
    }

    @Override
    public UUID getUniqueId() {
        return checkNotNull(this.getProperties().getUniqueId(), "World Properties has a null UUID");
    }

    @Override
    public Path getDirectory() {
        return null; // TODO - Zidane
    }

    @Override
    public Optional<Entity> getEntity(UUID uuid) {
        return Optional.ofNullable((Entity) this.getEntityFromUuid(uuid));
    }
    @Override
    public boolean setBlock(int x, int y, int z, BlockState blockState, BlockChangeFlag flag) {
        checkBlockBounds(x, y, z);
        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        final PhaseData peek = phaseTracker.getCurrentPhaseData();
        boolean isWorldGen = peek.state.isWorldGeneration();
        boolean handlesOwnCompletion = peek.state.handlesOwnStateCompletion();
        if (!isWorldGen) {
            checkArgument(flag != null, "BlockChangeFlag cannot be null!");
        }
        try (PhaseContext<?> context = isWorldGen || handlesOwnCompletion
                                       ? null
                                       : PluginPhase.State.BLOCK_WORKER.createPhaseContext()) {
            if (context != null) {
                context.buildAndSwitch();
            }
            return setBlockState(new BlockPos(x, y, z), (IBlockState) blockState, flag);
        }
    }
    @Override
    public BlockSnapshot createSnapshot(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState currentState = this.getBlockState(pos);
        return this.createSpongeBlockSnapshot(currentState, currentState.getActualState((WorldServer) (Object) this, pos), pos,
            // PHYSICS_OBSERVER does not actually perform any changes except running physics
            // and notifying observer blocks. It does NOT perform Neighbor notifications, and
            // it DOES tell the client about the block change.
            BlockChangeFlags.PHYSICS_OBSERVER);
    }

    @Override
    public SpongeBlockSnapshot createSpongeBlockSnapshot(IBlockState state, IBlockState extended, BlockPos pos, BlockChangeFlag updateFlag) {
        final SpongeBlockSnapshotBuilder builder = new SpongeBlockSnapshotBuilder();
        builder.reset();
        builder.blockState((BlockState) state)
            .worldId(this.getUniqueId())
            .position(VecHelper.toVector3i(pos));
        Optional<UUID> creator = getCreator(pos.getX(), pos.getY(), pos.getZ());
        Optional<UUID> notifier = getNotifier(pos.getX(), pos.getY(), pos.getZ());
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
                TileEntity tile = (TileEntity) tileEntity;
                for (DataManipulator<?, ?> manipulator : ((IMixinCustomDataHolder) tile).getCustomManipulators()) {
                    builder.add(manipulator);
                }
                NBTTagCompound nbt = new NBTTagCompound();
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
    public Collection<Entity> spawnEntities(Iterable<? extends Entity> entities) {
        List<Entity> entitiesToSpawn = new NonNullArrayList<>();
        entities.forEach(entitiesToSpawn::add);
        final SpawnEntityEvent.Custom event = SpongeEventFactory
            .createSpawnEntityEventCustom(Sponge.getCauseStackManager().getCurrentCause(), entitiesToSpawn);
        if (Sponge.getEventManager().post(event)) {
            return ImmutableList.of();
        }
        for (Entity entity : event.getEntities()) {
            EntityUtil.processEntitySpawn(entity, Optional::empty);
        }

        return event.getEntities().stream().filter(Entity::isLoaded).collect(ImmutableList.toImmutableList());
    }
    @Override
    public boolean save() throws IOException {
        if (!getChunkProvider().canSave()) {
            return false;
        }

        // TODO: Expose flush parameter in SpongeAPI?
        try {
            WorldManager.saveWorld((WorldServer) (Object) this, true);
        } catch (SessionLockException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    @Override
    public void triggerExplosion(org.spongepowered.api.world.explosion.Explosion explosion) {
        checkNotNull(explosion, "explosion");
        triggerInternalExplosion(explosion, e -> GeneralPhase.State.EXPLOSION.createPhaseContext().explosion(e));
    }

    /**
     * Based off {@link WorldServer#newExplosion(net.minecraft.entity.Entity, double, double, double, float, boolean, boolean)}.
     */
    @Override
    public Explosion triggerInternalExplosion(org.spongepowered.api.world.explosion.Explosion explosion,
        Function<Explosion, PhaseContext<?>> contextCreator) {
        // Sponge start
        this.processingExplosion = true;
        final Explosion originalExplosion = (Explosion) explosion;
        // Set up the pre event
        final ExplosionEvent.Pre
            event =
            SpongeEventFactory.createExplosionEventPre(Sponge.getCauseStackManager().getCurrentCause(),
                explosion, this);
        if (SpongeImpl.postEvent(event)) {
            this.processingExplosion = false;
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
            .source(((Optional) explosion.getSourceExplosive()).orElse(this))
            .buildAndSwitch()) {
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

            for (EntityPlayer entityplayer : this.playerEntities) {
                if (entityplayer.getDistanceSq(x, y, z) < 4096.0D) {
                    ((EntityPlayerMP) entityplayer).connection
                        .sendPacket(new SPacketExplosion(x, y, z, strength, mcExplosion.getAffectedBlockPositions(),
                            mcExplosion.getPlayerKnockbackMap().get(entityplayer)));
                }
            }

            // Sponge Start - end processing
            this.processingExplosion = false;
        }
        // Sponge End
        return mcExplosion;
    }

    // ------------------------- Start Cause Tracking overrides of Minecraft World methods ----------

    @Override
    public boolean spawnEntity(Entity entity) {
        checkNotNull(entity, "The entity cannot be null!");
        if (PhaseTracker.isEntitySpawnInvalid(entity)) {
            return true;
        }
        final PhaseTracker phaseTracker = PhaseTracker.getInstance();
        final IPhaseState<?> state = phaseTracker.getCurrentState();
        if (!state.alreadyCapturingEntitySpawns()) {
            try (final BasicPluginContext context = PluginPhase.State.CUSTOM_SPAWN.createPhaseContext()
                .addCaptures()) {
                context.buildAndSwitch();
                phaseTracker.spawnEntityWithCause(this, entity);
                return true;
            }
        }
        return phaseTracker.spawnEntityWithCause(this, entity);
    }

    @Override
    public Weather getWeather() {
        if (this.worldInfo.isThundering()) {
            return Weathers.THUNDER_STORM;
        } else if (this.worldInfo.isRaining()) {
            return Weathers.RAIN;
        } else {
            return Weathers.CLEAR;
        }
    }

    @Override
    public void setWeather(Weather weather) {
        this.setWeather(weather, (300 + this.rand.nextInt(600)) * 20);
    }

    @Override
    public void setWeather(Weather weather, Duration duration) {
        this.prevWeather = getWeather();
        if (weather.equals(Weathers.CLEAR)) {
            this.worldInfo.setClearWeatherTime((int) duration);
            this.worldInfo.setRainTime(0);
            this.worldInfo.setThunderTime(0);
            this.worldInfo.setRaining(false);
            this.worldInfo.setThundering(false);
        } else if (weather.equals(Weathers.RAIN)) {
            this.worldInfo.setClearWeatherTime(0);
            this.worldInfo.setRainTime((int) duration);
            this.worldInfo.setThunderTime((int) duration);
            this.worldInfo.setRaining(true);
            this.worldInfo.setThundering(false);
        } else if (weather.equals(Weathers.THUNDER_STORM)) {
            this.worldInfo.setClearWeatherTime(0);
            this.worldInfo.setRainTime((int) duration);
            this.worldInfo.setThunderTime((int) duration);
            this.worldInfo.setRaining(true);
            this.worldInfo.setThundering(true);
        }
    }
    @Override
    public long getWeatherStartTime() {
        return this.weatherStartTime;
    }

    @Override
    public void setWeatherStartTime(long weatherStartTime) {
        this.weatherStartTime = weatherStartTime;
    }

    @Override
    public int getChunkGCTickInterval() {
        return this.chunkGCTickInterval;
    }

    @Override
    public long getChunkUnloadDelay() {
        return this.chunkUnloadDelay;
    }

    @Override
    public int getViewDistance() {
        return this.playerChunkMap.playerViewRadius;
    }

    @Override
    public void setViewDistance(final int viewDistance) {
        this.setMemoryViewDistance(viewDistance);
        final SpongeConfig<? extends GeneralConfigBase> config = this.getActiveConfig();
        // don't use the parameter, use the field that has been clamped
        config.getConfig().getWorld().setViewDistance(this.playerChunkMap.playerViewRadius);
        config.save();
    }

    private void setMemoryViewDistance(final int viewDistance) {
        this.playerChunkMap.setPlayerViewRadius(viewDistance);
    }

    @Override
    public void resetViewDistance() {
        this.setViewDistance(this.chooseViewDistanceValue(WorldCategory.USE_SERVER_VIEW_DISTANCE));
    }

    private int chooseViewDistanceValue(final int value) {
        if (value == WorldCategory.USE_SERVER_VIEW_DISTANCE) {
            return this.server.getPlayerList().getViewDistance();
        }
        return value;
    }
}
