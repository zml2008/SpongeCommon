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
package org.spongepowered.common.bridge.entity.player;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public interface InventoryPlayerBridge {

    int bridge$getHeldItemIndex(EnumHand hand);

    /**
     * Set the current hotbar item and optionally notify the client
     *
     * @param itemIndex Hotbar index to set
     * @param notify True to send an update packet to the client if this is a
     *      server
     */
    void bridge$setSelectedItem(int itemIndex, boolean notify);

    /**
     * Gets the first available slot id for itemstack.
     *
     * @param itemstack The itemstack attempting to be stored
     * @return The slot id or -1 if no slot found.
     */
    int bridge$getFirstAvailableSlot(ItemStack itemstack);

    /**
     * Cleanup dirty Inventory State. E.g. after changes made through a scheduled task.
     */
    void bridge$cleanupDirty();

    /**
     * Removes dirty Inventory State. Used after detectAndSendChanges.
     */
    void bridge$markClean();
}
