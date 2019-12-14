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
package org.spongepowered.common.data.processor.value.tileentity;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.BoundedValue.Mutable;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.SpongeValueFactory;

import java.util.Optional;
import net.minecraft.tileentity.FurnaceTileEntity;

public class MaxCookTimeValueProcessor extends AbstractSpongeValueProcessor<FurnaceTileEntity, Integer, Mutable<Integer>> {

    public MaxCookTimeValueProcessor() {
        super(FurnaceTileEntity.class, Keys.MAX_COOK_TIME);
    }

    @Override
    protected Mutable<Integer> constructValue(Integer defaultValue) {
        return SpongeValueFactory.boundedBuilder(Keys.MAX_COOK_TIME)
                .minimum(0)
                .maximum(Integer.MAX_VALUE)
                .defaultValue(200)
                .actualValue(defaultValue)
                .build();
    }

    @Override
    protected boolean set(FurnaceTileEntity container, Integer value) {
        if(container.getStackInSlot(0).isEmpty()) return false; //Item cannot be null, the time depends on it

        container.setField(3, value);
        return true;
    }

    @Override
    protected Optional<Integer> getVal(FurnaceTileEntity container) {
        return Optional.of(container.getStackInSlot(0).isEmpty() ? 0 : container.getField(3)); //Item cannot be null, the time depends on it
    }

    @Override
    protected Immutable<Integer> constructImmutableValue(Integer value) {
        return SpongeValueFactory.boundedBuilder(Keys.MAX_COOK_TIME)
                .minimum(0)
                .maximum(Integer.MAX_VALUE)
                .defaultValue(200)
                .actualValue(value)
                .build()
                .asImmutable();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData(); //cannot be removed
    }
}
