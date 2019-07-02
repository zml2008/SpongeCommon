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
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableWetData;
import org.spongepowered.api.data.manipulator.mutable.WetData;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.manipulator.mutable.SpongeWetData;
import org.spongepowered.common.data.processor.common.AbstractSingleDataSingleTargetProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.mixin.core.entity.passive.EntityWolfAccessor;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class WolfWetDataProcessor extends
    AbstractSingleDataSingleTargetProcessor<EntityWolfAccessor, Boolean, Value<Boolean>, WetData, ImmutableWetData> {

    public WolfWetDataProcessor() {
        super(Keys.IS_WET, EntityWolfAccessor.class);
    }

    @Override
    protected boolean set(final EntityWolfAccessor entity, final Boolean value) {
        if (value) {
            entity.accessor$setIsWet(true);
            entity.accessor$setIsShaking(true);
            entity.accessor$setTimeShaking(0F);
            entity.accessor$setPreviousTimeShaking(0F);
        } else {
            entity.accessor$setIsWet(false);
            entity.accessor$setIsShaking(false);
            entity.accessor$setTimeShaking(0F);
            entity.accessor$setPreviousTimeShaking(0F);
        }
        return true;
    }

    @Override
    protected Optional<Boolean> getVal(final EntityWolfAccessor entity) {
        final boolean isWet = entity.accessor$getIsWet() || entity.accessor$getIsShaking();
        return Optional.of(isWet);
    }

    @Override
    protected Value<Boolean> constructValue(final Boolean actualValue) {
        return new SpongeValue<>(Keys.IS_WET, Constants.Entity.Wolf.IS_WET_DEFAULT, actualValue);
    }

    @Override
    protected ImmutableValue<Boolean> constructImmutableValue(final Boolean value) {
        return ImmutableSpongeValue.cachedOf(Keys.IS_WET, Constants.Entity.Wolf.IS_WET_DEFAULT, value);
    }

    @Override
    protected WetData createManipulator() {
        return new SpongeWetData();
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

}
