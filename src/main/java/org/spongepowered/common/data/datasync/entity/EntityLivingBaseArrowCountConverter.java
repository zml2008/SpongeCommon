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
package org.spongepowered.common.data.datasync.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.spongepowered.common.data.datasync.DataParameterConverter;
import org.spongepowered.common.data.value.SpongeValueFactory;

import java.util.List;
import java.util.Optional;

public class EntityLivingBaseArrowCountConverter extends DataParameterConverter<Integer> {

    public EntityLivingBaseArrowCountConverter() {
        super(EntityLivingBase.ARROW_COUNT_IN_ENTITY);
    }

    @Override
    public Optional<DataTransactionResult> createTransaction(final Entity entity, final Integer currentValue, final Integer value) {
        return Optional.of(DataTransactionResult.builder()
            .replace(SpongeValueFactory.boundedBuilder(Keys.STUCK_ARROWS)
                .defaultValue(0)
                .actualValue(currentValue)
                .minimum(0)
                .maximum(Integer.MAX_VALUE)
                .build()
                .asImmutable())
            .success(SpongeValueFactory.boundedBuilder(Keys.STUCK_ARROWS)
                .defaultValue(0)
                .actualValue(value)
                .minimum(0)
                .maximum(Integer.MAX_VALUE)
                .build()
                .asImmutable())
            .result(DataTransactionResult.Type.SUCCESS)
            .build());
    }

    @Override
    public Integer getValueFromEvent(final Integer originalValue, final List<ImmutableValue<?>> immutableValues) {
        for (final ImmutableValue<?> immutableValue : immutableValues) {
            if (immutableValue.getKey() == Keys.STUCK_ARROWS) {
                return (Integer) immutableValue.get();
            }
        }
        return originalValue;
    }
}
