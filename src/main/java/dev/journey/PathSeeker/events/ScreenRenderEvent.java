package dev.journey.PathSeeker.events;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.DrawContext;

public class ScreenRenderEvent {
    private static final ScreenRenderEvent INSTANCE = new ScreenRenderEvent();

    public DrawContext drawContext;
    public double frameTime;
    public float tickDelta;

    public static ScreenRenderEvent get(DrawContext drawContext, float tickDelta) {
        INSTANCE.drawContext = drawContext;
        INSTANCE.frameTime = Utils.frameTime;
        INSTANCE.tickDelta = tickDelta;
        return INSTANCE;
    }
}
