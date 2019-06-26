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
package org.spongepowered.common.registry.type.world.gen;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenCanopyTree;
import net.minecraft.world.gen.feature.WorldGenDesertWells;
import net.minecraft.world.gen.feature.WorldGenMegaJungle;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import net.minecraft.world.gen.feature.WorldGenSavannaTree;
import net.minecraft.world.gen.feature.WorldGenShrub;
import net.minecraft.world.gen.feature.WorldGenSwamp;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenerator;
import org.spongepowered.api.registry.AdditionalCatalogRegistryModule;
import org.spongepowered.api.registry.AlternateCatalogRegistryModule;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.world.gen.PopulatorObject;
import org.spongepowered.api.world.gen.PopulatorObjects;
import org.spongepowered.common.bridge.world.gen.WorldGenTreesBridge;
import org.spongepowered.common.registry.type.AbstractPrefixAlternateCatalogTypeRegistryModule;

@RegisterCatalog(PopulatorObjects.class)
public class PopulatorObjectRegistryModule extends AbstractPrefixAlternateCatalogTypeRegistryModule<PopulatorObject>
    implements AlternateCatalogRegistryModule<PopulatorObject>, AdditionalCatalogRegistryModule<PopulatorObject> {


    public PopulatorObjectRegistryModule() {
        super("minecraft");
    }

    @Override
    public void registerAdditionalCatalog(PopulatorObject extraCatalog) {
        checkNotNull(extraCatalog, "CatalogType cannot be null");
        checkArgument(!extraCatalog.getId().isEmpty(), "Id cannot be empty");
        checkArgument(!this.catalogTypeMap.containsKey(extraCatalog.getId()), "Duplicate Id");
        this.catalogTypeMap.put(extraCatalog.getId(), extraCatalog);
    }

    @Override
    public void registerDefaults() {
        // Populators
        register(new WorldGenDesertWells());

        // Trees
        register(new WorldGenTrees(false));
        register(new WorldGenBigTree(false));
        register(new WorldGenBirchTree(false, false));
        register(new WorldGenBirchTree(false, true));
        register(new WorldGenTaiga2(false));
        register(new WorldGenTaiga1());
        register(new WorldGenMegaPineTree(false, true));
        register(new WorldGenMegaPineTree(false, false));
        IBlockState jlog = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE);
        IBlockState jleaf = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE).withProperty(BlockLeaves.CHECK_DECAY, Boolean.valueOf(false));
        IBlockState leaf = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE).withProperty(BlockLeaves.CHECK_DECAY, Boolean.valueOf(false));
        WorldGenTreesBridge trees = (WorldGenTreesBridge) new WorldGenTrees(false, 4, jlog, jleaf, true);
        trees.bridge$setId("minecraft:jungle");
        trees.bridge$setName("Jungle tree");
        trees.bridge$setMinHeight(VariableAmount.baseWithRandomAddition(4, 7));
        register((WorldGenTrees) trees);
        register(new WorldGenMegaJungle(false, 10, 20, jlog, jleaf));
        WorldGenShrub bush = new WorldGenShrub(jlog, leaf);
        register(bush);
        register( new WorldGenSavannaTree(false));
        register(new WorldGenCanopyTree(false));
        register(new WorldGenSwamp());

        // Mushrooms
        register(new WorldGenBigMushroom(Blocks.BROWN_MUSHROOM_BLOCK));
        register(new WorldGenBigMushroom(Blocks.RED_MUSHROOM_BLOCK));
    }

    private void register(WorldGenerator worldGenerator) {
        register((PopulatorObject) worldGenerator);
    }

}
