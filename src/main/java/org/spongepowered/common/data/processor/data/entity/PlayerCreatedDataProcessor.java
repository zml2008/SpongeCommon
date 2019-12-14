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
package org.spongepowered.common.data.processor.data.entity;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutablePlayerCreatedData;
import org.spongepowered.api.data.manipulator.mutable.entity.PlayerCreatedData;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongePlayerCreatedData;
import org.spongepowered.common.data.processor.common.AbstractEntitySingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;
import net.minecraft.entity.passive.IronGolemEntity;

public class PlayerCreatedDataProcessor
        extends AbstractEntitySingleDataProcessor<IronGolemEntity, Boolean, Mutable<Boolean>, PlayerCreatedData, ImmutablePlayerCreatedData> {

    public PlayerCreatedDataProcessor() {
        super(IronGolemEntity.class, Keys.PLAYER_CREATED);
    }

    @Override
    protected boolean set(IronGolemEntity entity, Boolean value) {
        entity.setPlayerCreated(value);
        return true;
    }

    @Override
    protected Optional<Boolean> getVal(IronGolemEntity entity) {
        return Optional.of(entity.isPlayerCreated());
    }

    @Override
    protected Mutable<Boolean> constructValue(Boolean value) {
        return new SpongeValue<>(this.key, false, value);
    }

    @Override
    protected Immutable<Boolean> constructImmutableValue(Boolean value) {
        return ImmutableSpongeValue.cachedOf(this.key, false, value);
    }

    @Override
    protected PlayerCreatedData createManipulator() {
        return new SpongePlayerCreatedData();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

}
