package org.spongepowered.common.data.nbt.data.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.common.data.nbt.AbstractSpongeNbtProcessor;
import org.spongepowered.common.data.nbt.NbtDataTypes;
import org.spongepowered.common.data.type.SpongeTileEntityType;
import org.spongepowered.common.data.util.NbtDataUtil;

public abstract class AbstractTileEntityNbtProcessor<M extends DataManipulator<M, I>, I extends ImmutableDataManipulator<I, M>> extends
    AbstractSpongeNbtProcessor<M, I> {

    private final SpongeTileEntityType supportedType;

    protected AbstractTileEntityNbtProcessor(SpongeTileEntityType supportedType) {
        super(NbtDataTypes.TILE_ENTITY);
        this.supportedType = supportedType;
    }



    @Override
    public boolean isCompatible(NBTTagCompound compound) {
        return compound.getString(NbtDataUtil.TileEntity.POSITION_Y);
    }
}
