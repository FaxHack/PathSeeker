package dev.journey.PathSeeker;

import dev.journey.PathSeeker.commands.ViewNbtCommand;
import dev.journey.PathSeeker.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.journey.PathSeeker.commands.*;
import dev.journey.PathSeeker.modules.*;


public class PathSeeker extends MeteorAddon {
        public static final Logger LOG = LoggerFactory.getLogger(PathSeeker.class);
        public static final Category Main = new Category("PathSeeker");

        @Override
        public void onInitialize() {
                LOG.info("Initializing Path-Seeker!");

                Modules.get().add(new ActivatedSpawnerDetector());
                Modules.get().add(new CaveDisturbanceDetector());
                Modules.get().add(new PortalPatternFinder());
                Modules.get().add(new HoleAndTunnelAndStairsESP());
                Modules.get().add(new NewerNewChunks());
                Modules.get().add(new BaseFinder());
                Modules.get().add(new StorageLooter());
                Modules.get().add(new PotESP());
                Modules.get().add(new NbtEditor());
                Commands.add(new ViewNbtCommand());
        }

        @Override
        public void onRegisterCategories() {
                Modules.registerCategory(Main);
        }

        public String getPackage() {
                return "dev.journey.PathSeeker";
        }

}