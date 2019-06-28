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
package org.spongepowered.common.mixin.core.world;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.Dimension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.config.SpongeConfig;
import org.spongepowered.common.config.type.DimensionConfig;
import org.spongepowered.common.bridge.world.DimensionTypeBridge;
import org.spongepowered.common.registry.type.world.DimensionTypeRegistryModule;
import org.spongepowered.common.world.WorldManager;

import java.nio.file.Path;

@Mixin(DimensionType.class)
public abstract class MixinDimensionType implements DimensionTypeBridge {

    @Shadow public abstract String getName();

    private String sanitizedId;
    private String enumName;
    private String modId;
    private Path configPath;
    private SpongeConfig<DimensionConfig> config;
    private volatile Context context;
    private boolean generateSpawnOnLoad;
    private boolean loadSpawn;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(String enumName, int ordinal, int idIn, String nameIn, String suffixIn, Class<? extends WorldProvider> clazzIn, CallbackInfo ci) {
        final String dimName = enumName.toLowerCase().replace(" ", "_").replaceAll("[^A-Za-z0-9_]", "");
        this.enumName = dimName;
        this.modId = SpongeImplHooks.getModIdFromClass(clazzIn);
        this.configPath = SpongeImpl.getSpongeConfigDir().resolve("worlds").resolve(this.modId).resolve(this.enumName);
        this.config = new SpongeConfig<>(SpongeConfig.Type.DIMENSION, this.configPath.resolve("dimension.conf"), SpongeImpl.ECOSYSTEM_ID, SpongeImpl.getGlobalConfigAdapter(), false);
        this.generateSpawnOnLoad = idIn == 0;
        this.loadSpawn = this.generateSpawnOnLoad;
        this.config.getConfig().getWorld().setGenerateSpawnOnLoad(this.generateSpawnOnLoad);
        this.sanitizedId = this.modId + ":" + dimName;
        final String contextId = this.sanitizedId.replace(":", ".");
        this.context = new Context(Context.DIMENSION_KEY, contextId);
    }

    @Override
    public boolean bridge$shouldGenerateSpawnOnLoad() {
        return this.generateSpawnOnLoad;
    }

    @Override
    public boolean bridge$shouldLoadSpawn() {
        return this.loadSpawn;
    }

    @Override
    public void bridge$setShouldLoadSpawn(boolean keepSpawnLoaded) {
        this.loadSpawn = keepSpawnLoaded;
    }

    @Override
    public String bridge$getEnumName() {
        return this.enumName;
    }

    @Override
    public String bridge$getModId() {
        return this.modId;
    }

    @Override
    public Path bridge$getConfigPath() {
        return this.configPath;
    }

    @Override
    public SpongeConfig<DimensionConfig> bridge$getDimensionConfig() {
        return this.config;
    }

    @Override
    public Context bridge$getContext() {
        return this.context;
    }

    @Override
    public String bridge$getProperId() {
        return this.sanitizedId;
    }
}
