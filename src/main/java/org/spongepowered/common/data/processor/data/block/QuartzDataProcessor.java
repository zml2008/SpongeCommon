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
package org.spongepowered.common.data.processor.data.block;

import net.minecraft.block.BlockQuartz;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableQuartzData;
import org.spongepowered.api.data.manipulator.mutable.block.QuartzData;
import org.spongepowered.api.data.type.QuartzType;
import org.spongepowered.api.data.type.QuartzTypes;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.common.data.manipulator.mutable.block.SpongeQuartzData;
import org.spongepowered.common.data.processor.common.AbstractCatalogDataProcessor;
import org.spongepowered.common.data.value.mutable.SpongeValue;

public class QuartzDataProcessor extends AbstractCatalogDataProcessor<QuartzType, Mutable<QuartzType>, QuartzData, ImmutableQuartzData> {

    public QuartzDataProcessor() {
        super(Keys.QUARTZ_TYPE, input -> input.getItem() == ItemTypes.QUARTZ_BLOCK || input.getItem() == ItemTypes.QUARTZ_STAIRS);
    }

    @Override
    protected int setToMeta(QuartzType value) {
        return ((BlockQuartz.EnumType) (Object) value).getMetadata();
    }

    @Override
    protected QuartzType getFromMeta(int meta) {
        return (QuartzType) (Object) BlockQuartz.EnumType.byMetadata(meta);
    }

    @Override
    public QuartzData createManipulator() {
        return new SpongeQuartzData();
    }

    @Override
    protected QuartzType getDefaultValue() {
        return QuartzTypes.DEFAULT;
    }

    @Override
    protected Mutable<QuartzType> constructValue(QuartzType actualValue) {
        return new SpongeValue<>(this.key, this.getDefaultValue(), actualValue);
    }

}
