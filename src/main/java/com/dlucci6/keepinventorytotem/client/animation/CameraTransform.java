package com.dlucci6.keepinventorytotem.client.animation;

import net.minecraft.world.phys.Vec3;

public record CameraTransform(Vec3 position, Vec3 rotation) {
    public static final CameraTransform IDENTITY = new CameraTransform(Vec3.ZERO, Vec3.ZERO);
}
