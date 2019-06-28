package org.spongepowered.common.bridge.world.storage;

import net.minecraft.world.storage.WorldInfo;

import java.io.File;
import java.io.IOException;

public interface SaveHandlerBridge {

    void bridge$loadSpongeData(WorldInfo info) throws IOException;

    File bridge$getWorldDirectory();
}
