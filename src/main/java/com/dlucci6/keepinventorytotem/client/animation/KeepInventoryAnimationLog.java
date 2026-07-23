package com.dlucci6.keepinventorytotem.client.animation;

import com.dlucci6.keepinventorytotem.KeepInventoryTotem;
import net.minecraft.resources.ResourceLocation;

final class KeepInventoryAnimationLog {
    private KeepInventoryAnimationLog() {
    }

    static void error(ResourceLocation file, Exception exception) {
        KeepInventoryTotem.LOGGER.error("Could not load camera animation {}", file, exception);
    }
}
