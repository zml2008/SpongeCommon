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
package org.spongepowered.common.mixin.core.entity.passive;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.village.MerchantRecipeList;
import org.spongepowered.api.data.type.Career;
import org.spongepowered.api.data.type.Profession;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.item.merchant.TradeOffer;
import org.spongepowered.api.item.merchant.TradeOfferGenerator;
import org.spongepowered.api.item.merchant.VillagerRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.bridge.entity.EntityVillagerBridge;
import org.spongepowered.common.bridge.inventory.TrackedInventoryBridge;
import org.spongepowered.common.entity.SpongeCareer;
import org.spongepowered.common.entity.SpongeEntityMeta;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.item.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.item.inventory.adapter.impl.MinecraftInventoryAdapter;
import org.spongepowered.common.item.inventory.lens.Fabric;
import org.spongepowered.common.item.inventory.lens.Lens;
import org.spongepowered.common.item.inventory.lens.SlotProvider;
import org.spongepowered.common.item.inventory.lens.impl.collections.SlotCollection;
import org.spongepowered.common.item.inventory.lens.impl.comp.OrderedInventoryLensImpl;
import org.spongepowered.common.item.inventory.lens.impl.fabric.IInventoryFabric;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.mixin.core.entity.MixinEntityAgeable;
import org.spongepowered.common.registry.SpongeVillagerRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.annotation.Nullable;

@SuppressWarnings("rawtypes")
@Mixin(EntityVillager.class)
public abstract class MixinEntityVillager extends MixinEntityAgeable implements EntityVillagerBridge, MinecraftInventoryAdapter {

    @Shadow private boolean isPlaying; // isPlaying
    @Shadow private int careerId; // careerId
    @Shadow private int careerLevel; // careerLevel
    @Shadow @Nullable public MerchantRecipeList buyingList; // buyingList
    @Shadow @Final private InventoryBasic villagerInventory; // villagerInventory

    @Shadow public abstract void setProfession(int professionId); // setProfession
    @Shadow public abstract MerchantRecipeList getRecipes(EntityPlayer player);
    @Shadow private boolean canVillagerPickupItem(final Item itemIn) {
        throw new UnsupportedOperationException("Shadowed");
    }

    private Fabric fabric = new IInventoryFabric(this.villagerInventory);
    private SlotCollection slots = new SlotCollection.Builder().add(8).build();
    private Lens lens = new OrderedInventoryLensImpl(0, 8, 1, this.slots);

    @Nullable private Profession profession;

    @Inject(method = "setProfession(I)V", at = @At("RETURN"))
    private void onSetProfession(final int professionId, final CallbackInfo ci) {
        this.profession = SpongeImplHooks.validateProfession(professionId);
    }


    @Override
    public SlotProvider getSlotProvider() {
        return this.slots;
    }

    @Override
    public Lens getRootLens() {
        return this.lens;
    }

    @Override
    public Fabric getFabric() {
        return this.fabric;
    }

    @Override
    public Career bridge$getCareer() {
        final List<Career> careers = (List<Career>) this.profession.getCareers();
        if (this.careerId == 0 || this.careerId > careers.size()) {
            this.careerId = new Random().nextInt(careers.size()) + 1;
        }
        this.getRecipes(null);
        return careers.get(this.careerId - 1);
    }

    @Override
    public Optional<Profession> bridge$getProfessionOptional() {
        return Optional.ofNullable(this.profession);
    }

    @Nullable
    @Override
    public Profession bridge$getProfession() {
        return this.profession;
    }

    @Override
    public void bridge$setProfession(final Profession profession) {
        this.profession = checkNotNull(profession, "VillagerProfession cannot be null!");
    }

    @Override
    public void bridge$setCareer(final Career career) {
        setProfession(((SpongeEntityMeta) career.getProfession()).type);
        this.buyingList = null;
        this.careerId = ((SpongeCareer) career).type + 1;
        this.careerLevel = 1;
        this.getRecipes(null);
    }

    /**
     * @author gabizou - January 13th, 2016
     * @reason This overwrites the current method using the multi-dimension array with
     * our {@link VillagerRegistry} to handle career levels and registrations
     * for {@link TradeOfferGenerator}s. Note that this takes over entirely
     * whatever vanilla does, but this allows for maximum customization for
     * plugins to handle gracefully.
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public void populateBuyingList() { // populateBuyingList
        // Sponge
        final List<Career> careers = (List<Career>) this.profession.getCareers();

        // EntityVillager.ITradeList[][][] aentityvillager$itradelist = DEFAULT_TRADE_LIST_MAP[this.getProfession()];

        if (this.careerId != 0 && this.careerLevel != 0) {
            ++this.careerLevel;
        } else {
            // Sponge change aentityvillager$itradelist to use this.profession.getCareers()
            this.careerId = this.rand.nextInt(careers.size()) + 1;
            this.careerLevel = 1;
        }

        if (this.buyingList == null) {
            this.buyingList = new MerchantRecipeList();
        }

        // Sponge start - use our own registry stuffs
        checkState(this.careerId <= careers.size(), "The villager career id is out of bounds fo the available Careers! Found: " + this.careerId
                                                    + " when the current maximum is: " + careers.size());
        final Career careerLevel = careers.get(this.careerId - 1);
        SpongeVillagerRegistry.getInstance().populateOffers((Villager) this, (List<TradeOffer>) (List<?>) this.buyingList, careerLevel, this.careerLevel, this.rand);
        // Sponge end
    }


    @Redirect(method = "updateEquipmentIfNeeded",  at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/EntityVillager;canVillagerPickupItem(Lnet/minecraft/item/Item;)Z"))
    private boolean onCanVillagerPickUpItem(final EntityVillager villager, final Item item, final EntityItem itemEntity) {
        final boolean result = this.canVillagerPickupItem(item);
        if (!SpongeCommonEventFactory.callChangeInventoryPickupPreEvent(((EntityLiving)(Object) this), itemEntity)) {
            return false;
        }
        return result;
    }

    private InventoryBasic inventoryTracker;

    @Redirect(method = "updateEquipmentIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/InventoryBasic;addItem(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack onSetItemStackToSlot(final InventoryBasic inventory, final net.minecraft.item.ItemStack stack) {
        final int prev = stack.getCount();

        if (this.inventoryTracker == null) {
             this.inventoryTracker = new InventoryBasic(this.villagerInventory.getName(), this.villagerInventory.hasCustomName(), this.villagerInventory.getSizeInventory());
        }

        // Prepare tracker inventory
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack1 = inventory.getStackInSlot(i);
            this.inventoryTracker.setInventorySlotContents(i, stack1.copy());
        }

        // Modify inventory
        final ItemStack result = inventory.addItem(stack);

        // Compare with tracker inventory
        List<SlotTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stackNew = inventory.getStackInSlot(i);
            ItemStack stackOld = this.inventoryTracker.getStackInSlot(i);
            if (!ItemStack.areItemStacksEqual(stackNew, stackOld)) {
                Slot slot = ((InventoryAdapter) inventory).getSlot(i).get();
                transactions.add(new SlotTransaction(slot, ItemStackUtil.snapshotOf(stackOld), ItemStackUtil.snapshotOf(stackNew)));
            }
        }

        if (!SpongeCommonEventFactory.callChangeInventoryPickupEvent(((EntityLiving) (Object) this), this, transactions)) {
            stack.setCount(prev);
            return stack;
        }
        return result;
    }

}
