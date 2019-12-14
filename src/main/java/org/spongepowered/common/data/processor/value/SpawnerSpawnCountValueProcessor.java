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
package org.spongepowered.common.data.processor.value;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.BoundedValue.Mutable;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.SpongeValueFactory;
import org.spongepowered.common.mixin.accessor.world.spawner.AbstractSpawnerAccessor;
import org.spongepowered.common.mixin.accessor.tileentity.MobSpawnerTileEntityAccessor;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class SpawnerSpawnCountValueProcessor extends AbstractSpongeValueProcessor<MobSpawnerTileEntityAccessor, Short, Mutable<Short>> {

    public SpawnerSpawnCountValueProcessor() {
        super(MobSpawnerTileEntityAccessor.class, Keys.SPAWNER_SPAWN_COUNT);
    }

    @Override
    protected Mutable<Short> constructValue(Short actualValue) {
        return SpongeValueFactory.boundedBuilder(this.key)
                .minimum((short) 0)
                .maximum(Short.MAX_VALUE)
                .defaultValue(Constants.TileEntity.Spawner.DEFAULT_SPAWN_COUNT)
                .actualValue(actualValue)
                .build();
    }

    @Override
    protected boolean set(MobSpawnerTileEntityAccessor container, Short value) {
        ((AbstractSpawnerAccessor) container.accessor$getSpawnerLogic()).accessor$setSpawnCount(value);
        return true;
    }

    @Override
    protected Optional<Short> getVal(MobSpawnerTileEntityAccessor container) {
        return Optional.of((short) ((AbstractSpawnerAccessor) container.accessor$getSpawnerLogic()).accessor$getSpawnCount());
    }

    @Override
    protected Immutable<Short> constructImmutableValue(Short value) {
        return this.constructValue(value).asImmutable();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }
}
