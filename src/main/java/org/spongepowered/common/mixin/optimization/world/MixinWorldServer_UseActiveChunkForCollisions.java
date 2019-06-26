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
package org.spongepowered.common.mixin.optimization.world;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.bridge.world.ServerWorldBridge;
import org.spongepowered.common.bridge.world.WorldBridge;
import org.spongepowered.common.bridge.world.chunk.ActiveChunkReferantBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.event.tracking.PhaseTracker;

import java.util.Objects;
import java.util.Optional;

@Mixin(value = WorldServer.class, priority = 1500)
public abstract class MixinWorldServer_UseActiveChunkForCollisions extends MixinWorld_UseActiveChunkForCollisions {

    @Override
    public boolean isFlammableWithin(AxisAlignedBB bb) {
        if (((WorldBridge) this).isFake()) {
            return super.isFlammableWithin(bb);
        }
        final Optional<ActiveChunkReferantBridge> source = PhaseTracker.getInstance().getCurrentContext().getSource(Entity.class)
            .map(entity -> (ActiveChunkReferantBridge) entity);
        if (source.isPresent()) {
            final ChunkBridge activeChunk = source.get().bridge$getActiveChunk();
            if (activeChunk == null || activeChunk.isQueuedForUnload() || !activeChunk.areNeighborsLoaded()) {
                return false;
            }
        } else {
            final int xStart = MathHelper.floor(bb.minX);
            final int xEnd = MathHelper.ceil(bb.maxX);
            final int yStart = MathHelper.floor(bb.minY);
            final int yEnd = MathHelper.ceil(bb.maxY);
            final int zStart = MathHelper.floor(bb.minZ);
            final int zEnd = MathHelper.ceil(bb.maxZ);
            if (!this.isAreaLoaded(xStart, yStart, zStart, xEnd, yEnd, zEnd, true)) {
                return false;
            }
        }
        return super.isFlammableWithin(bb);
    }
}
