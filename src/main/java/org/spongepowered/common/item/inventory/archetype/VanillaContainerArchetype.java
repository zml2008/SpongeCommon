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
package org.spongepowered.common.item.inventory.archetype;

import com.google.common.base.Preconditions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.world.IInteractionObject;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetype;
import org.spongepowered.api.item.inventory.InventoryProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public class VanillaContainerArchetype implements InventoryArchetype {

    private final String id;
    private final String name;
    private final Map<String, InventoryProperty<String, ?>> properties = new HashMap<>();
    private final ContainerProvider provider;

    public VanillaContainerArchetype(String id, String name, ContainerProvider provider) {
        this.id = id;
        this.name = name;
        this.provider = provider;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<InventoryArchetype> getChildArchetypes() {
        return Collections.emptyList();
    }

    @Override
    public Optional<InventoryProperty<String, ?>> getProperty(String key) {
        return Optional.ofNullable(this.properties.get(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends InventoryProperty<String, ?>> Optional<T> getProperty(Class<T> type, String key) {

        InventoryProperty<String, ?> property = this.properties.get(key);
        if (property == null) {
            return Optional.empty();
        }
        if (type.isAssignableFrom(property.getClass())) {
            return Optional.of((T) property);
        }
        return Optional.empty();
    }

    @Override
    public Map<String, InventoryProperty<String, ?>> getProperties() {
        return this.properties;
    }

    @Nullable
    public ContainerProvider getContainerProvider() {
        return null;
    }

    public Container build(Carrier carrier) {
        return this.provider.provide(null, ((EntityPlayer) Preconditions.checkNotNull(carrier)));
    }

    /**
     * Provides a {@link Container} for a {@link EntityPlayer} viewing an {@link IInventory}
     */
    public interface ContainerProvider {
        Container provide(IInventory viewed, EntityPlayer viewing);
    }

}
