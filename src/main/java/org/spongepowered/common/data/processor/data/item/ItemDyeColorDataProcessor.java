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
package org.spongepowered.common.data.processor.data.item;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.manipulator.immutable.ImmutableDyeableData;
import org.spongepowered.api.data.manipulator.mutable.DyeableData;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.Value.Mutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.manipulator.mutable.SpongeDyeableData;
import org.spongepowered.common.data.processor.common.AbstractItemSingleDataProcessor;
import org.spongepowered.common.data.value.SpongeValueFactory;

import java.util.Optional;

public class ItemDyeColorDataProcessor extends AbstractItemSingleDataProcessor<DyeColor, Mutable<DyeColor>, DyeableData, ImmutableDyeableData> {

    public ItemDyeColorDataProcessor() {
        super(x -> isDyeable(x.getItem()), Keys.DYE_COLOR);
    }

    public static boolean isDyeable(Item item) {
        if (item.equals(Items.DYE) || item.equals(Items.BED)) {
            return true;
        }

        final Block block = Block.getBlockFromItem(item);

        return block.equals(Blocks.WOOL)
                || block.equals(Blocks.STANDING_BANNER)
                || block.equals(Blocks.STAINED_GLASS)
                || block.equals(Blocks.CARPET)
                || block.equals(Blocks.STAINED_GLASS_PANE)
                || block.equals(Blocks.STAINED_HARDENED_CLAY)
                || block.equals(Blocks.CONCRETE)
                || block.equals(Blocks.CONCRETE_POWDER);
    }

    @Override
    protected Mutable<DyeColor> constructValue(DyeColor actualValue) {
        return SpongeValueFactory.getInstance().createValue(Keys.DYE_COLOR, actualValue, DyeColors.BLACK);
    }

    @Override
    protected boolean set(ItemStack container, DyeColor value) {
        Item item = container.getItem();

        if(item.equals(Items.DYE) || item.equals(Items.BANNER)) {
            container.setItemDamage(((net.minecraft.item.DyeColor) (Object) value).getDyeDamage());
        } else {
            container.setItemDamage(((net.minecraft.item.DyeColor) (Object) value).getMetadata());
        }
        return true;
    }

    @Override
    protected Optional<DyeColor> getVal(ItemStack container) {
        Item item = container.getItem();

        if(item.equals(Items.DYE) || item.equals(Items.BANNER)) {
            return Optional.of((DyeColor) (Object) net.minecraft.item.DyeColor.byDyeDamage(container.getDamage()));
        }
        return Optional.of((DyeColor) (Object) net.minecraft.item.DyeColor.byMetadata(container.getDamage()));
    }

    @Override
    protected Immutable<DyeColor> constructImmutableValue(DyeColor value) {
        return this.constructValue(value).asImmutable();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    protected DyeableData createManipulator() {
        return new SpongeDyeableData();
    }
}
