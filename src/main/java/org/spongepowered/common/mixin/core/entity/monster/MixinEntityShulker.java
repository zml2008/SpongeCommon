package org.spongepowered.common.mixin.core.entity.monster;

import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.EnumFacing;
import org.spongepowered.api.util.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.data.util.DirectionResolver;
import org.spongepowered.common.mixin.core.entity.MixinEntityLiving;

@Mixin(EntityShulker.class)
public abstract class MixinEntityShulker extends MixinEntityLiving {

    @Shadow @Final protected static DataParameter<EnumFacing> ATTACHED_FACE;

    @Override
    public Direction bridge$getDirection() {
        return DirectionResolver.getFor(this.dataManager.get(ATTACHED_FACE));
    }

    @Override
    public void bridge$setDirection(Direction direction) {
        this.dataManager.set(ATTACHED_FACE, DirectionResolver.getFor(direction));
    }
}
