package dev.journey.PathSeeker.utils;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.Helper;

public class BaritoneHelper {

    public static void walkTo(int x, int y, int z) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z));
    }

    public static void cancelPath() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public static void log(String message) {
        Helper.HELPER.logDirect(message);
    }
}