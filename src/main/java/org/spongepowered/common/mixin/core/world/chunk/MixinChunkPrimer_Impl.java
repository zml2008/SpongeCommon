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

import static com.google.common.base.Preconditions.checkState;

import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.WorldGenRegion;
import org.spongepowered.api.world.gen.GenerationRegion;
import org.spongepowered.api.world.gen.PrimitiveChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.common.interfaces.world.gen.IMixinChunkPrimer_Impl;

import java.util.Optional;

import javax.annotation.Nullable;

@Mixin(ChunkPrimer.class)
public abstract class MixinChunkPrimer_Impl implements IMixinChunkPrimer_Impl, MixinIChunk_API<PrimitiveChunk>, PrimitiveChunk {

    @Nullable private WorldGenRegion worldGenRegion;

    @Override
    public Optional<WorldGenRegion> getWorldGenRegion() {
        return Optional.ofNullable(this.worldGenRegion);
    }

    @Override
    public void setWorldGenRegion(WorldGenRegion worldGenRegion) {
        this.worldGenRegion = worldGenRegion;
    }

    @Override
    public GenerationRegion getWorld() {
        checkState(this.worldGenRegion != null, "WorldGenRegion is not valid. This PrimitiveChunk is not belonging to a GenerationRegion!");
        return (GenerationRegion) this.worldGenRegion;
    }
}
