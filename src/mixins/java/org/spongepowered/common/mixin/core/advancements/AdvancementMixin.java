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
package org.spongepowered.common.mixin.core.advancements;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.api.advancement.criteria.AdvancementCriterion;
import org.spongepowered.api.advancement.criteria.AndCriterion;
import org.spongepowered.api.advancement.criteria.OrCriterion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.advancement.criterion.DefaultedAdvancementCriterion;
import org.spongepowered.common.advancement.criterion.SpongeScoreCriterion;
import org.spongepowered.common.advancement.criterion.SpongeScoreTrigger;
import org.spongepowered.common.adventure.SpongeAdventure;
import org.spongepowered.common.bridge.advancements.AdvancementBridge;
import org.spongepowered.common.bridge.advancements.CriterionBridge;
import org.spongepowered.common.bridge.advancements.DisplayInfoBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

@Mixin(Advancement.class)
public abstract class AdvancementMixin implements AdvancementBridge {

    @Shadow @Final @Mutable @Nullable private Advancement parent;
    @Shadow @Final @Mutable private String[][] requirements;
    @Shadow @Final @Mutable private Map<String, Criterion> criteria;
    @Shadow @Final @Nullable private DisplayInfo display;
    @Shadow @Final private ResourceLocation id;

    @Shadow @Final private ITextComponent displayText;
    @Shadow @Final private AdvancementRewards rewards;
    private AdvancementCriterion impl$criterion;
    private List<Component> impl$toastText;

    @SuppressWarnings({"ConstantConditions"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void impl$setUpSpongeFields(ResourceLocation location, @Nullable Advancement parent, @Nullable DisplayInfo displayInfo,
            AdvancementRewards rewards, Map<String, Criterion> criteria, String[][] requirements, CallbackInfo ci) {
        // Don't do anything on the client, unless we're performing registry initialization
        if (!SpongeImplHooks.onServerThread()) {
            return;
        }
        if (displayInfo != null) {
            ((DisplayInfoBridge) displayInfo).bridge$setAdvancement((org.spongepowered.api.advancement.Advancement) this);
        }

        this.impl$toastText = this.impl$generateToastText();

        final Map<String, DefaultedAdvancementCriterion> criteriaMap = new LinkedHashMap<>();
        final Map<String, List<DefaultedAdvancementCriterion>> scoreCriteria = new HashMap<>();
        for (Map.Entry<String, Criterion> entry : criteria.entrySet()) {
            final CriterionBridge mixinCriterion = (CriterionBridge) entry.getValue();
            final String groupName = mixinCriterion.bridge$getScoreCriterionName();
            if (groupName != null) {
                scoreCriteria.computeIfAbsent(groupName, k -> new ArrayList<>()).add((DefaultedAdvancementCriterion) entry.getValue());
            }

            criteriaMap.put(entry.getKey(), (DefaultedAdvancementCriterion) mixinCriterion);
            mixinCriterion.bridge$setName(entry.getKey());
        }
        for (Map.Entry<String, List<DefaultedAdvancementCriterion>> groupEntry : scoreCriteria.entrySet()) {
            criteriaMap.put(groupEntry.getKey(), new SpongeScoreCriterion(groupEntry.getKey(), groupEntry.getValue()));
            groupEntry.getValue().forEach(c -> criteriaMap.remove(c.getName()));
        }

        final Set<AdvancementCriterion> andCriteria = new HashSet<>();
        for (final String[] array : requirements) {
            final Set<AdvancementCriterion> orCriteria = new HashSet<>();
            for (final String name : array) {
                DefaultedAdvancementCriterion criterion = criteriaMap.get(name);
                if (criterion == null && criteria.get(name) != null) { // internal removed by scoreCriterion
                    criterion = criteriaMap.get(((CriterionBridge) criteria.get(name)).bridge$getScoreCriterionName());
                }
                orCriteria.add(criterion);
            }
            andCriteria.add(OrCriterion.of(orCriteria));
        }
        this.impl$criterion = AndCriterion.of(andCriteria);
    }

    private ImmutableList<Component> impl$generateToastText() {
        final ImmutableList.Builder<Component> toastText = ImmutableList.builder();
        if (this.display != null) {
            final FrameType frameType = this.display.getFrame();
            toastText.add(Component.translatable("advancements.toast." + frameType.getName(), SpongeAdventure.asAdventureNamed(frameType.getFormat())));
            toastText.add(SpongeAdventure.asAdventure(this.display.getTitle()));
        } else {

            toastText.add(Component.text("Unlocked advancement"));
            toastText.add(Component.text(this.id.toString()));
        }
        return toastText.build();
    }

    @Override
    public Optional<Advancement> bridge$getParent() {
        checkState(SpongeImplHooks.onServerThread());
        return Optional.ofNullable(this.parent);
    }

    @Override
    public void bridge$setParent(@Nullable final Advancement advancement) {
        checkState(SpongeImplHooks.onServerThread());
        this.parent = advancement;
    }

    @Override
    public AdvancementCriterion bridge$getCriterion() {
        checkState(SpongeImplHooks.onServerThread());
        return this.impl$criterion;
    }

    @Override
    public void bridge$setCriterion(final AdvancementCriterion criterion) {
        checkState(SpongeImplHooks.onServerThread());
        this.impl$criterion = criterion;
    }

    @Override
    public List<Component> bridge$getToastText() {
        checkState(SpongeImplHooks.onServerThread());
        return this.impl$toastText;
    }

    /**
     * @author faithcaio - 2020-10-01
     * @reason Fix vanilla deserializing empty rewards as null
     *
     * @return the fixed AdvancementRewards
     */
    @Overwrite
    public AdvancementRewards getRewards() {
        return this.rewards == null ? AdvancementRewards.EMPTY : this.rewards;
    }
}