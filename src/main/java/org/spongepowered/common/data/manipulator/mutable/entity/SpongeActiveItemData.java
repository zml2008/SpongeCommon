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
package org.spongepowered.common.data.manipulator.mutable.entity;

import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableActiveItemData;
import org.spongepowered.api.data.manipulator.mutable.entity.ActiveItemData;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.common.data.manipulator.immutable.entity.ImmutableSpongeActiveItemData;
import org.spongepowered.common.data.manipulator.mutable.common.AbstractSingleData;
import org.spongepowered.common.data.value.mutable.SpongeValue;

public class SpongeActiveItemData extends AbstractSingleData<ItemStackSnapshot, ActiveItemData, ImmutableActiveItemData> implements ActiveItemData {

    public SpongeActiveItemData() {
        this(ItemStackSnapshot.NONE);
    }

    public SpongeActiveItemData(ItemStackSnapshot snapshot) {
        super(ActiveItemData.class, snapshot, Keys.ACTIVE_ITEM);
    }

    @Override
    public Mutable<ItemStackSnapshot> activeItem() {
        return new SpongeValue<>(Keys.ACTIVE_ITEM, this.getValue());
    }

    @Override
    protected Mutable<ItemStackSnapshot> getValueGetter() {
        return this.activeItem();
    }

    @Override
    public ActiveItemData copy() {
        return new SpongeActiveItemData(this.getValue());
    }

    @Override
    public ImmutableActiveItemData asImmutable() {
        return new ImmutableSpongeActiveItemData(this.getValue());
    }

}
