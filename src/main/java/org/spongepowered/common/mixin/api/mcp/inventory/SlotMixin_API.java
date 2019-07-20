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
package org.spongepowered.common.mixin.api.mcp.inventory;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.comp.OrderedInventoryAdapter;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotCollection;
import org.spongepowered.common.item.inventory.lens.impl.comp.OrderedInventoryLensImpl;

import javax.annotation.Nullable;

@Mixin(Slot.class)
public abstract class SlotMixin_API implements org.spongepowered.api.item.inventory.Slot {

    @Shadow @Final public IInventory inventory;

    @Nullable private InventoryAdapter api$parentAdapter;

    @Override
    public Inventory parent() {
        if (this.inventory instanceof Inventory) {
            return ((Inventory) this.inventory);
        }
        if (this.api$parentAdapter == null) {
            final int size = ((InventoryAdapter) this).bridge$getFabric().fabric$getSize();
            final SlotCollection slots = new SlotCollection.Builder().add(size).build();
            final OrderedInventoryLensImpl lens = new OrderedInventoryLensImpl(0, size, 1, slots);
            this.api$parentAdapter = new OrderedInventoryAdapter(((InventoryAdapter) this).bridge$getFabric(), lens);
        }
        return (Inventory) this.api$parentAdapter;
    }

    @Override
    public org.spongepowered.api.item.inventory.Slot transform(final Type type) {
        return this;
    }

    @Override
    public org.spongepowered.api.item.inventory.Slot transform() {
        return this.transform(Type.INVENTORY);
    }

}
