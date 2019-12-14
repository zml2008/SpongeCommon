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
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;
import net.minecraft.tileentity.CommandBlockLogic;
import net.minecraft.tileentity.CommandBlockTileEntity;

public class TileEntityCommandValueProcessor extends AbstractSpongeValueProcessor<CommandBlockTileEntity, String, Mutable<String>> {

    public TileEntityCommandValueProcessor() {
        super(CommandBlockTileEntity.class, Keys.COMMAND);
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected Mutable<String> constructValue(final String actualValue) {
        return new SpongeValue<>(Keys.COMMAND, actualValue);
    }

    @Override
    protected boolean set(final CommandBlockTileEntity container, final String value) {
        final CommandBlockLogic logic = container.getCommandBlockLogic();
        final int successCount = logic.getSuccessCount();
        logic.setCommand(value);
        logic.setSuccessCount(successCount);
        return true;
    }

    @Override
    protected Optional<String> getVal(final CommandBlockTileEntity container) {
        return Optional.ofNullable(container.getCommandBlockLogic().getCommand());
    }

    @Override
    protected Immutable<String> constructImmutableValue(final String value) {
        return this.constructValue(value).asImmutable();
    }

}
