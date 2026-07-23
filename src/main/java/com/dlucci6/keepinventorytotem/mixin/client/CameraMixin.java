package com.dlucci6.keepinventorytotem.mixin.client;

import com.dlucci6.keepinventorytotem.client.KeepingDeathAnimation;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("TAIL"))
    private void keepinventorytotem$applyBedWakePosition(
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean thirdPersonReverse,
            float partialTick,
            CallbackInfo callback
    ) {
        if (detached) {
            return;
        }

        Camera camera = (Camera)(Object)this;
        Vec3 wakePosition = KeepingDeathAnimation.getBedWakeCameraPosition(camera.getPosition(), partialTick);
        if (wakePosition != null) {
            setPosition(wakePosition);
        }
    }
}
