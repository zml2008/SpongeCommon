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

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenCanopyTree;
import net.minecraft.world.gen.feature.WorldGenMegaJungle;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import net.minecraft.world.gen.feature.WorldGenSavannaTree;
import net.minecraft.world.gen.feature.WorldGenShrub;
import net.minecraft.world.gen.feature.WorldGenSwamp;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenerator;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.world.gen.PopulatorObject;
import org.spongepowered.api.world.gen.type.BiomeTreeType;
import org.spongepowered.api.world.gen.type.BiomeTreeTypes;
import org.spongepowered.common.bridge.world.gen.WorldGenTreesBridge;
import org.spongepowered.common.registry.type.AbstractPrefixAlternateCatalogTypeRegistryModule;
import org.spongepowered.common.world.gen.type.SpongeBiomeTreeType;

import javax.annotation.Nullable;

@RegisterCatalog(BiomeTreeTypes.class)
public class BiomeTreeTypeRegistryModule extends AbstractPrefixAlternateCatalogTypeRegistryModule<BiomeTreeType> {

    public BiomeTreeTypeRegistryModule() {
        super("minecraft");
    }

    @Override
    public void registerDefaults() {
        register(create("oak", new WorldGenTrees(false), new WorldGenBigTree(false)));
        register(create("birch", new WorldGenBirchTree(false, false), new WorldGenBirchTree(false, true)));

        WorldGenMegaPineTree tall_megapine = new WorldGenMegaPineTree(false, true);
        WorldGenMegaPineTree megapine = new WorldGenMegaPineTree(false, false);

        register(create("tall_taiga", new WorldGenTaiga2(false), tall_megapine));
        register(create("pointy_taiga", new WorldGenTaiga1(), megapine));

        IBlockState jlog = Blocks.LOG.getDefaultState()
            .withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE);

        IBlockState jleaf = Blocks.LEAVES.getDefaultState()
            .withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE)
            .withProperty(BlockLeaves.CHECK_DECAY, Boolean.valueOf(false));

        IBlockState leaf = Blocks.LEAVES.getDefaultState()
            .withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE)
            .withProperty(BlockLeaves.CHECK_DECAY, Boolean.valueOf(false));

        WorldGenTreesBridge trees = (WorldGenTreesBridge) new WorldGenTrees(false, 4, jlog, jleaf, true);
        trees.bridge$setMinHeight(VariableAmount.baseWithRandomAddition(4, 7));
        WorldGenMegaJungle mega = new WorldGenMegaJungle(false, 10, 20, jlog, jleaf);

        register(create("jungle", (WorldGenTrees) trees, mega));

        WorldGenShrub bush = new WorldGenShrub(jlog, leaf);

        register(create("jungle_bush", bush, null));
        register(create("savanna", new WorldGenSavannaTree(false), null));
        register(create("canopy", new WorldGenCanopyTree(false), null));
        register(create("swamp", new WorldGenSwamp(), null));
    }

    private SpongeBiomeTreeType create(String name, WorldGenerator small, @Nullable WorldGenerator large) {
        return new SpongeBiomeTreeType("minecraft:" + name, name, (PopulatorObject) small, (PopulatorObject) large);
    }
}
