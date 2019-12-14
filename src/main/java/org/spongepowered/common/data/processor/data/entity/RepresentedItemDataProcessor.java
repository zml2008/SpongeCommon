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

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableRepresentedItemData;
import org.spongepowered.api.data.manipulator.mutable.RepresentedItemData;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.data.manipulator.mutable.SpongeRepresentedItemData;
import org.spongepowered.common.data.processor.common.AbstractEntitySingleDataProcessor;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;

public class RepresentedItemDataProcessor extends
        AbstractEntitySingleDataProcessor<Entity, ItemStackSnapshot, Mutable<ItemStackSnapshot>, RepresentedItemData, ImmutableRepresentedItemData> {

    public RepresentedItemDataProcessor() {
        super(Entity.class, Keys.REPRESENTED_ITEM);
    }

    @Override
    protected boolean set(Entity container, ItemStackSnapshot value) {
        if (container instanceof ItemFrameEntity) {
            ((ItemFrameEntity) container).setDisplayedItem((ItemStack) value.createStack());
            return true;
        } else if (container instanceof ItemEntity) {
            ((ItemEntity) container).setItem((ItemStack) value.createStack());
            return true;
        }
        return false;
    }

    @Override
    protected Optional<ItemStackSnapshot> getVal(Entity container) {
        if (container instanceof ItemFrameEntity) {
            final ItemStack itemStack = ((ItemFrameEntity) container).getDisplayedItem();
            if (!itemStack.isEmpty()) {
                return Optional.of(((org.spongepowered.api.item.inventory.ItemStack) itemStack).createSnapshot());
            }
        } else if (container instanceof ItemEntity) {
            return Optional.of(((org.spongepowered.api.item.inventory.ItemStack) ((ItemEntity) container).getItem()).createSnapshot());
        }
        return Optional.empty();
    }

    @Override
    protected Mutable<ItemStackSnapshot> constructValue(ItemStackSnapshot defaultValue) {
        return new SpongeValue<>(this.key, defaultValue);
    }

    @Override
    protected Immutable<ItemStackSnapshot> constructImmutableValue(ItemStackSnapshot value) {
        return new ImmutableSpongeValue<>(this.key, value);
    }

    @Override
    protected boolean supports(Entity holder) {
        return holder instanceof ItemEntity || holder instanceof ItemFrameEntity;
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        if (container instanceof ItemFrameEntity) {
            ItemFrameEntity frame = (ItemFrameEntity) container;
            if (!frame.getDisplayedItem().isEmpty()) {
                final Immutable<ItemStackSnapshot> old = this.constructImmutableValue(this.getVal(frame).get());
                frame.setDisplayedItem(ItemStack.EMPTY);
                return DataTransactionResult.successRemove(old);
            }
            return DataTransactionResult.successNoData();
        }
        return DataTransactionResult.failNoData();
    }

    @Override
    protected RepresentedItemData createManipulator() {
        return new SpongeRepresentedItemData();
    }

}
