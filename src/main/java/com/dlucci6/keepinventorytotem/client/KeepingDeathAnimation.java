package com.dlucci6.keepinventorytotem.client;

import com.dlucci6.keepinventorytotem.KeepInventoryTotem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = KeepInventoryTotem.MOD_ID, value = Dist.CLIENT)
public final class KeepingDeathAnimation {
    private static final ResourceLocation END_PORTAL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");
    private static final int FLY_END = 20;
    private static final int CRACK_END = 42;
    private static final int PORTAL_END = 72;
    private static final int ANIMATION_END = 92;

    private static boolean hadTotem;
    private static boolean active;
    private static boolean awaitingRespawn;
    private static int animationTick;

    private KeepingDeathAnimation() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }

        if (!minecraft.player.isDeadOrDying()) {
            hadTotem = hasTotem(minecraft.player.getInventory());
            awaitingRespawn = false;
            return;
        }

        if (!active) {
            return;
        }

        animationTick++;
        if (animationTick == FLY_END) {
            minecraft.player.playSound(SoundEvents.ITEM_BREAK, 1.0F, 0.65F);
        } else if (animationTick == CRACK_END) {
            minecraft.player.playSound(SoundEvents.END_PORTAL_SPAWN, 0.8F, 1.25F);
        } else if (animationTick >= ANIMATION_END) {
            active = false;
            awaitingRespawn = true;
            minecraft.player.respawn();
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof DeathScreen) || (!hadTotem && !active && !awaitingRespawn)) {
            return;
        }

        if (!active && !awaitingRespawn) {
            active = true;
            animationTick = 0;
        }

        // A null replacement closes any previous UI while preventing the death screen.
        event.setNewScreen(null);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!active) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        float tick = animationTick + event.getPartialTick().getGameTimeDeltaTicks();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        graphics.fill(0, 0, width, height, 0x66000000);

        if (tick >= FLY_END) {
            renderPortal(graphics, tick, width, height);
        }

        if (tick < CRACK_END + 7) {
            renderTotem(graphics, tick, width, height);
        }

        if (tick >= PORTAL_END) {
            float fade = ease((tick - PORTAL_END) / (ANIMATION_END - PORTAL_END));
            graphics.fill(0, 0, width, height, ((int)(fade * 225.0F) << 24) | 0x09000F);
        }
    }

    private static void renderTotem(GuiGraphics graphics, float tick, int width, int height) {
        float fly = ease(Mth.clamp(tick / FLY_END, 0.0F, 1.0F));
        float startX = width - 28.0F;
        float startY = height - 28.0F;
        float centerX = width / 2.0F;
        float centerY = height / 2.0F;
        float x = Mth.lerp(fly, startX, centerX);
        float y = Mth.lerp(fly, startY, centerY);
        float scale = Mth.lerp(fly, 1.0F, 4.0F);

        if (tick >= FLY_END) {
            float crackProgress = Mth.clamp((tick - FLY_END) / (CRACK_END - FLY_END), 0.0F, 1.0F);
            x += Mth.sin(tick * 2.7F) * crackProgress * 2.5F;
            y += Mth.cos(tick * 2.1F) * crackProgress * 1.5F;
            scale *= 1.0F + crackProgress * 0.12F;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 300.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.renderItem(new ItemStack(KeepInventoryTotem.KEEP_INVENTORY_TOTEM.get()), -8, -8);
        pose.popPose();

        if (tick >= FLY_END) {
            renderCracks(graphics, tick, (int)x, (int)y);
        }
    }

    private static void renderCracks(GuiGraphics graphics, float tick, int centerX, int centerY) {
        float progress = Mth.clamp((tick - FLY_END) / (CRACK_END - FLY_END), 0.0F, 1.0F);
        int color = 0xFF2B063D;
        int length = (int)(28.0F * progress);

        graphics.fill(centerX - 1, centerY - length, centerX + 2, centerY + length, color);
        if (progress > 0.25F) {
            graphics.fill(centerX - length, centerY - 10, centerX, centerY - 7, color);
            graphics.fill(centerX, centerY + 8, centerX + length, centerY + 11, color);
        }
        if (progress > 0.55F) {
            graphics.fill(centerX - 18, centerY - 19, centerX - 15, centerY - 7, color);
            graphics.fill(centerX + 14, centerY + 9, centerX + 17, centerY + 23, color);
        }
    }

    private static void renderPortal(GuiGraphics graphics, float tick, int width, int height) {
        float opening = ease(Mth.clamp((tick - FLY_END) / (PORTAL_END - FLY_END), 0.0F, 1.0F));
        float cameraFlight = ease(Mth.clamp((tick - PORTAL_END) / (ANIMATION_END - PORTAL_END), 0.0F, 1.0F));
        int initialSize = 8;
        int openSize = (int)(Math.min(width, height) * 0.82F);
        int finalSize = (int)(Math.max(width, height) * 2.2F);
        int size = (int)Mth.lerp(cameraFlight, Mth.lerp(opening, initialSize, openSize), finalSize);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        int border = Math.max(3, size / 45);

        graphics.fill(x - border, y - border, x + size + border, y + size + border, 0xFF210036);
        graphics.blit(END_PORTAL_TEXTURE, x, y, size, size, 0.0F, 0.0F, 16, 16, 16, 16);
    }

    private static boolean hasTotem(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(KeepInventoryTotem.KEEP_INVENTORY_TOTEM.get())) {
                return true;
            }
        }
        return false;
    }

    private static float ease(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static void reset() {
        hadTotem = false;
        active = false;
        awaitingRespawn = false;
        animationTick = 0;
    }
}
