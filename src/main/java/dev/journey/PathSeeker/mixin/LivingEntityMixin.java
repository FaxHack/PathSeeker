package dev.journey.PathSeeker.mixin;

import dev.journey.PathSeeker.modules.utility.ElytraFlyPlusPlus;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    ElytraFlyPlusPlus efly = Modules.get().get(ElytraFlyPlusPlus.class);
    @Shadow
    private int jumpingCooldown;

    @Shadow
    public abstract Brain<?> getBrain();

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/LivingEntity;tickMovement()V")
    private void tickMovement(CallbackInfo ci) {
        if (mc.player != null && mc.player.getBrain().equals(this.getBrain()) && efly != null && efly.enabled()) {
            this.jumpingCooldown = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/LivingEntity;isFallFlying()Z", cancellable = true)
    private void isFallFlying(CallbackInfoReturnable<Boolean> cir) {
        if (mc.player != null && mc.player.getBrain().equals(this.getBrain()) && efly != null && efly.enabled()) {
            cir.setReturnValue(true);
        }
    }
}
