package org.spongepowered.common.mixin.api.minecraft.server;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.api.Server;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.resourcepack.ResourcePack;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldArchetype;
import org.spongepowered.api.world.storage.ChunkLayout;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.server.MinecraftServerBridge;
import org.spongepowered.common.bridge.world.WorldInfoBridge;
import org.spongepowered.common.profile.SpongeGameProfileManager;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.world.storage.SpongeChunkLayout;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_API implements Server, ConsoleSource {

    @Shadow public abstract PlayerList getPlayerList();
    @Shadow public abstract boolean isServerInOnlineMode();
    @Shadow private String motd;
    @Shadow private int tickCounter;
    @Shadow @Final public long[] tickTimeArray;
    @Shadow public abstract void initiateShutdown();

    @Shadow public abstract int getMaxPlayerIdleMinutes();

    @Shadow public abstract void setPlayerIdleTimeout(int idleTimeout);

    @Shadow public Thread serverThread;
    private SpongeGameProfileManager profileManager = new SpongeGameProfileManager();
    private MessageChannel broadcastChannel = MessageChannel.TO_ALL;
    private SpongeChunkLayout chunkLayout = new SpongeChunkLayout();
    private Text spongeMOTD;

    @Override
    public ChunkLayout getChunkLayout() {
        return this.chunkLayout;
    }

    @Override
    public MessageChannel getBroadcastChannel() {
        return this.broadcastChannel;
    }

    @Override
    public void setBroadcastChannel(MessageChannel channel) {
        this.broadcastChannel = checkNotNull(channel, "channel");
    }

    @Override
    public Optional<InetSocketAddress> getBoundAddress() {
        return Optional.empty();
    }

    @Override
    public boolean hasWhitelist() {
        return this.getPlayerList().whiteListEnforced;
    }

    @Override
    public void setHasWhitelist(boolean enabled) {
        this.getPlayerList().setWhiteListEnabled(enabled);
    }

    @Override
    public boolean getOnlineMode() {
        return this.isServerInOnlineMode();
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return ImmutableList.copyOf((List<Player>) (Object) this.getPlayerList().getPlayers());
    }

    @Override
    public Optional<Player> getPlayer(UUID uniqueId) {
        return Optional.ofNullable((Player) this.getPlayerList().getPlayerByUUID(uniqueId));
    }

    @Override
    public Optional<Player> getPlayer(String name) {
        return Optional.ofNullable((Player) this.getPlayerList().getPlayerByUsername(name));
    }

    @Override
    public Text getMotd() {
        if (this.spongeMOTD == null) {
            this.spongeMOTD = SpongeTexts.fromLegacy(this.motd);
        }
        return this.spongeMOTD;
    }

    @Override
    public int getMaxPlayers() {
        return this.getPlayerList().getMaxPlayers();
    }

    @Override
    public int getRunningTimeTicks() {
        return this.tickCounter;
    }

    @Override
    public double getTicksPerSecond() {
        double nanosPerTick = MathHelper.average(this.tickTimeArray);
        return 1000 / Math.max(50, nanosPerTick / 1000000);
    }

    @Override
    public String getIdentifier() {
        return this.getName();
    }

    @Override
    public ConsoleSource getConsole() {
        return this;
    }

    @Override
    public void shutdown() {
        this.initiateShutdown();
    }

    @Override
    public void shutdown(Text kickMessage) {
        for (Player player : this.getOnlinePlayers()) {
            player.kick(kickMessage);
        }

        this.initiateShutdown();
    }

    @Override
    public GameProfileManager getGameProfileManager() {
        return this.profileManager;
    }

    @Override
    public Optional<ResourcePack> getDefaultResourcePack() {
        return Optional.ofNullable(((MinecraftServerBridge) this).bridge$getSpongeResourcePack());
    }

    @Override
    public Optional<Scoreboard> getServerScoreboard() {
        final WorldServer world = SpongeImpl.getWorldManager().getDefaultWorld();
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of((Scoreboard) world.getScoreboard());
    }

    @Override
    public int getPlayerIdleTimeout() {
        return this.getMaxPlayerIdleMinutes();
    }

    @Intrinsic
    public void server$setPlayerIdleTimeout(int timeout) {
        this.setPlayerIdleTimeout(timeout);
    }

    @Override
    public boolean isMainThread() {
        return this.serverThread == Thread.currentThread();
    }

    @Override
    public Optional<World> loadWorld(UUID uuid) {
        return Optional.ofNullable((World) SpongeImpl.getWorldManager().loadWorld(uuid));
    }

    @Override
    public Optional<World> loadWorld(WorldProperties properties) {
        return Optional.ofNullable((World) SpongeImpl.getWorldManager().loadWorld((WorldInfo) properties));
    }

    @Override
    public Optional<World> loadWorld(String worldName) {
        return Optional.ofNullable((World) SpongeImpl.getWorldManager().loadWorld(worldName));
    }

    @Override
    public WorldProperties createWorldProperties(String folderName, WorldArchetype archetype) {
        return (WorldProperties) SpongeImpl.getWorldManager().createWorldInfo(folderName, (net.minecraft.world.DimensionType) (Object) archetype.getDimensionType(), (WorldSettings) (Object) archetype);
    }

    @Override
    public boolean unloadWorld(World world) {
        return SpongeImpl.getWorldManager().unloadWorld((WorldServer) world);
    }

    @Override
    public Collection<World> getWorlds() {
        return (Collection<World>) (Object) Collections.unmodifiableCollection(SpongeImpl.getWorldManager().getWorlds());
    }

    @Override
    public Optional<World> getWorld(UUID uniqueId) {
        return Optional.ofNullable((World) SpongeImpl.getWorldManager().getWorld(uniqueId));
    }

    @Override
    public Optional<WorldProperties> getDefaultWorld() {
        return Optional.ofNullable((WorldProperties) SpongeImpl.getWorldManager().getDefaultWorld().getWorldInfo());
    }

    @Override
    public String getDefaultWorldName() {
        return SpongeImpl.getWorldManager().getDefaultWorldDirectory();
    }

    @Override
    public Collection<WorldProperties> getUnloadedWorlds() {
        return (Collection<WorldProperties>) (Object) SpongeImpl.getWorldManager().getUnloadedWorldInfos();
    }

    @Override
    public Optional<WorldProperties> getWorldProperties(UUID uniqueId) {
        return Optional.ofNullable((WorldProperties) SpongeImpl.getWorldManager().getWorldInfo(uniqueId));
    }

    @Override
    public CompletableFuture<Optional<WorldProperties>> copyWorld(WorldProperties worldProperties, String copyName) {
        return (CompletableFuture<Optional<WorldProperties>>) (Object) SpongeImpl.getWorldManager().copyWorld((WorldInfo) worldProperties, copyName);
    }

    @Override
    public Optional<WorldProperties> renameWorld(WorldProperties worldProperties, String newName) {
        return Optional.ofNullable((WorldProperties) SpongeImpl.getWorldManager().renameWorld((WorldInfo) worldProperties, newName));
    }

    @Override
    public CompletableFuture<Boolean> deleteWorld(WorldProperties worldProperties) {
        return SpongeImpl.getWorldManager().deleteWorld((WorldInfo) (Object) worldProperties.getWorldName());
    }

    @Override
    public boolean saveWorldProperties(WorldProperties properties) {
        return ((WorldInfoBridge) properties).bridge$save();
    }

    @Override
    public Optional<World> getWorld(String worldName) {
        return Optional.ofNullable((World) SpongeImpl.getWorldManager().getWorld(worldName));
    }

    @Override
    public Optional<WorldProperties> getWorldProperties(String worldName) {
        return Optional.ofNullable((WorldProperties) SpongeImpl.getWorldManager().getWorldInfo(worldName));
    }

    @Override
    public Collection<WorldProperties> getAllWorldProperties() {
        return (Collection<WorldProperties>) (Object) SpongeImpl.getWorldManager().getLoadedWorldInfos();
    }
}
