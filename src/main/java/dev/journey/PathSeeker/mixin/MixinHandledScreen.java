package dev.journey.PathSeeker.mixin;

import dev.journey.PathSeeker.events.ScreenRenderEvent;
import dev.journey.PathSeeker.modules.render.InventoryInfoModule;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class MixinHandledScreen extends Screen {
    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"))
    private void drawMouseoverTooltipHook(DrawContext context, int x, int y, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ScreenRenderEvent.get(context, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true)));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void click(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        InventoryInfoModule m = Modules.get().get(InventoryInfoModule.class);
        if (!m.isActive()) return;
        Modules.get().get(InventoryInfoModule.class).setClicked(new Vec2f((float) mouseX, (float) mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        InventoryInfoModule m = Modules.get().get(InventoryInfoModule.class);
        if (!m.isActive() && verticalAmount == 0)
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        m.setOffset((int) (m.getOffset() + Math.ceil(verticalAmount) * 18));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
