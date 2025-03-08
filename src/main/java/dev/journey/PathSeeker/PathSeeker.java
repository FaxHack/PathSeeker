package dev.journey.PathSeeker;

import dev.journey.PathSeeker.commands.MeteorFolderCommand;
import dev.journey.PathSeeker.commands.ScreenshotFolderCommand;
import dev.journey.PathSeeker.commands.Stats2b2t;
import dev.journey.PathSeeker.modules.automation.TridentDupe;
import dev.journey.PathSeeker.modules.exploration.*;
import dev.journey.PathSeeker.modules.render.*;
import dev.journey.PathSeeker.modules.utility.*;
import dev.journey.PathSeeker.utils.Update.UpdateChecker;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PathSeeker extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(PathSeeker.class);
    //public static final Category Main = new Category("PathSeeker");
    public static final Category Hunting = new Category("PathHunting");
    public static final Category Render = new Category("PathRender");
    public static final Category Utility = new Category("PathUtils");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Path-Seeker!");
        UpdateChecker.checkForUpdate();

        //Modules
        Modules.get().add(new ActivatedSpawnerDetector());
        Modules.get().add(new StackedMinecartsDetector());
        Modules.get().add(new CaveDisturbanceDetector());
        Modules.get().add(new PortalPatternFinder());
        Modules.get().add(new HoleAndTunnelAndStairsESP());
        Modules.get().add(new NewerNewChunks());
        Modules.get().add(new BaseFinder());
        Modules.get().add(new PotESP());
        Modules.get().add(new MobGearESP());
        Modules.get().add(new GrimDuraFirework());
        Modules.get().add(new SignHistorian());
        Modules.get().add(new Pitch40Util());
        Modules.get().add(new NOJumpDelay());
        Modules.get().add(new DroppedItemESP());
        Modules.get().add(new EntityClusterESP());
        Modules.get().add(new TridentDupe());
        //Modules.get().add(new AreaLoader());
        //Modules.get().add(new ChestIndex());


        //Commands
        Commands.add(new MeteorFolderCommand());
        Commands.add(new ScreenshotFolderCommand());
        Commands.add(new Stats2b2t());

        if (FabricLoader.getInstance().isModLoaded("xaeroplus")) {
            Modules.get().add(new TrailFollower());
        } else {
            LOG.info("XaeroPlus not found, disabling TrailFollower and OldChunkNotifier");
        }
    }

    @Override
    public void onRegisterCategories() {
        //Modules.registerCategory(Main);
        Modules.registerCategory(Hunting);
        Modules.registerCategory(Utility);
        Modules.registerCategory(Render);
    }

    public String getPackage() {
        return "dev.journey.PathSeeker";
    }

}