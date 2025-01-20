package dev.journey.PathSeeker.modules.utility;

/*
 * Ported from https://github.com/miles352/meteor-mod/blob/master/src/main/java/com/example/addon/modules/GrimDuraFirework.java
 */

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import static dev.journey.PathSeeker.utils.PathSeekerUtil.firework;


public class GrimDuraFirework extends Module
{

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> fireworkDelay = sgGeneral.add(new IntSetting.Builder()
            .name("Firework Tick Delay")
            .description("The delay between possible firework uses.")
            .defaultValue(10)
            .build()
    );

    public GrimDuraFirework()
    {
        super(PathSeeker.Utility, "GrimFirework", "Swaps to your elytra so fireworks actually work.");
    }

    int fireworkTickDelay = 0;
    int elytraSwapSlot = -1;
    boolean currentlyFiring = false;

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        if (fireworkTickDelay > 0) fireworkTickDelay--;
        if (elytraSwapSlot != -1)
        {
            InvUtils.swap(elytraSwapSlot, true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swapBack();
            elytraSwapSlot = -1;
        }
    }

    // currentlyFiring is true when a firework is being fired by firework()

    @EventHandler
    private void onInteractItem(InteractItemEvent event) {
        ItemStack itemStack = mc.player.getStackInHand(event.hand);

        if (itemStack.getItem() instanceof FireworkRocketItem) {
            if (!currentlyFiring && fireworkTickDelay <= 0)
            {
                currentlyFiring = true;
                int result = firework(mc);
                if (result != 200 && result != -1)
                {
                    elytraSwapSlot = result;
                }
            }
            // let the firework through if its a valid one
            else if (currentlyFiring)
            {
                currentlyFiring = false;
                fireworkTickDelay = fireworkDelay.get();
                return;
            }
            event.toReturn = ActionResult.PASS;
        }
    }
}