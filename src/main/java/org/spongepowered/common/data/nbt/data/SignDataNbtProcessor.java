package org.spongepowered.common.data.nbt.data;

import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.immutable.tileentity.ImmutableSignData;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.common.data.nbt.AbstractSpongeNbtProcessor;
import org.spongepowered.common.data.nbt.NbtDataType;
import org.spongepowered.common.data.nbt.NbtDataTypes;

import java.util.Optional;

public class SignDataNbtProcessor extends AbstractSpongeNbtProcessor<SignData, ImmutableSignData> {

    protected SignDataNbtProcessor(NbtDataType dataType) {
        super(NbtDataTypes.TILE_ENTITY);
    }

    @Override
    public boolean isCompatible(NBTTagCompound compound) {
        return false;
    }

    @Override
    public Optional<SignData> readFrom(NBTTagCompound compound) {
        return Optional.empty();
    }

    @Override
    public Optional<NBTTagCompound> storeToCompound(NBTTagCompound compound, SignData manipulator) {
        return Optional.empty();
    }

    @Override
    public Optional<DataView> storeToView(DataView view, SignData manipulator) {
        return Optional.empty();
    }

    @Override
    public DataTransactionResult remove(NBTTagCompound data) {
        return null;
    }

}
