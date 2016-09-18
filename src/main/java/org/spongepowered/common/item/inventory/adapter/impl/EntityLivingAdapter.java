package org.spongepowered.common.item.inventory.adapter.impl;

import net.minecraft.item.ItemStack;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.observer.InventoryEventArgs;
import org.spongepowered.common.item.inventory.query.Query;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

public class EntityLivingAdapter implements InventoryAdapter<ItemStack[], ItemStack> {

    @Override public Inventory parent() {
        return null;
    }

    @Override public <T extends Inventory> Iterable<T> slots() {
        return null;
    }

    @Override public <T extends Inventory> T first() {
        return null;
    }

    @Override public <T extends Inventory> T next() {
        return null;
    }

    @Override public Optional<org.spongepowered.api.item.inventory.ItemStack> poll() {
        return null;
    }

    @Override public Optional<org.spongepowered.api.item.inventory.ItemStack> poll(int limit) {
        return null;
    }

    @Override public Optional<org.spongepowered.api.item.inventory.ItemStack> peek() {
        return null;
    }

    @Override public Optional<org.spongepowered.api.item.inventory.ItemStack> peek(int limit) {
        return null;
    }

    @Override public InventoryTransactionResult offer(org.spongepowered.api.item.inventory.ItemStack stack) {
        return null;
    }

    @Override public InventoryTransactionResult set(org.spongepowered.api.item.inventory.ItemStack stack) {
        return null;
    }

    @Override public void clear() {

    }

    @Override public int size() {
        return 0;
    }

    @Override public int totalItems() {
        return 0;
    }

    @Override public int capacity() {
        return 0;
    }

    @Override public boolean isEmpty() {
        return false;
    }

    @Override public boolean contains(org.spongepowered.api.item.inventory.ItemStack stack) {
        return false;
    }

    @Override public boolean contains(ItemType type) {
        return false;
    }

    @Override public int getMaxStackSize() {
        return 0;
    }

    @Override public void setMaxStackSize(int size) {

    }

    @Override public <T extends InventoryProperty<?, ?>> Collection<T> getProperties(Inventory child, Class<T> property) {
        return null;
    }

    @Override public <T extends InventoryProperty<?, ?>> Collection<T> getProperties(Class<T> property) {
        return null;
    }

    @Override public <T extends InventoryProperty<?, ?>> Optional<T> getProperty(Inventory child, Class<T> property, Object key) {
        return null;
    }

    @Override public <T extends InventoryProperty<?, ?>> Optional<T> getProperty(Class<T> property, Object key) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(Class<?>... types) {
        return (T) Query.compile(this, types).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(ItemType... types) {
        return (T) Query.compile(this, types).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(org.spongepowered.api.item.inventory.ItemStack... types) {
        return (T) Query.compile(this, types).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(InventoryProperty<?, ?>... props) {
        return (T) Query.compile(this, props).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(Translation... names) {
        return (T) Query.compile(this, names).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(String... args) {
        return (T) Query.compile(this, args).execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Inventory> T query(Object... args) {
        return (T) Query.compile(this, args).execute();
    }

    @Override public SlotProvider<ItemStack[], ItemStack> getSlotProvider() {
        return null;
    }

    @Override public Lens<ItemStack[], ItemStack> getRootLens() {
        return null;
    }

    @Override public Fabric<ItemStack[]> getInventory() {
        return null;
    }

    @Override public Inventory getChild(int index) {
        return null;
    }

    @Override public Inventory getChild(Lens<ItemStack[], ItemStack> lens) {
        return null;
    }

    @Override public Iterator<Inventory> iterator() {
        return null;
    }

    @Override public Translation getName() {
        return null;
    }

    @Override public void notify(Object source, InventoryEventArgs eventArgs) {

    }
}
