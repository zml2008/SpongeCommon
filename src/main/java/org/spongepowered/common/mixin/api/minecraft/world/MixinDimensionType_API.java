package org.spongepowered.common.mixin.api.minecraft.world;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.DimensionTypeBridge;

@Mixin(DimensionType.class)
@Implements(@Interface(iface = org.spongepowered.api.world.DimensionType.class, prefix = "dimensionType$"))
public abstract class MixinDimensionType_API {

    @Shadow @Final private Class <? extends WorldProvider> clazz;
    @Shadow public abstract String getName();

    public String dimensionType$getId() {
        return ((DimensionTypeBridge) this).bridge$getProperId();
    }

    @Intrinsic
    public String dimensionType$getName() {
        return this.getName();
    }

    public Class<? extends Dimension> dimensionType$getDimensionClass() {
        return (Class<? extends Dimension>) this.clazz;
    }
}
