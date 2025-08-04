package dev.journey.PathSeeker.modules.render;

import dev.journey.PathSeeker.PathSeeker;
import dev.journey.PathSeeker.events.ScreenRenderEvent;
import dev.journey.PathSeeker.utils.ShulkerInfo;
import dev.journey.PathSeeker.utils.Type;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;


public class InventoryInfoModule extends Module {
    private static final Color BACKGROUND = new Color(0, 0, 0, 75);
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> compact = sgGeneral.add(new BoolSetting.Builder()
            .name("Compact")
            .defaultValue(true)
            .build()
    );

    private final List<ShulkerInfo> info = new ArrayList<>();
    private final Queue<Runnable> renderQueue = new ArrayDeque<>();
    private int height, offset;
    private Vec2f clicked;

    public InventoryInfoModule() {
        super(PathSeeker.Utility, "inventory-info", "prigozhinplugg");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!(mc.currentScreen instanceof HandledScreen<?>) || mc.player.age % 4 != 0) return;
        refresh((HandledScreen<?>) mc.currentScreen);
    }

    @EventHandler
    private void onRenderScreen(ScreenRenderEvent event) {
        int y = 3 + offset;
        for (ShulkerInfo shulkerInfo : info) {
            int count = 0, x = 2, startY = y, maxX = 22;
            for (ItemStack stack : shulkerInfo.stacks()) {
                if (shulkerInfo.type() == Type.COMPACT && stack.isEmpty()) break;
                if (count > 0 && count % 9 == 0) {
                    x = 2;
                    y += 18;
                }
                int finalX = x, finalY = y;
                renderQueue.add(() -> {
                    event.drawContext.drawItem(stack, finalX + 2, finalY);
                    if (stack.getCount() > 999) {
                        event.drawContext.drawItemInSlot(mc.textRenderer, stack, finalX + 2, finalY, "%.1fk".formatted(stack.getCount() / 1000f));
                    } else {
                        event.drawContext.drawItemInSlot(mc.textRenderer, stack, finalX + 2, finalY);
                    }
                });
                x += 20;
                count++;
                if (x > maxX) maxX = x;
            }
            y += 18;
            if (clicked != null
                    && clicked.x >= 2 && clicked.x <= maxX
                    && clicked.y >= startY && clicked.y <= y) {
                renderQueue.add(() -> {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, shulkerInfo.slot(), 0, SlotActionType.PICKUP, mc.player);
                });
                setClicked(null);
            }
            event.drawContext.fill(2, startY, maxX, y, BACKGROUND.hashCode());
            event.drawContext.fill(2, startY - 1, maxX, startY, shulkerInfo.color());
            y += 2;
        }

        while (!renderQueue.isEmpty()) renderQueue.poll().run();

        height = y - offset;
        setClicked(null);
    }

    private void refresh(HandledScreen<?> screen) {
        info.clear();
        for (Slot slot : screen.getScreenHandler().slots) {
            ShulkerInfo shulkerInfo = ShulkerInfo.create(slot.getStack(), slot.id);
            if (shulkerInfo == null) continue;
            info.add(shulkerInfo);
        }
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = MathHelper.clamp(offset, -Math.max(height - mc.getWindow().getScaledHeight(), 0), 0);
    }

    public void setClicked(Vec2f clicked) {
        this.clicked = clicked;
    }
}
