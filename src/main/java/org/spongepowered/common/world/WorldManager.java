package org.spongepowered.common.world;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

public interface WorldManager {

    Path getSavesDirectory();

    void registerDimension(DimensionType dimensionType, int dimensionId);
    
    void unregisterDimension(int dimensionId);

    @Nullable
    RegistetedDimension getRegisteredDimension(int dimensionId);

    Collection<RegistetedDimension> getRegisteredDimensions();

    @Nullable
    WorldInfo createWorldInfo(String directoryName, DimensionType dimensionType, WorldSettings worldSettings);

    @Nullable
    WorldInfo getWorldInfo(DimensionType dimensionType);

    @Nullable
    WorldInfo getWorldInfo(int dimensionId);

    @Nullable
    WorldInfo getWorldInfo(String directoryName);

    @Nullable
    WorldInfo getWorldInfo(UUID uniqueId);

    Collection<WorldInfo> getLoadedWorldInfos();

    Collection<WorldInfo> getUnloadedWorldInfos();

    @Nullable
    WorldServer loadWorld(DimensionType dimensionType);

    @Nullable
    WorldServer loadWorld(int dimensionId);

    @Nullable
    WorldServer loadWorld(String directoryName);

    @Nullable
    WorldServer loadWorld(UUID uniqueId);

    @Nullable
    WorldServer loadWorld(WorldInfo worldInfo);

    String getDefaultWorldDirectory();

    @Nullable
    WorldServer getDefaultWorld();

    @Nullable
    WorldServer getWorld(DimensionType dimensionType);

    @Nullable
    WorldServer getWorld(int dimensionId);

    @Nullable
    WorldServer getWorld(String directoryName);

    @Nullable
    WorldServer getWorld(UUID uniqueId);

    boolean unloadWorld(WorldServer world);

    Collection<WorldServer> getWorlds();

    CompletableFuture<Optional<WorldInfo>> copyWorld(WorldInfo worldInfo, String copyName);

    @Nullable
    WorldInfo renameWorld(WorldInfo worldInfo, String newDirectoryName);

    CompletableFuture<Boolean> deleteWorld(WorldInfo worldInfo);
}
