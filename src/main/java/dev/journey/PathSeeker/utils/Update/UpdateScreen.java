package dev.journey.PathSeeker.utils.Update;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.awt.*;
import java.net.URI;

public class UpdateScreen extends Screen {
    private final String latestVersion;

    public UpdateScreen(String latestVersion) {
        super(Text.literal("Update Available"));
        this.latestVersion = latestVersion;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // "Update Now" button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Update Now"), button -> {
                    openURL("https://github.com/FaxHack/PathSeeker/releases");
                    this.close();
                })
                .dimensions(centerX - 100, centerY + 20, 200, 20)
                .build());

        // "Remind Me Later" button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Remind Me Later"), button ->
                        this.close())
                .dimensions(centerX - 100, centerY + 45, 200, 20)
                .build());

        // "Don't Show Again" button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Don't Show Again"), button -> {
                    UserConfig.setUpdateCheckDisabled(true);
                    this.close();
                })
                .dimensions(centerX - 100, centerY + 70, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        // Draw centered text
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("A new update (" + latestVersion + ") is available!"),
                this.width / 2,
                this.height / 2 - 40,
                0xFFFFFF
        );

        super.render(context, mouseX, mouseY, delta);
    }

    private void openURL(String urlString) {
        try {
            Desktop.getDesktop().browse(new URI(urlString));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}