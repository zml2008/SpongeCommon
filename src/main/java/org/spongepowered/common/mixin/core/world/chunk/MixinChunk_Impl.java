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

import net.minecraft.util.math.ChunkPos;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.interfaces.IMixinCachable;
import org.spongepowered.common.interfaces.world.IMixinWorld_Impl;
import org.spongepowered.common.interfaces.world.chunk.IMixinChunk_Impl;

import java.util.UUID;

import javax.annotation.Nullable;

@Mixin(net.minecraft.world.chunk.Chunk.class)
public abstract class MixinChunk_Impl implements MixinIChunk_API<Chunk>, IMixinChunk_Impl, Chunk, IMixinCachable {

    @Shadow @Final private net.minecraft.world.World world;
    @Shadow @Final public int x;
    @Shadow @Final public int z;

    @Nullable private UUID spongeUuid = fetchUniqueId();
    private final long cacheKey = ChunkPos.asLong(this.x, this.z);


    @Nullable
    private UUID fetchUniqueId() {
        if (this.world instanceof IMixinWorld_Impl && !((IMixinWorld_Impl) this.world).isFake()) {
            @Nullable final UUID worldId = ((World) this.world).getUniqueId();
            if (worldId != null) {
                return new UUID(worldId.getMostSignificantBits() ^ (this.x * 2 + 1), worldId.getLeastSignificantBits() ^ (this.z * 2 + 1));
            }
        }
        return null;
    }

    @Override
    public World getWorld() {
        return (World) this.world;
    }

    @Override
    public long getCacheKey() {
        return this.cacheKey;
    }
}
