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
package org.spongepowered.common.data.persistence;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.tileentity.TileEntityArchetype;
import org.spongepowered.api.block.tileentity.TileEntityType;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.persistence.DataTranslator;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.schematic.BlockPalette;
import org.spongepowered.api.world.schematic.Schematic;
import org.spongepowered.common.block.SpongeTileEntityArchetypeBuilder;
import org.spongepowered.common.data.util.DataQueries;
import org.spongepowered.common.registry.type.block.TileEntityTypeRegistryModule;
import org.spongepowered.common.util.gen.ArrayMutableBlockBuffer;
import org.spongepowered.common.util.gen.ArrayMutableBlockBuffer.BackingDataType;
import org.spongepowered.common.world.schematic.BimapPalette;
import org.spongepowered.common.world.schematic.GlobalPalette;
import org.spongepowered.common.world.schematic.SpongeSchematic;

import java.util.List;
import java.util.Map;

public class StructureTranslator implements DataTranslator<Schematic> {

    private static final StructureTranslator INSTANCE = new StructureTranslator();
    private static final TypeToken<Schematic> TYPE_TOKEN = TypeToken.of(Schematic.class);
    private static final int MAX_SIZE = 32;

    private final Cause cause = Cause.source(this).build();

    public static StructureTranslator get() {
        return INSTANCE;
    }

    private StructureTranslator() {
    }

    @Override
    public String getId() {
        return "sponge:structure";
    }

    @Override
    public String getName() {
        return "Structure translator";
    }

    @Override
    public TypeToken<Schematic> getToken() {
        return TYPE_TOKEN;
    }

    @Override
    public Schematic translate(DataView view) throws InvalidDataException {
        List<Integer> list = view.getIntegerList(DataQueries.Schematic.STRUCTURE_SIZE)
                .orElseThrow(() -> new InvalidDataException("Structure size not found."));
        if (list.size() != 3) {
            throw new InvalidDataException("Structure size list did not contain 3 integers");
        }
        int width = list.get(0);
        int height = list.get(1);
        int length = list.get(2);
        if (width > MAX_SIZE || height > MAX_SIZE || length > MAX_SIZE) {
            throw new InvalidDataException(String.format(
                    "Structure is larger than maximum allowable size (found: (%d, %d, %d) max: (%d, %<d, %<d)", width, height, length, MAX_SIZE));
        }
        BimapPalette palette = new BimapPalette();
        // TODO create palette from data
        ArrayMutableBlockBuffer buffer = new ArrayMutableBlockBuffer(palette, Vector3i.ZERO,
                new Vector3i(width, height, length), BackingDataType.CHAR);
        List<DataView> blocks = view.getViewList(DataQueries.Schematic.STRUCTURE_BLOCKS)
                .orElseThrow(() -> new InvalidDataException("Structure blocks not found."));
        Map<Vector3i, TileEntityArchetype> tiles = Maps.newHashMap();
        for (DataView block : blocks) {
            BlockState state = palette.get(block.getInt(DataQueries.Schematic.STRUCTURE_BLOCK_STATE).get()).get();
            List<Integer> pos = view.getIntegerList(DataQueries.Schematic.STRUCTURE_SIZE)
                    .orElseThrow(() -> new InvalidDataException("Structure block position not found."));
            if (pos.size() != 3) {
                throw new InvalidDataException("Structure block position did not contain 3 integers");
            }
            int posX = pos.get(0);
            int posY = pos.get(1);
            int posZ = pos.get(2);
            buffer.setBlock(posX, posY, posZ, state, this.cause);
            DataView nbt = block.getView(DataQueries.Schematic.STRUCTURE_BLOCK_NBT).orElse(null);
            if (nbt != null) {
                // TODO create tile entity archetype from nbt
            }
        }

        // TODO load entities

        SpongeSchematic schematic = new SpongeSchematic(buffer, tiles);
        if (view.contains(DataQueries.Schematic.STRUCTURE_AUTHOR)) {
            schematic.getMetadata().set(DataQuery.of(Schematic.METADATA_AUTHOR), view.getString(DataQueries.Schematic.STRUCTURE_AUTHOR).get());
        }
        return schematic;
    }

    @Override
    public DataContainer translate(Schematic schematic) throws InvalidDataException {
        DataContainer data = new MemoryDataContainer(DataView.SafetyMode.NO_DATA_CLONED);
        final int width = schematic.getBlockSize().getX();
        final int height = schematic.getBlockSize().getY();
        final int length = schematic.getBlockSize().getZ();
        if (width > MAX_SIZE || height > MAX_SIZE || length > MAX_SIZE) {
            throw new IllegalArgumentException(String.format(
                    "Schematic is larger than maximum allowable size (found: (%d, %d, %d) max: (%d, %<d, %<d)", width, height, length, MAX_SIZE));
        }
        List<Integer> size = Lists.newArrayList(width, height, length);
        data.set(DataQueries.Schematic.STRUCTURE_SIZE, size);

        // TODO unfinished

        if (schematic.getMetadata().contains(DataQuery.of(Schematic.METADATA_AUTHOR))) {
            data.set(DataQueries.Schematic.STRUCTURE_AUTHOR, schematic.getMetadata().getString(DataQuery.of(Schematic.METADATA_AUTHOR)).get());
        }
        return data;
    }
}
