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
package org.spongepowered.common.registry.type.item;

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.item.CreativeTab;
import org.spongepowered.api.item.CreativeTabs;
import org.spongepowered.api.registry.CatalogRegistryModule;
import org.spongepowered.api.registry.util.RegisterCatalog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CreativeTabRegistryModule implements CatalogRegistryModule<CreativeTab> {

    @RegisterCatalog(CreativeTabs.class)
    public final Map<String, CreativeTab> creativeTabMap = new HashMap<>();

    @Override
    public Optional<CreativeTab> getById(String id) {
        return Optional.ofNullable(this.creativeTabMap.get(id.toLowerCase()));
    }

    @Override
    public Collection<CreativeTab> getAll() {
        return ImmutableList.copyOf(this.creativeTabMap.values());
    }

    @Override
    public void registerDefaults() {
        this.creativeTabMap.put("block", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabBlock);
        this.creativeTabMap.put("brewing", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabBrewing);
        this.creativeTabMap.put("combat",  (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabCombat);
        this.creativeTabMap.put("decorations", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabDecorations);
        this.creativeTabMap.put("food", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabFood);
        this.creativeTabMap.put("materials", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabMaterials);
        this.creativeTabMap.put("misc", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabMisc);
        this.creativeTabMap.put("redstone", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabRedstone);
        this.creativeTabMap.put("transport", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabTransport);
        this.creativeTabMap.put("tools", (CreativeTab) net.minecraft.creativetab.CreativeTabs.tabTools);
    }
}
