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
package org.spongepowered.common.item.inventory.lens.impl.minecraft;

import net.minecraft.inventory.EntityEquipmentSlot;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.item.inventory.property.EquipmentSlotType;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.VanillaAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.AbstractLens;
import org.spongepowered.common.item.inventory.lens.impl.comp.EquipmentInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.fabric.EquipmentSlotsFabric;
import org.spongepowered.common.item.inventory.lens.slots.SlotLens;

public class EntityLivingLens extends AbstractLens {

    private static final int EQUIPMENT = 4;

    private EquipmentInventoryLensImpl equipment;
    private SlotLens mainHand;
    private SlotLens offHand;

    public EntityLivingLens(InventoryAdapter adapter, SlotProvider slots) {
        super(0, adapter.getFabric().getSize(), EquipmentInventory.class, slots);
        this.init(slots);
    }

    /**
     * See {@link EntityEquipmentSlot}.
     */
    @Override
    protected void init(SlotProvider slots) {
        // Adding basic slots
        for (int ord = 0, slot = this.base; ord < this.size; ord++, slot++) {
            this.addChild(slots.getSlot(slot), new SlotIndex(ord));
        }

        // Fabric ordering is based on EntityEquipmentSlot.slotIndex
        int base = this.base;
        this.mainHand = slots.getSlot(base);
        this.addChild(slots.getSlot(base), new EquipmentSlotType(EquipmentTypes.MAIN_HAND));
        base += 1;
        this.equipment = new EquipmentInventoryLensImpl(base, EQUIPMENT, 1, slots, false);
        this.addChild(slots.getSlot(base + 0), new EquipmentSlotType(EquipmentTypes.BOOTS));
        this.addChild(slots.getSlot(base + 1), new EquipmentSlotType(EquipmentTypes.LEGGINGS));
        this.addChild(slots.getSlot(base + 2), new EquipmentSlotType(EquipmentTypes.CHESTPLATE));
        this.addChild(slots.getSlot(base + 3), new EquipmentSlotType(EquipmentTypes.HEADWEAR));
        base += EQUIPMENT;
        this.offHand = slots.getSlot(base);
        this.addChild(slots.getSlot(base), new EquipmentSlotType(EquipmentTypes.OFF_HAND));

        // Spanning childs
        this.addSpanningChild(this.mainHand);
        this.addSpanningChild(this.equipment);
        this.addSpanningChild(this.offHand);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public InventoryAdapter getAdapter(Fabric inventory, Inventory parent) {
        if (inventory instanceof EquipmentSlotsFabric) {
            Object entity = inventory.get(0);
            if (entity instanceof InventoryAdapter) {
                // return the Entity when it also is a InventoryAdapter
                return ((InventoryAdapter) entity);
            }
        }
        // fallback to vanilla adapter
        return new VanillaAdapter(inventory, this, parent);
    }
}
