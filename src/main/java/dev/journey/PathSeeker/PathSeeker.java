package dev.journey.PathSeeker;

//Meteor imports

import dev.journey.PathSeeker.commands.GarbageCleanerCommand;
import dev.journey.PathSeeker.commands.Stats2b2t;
import dev.journey.PathSeeker.commands.ViewNbtCommand;
import dev.journey.PathSeeker.commands.WorldInfoCommand;
import dev.journey.PathSeeker.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathSeeker extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(PathSeeker.class);
    public static final Category Main = new Category("PathSeeker");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Path-Seeker!");
        Modules.get().add(new ActivatedSpawnerDetector());
        Modules.get().add(new HoleAndTunnelAndStairsESP());
        Modules.get().add(new CaveDisturbanceDetector());
        Modules.get().add(new PortalPatternFinder());
        Modules.get().add(new NewerNewChunks());
        Modules.get().add(new BaseFinder());
        Modules.get().add(new StorageLooter());
        Modules.get().add(new MobGearESP());
        Modules.get().add(new PotESP());
        Commands.add(new Stats2b2t());
        Commands.add(new ViewNbtCommand());
        Commands.add(new GarbageCleanerCommand());
        Commands.add(new WorldInfoCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Main);
    }

    public String getPackage() {
        return "dev.journey.PathSeeker";
    }

}