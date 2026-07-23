package com.dlucci6.keepinventorytotem.client;

import com.dlucci6.keepinventorytotem.KeepInventoryTotem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = KeepInventoryTotem.MOD_ID, value = Dist.CLIENT)
public final class KeepingDeathAnimation {
    // Vanilla's item activation lasts 40 ticks. The portal now completes within
    // that same window so respawn happens as the totem leaves the screen.
    private static final int FISSURE_START = 6;
    private static final int CRACK_END = 20;
    private static final int PORTAL_END = 31;
    private static final int ANIMATION_END = 40;
    private static final int WAKE_ANIMATION_END = 36;
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
    private static boolean wakeActive;
    private static int wakeTick;
    private static float activationOffsetX;
    private static float activationOffsetY;

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
            if (awaitingRespawn) {
                awaitingRespawn = false;
                if (isNextToBed(minecraft)) {
                    wakeActive = true;
                    wakeTick = 0;
                }
            }
            if (wakeActive && ++wakeTick >= WAKE_ANIMATION_END) {
                wakeActive = false;
            }
            return;
        }

        if (!active) {
            return;
        }

        animationTick++;
        if (animationTick == CRACK_END) {
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
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                activationOffsetX = minecraft.player.getRandom().nextFloat() * 2.0F - 1.0F;
                activationOffsetY = minecraft.player.getRandom().nextFloat() * 2.0F - 1.0F;
                minecraft.player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
            }
        }

        // A null replacement closes any previous UI while preventing the death screen.
        event.setNewScreen(null);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (wakeActive) {
            renderWakeOverlay(event);
            return;
        }

        if (!active) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        float tick = animationTick + event.getPartialTick().getGameTimeDeltaTicks();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        graphics.fill(0, 0, width, height, 0x66000000);

        if (tick >= FISSURE_START) {
            renderPortalCracks(graphics, tick, width, height);
        }

        if (tick >= CRACK_END) {
            renderCrackingPortal(graphics, tick, width, height);
        }

        if (tick >= PORTAL_END) {
            float fade = ease((tick - PORTAL_END) / (ANIMATION_END - PORTAL_END));
            graphics.fill(0, 0, width, height, ((int)(fade * 225.0F) << 24) | 0x09000F);
        }

        // Vanilla normally draws item activation before the HUD. Replaying its
        // exact transform here preserves the animation while compositing it last.
        renderVanillaTotemActivation(graphics, tick);

        // The animation was rendered first; cancel the normal HUD so no hotbar,
        // crosshair, status effects, or other vanilla UI can bleed through it.
        event.setCanceled(true);
    }

    private static void renderVanillaTotemActivation(GuiGraphics graphics, float tick) {
        Minecraft minecraft = Minecraft.getInstance();
        float progress = Mth.clamp(tick / 40.0F, 0.0F, 1.0F);
        float squared = progress * progress;
        float cubed = progress * squared;
        float curve = 10.25F * cubed * squared
                - 24.95F * squared * squared
                + 25.5F * cubed
                - 13.8F * squared
                + 4.0F * progress;
        float angle = curve * Mth.PI;
        float offsetX = activationOffsetX * (graphics.guiWidth() / 4.0F);
        float offsetY = activationOffsetY * (graphics.guiHeight() / 4.0F);

        PoseStack itemPose = new PoseStack();
        itemPose.pushPose();
        itemPose.translate(
                graphics.guiWidth() / 2.0F + offsetX * Mth.abs(Mth.sin(angle * 2.0F)),
                graphics.guiHeight() / 2.0F + offsetY * Mth.abs(Mth.sin(angle * 2.0F)),
                -50.0F
        );
        float scale = 50.0F + 175.0F * Mth.sin(angle);
        itemPose.scale(scale, -scale, scale);
        itemPose.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(angle))));
        itemPose.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(progress * 8.0F)));
        itemPose.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(progress * 8.0F)));

        graphics.drawManaged(() -> minecraft.getItemRenderer().renderStatic(
                new ItemStack(KeepInventoryTotem.KEEP_INVENTORY_TOTEM.get()),
                ItemDisplayContext.FIXED,
                15728880,
                OverlayTexture.NO_OVERLAY,
                itemPose,
                graphics.bufferSource(),
                minecraft.level,
                0
        ));
        itemPose.popPose();
    }

    private static void renderPortalCracks(GuiGraphics graphics, float tick, int width, int height) {
        float overallProgress = Mth.clamp(
                (tick - FISSURE_START) / (CRACK_END - FISSURE_START),
                0.0F,
                1.0F
        );
        float centerX = width / 2.0F;
        float centerY = height / 2.0F;

        for (int index = 0; index < FRACTURE_ANGLES.length; index++) {
            float progress = jerkyProgress(overallProgress, index);
            float length = Math.min(width, height) * 0.34F * FRACTURE_LENGTHS[index] * progress;
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
        // Portal energy is deliberately wider and rendered after the outline,
        // making it look as though the fissure has torn through its own edge.
        graphics.fillRenderType(RenderType.endPortal(), 0, -glowWidth - 1, visibleLength, glowWidth + 1, 0);
        pose.popPose();
    }

    private static void renderCrackingPortal(GuiGraphics graphics, float tick, int width, int height) {
        float openingTime = Mth.clamp((tick - CRACK_END) / (PORTAL_END - CRACK_END), 0.0F, 1.0F);
        float cameraFlight = ease(Mth.clamp((tick - PORTAL_END) / (ANIMATION_END - PORTAL_END), 0.0F, 1.0F));
        int centerX = width / 2;
        int centerY = height / 2;
        float openReach = Math.min(width, height) * 0.78F;
        float finalReach = Math.max(width, height) * 2.2F;
        float reach = Mth.lerp(cameraFlight, openReach, finalReach);

        // Every original fissure widens at its own delayed, stepped rate. These
        // independent portal tears are the opening; there is no portal quad behind them.
        for (int index = 0; index < FRACTURE_ANGLES.length; index++) {
            float progress = jerkyProgress(openingTime, index + 3);
            renderPortalTear(
                    graphics,
                    centerX,
                    centerY,
                    FRACTURE_ANGLES[index],
                    reach * FRACTURE_LENGTHS[index] * progress,
                    (5.0F + reach * 0.20F * progress * progress)
                            * (0.78F + (index % 3) * 0.13F)
            );
        }

        // Once the tears overlap, an irregular core bridges their remaining gaps.
        float mergeTime = Mth.clamp((openingTime - 0.38F) / 0.62F, 0.0F, 1.0F);
        float merge = 1.0F - (float)Math.pow(1.0F - mergeTime, 3.0);
        int size = (int)(reach * 1.45F * merge);
        if (size < 2) {
            return;
        }

        int bandHeight = Math.max(2, size / APERTURE_JITTER.length);
        int top = centerY - (bandHeight * APERTURE_JITTER.length) / 2;

        for (int band = 0; band < APERTURE_JITTER.length; band++) {
            float vertical = ((band + 0.5F) / APERTURE_JITTER.length) * 2.0F - 1.0F;
            float profile = Mth.sqrt(Math.max(0.0F, 1.0F - vertical * vertical));
            float bandProgress = jerkyProgress(mergeTime, band + 11);
            int halfWidth = Math.max(1, (int)(size * 0.52F * profile * APERTURE_JITTER[band] * bandProgress));
            int y0 = top + band * bandHeight;
            int y1 = y0 + bandHeight + 1;
            int edge = Math.max(2, size / 90);

            graphics.fill(centerX - halfWidth - edge, y0 - edge, centerX + halfWidth + edge, y1 + edge, 0xFF31004D);
            graphics.fillRenderType(RenderType.endPortal(), centerX - halfWidth, y0, centerX + halfWidth, y1, 0);
        }
    }

    private static void renderPortalTear(
            GuiGraphics graphics,
            float centerX,
            float centerY,
            float angle,
            float length,
            float thickness
    ) {
        if (length < 1.0F) {
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 210.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));

        int totalLength = Math.max(1, (int)length);
        for (int segment = 0; segment < 3; segment++) {
            int x0 = totalLength * segment / 3;
            int x1 = totalLength * (segment + 1) / 3 + 2;
            float segmentVariation = 0.72F + ((segment * 2 + (int)Math.abs(angle)) % 4) * 0.11F;
            int halfThickness = Math.max(2, (int)(thickness * segmentVariation));
            int offset = segment == 1 ? (int)(thickness * 0.12F) : 0;

            graphics.fill(x0 - 2, -halfThickness - 3 + offset, x1 + 2, halfThickness + 3 + offset, 0xFF390057);
            graphics.fillRenderType(
                    RenderType.endPortal(),
                    x0,
                    -halfThickness - 1 + offset,
                    x1,
                    halfThickness + 1 + offset,
                    0
            );
        }
        pose.popPose();
    }

    private static float jerkyProgress(float overallProgress, int index) {
        float delayed = Mth.clamp(overallProgress * 1.22F - (index % 5) * 0.045F, 0.0F, 1.0F);
        int steps = 4 + index % 4;
        float stepped = (float)Math.floor(delayed * steps) / steps;
        float kick = Mth.sin((animationTick + index * 3) * 2.35F) > 0.72F ? 0.055F : 0.0F;
        return Mth.clamp(stepped + kick, 0.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!wakeActive) {
            return;
        }

        float progress = easeOutBack(Mth.clamp(
                (wakeTick + (float)event.getPartialTick()) / WAKE_ANIMATION_END,
                0.0F,
                1.0F
        ));
        event.setRoll(Mth.lerp(progress, 82.0F, 0.0F));
        event.setPitch(Mth.lerp(progress, 28.0F, event.getPitch()));
        event.setYaw(event.getYaw() + Mth.sin(progress * Mth.PI) * 9.0F);
    }

    private static void renderWakeOverlay(RenderGuiEvent.Pre event) {
        float progress = ease(Mth.clamp(
                (wakeTick + event.getPartialTick().getGameTimeDeltaTicks()) / WAKE_ANIMATION_END,
                0.0F,
                1.0F
        ));
        int alpha = (int)((1.0F - progress) * 255.0F);
        if (alpha > 0) {
            GuiGraphics graphics = event.getGuiGraphics();
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
        }
        event.setCanceled(true);
    }

    private static boolean isNextToBed(Minecraft minecraft) {
        BlockPos center = minecraft.player.blockPosition();
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (minecraft.level.getBlockState(center.offset(x, y, z)).getBlock() instanceof BedBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        wakeActive = false;
        wakeTick = 0;
    }
}
