package org.spongepowered.common.item.inventory.lens.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.text.translation.FixedTranslation;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.common.item.inventory.lens.Fabric;

import java.util.Arrays;
import java.util.Collection;

/**
 * A wrapper-like {@link Fabric} for {@link EntityLiving}s that simply have {@link ItemStack} arrays that act
 * as an "Inventory".
 */
public class EntityLivingFabric implements Fabric<ItemStack[]> {

    final EntityLiving carrier;
    final ItemStack[][] inventory;

    public EntityLivingFabric(EntityLiving carrier, ItemStack[]... subInventories) {
        checkNotNull(subInventories);
        checkNotNull(carrier);
        this.carrier = carrier;
        this.inventory = subInventories;
    }

    @Override
    public Collection<ItemStack[]> allInventories() {
        // TODO Deep clone sub inventory stacks? Is it fine that the sub inventories are mutable?
        final ImmutableSet.Builder<ItemStack[]> builder = ImmutableSet.builder();
        for (ItemStack[] subInventory : inventory) {
            builder.add(subInventory);
        }
        return builder.build();
    }

    @Override
    public ItemStack[] get(int index) {
        checkState(this.inventory.length > index, "Attempt made to get index of sub-inventory that is out of range!");
        return this.inventory[index];
    }

    @Override
    public ItemStack getStack(int index) {
        ItemStack found = null;

        for (ItemStack[] subInventory : this.inventory) {
            if (subInventory.length <= index) {
                continue;
            }

            found = subInventory[index];
        }

        return found;
    }

    @Override
    public void setStack(int index, ItemStack stack) {
        for (ItemStack[] subInventory : this.inventory) {
            if (subInventory.length <= index) {
                continue;
            }

            subInventory[index] = stack;
        }
    }

    @Override
    public int getMaxStackSize() {
        // TODO I've no idea the best way to do this...
        return 1;
    }

    @Override
    public Translation getDisplayName() {
        // TODO Is it necessary to return anything relevant here?
        return new FixedTranslation(carrier.getName());
    }

    @Override
    public int getSize() {
        // TODO No idea if this should be across all arrays
        int count = 0;

        for (ItemStack[] subInventory : this.inventory) {
            count += subInventory.length;
        }

        return count;
    }

    @Override
    public void clear() {
        for (ItemStack[] subInventory : this.inventory) {
            Arrays.fill(subInventory, null);
        }
    }

    @Override
    public void markDirty() {
        // TODO Alert the entity to save?
    }
}
