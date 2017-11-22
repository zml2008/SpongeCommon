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
package org.spongepowered.common.mixin.core.item;

import com.flowpowered.math.vector.Vector3d;
import net.minecraft.block.state.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEnderEye;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.projectile.EyeOfEnder;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.world.ConstructPortalEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.util.VecHelper;

import javax.annotation.Nullable;

@Mixin(ItemEnderEye.class)
public class MixinItemEnderEye extends Item {

    @Redirect(method = "onItemRightClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean onspawnEntity(World world, Entity enderEye, World worldIn, EntityPlayer player, EnumHand hand) {
        ((EyeOfEnder) enderEye).setShooter((ProjectileSource) player);
        // TODO direct this appropriately
        return world.spawnEntity(enderEye);
    }

    @Redirect(method = "onItemUse", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/state/pattern/BlockPattern;match(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/pattern/BlockPattern$PatternHelper;"))
    @Nullable
    private BlockPattern.PatternHelper onConstructPortal(BlockPattern pattern, World worldIn, BlockPos pos) {
        BlockPattern.PatternHelper match = pattern.match(worldIn, pos);
        if (match == null) {
            return null;
        }
        Vector3d center = VecHelper.toVector3d(match.getFrontTopLeft())
                .sub(pattern.getThumbLength() / 2.0, pattern.getFingerLength() / 2.0, pattern.getPalmLength() / 2.0);
        ConstructPortalEvent event = SpongeEventFactory.createConstructPortalEvent(Sponge.getCauseStackManager().getCurrentCause(),
                new Location<>((org.spongepowered.api.world.World) worldIn, center));
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            // TODO The eye has already been placed in the portal, so no one
            // can try to spawn the portal again. Should the interaction be
            // undone?
            return null;
        }
        return match;
    }

}
