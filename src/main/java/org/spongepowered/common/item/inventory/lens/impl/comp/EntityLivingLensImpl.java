package org.spongepowered.common.item.inventory.lens.impl.comp;

import net.minecraft.item.ItemStack;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.comp.EntityLivingLens;
import org.spongepowered.common.item.inventory.lens.impl.AbstractLens;

import java.lang.reflect.Constructor;

public class EntityLivingLensImpl extends AbstractLens<ItemStack[], ItemStack> implements EntityLivingLens {

    public EntityLivingLensImpl(int base, int size, InventoryAdapter<ItemStack[], ItemStack> adapter, SlotProvider<ItemStack[], ItemStack> slots) {
        super(base, size, adapter, slots);
    }

    public EntityLivingLensImpl(int base, int size, Class<? extends Inventory> adapterType, SlotProvider<ItemStack[], ItemStack> slots) {
        super(base, size, adapterType, slots);
    }

    public EntityLivingLensImpl(int base, int size,
            InventoryAdapter<ItemStack[], ItemStack> adapter, Class<? extends Inventory> adapterType, SlotProvider<ItemStack[], ItemStack> slots) {
        super(base, size, adapter, adapterType, slots);
    }

    @Override
    public int getMaxStackSize(Fabric<ItemStack[]> inv) {
        return 0;
    }

    @Override
    protected void init(SlotProvider<ItemStack[], ItemStack> slots) {

    }

    @Override
    protected Constructor<InventoryAdapter<ItemStack[], ItemStack>> getAdapterCtor() throws NoSuchMethodException {
        return null;
    }
}
