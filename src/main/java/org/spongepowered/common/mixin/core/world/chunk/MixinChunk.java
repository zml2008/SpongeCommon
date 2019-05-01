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
package org.spongepowered.common.mixin.core.world.chunk;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumLightType;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk.EnumCreateEntityType;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.volume.biome.worker.MutableBiomeVolumeWorker;
import org.spongepowered.api.world.chunk.Chunk;
import org.spongepowered.api.world.volume.EntityHit;
import org.spongepowered.api.world.volume.block.worker.MutableBlockVolumeWorker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.entity.PlayerTracker;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.event.tracking.IPhaseState;
import org.spongepowered.common.event.tracking.PhaseData;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.event.tracking.phase.generation.GenerationPhase;
import org.spongepowered.common.interfaces.IMixinCachable;
import org.spongepowered.common.interfaces.IMixinChunk;
import org.spongepowered.common.interfaces.block.tile.IMixinTileEntity;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.interfaces.world.IMixinWorld_Impl;
import org.spongepowered.common.interfaces.world.gen.IMixinChunkProviderServer;
import org.spongepowered.common.util.SpongeHooks;
import org.spongepowered.common.world.extent.ExtentViewDownsize;
import org.spongepowered.common.world.extent.worker.SpongeMutableBiomeVolumeWorker;
import org.spongepowered.common.world.extent.worker.SpongeMutableBlockVolumeWorker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

@NonnullByDefault
@Mixin(net.minecraft.world.chunk.Chunk.class)
@Implements(@Interface(iface = IMixinChunk.class, prefix = "imixinchunk$"))
public abstract class MixinChunk implements Chunk, IMixinChunk, IMixinCachable {

    private org.spongepowered.api.world.World sponge_world;
    private UUID uuid;
    private long scheduledForUnload = -1; // delay chunk unloads
    private boolean persistedChunk = false;
    private boolean isSpawning = false;
    private net.minecraft.world.chunk.Chunk[] neighbors = new net.minecraft.world.chunk.Chunk[4];
    private long cacheKey;
    private static final Direction[] CARDINAL_DIRECTIONS = new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    @Shadow @Final private World world;
    @Shadow @Final public int x;
    @Shadow @Final public int z;
    @Shadow @Final private ChunkSection[] sections;
    @Shadow @Final private ClassInheritanceMultiMap<Entity>[] entityLists;
    @Shadow @Final private Map<BlockPos, TileEntity> tileEntities;
    @Shadow @Final private Map<Heightmap.Type, Heightmap> heightMap;
    @Shadow private boolean loaded;
    @Shadow private boolean dirty;

    // @formatter:off
    @Shadow @Nullable public abstract TileEntity getTileEntity(BlockPos pos, EnumCreateEntityType p_177424_2_);
    @Shadow public abstract boolean isEmpty();
    @Shadow public abstract void generateSkylightMap();
    @Shadow public abstract IBlockState getBlockState(BlockPos pos);
    @Shadow private void propagateSkylightOcclusion(int x, int z) { };

    // @formatter:on
    @Shadow public abstract int getLightFor(EnumLightType p_177413_1_, BlockPos p_177413_2_);


    @Intrinsic
    public boolean imixinchunk$isEmpty() {
        return this.isEmpty();
    }

    @Override
    public long getCacheKey() {
        return this.cacheKey;
    }

    @Override
    public void markChunkDirty() {
        this.dirty = true;
    }

    @Override
    public boolean isChunkLoaded() {
        return this.loaded;
    }

    @Override
    public boolean isQueuedForUnload() {
        return this.unloadQueued;
    }

    @Override
    public boolean isPersistedChunk() {
        return this.persistedChunk;
    }

    @Override
    public void setPersistedChunk(boolean flag) {
        this.persistedChunk = flag;
        // update persisted status for entities and TE's
        for (TileEntity tileEntity : this.tileEntities.values()) {
            ((IMixinTileEntity) tileEntity).setActiveChunk(this);
        }
        for (ClassInheritanceMultiMap<Entity> entityList : this.entityLists) {
            for (Entity entity : entityList) {
                ((IMixinEntity) entity).setActiveChunk(this);
            }
        }
    }

    @Override
    public boolean isSpawning() {
        return this.isSpawning;
    }

    @Override
    public void setIsSpawning(boolean spawning) {
        this.isSpawning = spawning;
    }

    @Inject(method = "addEntity", at = @At("RETURN"))
    private void onChunkAddEntity(Entity entityIn, CallbackInfo ci) {
        ((IMixinEntity) entityIn).setActiveChunk(this);
    }

    @Inject(method = "addTileEntity(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntity;validate()V"))
    private void onChunkAddTileEntity(BlockPos pos, TileEntity tileEntityIn, CallbackInfo ci) {
        ((IMixinTileEntity) tileEntityIn).setActiveChunk(this);
    }

    @Inject(method = "removeEntityAtIndex", at = @At("RETURN"))
    private void onChunkRemoveEntityAtIndex(Entity entityIn, int index, CallbackInfo ci) {
        ((IMixinEntity) entityIn).setActiveChunk(null);
    }

    @Redirect(method = "removeTileEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/TileEntity;invalidate()V"))
    private void onChunkRemoveTileEntity(TileEntity tileEntityIn) {
        ((IMixinTileEntity) tileEntityIn).setActiveChunk(null);
        tileEntityIn.remove();
    }

    @Inject(method = "onLoad", at = @At("RETURN"))
    public void onLoadReturn(CallbackInfo ci) {
        for (Direction direction : CARDINAL_DIRECTIONS) {
            Vector3i neighborPosition = this.getChunkPosition().add(direction.asBlockOffset());
            IMixinChunkProviderServer spongeChunkProvider = (IMixinChunkProviderServer) this.world.getChunkProvider();
            net.minecraft.world.chunk.Chunk neighbor = spongeChunkProvider.getLoadedChunkWithoutMarkingActive
                    (neighborPosition.getX(), neighborPosition.getZ());
            if (neighbor != null) {
                int neighborIndex = directionToIndex(direction);
                int oppositeNeighborIndex = directionToIndex(direction.getOpposite());
                this.setNeighborChunk(neighborIndex, neighbor);
                ((IMixinChunk) neighbor).setNeighborChunk(oppositeNeighborIndex, (net.minecraft.world.chunk.Chunk) (Object) this);
            }
        }

        if (ShouldFire.LOAD_CHUNK_EVENT) {
            SpongeImpl.postEvent(SpongeEventFactory.createLoadChunkEvent(Sponge.getCauseStackManager().getCurrentCause(), (Chunk) this));
        }
        if (!this.world.isRemote) {
            SpongeHooks.logChunkLoad(this.world, this.chunkPos);
        }
    }

    @Inject(method = "onUnload", at = @At("RETURN"))
    public void onUnload(CallbackInfo ci) {
        for (Direction direction : CARDINAL_DIRECTIONS) {
            Vector3i neighborPosition = this.getChunkPosition().add(direction.asBlockOffset());
            IMixinChunkProviderServer spongeChunkProvider = (IMixinChunkProviderServer) this.world.getChunkProvider();
            net.minecraft.world.chunk.Chunk neighbor = spongeChunkProvider.getLoadedChunkWithoutMarkingActive
                    (neighborPosition.getX(), neighborPosition.getZ());
            if (neighbor != null) {
                int neighborIndex = directionToIndex(direction);
                int oppositeNeighborIndex = directionToIndex(direction.getOpposite());
                this.setNeighborChunk(neighborIndex, null);
                ((IMixinChunk) neighbor).setNeighborChunk(oppositeNeighborIndex, null);
            }
        }

        if (!this.world.isRemote) {
            SpongeImpl.postEvent(SpongeEventFactory.createUnloadChunkEvent(Sponge.getCauseStackManager().getCurrentCause(), (Chunk) this));
            SpongeHooks.logChunkUnload(this.world, this.chunkPos);
        }
    }

    @Inject(method = "getEntitiesWithinAABBForEntity", at = @At(value = "RETURN"))
    public void onGetEntitiesWithinAABBForEntity(@Nullable Entity entityIn, AxisAlignedBB aabb, List<Entity> listToFill, Predicate<? super Entity> p_177414_4_,
            CallbackInfo ci) {
        if (this.world.isRemote || PhaseTracker.getInstance().getCurrentPhaseData().state.ignoresEntityCollisions()) {
            return;
        }

        if (listToFill.size() == 0) {
            return;
        }

        if (!ShouldFire.COLLIDE_ENTITY_EVENT) {
            return;
        }

        CollideEntityEvent event = SpongeCommonEventFactory.callCollideEntityEvent(this.world, entityIn, listToFill);
        final PhaseData peek = PhaseTracker.getInstance().getCurrentPhaseData();

        if (event == null || event.isCancelled()) {
            if (event == null && !peek.state.isTicking()) {
                return;
            }
            listToFill.clear();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "getEntitiesOfTypeWithinAABB", at = @At(value = "RETURN"))
    public void onGetEntitiesOfTypeWithinAAAB(Class<? extends Entity> entityClass, AxisAlignedBB aabb, List listToFill, Predicate<Entity> p_177430_4_,
            CallbackInfo ci) {
        if (this.world.isRemote || PhaseTracker.getInstance().getCurrentContext().state.ignoresEntityCollisions()) {
            return;
        }

        if (listToFill.size() == 0) {
            return;
        }

        CollideEntityEvent event = SpongeCommonEventFactory.callCollideEntityEvent(this.world, null, listToFill);
        final PhaseData peek = PhaseTracker.getInstance().getCurrentPhaseData();

        if (event == null || event.isCancelled()) {
            if (event == null && !peek.state.isTicking()) {
                return;
            }
            listToFill.clear();
        }
    }


    /**
     * @author blood
     * @reason cause tracking
     *
     * @param pos The position to set
     * @param state The state to set
     * @return The changed state
     */
    @Nullable
    @Overwrite
    public IBlockState setBlockState(BlockPos pos, IBlockState state, boolean b) {
        IBlockState iblockstate1 = this.getBlockState(pos);

        // Sponge - reroute to new method that accepts snapshot to prevent a second snapshot from being created.
        return setBlockState(pos, state, iblockstate1, null, BlockChangeFlags.ALL);
    }

    @Nullable
    @Override
    public IBlockState setBlockState(BlockPos pos, IBlockState newState, IBlockState currentState, @Nullable BlockSnapshot originalBlockSnapshot) {
        return this.setBlockState(pos, newState, currentState, originalBlockSnapshot, BlockChangeFlags.ALL);
    }

    /**
     * @author blood - November 2015
     * @author gabizou - updated April 10th, 2016 - Add cause tracking refactor changes
     * @author gabizou - Updated June 26th, 2018 - Bulk capturing changes
     * @author Aaron1011 - February 6th, 2018 - Update for 1.13
     *
     *
     * @param pos The position changing
     * @param newState The new state
     * @param currentState The current state - passed in from either chunk or world
     * @param newBlockSnapshot The new snapshot. This can be null when calling {@link MixinChunk#setBlockState(BlockPos, IBlockState)} directly,
     *      as there's no block snapshot to change.
     * @return The changed block state if not null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Nullable
    public IBlockState setBlockState(BlockPos pos, IBlockState newState, IBlockState currentState, @Nullable BlockSnapshot newBlockSnapshot, BlockChangeFlag flag, boolean bool) {
        int xPos = pos.getX() & 15;
        int yPos = pos.getY();
        int zPos = pos.getZ() & 15;
        int currentHeight = ((Heightmap)this.heightMap.get(Heightmap.Type.LIGHT_BLOCKING)).getHeight(xPos, zPos);

        // Sponge start - remove blockstate equality check, as this is hanlded in world.setBlockState
        //if (lvt_8_1_ == p_177436_2_) {
        //    return null;
        //} else {
        // Sponge end

        Block newBlock = newState.getBlock();
        Block currentBlock = currentState.getBlock();

        ChunkSection extendedblockstorage = this.sections[yPos >> 4];
        // Sponge - make this final so we don't change it later
        final boolean requiresNewLightCalculations;

        // Sponge - Forge moves this up here
        int newBlockLightOpacity = SpongeImplHooks.getBlockLightOpacity(newState, (IBlockReader) this.world, pos);

        if (extendedblockstorage == net.minecraft.world.chunk.Chunk.EMPTY_SECTION) {
            if (newState.isAir()) {
                return null;
            }

            extendedblockstorage = this.sections[yPos >> 4] = new ChunkSection(yPos >> 4 << 4, this.world.dimension.hasSkyLight());
            requiresNewLightCalculations = yPos >= currentHeight;
            // Sponge Start - properly initialize variable
        } else {
            requiresNewLightCalculations = false;
        }
        // Sponge end

        extendedblockstorage.set(xPos, yPos & 15, zPos, newState);
        ((Heightmap)this.heightMap.get(Heightmap.Type.MOTION_BLOCKING)).update(xPos, yPos, zPos, newState);
        ((Heightmap)this.heightMap.get(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES)).update(xPos, yPos, zPos, newState);
        ((Heightmap)this.heightMap.get(Heightmap.Type.OCEAN_FLOOR)).update(xPos, yPos, zPos, newState);
        ((Heightmap)this.heightMap.get(Heightmap.Type.WORLD_SURFACE)).update(xPos, yPos, zPos, newState);

        if (!this.world.isRemote) {
            currentState.onReplaced(this.world, pos, newState, bool);
        } else if (newBlock != currentBlock && SpongeImplHooks.hasBlockTileEntity(currentBlock, currentState)) { // Sponge - use SpongeImplsHooks for Forge compat
            this.world.removeTileEntity(pos);
        }


        // TODO 1.13: I believe this is now unecessary
        /*
        // if (block1 != block) // Sponge - Forge removes this change.
        {
            if (!this.world.isRemote) {
                // Sponge - Forge adds this change for block changes to only fire events when necessary
                if (currentState.getBlock() != newState.getBlock()) {
                    currentBlock.breakBlock(this.world, pos, currentState);
                }
                // Sponge - Add several tile entity hook checks. Mainly for forge added hooks, but these
                // still work by themselves in vanilla.
                TileEntity te = this.getTileEntity(pos, EnumCreateEntityType.CHECK);
                if (te != null && SpongeImplHooks.shouldRefresh(te, this.world, pos, currentState, newState)) {
                    this.world.removeTileEntity(pos);
                }
            // } else if (currentBlock instanceof ITileEntityProvider) { // Sponge - remove since forge has a special hook we need to add here
            } else if (SpongeImplHooks.hasBlockTileEntity(currentBlock, currentState)) {
                TileEntity tileEntity = this.getTileEntity(pos, EnumCreateEntityType.CHECK);
                // Sponge - Add hook for refreshing, because again, forge hooks.
                if (tileEntity != null && SpongeImplHooks.shouldRefresh(tileEntity, this.world, pos, currentState, newState)) {
                    this.world.removeTileEntity(pos);
                }
            }
        }*/


        final IBlockState blockAfterSet = extendedblockstorage.get(xPos, modifiedY, zPos);
        if (blockAfterSet.getBlock() != newBlock) {
            return null;
        }

        // } else { // Sponge - remove unnecessary else
        if (requiresNewLightCalculations) {
            this.generateSkylightMap();
        } else {

            // int newBlockLightOpacity = state.getLightOpacity(); - Sponge Forge moves this all the way up before tile entities are removed.
            // int postNewBlockLightOpacity = newState.getLightOpacity(this.worldObj, pos); - Sponge use the SpongeImplHooks for forge compatibility
            int postNewBlockLightOpacity = SpongeImplHooks.getBlockLightOpacity(newState, this.world, pos);
            // Sponge End
            this.relightBlock(xPos, yPos, zPos);

            if (newBlockLightOpacity != postNewBlockLightOpacity && (newBlockLightOpacity < postNewBlockLightOpacity || this.getLightFor(EnumLightType.SKY, pos) > 0 || this.getLightFor(EnumLightType.BLOCK, pos) > 0)) {
                this.propagateSkylightOcclusion(xPos, zPos);
            }
        }

        if (SpongeImplHooks.hasBlockTileEntity(currentBlock, currentState)) { // Sponge - use SpongeImplHooks for Forge compat
            TileEntity tileentity = this.getTileEntity(pos, net.minecraft.world.chunk.Chunk.EnumCreateEntityType.CHECK);
            if (tileentity != null) {
                tileentity.updateContainingBlockInfo();
            }
        }

        // Sponge start -add phase checks
        if (!((IMixinWorld_Impl) this.world).isFake() && currentBlock != newBlock) {
            final PhaseData peek = PhaseTracker.getInstance().getCurrentPhaseData();
            final boolean isBulkCapturing = ((IPhaseState) peek.state).doesBulkBlockCapture(peek.context);
            // Sponge start - Ignore block activations during block placement captures unless it's
            // a BlockContainer. Prevents blocks such as TNT from activating when cancelled.
            if (!isBulkCapturing || SpongeImplHooks.hasBlockTileEntity(newBlock, newState)) {
                // The new block state is null if called directly from Chunk#setBlockState(BlockPos, IBlockState)
                // If it is null, then directly call the onBlockAdded logic.
                if (flag.performBlockPhysics()) {
                    newState.onBlockAdded(this.world, pos, currentState);
                }
            }
            // Sponge end
        }

        // Sponge Start - Use SpongeImplHooks for forge compatibility
        // if (block instanceof ITileEntityProvider) { // Sponge
        if (SpongeImplHooks.hasBlockTileEntity(newBlock, newState)) {
            // Sponge End
            TileEntity tileentity = this.getTileEntity(pos, EnumCreateEntityType.CHECK);

            if (tileentity == null) {
                // Sponge Start - use SpongeImplHooks for forge compatibility
                if (!((IMixinWorld_Impl) this.world).isFake()) { // Surround with a server check
                    // tileentity = ((ITileEntityProvider)block).createNewTileEntity(this.worldObj, block.getMetaFromState(state)); // Sponge
                    tileentity = SpongeImplHooks.createTileEntity(newBlock, this.world, newState);
                    final User owner = PhaseTracker.getInstance().getCurrentContext().getOwner().orElse(null);
                    // If current owner exists, transfer it to newly created TE pos
                    // This is required for TE's that get created during move such as pistons and ComputerCraft turtles.
                    if (owner != null) {
                        this.addTrackedBlockPosition(newBlock, pos, owner, PlayerTracker.Type.OWNER);
                    }
                }

                // Sponge End
                this.world.setTileEntity(pos, tileentity);
            } else {
                tileentity.updateContainingBlockInfo();
            }
        }

        this.dirty = true;
        return currentState;
    }

    // These methods are enabled in MixinChunk_Tracker as a Mixin plugin

    @Override
    public void addTrackedBlockPosition(Block block, BlockPos pos, User user, PlayerTracker.Type trackerType) {

    }

    @Override
    public Map<Integer, PlayerTracker> getTrackedIntPlayerPositions() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Short, PlayerTracker> getTrackedShortPlayerPositions() {
        return Collections.emptyMap();
    }

    @Override
    public Optional<User> getBlockOwner(BlockPos pos) {
        return Optional.empty();
    }

    @Override
    public Optional<UUID> getBlockOwnerUUID(BlockPos pos) {
        return Optional.empty();
    }

    @Override
    public Optional<User> getBlockNotifier(BlockPos pos) {
        return Optional.empty();
    }

    @Override
    public Optional<UUID> getBlockNotifierUUID(BlockPos pos) {
        return Optional.empty();
    }

    @Override
    public void setBlockNotifier(BlockPos pos, @Nullable UUID uuid) {

    }

    @Override
    public void setBlockCreator(BlockPos pos, @Nullable UUID uuid) {

    }

    @Override
    public void setTrackedIntPlayerPositions(Map<Integer, PlayerTracker> trackedPositions) {
    }

    @Override
    public void setTrackedShortPlayerPositions(Map<Short, PlayerTracker> trackedPositions) {
    }



    @Redirect(method = "populate(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/gen/IChunkGenerator;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/IChunkProvider;getLoadedChunk(II)Lnet/minecraft/world/chunk/Chunk;"))
    public net.minecraft.world.chunk.Chunk onPopulateLoadChunk(IChunkProvider chunkProvider, int x, int z) {
        // Don't mark chunks as active
        return ((IMixinChunkProviderServer) chunkProvider).getLoadedChunkWithoutMarkingActive(x, z);
    }

    @Inject(method = "populate(Lnet/minecraft/world/gen/IChunkGenerator;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/IChunkGenerator;populate(II)V"))
    private void onPopulate(IChunkGenerator generator, CallbackInfo callbackInfo) {
        if (!this.world.isRemote) {
            GenerationPhase.State.TERRAIN_GENERATION.createPhaseContext()
                    .world(this.world)
                    .buildAndSwitch();
        }
    }


    @Inject(method = "populate(Lnet/minecraft/world/gen/IChunkGenerator;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;markDirty()V"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/IChunkGenerator;populate(II)V"))
    )
    private void onPopulateFinish(IChunkGenerator generator, CallbackInfo info) {
        if (!this.world.isRemote) {
            PhaseTracker.getInstance().getCurrentContext().close();
        }
    }

    @Override
    public void getIntersectingEntities(Vector3d start, Vector3d direction, double distance,
            java.util.function.Predicate<EntityHit> filter, double entryY, double exitY, Set<EntityHit> intersections) {
        // Order the entry and exit y coordinates by magnitude
        final double yMin = Math.min(entryY, exitY);
        final double yMax = Math.max(entryY, exitY);
        // Added offset matches the one in Chunk.getEntitiesWithinAABBForEntity
        final int lowestSubChunk = GenericMath.clamp(GenericMath.floor((yMin - 2) / 16D), 0, this.entityLists.length - 1);
        final int highestSubChunk = GenericMath.clamp(GenericMath.floor((yMax + 2) / 16D), 0, this.entityLists.length - 1);
        // For each sub-chunk, perform intersections in its entity list
        for (int i = lowestSubChunk; i <= highestSubChunk; i++) {
            getIntersectingEntities(this.entityLists[i], start, direction, distance, filter, intersections);
        }
    }

    private void getIntersectingEntities(Collection<Entity> entities, Vector3d start, Vector3d direction, double distance,
            java.util.function.Predicate<EntityHit> filter, Set<EntityHit> intersections) {
        // Check each entity in the list
        for (Entity entity : entities) {
            final org.spongepowered.api.entity.Entity spongeEntity = (org.spongepowered.api.entity.Entity) entity;
            final Optional<AABB> box = spongeEntity.getBoundingBox();
            // Can't intersect if the entity doesn't have a bounding box
            if (!box.isPresent()) {
                continue;
            }
            // Ignore entities that didn't intersect
            final Optional<Tuple<Vector3d, Vector3d>> optionalIntersection = box.get().intersects(start, direction);
            if (!optionalIntersection.isPresent()) {
                continue;
            }
            // Check that the entity isn't too far away
            final Tuple<Vector3d, Vector3d> intersection = optionalIntersection.get();
            final double distanceSquared = intersection.getFirst().sub(start).lengthSquared();
            if (distanceSquared > distance * distance) {
                continue;
            }
            // Now test the filter on the entity and intersection
            final EntityHit hit = new EntityHit(spongeEntity, intersection.getFirst(), intersection.getSecond(), Math.sqrt(distanceSquared));
            if (!filter.test(hit)) {
                continue;
            }
            // If everything passes we have an intersection!
            intersections.add(hit);
            // If the entity has part, recurse on these
            final Entity[] parts = entity.getParts();
            if (parts != null && parts.length > 0) {
                getIntersectingEntities(Arrays.asList(parts), start, direction, distance, filter, intersections);
            }
        }
    }

    // Fast neighbor methods for internal use
    @Override
    public void setNeighborChunk(int index, @Nullable net.minecraft.world.chunk.Chunk chunk) {
        this.neighbors[index] = chunk;
    }

    @Nullable
    @Override
    public net.minecraft.world.chunk.Chunk getNeighborChunk(int index) {
        return this.neighbors[index];
    }

    @Override
    public List<net.minecraft.world.chunk.Chunk> getNeighbors() {
        List<net.minecraft.world.chunk.Chunk> neighborList = new ArrayList<>();
        for (net.minecraft.world.chunk.Chunk neighbor : this.neighbors) {
            if (neighbor != null) {
                neighborList.add(neighbor);
            }
        }
        return neighborList;
    }

    @Override
    public boolean areNeighborsLoaded() {
        for (int i = 0; i < 4; i++) {
            if (this.neighbors[i] == null) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void setNeighbor(Direction direction, @Nullable Chunk neighbor) {
        this.neighbors[directionToIndex(direction)] = (net.minecraft.world.chunk.Chunk) neighbor;
    }

    @Override
    public Optional<Chunk> getNeighbor(Direction direction, boolean shouldLoad) {
        checkNotNull(direction, "direction");
        checkArgument(!direction.isSecondaryOrdinal(), "Secondary cardinal directions can't be used here");

        if (direction.isUpright() || direction == Direction.NONE) {
            return Optional.of(this);
        }

        int index = directionToIndex(direction);
        Direction secondary = getSecondaryDirection(direction);
        Chunk neighbor = null;
        neighbor = (Chunk) this.neighbors[index];

        if (neighbor == null && shouldLoad) {
            Vector3i neighborPosition = this.getPosition().add(getCardinalDirection(direction).asBlockOffset());
            Optional<Chunk> cardinal = this.getWorld().loadChunk(neighborPosition, true);
            if (cardinal.isPresent()) {
                neighbor = cardinal.get();
            }
        }

        if (neighbor != null && secondary != Direction.NONE) {
            return neighbor.getNeighbor(secondary, shouldLoad);
        }

        return Optional.ofNullable(neighbor);
    }

    private static int directionToIndex(Direction direction) {
        switch (direction) {
            case NORTH:
            case NORTHEAST:
            case NORTHWEST:
                return 0;
            case SOUTH:
            case SOUTHEAST:
            case SOUTHWEST:
                return 1;
            case EAST:
                return 2;
            case WEST:
                return 3;
            default:
                throw new IllegalArgumentException("Unexpected direction");
        }
    }

    private static Direction getCardinalDirection(Direction direction) {
        switch (direction) {
            case NORTH:
            case NORTHEAST:
            case NORTHWEST:
                return Direction.NORTH;
            case SOUTH:
            case SOUTHEAST:
            case SOUTHWEST:
                return Direction.SOUTH;
            case EAST:
                return Direction.EAST;
            case WEST:
                return Direction.WEST;
            default:
                throw new IllegalArgumentException("Unexpected direction");
        }
    }

    private static Direction getSecondaryDirection(Direction direction) {
        switch (direction) {
            case NORTHEAST:
            case SOUTHEAST:
                return Direction.EAST;
            case NORTHWEST:
            case SOUTHWEST:
                return Direction.WEST;
            default:
                return Direction.NONE;
        }
    }

    @Override
    public long getScheduledForUnload() {
        return this.scheduledForUnload;
    }

    @Override
    public void setScheduledForUnload(long scheduled) {
        this.scheduledForUnload = scheduled;
    }

    @Inject(method = "generateSkylightMap", at = @At("HEAD"), cancellable = true)
    public void onGenerateSkylightMap(CallbackInfo ci) {
        if (!WorldGenConstants.lightingEnabled) {
            ci.cancel();
        }
    }

    @Override
    public void fill(ChunkPrimer primer) {
        boolean flag = this.world.dimension.hasSkyLight();
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 256; ++y) {
                    IBlockState block = primer.getBlockState(new BlockPos(x, y, z));
                    if (block.getMaterial() != Material.AIR) {
                        int section = y >> 4;
                        if (this.storageArrays[section] == net.minecraft.world.chunk.Chunk.NULL_BLOCK_STORAGE) {
                            this.storageArrays[section] = new ChunkSection(section << 4, flag);
                        }
                        this.storageArrays[section].set(x, y & 15, z, block);
                    }
                }
            }
        }
    }

    @Redirect(method = "addTileEntity(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;)V", at = @At(target =
            "Lnet/minecraft/tileentity/TileEntity;invalidate()V", value = "INVOKE"))
    private void redirectInvalidate(TileEntity te) {
        SpongeImplHooks.onTileEntityInvalidate(te);
    }

    @Override
    public boolean isActive() {
        if (this.isPersistedChunk()) {
            return true;
        }

        if (!this.loaded || this.isQueuedForUnload() || this.getScheduledForUnload() != -1) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("World", this.world)
                .add("Position", this.x + this.z)
                .toString();
    }
}
