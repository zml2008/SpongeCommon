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
package org.spongepowered.common.item.inventory.custom;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.Arrays;
import java.util.List;

public class LivingInventory implements IInventory {

    private final NonNullList<ItemStack> armor;
    private final NonNullList<ItemStack> hands;
    private final List<NonNullList<ItemStack>> allInventories;
    private final EntityLiving carrier;

    public LivingInventory(final NonNullList<ItemStack> armor, final NonNullList<ItemStack> hands, final EntityLiving entityLiving) {
        this.armor = armor;
        this.hands = hands;
        this.allInventories = Arrays.asList(this.armor, this.hands);
        this.carrier = entityLiving;
    }

    @Override
    public int getSizeInventory() {
        return this.armor.size() + this.hands.size();
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack itemStack : this.armor) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }
        for (final ItemStack hand : this.hands) {
            if (!hand.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        List<ItemStack> list = null;

        for (final NonNullList<ItemStack> nonnulllist : this.allInventories)
        {
            if (index < nonnulllist.size())
            {
                list = nonnulllist;
                break;
            }

            index -= nonnulllist.size();
        }

        return list == null ? ItemStack.EMPTY : list.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, final int count) {
        List<ItemStack> list = null;

        for (final NonNullList<ItemStack> nonnulllist : this.allInventories)
        {
            if (index < nonnulllist.size())
            {
                list = nonnulllist;
                break;
            }

            index -= nonnulllist.size();
        }

        return list != null && !list.get(index).isEmpty() ? ItemStackHelper.getAndSplit(list, index, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        NonNullList<ItemStack> nonnulllist = null;

        for (final NonNullList<ItemStack> nonnulllist1 : this.allInventories)
        {
            if (index < nonnulllist1.size())
            {
                nonnulllist = nonnulllist1;
                break;
            }

            index -= nonnulllist1.size();
        }

        if (nonnulllist != null && !nonnulllist.get(index).isEmpty())
        {
            final ItemStack itemstack = nonnulllist.get(index);
            nonnulllist.set(index, ItemStack.EMPTY);
            return itemstack;
        }
        else
        {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setInventorySlotContents(int index, final ItemStack stack) {
        NonNullList<ItemStack> nonnulllist = null;

        for (final NonNullList<ItemStack> nonnulllist1 : this.allInventories)
        {
            if (index < nonnulllist1.size())
            {
                nonnulllist = nonnulllist1;
                break;
            }

            index -= nonnulllist1.size();
        }

        if (nonnulllist != null)
        {
            nonnulllist.set(index, stack);
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean isUsableByPlayer(final EntityPlayer player) {
        return false;
    }

    @Override
    public void openInventory(final EntityPlayer player) {

    }

    @Override
    public void closeInventory(final EntityPlayer player) {

    }

    @Override
    public boolean isItemValidForSlot(final int index, final ItemStack stack) {
        return true;
    }

    @Override
    public int getField(final int id) {
        return 0;
    }

    @Override
    public void setField(final int id, final int value) {

    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public String getName() {
        return "container.inventory";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(this.getName());
    }

    public EntityLiving getCarrier() {
        return carrier;
    }
}
