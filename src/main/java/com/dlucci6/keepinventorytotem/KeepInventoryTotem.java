package com.dlucci6.keepinventorytotem;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(KeepInventoryTotem.MOD_ID)
public final class KeepInventoryTotem {
    public static final String MOD_ID = "keepinventorytotem";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<Item> KEEP_INVENTORY_TOTEM = ITEMS.registerSimpleItem(
            "keep_inventory_totem",
            new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)
    );

    public KeepInventoryTotem(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(KeepInventoryTotem::addCreativeTabItems);
        NeoForge.EVENT_BUS.register(KeepInventoryHandler.class);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(KEEP_INVENTORY_TOTEM);
        }
    }
}
