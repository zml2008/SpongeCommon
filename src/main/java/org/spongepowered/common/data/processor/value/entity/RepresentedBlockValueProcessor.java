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
package org.spongepowered.common.data.processor.value.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.init.Blocks;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;

public class RepresentedBlockValueProcessor extends AbstractSpongeValueProcessor<EntityMinecart, BlockState, Value<BlockState>> {

    public RepresentedBlockValueProcessor() {
        super(EntityMinecart.class, Keys.REPRESENTED_BLOCK);
    }

    @Override
    protected Value<BlockState> constructValue(final BlockState value) {
        return new SpongeValue<>(Keys.REPRESENTED_BLOCK, (BlockState) Blocks.AIR.getDefaultState(), value);
    }

    @Override
    protected boolean set(final EntityMinecart container, final BlockState value) {
        container.setDisplayTile((IBlockState) value);
        return true;
    }

    @Override
    protected Optional<BlockState> getVal(final EntityMinecart container) {
        if(!container.hasDisplayTile()) return Optional.empty();
        return Optional.of((BlockState) container.getDisplayTile());
    }

    @Override
    protected ImmutableValue<BlockState> constructImmutableValue(final BlockState value) {
        return new ImmutableSpongeValue<>(Keys.REPRESENTED_BLOCK, (BlockState) Blocks.AIR.getDefaultState(), value);
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        if(container instanceof EntityMinecart) {
            final EntityMinecart cart = (EntityMinecart) container;
            final ImmutableValue<BlockState> block = new ImmutableSpongeValue<>(Keys.REPRESENTED_BLOCK, (BlockState) cart.getDisplayTile());
            cart.setHasDisplayTile(false);
            return DataTransactionResult.builder().replace(block).build();
        }
        return DataTransactionResult.failNoData();
    }
}
