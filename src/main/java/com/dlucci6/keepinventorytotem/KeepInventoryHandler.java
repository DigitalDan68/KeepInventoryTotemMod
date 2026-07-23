package com.dlucci6.keepinventorytotem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class KeepInventoryHandler {
    private static final Map<UUID, InventorySnapshot> SAVED_INVENTORIES = new HashMap<>();

    private KeepInventoryHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.isSpectator()
                || SAVED_INVENTORIES.containsKey(player.getUUID())) {
            return;
        }

        Inventory inventory = player.getInventory();
        InventorySnapshot snapshot = InventorySnapshot.captureAndConsumeOne(inventory);
        if (snapshot == null) {
            return;
        }

        SAVED_INVENTORIES.put(player.getUUID(), snapshot);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0, false, false, false));
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && SAVED_INVENTORIES.containsKey(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        InventorySnapshot snapshot = SAVED_INVENTORIES.remove(newPlayer.getUUID());
        if (snapshot != null) {
            snapshot.restore(newPlayer.getInventory());
        }
    }

    private record InventorySnapshot(ItemStack[] items) {
        private static InventorySnapshot captureAndConsumeOne(Inventory inventory) {
            ItemStack[] items = new ItemStack[inventory.getContainerSize()];
            boolean consumed = false;

            for (int slot = 0; slot < items.length; slot++) {
                items[slot] = inventory.getItem(slot).copy();
                if (!consumed && items[slot].is(KeepInventoryTotem.KEEP_INVENTORY_TOTEM.get())) {
                    items[slot].shrink(1);
                    consumed = true;
                }
            }

            return consumed ? new InventorySnapshot(items) : null;
        }

        private void restore(Inventory inventory) {
            inventory.clearContent();
            for (int slot = 0; slot < items.length; slot++) {
                inventory.setItem(slot, items[slot].copy());
            }
            inventory.setChanged();
        }
    }
}
