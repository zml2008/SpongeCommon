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
import com.google.common.collect.Sets;
import net.minecraft.block.state.IBlockState;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.block.BlockUtil;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.common.interfaces.server.management.IMixinPlayerChunkMapEntry;
import org.spongepowered.common.interfaces.world.IMixinWorld_Impl;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.storage.SpongeChunkLayout;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@Mixin(net.minecraft.world.chunk.Chunk.class)
@Implements(@Interface(iface = Chunk.class, prefix = "api$chunk$"))
public abstract class MixinChunk_API implements IChunk, MixinIChunk_API<Chunk>, Chunk {

    @Shadow @Final public int x;
    @Shadow @Final public int z;
    @Shadow @Final private net.minecraft.world.World world;
    @Shadow @Final private Map<BlockPos, net.minecraft.tileentity.TileEntity> tileEntities;
    @Shadow private long inhabitedTime;

    @Shadow public abstract boolean shadow$isEmpty();

    private final Vector3i chunkPos = new Vector3i(this.x, 0, this.z);
    private final Vector3i blockMin = SpongeChunkLayout.instance.forceToChunk(this.chunkPos);
    private final Vector3i blockMax = this.blockMin.add(SpongeChunkLayout.CHUNK_SIZE).sub(1, 1, 1);


    @Override
    public Vector3i getBlockMin() {
        return this.blockMin;
    }

    @Override
    public Vector3i getBlockMax() {
        return this.blockMax;
    }

    @Override
    public Vector3i getBlockSize() {
        return SpongeChunkLayout.CHUNK_SIZE;
    }

    @Override
    public Vector3i getChunkPosition() {
        return new Vector3i(this.x << 4, 0, this.z << 4);
    }

    @Intrinsic
    public boolean api$chunk$isEmpty() {
        return shadow$isEmpty();
    }

    @Intrinsic
    @Override
    public boolean isEmpty() {
        return shadow$isEmpty();
    }

    @Override
    public void refreshBiomes() {
        if (this.world instanceof WorldServer) {
            final PlayerChunkMapEntry entry = ((WorldServer) this.world).getPlayerChunkMap().getEntry(this.x, this.z);
            if (entry != null) {
                ((IMixinPlayerChunkMapEntry) entry).markBiomesForUpdate();
            }
        }
    }

    @Override
    public Collection<TileEntity> getTileEntities() {
        Set<TileEntity> tiles = Sets.newHashSet();
        for (Map.Entry<BlockPos, net.minecraft.tileentity.TileEntity> entry : this.tileEntities.entrySet()) {
            tiles.add((TileEntity) entry.getValue());
        }
        return tiles;
    }

    @Override
    public Collection<TileEntity> getTileEntities(Predicate<TileEntity> filter) {
        Set<TileEntity> tiles = Sets.newHashSet();
        for (Map.Entry<BlockPos, net.minecraft.tileentity.TileEntity> entry : this.tileEntities.entrySet()) {
            if (filter.test((TileEntity) entry.getValue())) {
                tiles.add((TileEntity) entry.getValue());
            }
        }
        return tiles;
    }

    @Override
    public boolean setBlock(Vector3i position, BlockState block) {
        checkBlockBounds(position.getX(), position.getY(), position.getZ());
        if (!((IMixinWorld_Impl) this.world).isFake()) {
            return PhaseTracker.SERVER
                .setBlockState((IMixinWorldServer) this.world, VecHelper.toBlockPos(position), BlockUtil.toNative(block), BlockChangeFlags.ALL);
        }
        return setBlockState(VecHelper.toBlockPos(position), (IBlockState) block, false) == block;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BlockState block) {
        checkBlockBounds(x, y, z);
        if (!((IMixinWorld_Impl) this.world).isFake()) {
            return PhaseTracker.SERVER
                .setBlockState((IMixinWorldServer) this.world, new BlockPos(x, y, z), BlockUtil.toNative(block), BlockChangeFlags.ALL);
        }
        return setBlockState(new BlockPos(x, y, z), (IBlockState) block, false) == block;
    }

    @Override
    public World getWorld() {
        return (World) this.world;
    }

    @Override
    public boolean loadChunk(boolean generate) {
        WorldServer worldserver = (WorldServer) this.world;
        net.minecraft.world.chunk.Chunk chunk = null;
        if (worldserver.getChunkProvider().chunkExists(this.x, this.z) || generate) {
            chunk = worldserver.getChunkProvider().getChunk(this.x, this.z, true, generate);
        }

        return chunk != null;
    }

    /**
     * Specifically does nothing in the common implementation. SpongeForge and SpongeVanilla
     * implement this separately.
     */
    @Override
    public boolean unloadChunk() {
        throw new UnsupportedOperationException("Common implementation does not support unloading chunks!");
    }

    @Override
    public long getInhabitedTime() {
        return this.inhabitedTime;
    }

    @Override
    public double getRegionalDifficultyFactor() {
        final boolean flag = this.world.getDifficulty() == EnumDifficulty.HARD;
        float moon = this.world.getCurrentMoonPhaseFactor();
        float f2 = MathHelper.clamp((this.world.getGameTime() - 72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
        float f3 = 0.0F;
        f3 += MathHelper.clamp(this.inhabitedTime / 3600000.0F, 0.0F, 1.0F) * (flag ? 1.0F : 0.75F);
        f3 += MathHelper.clamp(moon * 0.25F, 0.0F, f2);
        return f3;
    }

    @Override
    public double getRegionalDifficultyPercentage() {
        final double region = getRegionalDifficultyFactor();
        if (region < 2) {
            return 0;
        } else if (region > 4) {
            return 1.0;
        } else {
            return (region - 2.0) / 2.0;
        }
    }

    @Override
    public Optional<Chunk> getNeighbor(Direction direction) {
        return Optional.empty();
    }

    @Override
    public Optional<Chunk> getNeighbor(Direction direction, boolean shouldLoad) {
        return Optional.empty();
    }

    @Override
    public int getMaxLightLevel() {
        return shadow$getMaxLightLevel();
    }
}
