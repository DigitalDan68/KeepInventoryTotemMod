package com.dlucci6.keepinventorytotem.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class BlockbenchCameraClip {
    private static final String CAMERA_BONE = "camera";

    private final float lengthSeconds;
    private final Channel position;
    private final Channel rotation;

    private BlockbenchCameraClip(float lengthSeconds, Channel position, Channel rotation) {
        this.lengthSeconds = lengthSeconds;
        this.position = position;
        this.rotation = rotation;
    }

    static BlockbenchCameraClip parse(JsonObject root, String animationName) {
        JsonObject animations = root.getAsJsonObject("animations");
        if (animations == null || !animations.has(animationName)) {
            throw new IllegalArgumentException("Missing Blockbench animation: " + animationName);
        }

        JsonObject animation = animations.getAsJsonObject(animationName);
        JsonObject bones = animation.getAsJsonObject("bones");
        JsonObject camera = bones == null ? null : bones.getAsJsonObject(CAMERA_BONE);
        if (camera == null) {
            throw new IllegalArgumentException("Animation must contain a bone named 'camera'");
        }

        float declaredLength = animation.has("animation_length")
                ? animation.get("animation_length").getAsFloat()
                : 0.0F;
        Channel position = Channel.parse(camera.get("position"), true);
        Channel rotation = Channel.parse(camera.get("rotation"), false);
        float keyframeLength = Math.max(position.lastTime(), rotation.lastTime());
        return new BlockbenchCameraClip(Math.max(declaredLength, keyframeLength), position, rotation);
    }

    float lengthSeconds() {
        return lengthSeconds;
    }

    CameraTransform sample(float seconds) {
        return new CameraTransform(position.sample(seconds), rotation.sample(seconds));
    }

    private record Channel(NavigableMap<Float, Vec3> keyframes) {
        private static Channel parse(JsonElement element, boolean convertPixels) {
            NavigableMap<Float, Vec3> keyframes = new TreeMap<>();
            if (element == null || element.isJsonNull()) {
                keyframes.put(0.0F, Vec3.ZERO);
            } else if (element.isJsonArray()) {
                keyframes.put(0.0F, readVector(element.getAsJsonArray(), convertPixels));
            } else {
                for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                    JsonElement value = entry.getValue();
                    if (value.isJsonObject()) {
                        JsonObject keyframe = value.getAsJsonObject();
                        value = keyframe.has("post") ? keyframe.get("post") : keyframe.get("pre");
                    }
                    if (value != null && value.isJsonArray()) {
                        keyframes.put(Float.parseFloat(entry.getKey()), readVector(value.getAsJsonArray(), convertPixels));
                    }
                }
            }
            if (keyframes.isEmpty()) {
                keyframes.put(0.0F, Vec3.ZERO);
            }
            return new Channel(keyframes);
        }

        private static Vec3 readVector(JsonArray vector, boolean convertPixels) {
            double scale = convertPixels ? 1.0 / 16.0 : 1.0;
            return new Vec3(
                    component(vector, 0) * scale,
                    component(vector, 1) * scale,
                    component(vector, 2) * scale
            );
        }

        private static double component(JsonArray vector, int index) {
            return vector.size() > index ? vector.get(index).getAsDouble() : 0.0;
        }

        private float lastTime() {
            return keyframes.lastKey();
        }

        private Vec3 sample(float time) {
            Map.Entry<Float, Vec3> before = keyframes.floorEntry(time);
            Map.Entry<Float, Vec3> after = keyframes.ceilingEntry(time);
            if (before == null) {
                return keyframes.firstEntry().getValue();
            }
            if (after == null || before.getKey().equals(after.getKey())) {
                return before.getValue();
            }
            double progress = Mth.clamp(
                    (time - before.getKey()) / (after.getKey() - before.getKey()),
                    0.0F,
                    1.0F
            );
            return before.getValue().lerp(after.getValue(), progress);
        }
    }
}
