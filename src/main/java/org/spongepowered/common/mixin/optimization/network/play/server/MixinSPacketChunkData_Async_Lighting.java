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
package org.spongepowered.common.mixin.optimization.network.play.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SPacketChunkData.class, priority = 1005)
public abstract class MixinSPacketChunkData_Async_Lighting implements Packet<INetHandlerPlayClient>  {

    @Shadow private byte[] buffer;
    private ByteBuf buf;

    @Inject(method = "<init>(Lnet/minecraft/world/chunk/Chunk;I)V", at = @At("RETURN"))
    private void onInitChunkData(Chunk chunk, int changedSectionFilter, CallbackInfo ci) {
        try {
            final int written = this.buf.writerIndex();
            // More or less than expected bytes got written, we need to make a new buffer
            if (written != this.buffer.length) {
                this.buffer = new byte[written];
                this.buf.readBytes(this.buffer);
                // We could optimize when less bytes are written, but these
                // cases are so rare that it's not worth it
            }
        } finally {
            this.buf.release();
            this.buf = null;
        }
    }

    /**
     * @author Cybermaxke
     * @reason Overwrite to replace the fixed buffer to allow extra data to be
     *         written, this happens when the estimated size is no longer valid
     *         caused by async operations.
     */
    @Overwrite
    private ByteBuf getWriteBuffer() {
        // The estimated buffer, in most cases this buffer should be enough
        final ByteBuf estimatedBuf = Unpooled.wrappedBuffer(this.buffer);
        // An extra buffer in case that extra bytes are necessary
        final ByteBuf extraBuf = ByteBufAllocator.DEFAULT.buffer(0);
        return this.buf = new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, 2, estimatedBuf, extraBuf).setIndex(0, 0);
    }
}
