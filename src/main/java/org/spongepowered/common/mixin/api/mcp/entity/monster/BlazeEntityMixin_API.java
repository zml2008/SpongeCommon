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
package org.spongepowered.common.mixin.api.mcp.entity.monster;

import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.monster.Blaze;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeFlammableData;
import org.spongepowered.common.data.value.mutable.SpongeValue;
import Mutable;
import java.util.Collection;
import net.minecraft.entity.monster.BlazeEntity;

@Mixin(BlazeEntity.class)
public abstract class BlazeEntityMixin_API extends MonsterEntityMixin_API implements Blaze {

    @Shadow public abstract boolean isBurning();

    @Override
    public Mutable<Boolean> aflame() {
        return new SpongeValue<>(Keys.IS_AFLAME, false, this.isBurning());
    }

    @Override
    public void spongeApi$supplyVanillaManipulators(Collection<? super org.spongepowered.api.data.DataManipulator.Mutable<?, ?>> manipulators) {
        super.spongeApi$supplyVanillaManipulators(manipulators);
        manipulators.add(new SpongeFlammableData(this.isBurning()));
    }
}
