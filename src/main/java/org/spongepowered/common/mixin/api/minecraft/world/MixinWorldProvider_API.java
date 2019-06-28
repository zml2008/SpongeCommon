package org.spongepowered.common.mixin.api.minecraft.world;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.api.world.GeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.bridge.world.DimensionTypeBridge;

@Mixin(WorldProvider.class)
public abstract class MixinWorldProvider_API implements Dimension {

    @Shadow public WorldType terrainType;
    @Shadow public abstract DimensionType getDimensionType();
    @Shadow public abstract boolean canRespawnHere();
    @Shadow public abstract int getAverageGroundLevel();
    @Shadow public abstract boolean isNether();
    @Shadow public abstract boolean doesWaterVaporize();

    @Override
    public org.spongepowered.api.world.DimensionType getType() {
        return (org.spongepowered.api.world.DimensionType) (Object) this.getDimensionType();
    }

    @Override
    public GeneratorType getGeneratorType() {
        return (GeneratorType) this.terrainType;
    }

    @Override
    public boolean allowsPlayerRespawns() {
        return this.canRespawnHere();
    }

    @Override
    public int getMinimumSpawnHeight() {
        return this.getAverageGroundLevel();
    }

    @Override
    public boolean doesWaterEvaporate() {
        return this.doesWaterVaporize();
    }

    @Override
    public boolean hasSky() {
        return !this.isNether();
    }

    @Override
    public Context getContext() {
        return ((DimensionTypeBridge) (Object) this.getDimensionType()).bridge$getContext();
    }
}
