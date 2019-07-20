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
package org.spongepowered.common.item.inventory.query.result;

import org.spongepowered.api.item.inventory.InventoryProperty;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.MutableLensSet;
import org.spongepowered.common.item.inventory.lens.impl.AbstractLens;
import org.spongepowered.common.item.inventory.query.Result;

import java.util.Collection;

public interface QueryResult extends Result {

    abstract class QueryLens extends AbstractLens {

        private final MutableLensSet resultSet;

        public QueryLens(int size, MutableLensSet resultSet) {
            super(0, size, QueryResult.class);
            this.resultSet = resultSet;

            for (Lens result : this.resultSet) {
                Collection<InventoryProperty<?, ?>> properties = this.resultSet.getProperties(result);
                this.addSpanningChild(result, properties.toArray(new InventoryProperty[0]));
            }
        }
    }
}
