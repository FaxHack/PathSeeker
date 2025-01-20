package dev.journey.PathSeeker.modules.utility;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoJumpDelay extends Module
{
    public NoJumpDelay()
    {
        super(PathSeeker.Main, "NoJumpDelay", "Removes delay between jumps.");
    }
}
