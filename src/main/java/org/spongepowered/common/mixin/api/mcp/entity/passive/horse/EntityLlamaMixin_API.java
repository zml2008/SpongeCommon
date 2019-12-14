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
package org.spongepowered.common.mixin.api.mcp.entity.passive.horse;

import Mutable;
import net.minecraft.entity.passive.horse.LlamaEntity;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.LlamaType;
import org.spongepowered.api.data.type.LlamaTypes;
import org.spongepowered.api.entity.living.animal.horse.llama.Llama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.data.value.SpongeValueFactory;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import org.spongepowered.common.util.Constants;

@Mixin(LlamaEntity.class)
public abstract class EntityLlamaMixin_API extends AbstractHorseMixin_API implements Llama {

    @Shadow public abstract int getStrength();
    @Shadow public abstract int getVariant();
    @Shadow public abstract void setVariant(int p_190710_1_);

    @Override
    public Mutable<LlamaType> llamaVariant() {
        final int i = this.getVariant();
        final LlamaType variant;
        if (i == 0) {
            variant = LlamaTypes.CREAMY;
        } else if (i == 1) {
            variant = LlamaTypes.WHITE;
        } else if (i == 2) {
            variant = LlamaTypes.BROWN;
        } else if (i == 3) {
            variant = LlamaTypes.GRAY;
        } else {
            this.setVariant(0); // Basically some validation
            variant = LlamaTypes.CREAMY;
        }
        return new SpongeValue<>(Keys.LLAMA_VARIANT, Constants.Entity.Llama.DEFAULT_VARIANT, variant);
    }

    @Override
    public org.spongepowered.api.data.value.BoundedValue.Mutable<Integer> strength() {
        return SpongeValueFactory.getInstance()
                .createBoundedValueBuilder(Keys.LLAMA_STRENGTH)
                .defaultValue(Constants.Entity.Llama.DEFAULT_STRENGTH)
                .minimum(Constants.Entity.Llama.MINIMUM_STRENGTH)
                .maximum(Constants.Entity.Llama.MAXIMUM_STRENGTH)
                .actualValue(this.getStrength())
                .build();
    }


}
