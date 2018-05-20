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
package org.spongepowered.test;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.PortalAgent;
import org.spongepowered.api.world.PortalAgentType;
import org.spongepowered.api.world.PortalAgentTypes;
import org.spongepowered.api.world.World;

import java.util.Optional;

/**
 * Bedrock in hoppers prevents them from working
 */
@Plugin(id = "portalagenttest", name = "Portal Agent Test", description = "A plugin to test custom portal agents")
public class PortalAgentTest {

    private final TransferListener listener = new TransferListener();
    private boolean registered = false;

    @Listener
    public void onInit(GameInitializationEvent event) {
        Sponge.getCommandManager().register(this,
                CommandSpec.builder().executor((source, context) -> {
                    if (this.registered) {
                        this.registered = false;
                        Sponge.getEventManager().unregisterListeners(this.listener);
                    } else {
                        this.registered = true;
                        Sponge.getEventManager().registerListeners(this, this.listener);
                    }
                    return CommandResult.success();
                }).build(), "toggleportalagenttest");
    }

    public static class TransferListener {

        @Listener
        public void onPortal(MoveEntityEvent.Teleport.Portal event) {
            System.out.println(event.getPortalAgent().getClass().getName() + " enabled: " + event.getUsePortalAgent());

            // TODO Doesn't change anything
            //event.setPortalAgent(new CustomPortalAgent());

            // TODO Works for the next teleport from this world
            event.getPortalAgent().setCreationRadius(1);
            event.getPortalAgent().setSearchRadius(1);

            // TODO Only changes the final location but not the portal creation
            event.setToTransform(event.getToTransform().setPosition(new Vector3d(1000, 4, 1000)));
        }
    }

    public static class CustomPortalAgent implements PortalAgent
    {

        @Override
        public int getSearchRadius() {
            return 1;
        }

        @Override
        public PortalAgent setSearchRadius(int radius) {
            return this;
        }

        @Override
        public int getCreationRadius() {
            return 1;
        }

        @Override
        public PortalAgent setCreationRadius(int radius) {
            return this;
        }

        @Override
        public Optional<Location<World>> findOrCreatePortal(Location<World> targetLocation) {
            Optional<Location<World>> found = findPortal(targetLocation);
            if (!found.isPresent()) {
                Optional<Location<World>> created = this.createPortal(targetLocation);
                if (created.isPresent()) {
                    return created;
                }
            }
            return found;
        }

        @Override
        public Optional<Location<World>> findPortal(Location<World> targetLocation) {
            return Optional.empty();
        }

        @Override
        public Optional<Location<World>> createPortal(Location<World> targetLocation) {
            targetLocation.getRelative(Direction.DOWN).setBlock(BlockTypes.DIAMOND_BLOCK.getDefaultState());
            targetLocation.setBlock(BlockTypes.AIR.getDefaultState());
            targetLocation.getRelative(Direction.UP).setBlock(BlockTypes.AIR.getDefaultState());
            targetLocation.getRelative(Direction.UP).getRelative(Direction.UP).setBlock(BlockTypes.SEA_LANTERN.getDefaultState());
            return Optional.of(targetLocation);
        }

        @Override
        public PortalAgentType getType() {
            return PortalAgentTypes.DEFAULT;
        }
    }
}
