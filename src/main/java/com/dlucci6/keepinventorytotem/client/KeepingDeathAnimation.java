package com.dlucci6.keepinventorytotem.client;

import com.dlucci6.keepinventorytotem.KeepInventoryTotem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.renderer.RenderType;
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
    private static final int SWOOP_END = 22;
    private static final int SETTLE_END = 32;
    private static final int CRACK_END = 52;
    private static final int PORTAL_END = 70;
    private static final int ANIMATION_END = 90;
    private static final float[] FRACTURE_ANGLES = {
            -168.0F, -132.0F, -101.0F, -67.0F, -31.0F, 7.0F, 43.0F, 79.0F, 116.0F, 151.0F
    };
    private static final float[] FRACTURE_LENGTHS = {
            0.78F, 1.0F, 0.72F, 0.91F, 0.66F, 0.96F, 0.75F, 0.88F, 0.70F, 0.84F
    };
    private static final float[] APERTURE_JITTER = {
            0.72F, 0.90F, 0.83F, 1.00F, 0.92F, 1.04F, 0.95F, 1.01F, 0.86F, 0.93F, 0.76F
    };

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
        if (animationTick == SETTLE_END) {
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
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!active) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        float tick = animationTick + event.getPartialTick().getGameTimeDeltaTicks();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        graphics.fill(0, 0, width, height, 0x66000000);

        if (tick >= SETTLE_END + 6) {
            renderPortalCracks(graphics, tick, width, height);
        }

        if (tick >= CRACK_END) {
            renderCrackingPortal(graphics, tick, width, height);
        }

        if (tick < CRACK_END + 6) {
            renderTotem(graphics, tick, width, height);
        }

        if (tick >= PORTAL_END) {
            float fade = ease((tick - PORTAL_END) / (ANIMATION_END - PORTAL_END));
            graphics.fill(0, 0, width, height, ((int)(fade * 225.0F) << 24) | 0x09000F);
        }

        // The animation was rendered first; cancel the normal HUD so no hotbar,
        // crosshair, status effects, or other vanilla UI can bleed through it.
        event.setCanceled(true);
    }

    private static void renderTotem(GuiGraphics graphics, float tick, int width, int height) {
        float swoopTime = Mth.clamp(tick / SWOOP_END, 0.0F, 1.0F);
        float swoop = easeOutBack(swoopTime);
        float startX = -36.0F;
        float startY = height + 32.0F;
        float centerX = width / 2.0F;
        float centerY = height / 2.0F;
        float x = Mth.lerp(swoop, startX, centerX);
        float y = Mth.lerp(swoop, startY, centerY)
                - Mth.sin(swoopTime * Mth.PI) * height * 0.32F;
        float scale = Mth.lerp(ease(swoopTime), 1.0F, 4.0F);
        float rotation = Mth.lerp(swoop, -155.0F, 0.0F);

        if (tick >= SWOOP_END && tick < SETTLE_END) {
            float settle = (tick - SWOOP_END) / (SETTLE_END - SWOOP_END);
            float damping = 1.0F - settle;
            x = centerX + Mth.sin(settle * Mth.TWO_PI) * 7.0F * damping;
            y = centerY + Mth.sin(settle * Mth.TWO_PI + 1.2F) * 4.0F * damping;
            rotation = Mth.sin(settle * Mth.TWO_PI * 1.5F) * 16.0F * damping;
            scale = 4.0F + Mth.sin(settle * Mth.PI) * 0.25F;
        } else if (tick >= SETTLE_END) {
            x = centerX;
            y = centerY;
            rotation = 0.0F;
            scale = 4.0F;
        }

        if (tick >= SETTLE_END) {
            float crackProgress = Mth.clamp((tick - SETTLE_END) / (CRACK_END - SETTLE_END), 0.0F, 1.0F);
            x += Mth.sin(tick * 2.7F) * crackProgress * 2.5F;
            y += Mth.cos(tick * 2.1F) * crackProgress * 1.5F;
            scale *= 1.0F + crackProgress * 0.12F;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 300.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));
        pose.scale(scale, scale, 1.0F);
        graphics.renderItem(new ItemStack(KeepInventoryTotem.KEEP_INVENTORY_TOTEM.get()), -8, -8);
        pose.popPose();

        if (tick >= SETTLE_END) {
            renderCracks(graphics, tick, (int)x, (int)y);
        }
    }

    private static void renderCracks(GuiGraphics graphics, float tick, int centerX, int centerY) {
        float progress = Mth.clamp((tick - SETTLE_END) / (CRACK_END - SETTLE_END), 0.0F, 1.0F);
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

    private static void renderPortalCracks(GuiGraphics graphics, float tick, int width, int height) {
        float progress = ease(Mth.clamp(
                (tick - (SETTLE_END + 6.0F)) / (CRACK_END - SETTLE_END - 6.0F),
                0.0F,
                1.0F
        ));
        float baseLength = Math.min(width, height) * 0.34F * progress;
        float centerX = width / 2.0F;
        float centerY = height / 2.0F;

        for (int index = 0; index < FRACTURE_ANGLES.length; index++) {
            float length = baseLength * FRACTURE_LENGTHS[index];
            drawPortalFissure(graphics, centerX, centerY, FRACTURE_ANGLES[index], length, progress);

            if (progress > 0.42F && index % 2 == 0) {
                float branchStart = length * 0.55F;
                float branchLength = length * 0.32F * ((progress - 0.42F) / 0.58F);
                drawPortalFissure(
                        graphics,
                        centerX + Mth.cos(FRACTURE_ANGLES[index] * Mth.DEG_TO_RAD) * branchStart,
                        centerY + Mth.sin(FRACTURE_ANGLES[index] * Mth.DEG_TO_RAD) * branchStart,
                        FRACTURE_ANGLES[index] + (index % 4 == 0 ? 38.0F : -35.0F),
                        branchLength,
                        progress
                );
            }
        }
    }

    private static void drawPortalFissure(
            GuiGraphics graphics,
            float startX,
            float startY,
            float angle,
            float length,
            float progress
    ) {
        if (length < 1.0F) {
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(startX, startY, 190.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));

        int visibleLength = Math.max(1, (int)length);
        int glowWidth = progress > 0.7F ? 5 : 3;
        graphics.fill(-2, -glowWidth, visibleLength + 2, glowWidth, 0xFF4C0870);
        graphics.fillRenderType(RenderType.endPortal(), 0, -1, visibleLength, 2, 0);
        pose.popPose();
    }

    private static void renderCrackingPortal(GuiGraphics graphics, float tick, int width, int height) {
        float openingTime = Mth.clamp((tick - CRACK_END) / (PORTAL_END - CRACK_END), 0.0F, 1.0F);
        float opening = 1.0F - (float)Math.pow(1.0F - openingTime, 4.0);
        float cameraFlight = ease(Mth.clamp((tick - PORTAL_END) / (ANIMATION_END - PORTAL_END), 0.0F, 1.0F));
        int initialSize = 4;
        int openSize = (int)(Math.min(width, height) * 1.18F);
        int finalSize = (int)(Math.max(width, height) * 3.0F);
        int size = (int)Mth.lerp(cameraFlight, Mth.lerp(opening, initialSize, openSize), finalSize);
        int centerX = width / 2;
        int centerY = height / 2;
        int bandHeight = Math.max(2, size / APERTURE_JITTER.length);
        int top = centerY - (bandHeight * APERTURE_JITTER.length) / 2;

        // Horizontal bands form an irregular, fractured aperture. Their uneven
        // edges race outward from the center and merge into the camera fly-through.
        for (int band = 0; band < APERTURE_JITTER.length; band++) {
            float vertical = ((band + 0.5F) / APERTURE_JITTER.length) * 2.0F - 1.0F;
            float profile = Mth.sqrt(Math.max(0.0F, 1.0F - vertical * vertical));
            int halfWidth = Math.max(1, (int)(size * 0.52F * profile * APERTURE_JITTER[band]));
            int y0 = top + band * bandHeight;
            int y1 = y0 + bandHeight + 1;
            int edge = Math.max(2, size / 90);

            graphics.fill(centerX - halfWidth - edge, y0 - edge, centerX + halfWidth + edge, y1 + edge, 0xFF31004D);
            graphics.fillRenderType(RenderType.endPortal(), centerX - halfWidth, y0, centerX + halfWidth, y1, 0);
        }
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

    private static float easeOutBack(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        float overshoot = 1.70158F;
        float shifted = clamped - 1.0F;
        return 1.0F + (overshoot + 1.0F) * shifted * shifted * shifted
                + overshoot * shifted * shifted;
    }

    private static void reset() {
        hadTotem = false;
        active = false;
        awaitingRespawn = false;
        animationTick = 0;
    }
}
