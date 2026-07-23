package com.dlucci6.keepinventorytotem.client.animation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Runtime player for Blockbench camera-bone animations.
 *
 * <p>An id such as {@code keepinventorytotem:bed_wake} loads
 * {@code assets/keepinventorytotem/camera_animations/bed_wake.animation.json}.
 * Position values use Blockbench pixels and are converted to blocks. Coordinates
 * are local to the view: +X right, +Y up, +Z forward. Rotations are degrees in
 * X (pitch), Y (yaw), Z (roll) order.</p>
 */
public final class FirstPersonCameraAnimations {
    private static BlockbenchCameraClip activeClip;
    private static int elapsedTicks;

    private FirstPersonCameraAnimations() {
    }

    public static boolean play(ResourceLocation id, String animationName) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(),
                "camera_animations/" + id.getPath() + ".animation.json"
        );
        try (Reader reader = Minecraft.getInstance()
                .getResourceManager()
                .getResourceOrThrow(file)
                .openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            activeClip = BlockbenchCameraClip.parse(root, animationName);
            elapsedTicks = 0;
            return true;
        } catch (IOException | RuntimeException exception) {
            activeClip = null;
            KeepInventoryAnimationLog.error(file, exception);
            return false;
        }
    }

    public static void stop() {
        activeClip = null;
        elapsedTicks = 0;
    }

    public static void tick() {
        if (activeClip != null && ++elapsedTicks >= Math.ceil(activeClip.lengthSeconds() * 20.0F)) {
            stop();
        }
    }

    public static boolean isPlaying() {
        return activeClip != null;
    }

    public static CameraTransform sample(float partialTick) {
        if (activeClip == null) {
            return CameraTransform.IDENTITY;
        }
        return activeClip.sample((elapsedTicks + partialTick) / 20.0F);
    }
}
