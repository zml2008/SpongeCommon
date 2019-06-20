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
package org.spongepowered.common.mixin.core.common.item.inventory.custom;

import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.item.inventory.adapter.impl.MinecraftInventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.slots.EquipmentSlotAdapter;
import org.spongepowered.common.item.inventory.custom.LivingInventory;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotCollection;
import org.spongepowered.common.item.inventory.lens.impl.minecraft.LivingInventoryLens;
import org.spongepowered.common.item.inventory.lens.impl.slots.EquipmentSlotLensImpl;
import org.spongepowered.common.item.inventory.util.InventoryUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Mixin(LivingInventory.class)
public abstract class MixinLivingInventory implements MinecraftInventoryAdapter, Inventory, CarriedInventory<Carrier> {

    @Shadow @Final private NonNullList<ItemStack> armor;
    @Shadow @Final private NonNullList<ItemStack> hands;
    @Shadow @Final private List<NonNullList<ItemStack>> allInventories;
    @Shadow @Final private EntityLiving carrier;

    private SlotCollection slots;
    private LivingInventoryLens lens;

    @Inject(method = "<init>*", at = @At("RETURN"), remap = false)
    private void onConstructed(final NonNullList<ItemStack> armor, final NonNullList<ItemStack> hands, final EntityLiving entityLiving,
        final CallbackInfo ci) {
        final Iterator<ItemStack> iterator = this.carrier.getEquipmentAndArmor().iterator();
        int size = 0;
        while (iterator.hasNext()) {
            size++;
            iterator.next();
        }
        this.slots = new SlotCollection.Builder()
            .add(this.armor.size())
            .add(this.hands.size())
            // TODO predicates for ItemStack/ItemType?
            .add(EquipmentSlotAdapter.class, index -> new EquipmentSlotLensImpl(index, i -> true, t -> true, e -> e == EquipmentTypes.BOOTS))
            .add(EquipmentSlotAdapter.class, index -> new EquipmentSlotLensImpl(index, i -> true, t -> true, e -> e == EquipmentTypes.LEGGINGS))
            .add(EquipmentSlotAdapter.class, index -> new EquipmentSlotLensImpl(index, i -> true, t -> true, e -> e == EquipmentTypes.CHESTPLATE))
            .add(EquipmentSlotAdapter.class, index -> new EquipmentSlotLensImpl(index, i -> true, t -> true, e -> e == EquipmentTypes.HEADWEAR))
            // for mods providing bigger inventories
            .add(this.armor.size() - 4, EquipmentSlotAdapter.class)
            .add(size - this.armor.size() - 4 - this.hands.size(), EquipmentSlotAdapter.class)
            .build();
        this.lens = new LivingInventoryLens(this, this.slots);
    }

    @Override
    public Lens getRootLens() {
        return this.lens;
    }

    @Override
    public Inventory getChild(final Lens lens) {
        return null; // TODO ?
    }

    @Override
    public Optional<Carrier> getCarrier() {
        return Optional.ofNullable((Carrier) this.carrier);
    }

    @Override
    public PluginContainer getPlugin() {
        return InventoryUtil.getPluginContainer(this.carrier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SlotProvider getSlotProvider() {
        return this.slots;
    }
}
