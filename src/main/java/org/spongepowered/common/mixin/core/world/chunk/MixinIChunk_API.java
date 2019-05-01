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

import com.flowpowered.math.vector.Vector3i;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumLightType;
import net.minecraft.world.ITickList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.fluid.FluidState;
import org.spongepowered.api.fluid.FluidType;
import org.spongepowered.api.scheduler.ScheduledUpdateList;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.LightType;
import org.spongepowered.api.world.ProtoWorld;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.chunk.ChunkState;
import org.spongepowered.api.world.chunk.ProtoChunk;
import org.spongepowered.api.world.volume.biome.ImmutableBiomeVolume;
import org.spongepowered.api.world.volume.biome.UnmodifiableBiomeVolume;
import org.spongepowered.api.world.volume.biome.worker.MutableBiomeVolumeStream;
import org.spongepowered.api.world.volume.block.ImmutableBlockVolume;
import org.spongepowered.api.world.volume.block.UnmodifiableBlockVolume;
import org.spongepowered.api.world.volume.block.worker.MutableBlockVolumeStream;
import org.spongepowered.api.world.volume.tileentity.worker.MutableTileEntityStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.block.BlockUtil;
import org.spongepowered.common.entity.EntityUtil;
import org.spongepowered.common.mixin.core.world.MixinIBlockReader_API;
import org.spongepowered.common.util.VecHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

@SuppressWarnings("unchecked")
@Mixin(IChunk.class)
public interface MixinIChunk_API<P extends ProtoChunk<P>> extends MixinIBlockReader_API, ProtoChunk<P> {

    @Shadow void addEntity(net.minecraft.entity.Entity entityIn);
    @Shadow void addTileEntity(BlockPos pos, net.minecraft.tileentity.TileEntity tileEntityIn);
    @Shadow void removeTileEntity(BlockPos pos);
    @Shadow int shadow$getLight(EnumLightType lightType, BlockPos pos, boolean hasSkylight);
    @Shadow int getTopBlockY(Heightmap.Type heightmapType, int x, int z);
    @Shadow ChunkPos getPos();
    @Shadow ChunkStatus getStatus();
    @Shadow Biome[] getBiomes();
    @Shadow ITickList<Block> getBlocksToBeTicked();
    @Shadow ITickList<Fluid> getFluidsToBeTicked();
    @Shadow @Nullable IBlockState setBlockState(BlockPos pos, IBlockState state, boolean isMoving);

    default boolean containsBiome(int x, int y, int z) {
        return VecHelper.inBounds(x, y, z, this.getBlockMin(), this.getBlockMax());
    }

    default void checkBiomeBounds(int x, int y, int z) {
        if (!containsBiome(x, y, z)) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), this.getBlockMin(), this.getBlockMax());
        }
    }

    default void checkBlockBounds(int x, int y, int z) {
        if (!containsBlock(x, y, z)) {
            throw new PositionOutOfBoundsException(new Vector3i(x, y, z), this.getBlockMin(), this.getBlockMax());
        }
    }

    @Override
    default BlockState getBlock(int x, int y, int z) {
        checkBlockBounds(x, y, z);
        return BlockUtil.fromNative(getBlockState(new BlockPos(x, y, z)));
    }
    @Override
    default boolean containsBlock(Vector3i position) {
        return VecHelper.inBounds(position, this.getBlockMin(), this.getBlockMax());
    }
    @Override
    default boolean containsBlock(int x, int y, int z) {
        return VecHelper.inBounds(x, y, z, this.getBlockMin(), this.getBlockMax());
    }
    @Override
    default boolean isAreaAvailable(Vector3i position) {
        return VecHelper.inBounds(position, this.getBlockMin(), this.getBlockMax());
    }
    @Override
    default boolean isAreaAvailable(int x, int y, int z) {
        return VecHelper.inBounds(x, y, z, this.getBlockMin(), this.getBlockMax());
    }

    @Override
    default BlockState getBlock(Vector3i pos) {
        checkBlockBounds(pos.getX(), pos.getY(), pos.getZ());
        return BlockUtil.fromNative(getBlockState(VecHelper.toBlockPos(pos)));
    }

    // Does not need to be overridden in submixins because if a plugin is explicitly
    // having a Chunk add an entity, world generation literally should not be attempting to add
    // it to the world.
    @Override
    default void addEntity(Entity entity) {
        addEntity(EntityUtil.toNative(entity));
    }

    @Override
    default ChunkState getState() {
        return (ChunkState) getStatus();
    }

    /*
    To be overridden in MixinChunk_API since net.minecraft.world.chunk.Chunk can also be an empty chunk, and has the
     method #isEmpty()
     */
    @Override
    default boolean isEmpty() {
        return true;
    }

    // Overridden in MixinChunk_API because faster access to direct `this.x` and `this.z` fields, instead of creating ChunkPos.
    @Override
    default Vector3i getChunkPosition() {
        final ChunkPos pos = getPos();
        return new Vector3i(pos.x << 4, 0, pos.z << 4);
    }

    /*
    To be overridden in MixinChunk_API, MixinChunkPrimer_API and MixinChunkPrimer_Impl (ChunkPrimers can be created without WorldGenRegions)
    */
    @Override
    default ProtoWorld<?> getWorld() {
        throw new UnsupportedOperationException("Cannot get the World owning this chunk");
    }

    // Overridden since it's faster to use biome arrays and having the chunk refreshed.
    @Override
    default boolean setBiome(Vector3i pos, BiomeType biome) {
        checkBiomeBounds(pos.getX(), pos.getY(), pos.getZ());
        final Biome[] biomes = getBiomes();
        final int x = pos.getX() & 15;
        final int z = pos.getZ() & 15;
        biomes[z << 4 | x] = (Biome) biome;
        this.refreshBiomes();
        return true;
    }

    /**
     * To be overridden by World controlled chunks. ChunkPrimers etc. do not need
     * actual refreshing. Chunks on WorldServers do need to have the player chunk map
     * refreshed so clients will get the biome update.
     */
    default void refreshBiomes() {

    }

    @Override
    default boolean setBiome(int x, int y, int z, BiomeType biome) {
        checkBiomeBounds(x, y, z);
        final Biome[] biomes = getBiomes();
        biomes[z << 4 | x] = (Biome) biome;
        this.refreshBiomes();
        return true;
    }

    @Override
    default MutableBiomeVolumeStream<P> toBiomeStream() {
        return null; // TODO - implement streams
    }

    @Override
    default BiomeType getBiome(Vector3i position) {
        final Biome[] biomes = getBiomes();
        final int x = position.getX() & 15;
        final int z = position.getZ() & 15;
        return (BiomeType) biomes[z << 4 | x];
    }

    @Override
    default BiomeType getBiome(int x, int y, int z) {
        final Biome[] biomes = getBiomes();
        return (BiomeType) biomes[z << 4 | x];
    }

    @Override
    default UnmodifiableBiomeVolume<?> asUnmodifiableBiomeVolume() {
        return null; // TODO - implement this
    }

    @Override
    default ImmutableBiomeVolume asImmutableBiomeVolume() {
        return null; // TODO - implement this
    }

    @Override
    default void addTileEntity(Vector3i pos, TileEntity tileEntity) {
        addTileEntity(VecHelper.toBlockPos(pos), (net.minecraft.tileentity.TileEntity) tileEntity);
    }

    @Override
    default void addTileEntity(int x, int y, int z, TileEntity tileEntity) {
        addTileEntity(new BlockPos(x, y, z), (net.minecraft.tileentity.TileEntity) tileEntity);
    }

    @Override
    default void removeTileEntity(Vector3i pos) {
        removeTileEntity(VecHelper.toBlockPos(pos));
    }

    @Override
    default void removeTileEntity(int x, int y, int z) {
        removeTileEntity(new BlockPos(x, y, z));
    }

    @Override
    default Collection<TileEntity> getTileEntities() {
        return Collections.emptyList(); // TODO - determine if this is a viable thing
    }

    /**
     * To be implemented by sublcasses of this.
     * @param filter The filter to apply to the returned entities
     * @return
     */
    @Override
    default Collection<TileEntity> getTileEntities(Predicate<TileEntity> filter) {
        return Collections.emptyList(); // TODO - determine if this is a viable thing
    }

    @Override
    default Optional<TileEntity> getTileEntity(int x, int y, int z) {
        return Optional.ofNullable((TileEntity) this.getTileEntity(new BlockPos(x, y, z))); // most chunks don't support this
    }

    @Override
    default P getView(Vector3i newMin, Vector3i newMax) {
        return (P) this; // TODO - implement
    }

    @Override
    default MutableTileEntityStream<P> toTileEntityStream() {
        return null; // TODO - implement
    }

    @Override
    default int getLight(LightType type, int x, int y, int z) {
        return shadow$getLight((EnumLightType) (Object) type, new BlockPos(x, y, z), true);
    }

    @Override
    default int getLight(LightType type, Vector3i pos) {
        return shadow$getLight((EnumLightType) (Object) type, VecHelper.toBlockPos(pos), true);
    }

    @Override
    default int getLight(int x, int y, int z) {
        return shadow$getLight(EnumLightType.SKY, new BlockPos(x, y, z), true);
    }

    @Override
    default int getLight(Vector3i pos) {
        return shadow$getLight(EnumLightType.SKY, VecHelper.toBlockPos(pos), true);
    }

    @Override
    default ScheduledUpdateList<FluidType> getScheduledFluidUpdates() {
        return (ScheduledUpdateList<FluidType>) getFluidsToBeTicked();
    }

    @Override
    default ScheduledUpdateList<BlockType> getScheduledBlockUpdates() {
        return (ScheduledUpdateList<BlockType>) getBlocksToBeTicked();
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Override
    default boolean setBlock(Vector3i position, BlockState block) {
        return setBlockState(VecHelper.toBlockPos(position), (IBlockState) block, false) == block;
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Override
    default boolean setBlock(int x, int y, int z, BlockState block) {
        return setBlockState(new BlockPos(x, y, z), (IBlockState) block, false) == block;
    }

    @Override
    default boolean removeBlock(Vector3i position) {
        return setBlockState(VecHelper.toBlockPos(position), Blocks.AIR.getDefaultState(), false) != null;
    }

    @Override
    default boolean removeBlock(int x, int y, int z) {
        return setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), false) != null;
    }

    @Override
    default MutableBlockVolumeStream<P> toBlockStream() {
        return null; // TODO - implement streams
    }

    @Override
    default FluidState getFluid(int x, int y, int z) {
        return (FluidState) getFluidState(new BlockPos(x, y, z));
    }

    @Override
    default FluidState getFluid(Vector3i vector3i) {
        return (FluidState) getFluidState(VecHelper.toBlockPos(vector3i));
    }

    @Override
    default UnmodifiableBlockVolume<?> asUnmodifiableBlockVolume() {
        return null; // TODO - implement
    }

    @Override
    default ImmutableBlockVolume asImmutableBlockVolume() {
        return null; // TODO - implement
    }

    @Override
    default int getHighestYAt(int x, int z) {
        return getTopBlockY(Heightmap.Type.WORLD_SURFACE, x, z);
    }

    @Override
    default Optional<TileEntity> getTileEntity(Vector3i position) {
        return Optional.ofNullable((TileEntity) getTileEntity(VecHelper.toBlockPos(position)));
    }
}
