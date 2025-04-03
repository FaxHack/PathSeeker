package dev.journey.PathSeeker.modules.exploration;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.journey.PathSeeker.PathSeeker;
import dev.journey.PathSeeker.modules.utility.Pitch40Util;
import dev.journey.PathSeeker.modules.automation.AFKVanillaFly;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.World;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.PaletteNewChunks;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;

import java.time.Duration;
import java.util.ArrayDeque;
// removed the no loner needed imports since I seperated my AFKVanilla logic into a different module so it looks cleaner

public class TrailFollower extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // TODO: Set this automatically either by looking at the rate of chunk loads or by using yaw instead of block pos so size doesnt negatively effect result
    public final Setting<Integer> maxTrailLength = sgGeneral.add(new IntSetting.Builder()
            .name("Max Trail Length")
            .description("The number of trail points to keep for the average. Adjust to change how quickly the average will change. More does not necessarily equal better because if the list is too long it will contain chunks behind you.")
            .defaultValue(20*10) // temporary until nether logic separated
            .sliderRange(1, 100)
            .build()
    );

    public final Setting<Integer> chunksBeforeStarting = sgGeneral.add(new IntSetting.Builder()
            .name("Chunks Before Starting")
            .description("Useful for afking looking for a trail. The amount of chunks before it gets detected as a trail.")
            .defaultValue(10)
            .sliderRange(1, 50)
            .build()
    );

    public final Setting<Integer> chunkConsiderationWindow = sgGeneral.add(new IntSetting.Builder()
            .name("Chunk Timeframe")
            .description("The amount of time in seconds that the chunks must be found in before starting.")
            .defaultValue(5)
            .sliderRange(1, 20)
            .build()
    );

    public final Setting<TrailEndBehavior> trailEndBehavior = sgGeneral.add(new EnumSetting.Builder<TrailEndBehavior>()
            .name("Trail End Behavior")
            .description("What to do when the trail ends.")
            .defaultValue(TrailEndBehavior.DISABLE)
            .build()
    );
    // using enum dropdown item instead of boolean
    public enum FlightMode {
        PITCH40,
        VANILLA
    }

    public final Setting<FlightMode> flightMode = sgGeneral.add(new EnumSetting.Builder<FlightMode>()
            .name("Flight Mode")
            .description("Choose how TrailFollower flies.")
            .defaultValue(FlightMode.PITCH40)
            .build()
    );


    public final Setting<Boolean> pitch40Firework = sgGeneral.add(new BoolSetting.Builder()
            .name("Auto Firework")
            .description("Uses a firework automatically if your velocity is too low.")
            .defaultValue(true)
            .visible(() -> flightMode.get() == FlightMode.PITCH40)
            .build()
    );

    public final Setting<Double> rotateScaling = sgGeneral.add(new DoubleSetting.Builder()
            .name("Rotate Scaling")
            .description("Scaling of how fast the yaw changes. 1 = instant, 0 = doesn't change")
            .defaultValue(0.1)
            .sliderRange(0.0, 1.0)
            .build()
    );

    public final Setting<Boolean> autoElytra = sgGeneral.add(new BoolSetting.Builder()
            .name("[Baritone] Auto Start Baritone Elytra")
            .description("Starts baritone elytra for you.")
            .defaultValue(false)
            .build()
    );

    private final SettingGroup sgAdvanced = settings.createGroup("Advanced", false);

    public final Setting<Double> pathDistance = sgAdvanced.add(new DoubleSetting.Builder()
            .name("Path Distance")
            .description("The distance to add trail positions in the direction the player is facing.")
            .defaultValue(500)
            .sliderRange(100, 2000)
            .build()
    );

    public final Setting<Double> startDirectionWeighting = sgAdvanced.add(new DoubleSetting.Builder()
            .name("Start Direction Weight")
            .description("The weighting of the direction the player is facing when starting the trail. 0 for no weighting (not recommended) 1 for max weighting (will take a bit for direction to change)")
            .defaultValue(0.5)
            .min(0)
            .sliderMax(1)
            .build()
    );

    public final Setting<DirectionWeighting> directionWeighting = sgAdvanced.add(new EnumSetting.Builder<DirectionWeighting>()
            .name("Direction Weighting")
            .description("How the chunks found should be weighted. Useful for path splits. Left will weight chunks to the left of the player higher, right will weigh chunks to the right higher, and none will be in the middle/random. ")
            .defaultValue(DirectionWeighting.NONE)
            .build()
    );

    public final Setting<Integer> directionWeightingMultiplier = sgAdvanced.add(new IntSetting.Builder()
            .name("Direction Weighting Multiplier")
            .description("The multiplier for how much weight should be given to chunks in the direction specified. Values are capped to be in the range [2, maxTrailLength].")
            .defaultValue(2)
            .min(2)
            .sliderMax(10)
            .visible(() -> directionWeighting.get() != DirectionWeighting.NONE)
            .build()
    );

    public final Setting<Double> chunkFoundTimeout = sgAdvanced.add(new DoubleSetting.Builder()
            .name("Chunk Found Timeout")
            .description("The amount of MS without a chunk found to trigger circling.")
            .defaultValue(1000 * 5)
            .min(1000)
            .sliderMax(1000 * 10)
            .build()
    );

    public final Setting<Double> circlingDegPerTick = sgAdvanced.add(new DoubleSetting.Builder()
            .name("Circling Degrees Per Tick")
            .description("The amount of degrees to change per tick while circling.")
            .defaultValue(2.0)
            .min(1.0)
            .sliderMax(20.0)
            .build()
    );

    public final Setting<Double> trailTimeout = sgAdvanced.add(new DoubleSetting.Builder()
            .name("Trail Timeout")
            .description("The amount of MS without a chunk found to stop following the trail.")
            .defaultValue(1000 * 30)
            .min(1000 * 10)
            .sliderMax(1000 * 60)
            .build()
    );

    // added trail deviation slider now that baritone is locked to trail pathing
    public final Setting<Double> maxTrailDeviation = sgAdvanced.add(new DoubleSetting.Builder()
            .name("Max Trail Deviation")
            .description("Maximum allowed angle (in degrees) from the original trail direction. Helps avoid switching to intersecting trails.")
            .defaultValue(180.0)
            .min(1.0)
            .sliderMax(270.0)
            .build()
    );

    public final Setting<Integer> chunkCacheLength = sgAdvanced.add(new IntSetting.Builder()
            .name("Chunk Cache Length")
            .description("The amount of chunks to keep in the cache. (Won't be applied until deactivating)")
            .defaultValue(100_000)
            .sliderRange(0, 10_000_000)
            .build()
    );
    private Cache<Long, Byte> seenChunksCache = Caffeine.newBuilder()
            .maximumSize(chunkCacheLength.get())
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    public final Setting<Integer> baritoneUpdateTicks = sgAdvanced.add(new IntSetting.Builder()
            .name("[Baritone] Baritone Path Update Ticks")
            .description("The amount of ticks between updates to the baritone goal. Low values may cause high instability.")
            .defaultValue(5 * 20) // 5 seconds
            .sliderRange(20, 30 * 20)
            .build()
    );

    // TODO: Auto disconnect at certain chunk load speed
    public final Setting<Boolean> debug = sgAdvanced.add(new BoolSetting.Builder()
            .name("Debug")
            .description("Debug mode.")
            .defaultValue(false)
            .build()
    );
    Vec3d posDebug;
    private boolean oldAutoFireworkValue;
    private FollowMode followMode;
    private boolean followingTrail = false;
    private ArrayDeque<Vec3d> trail = new ArrayDeque<>();
    private ArrayDeque<Vec3d> possibleTrail = new ArrayDeque<>();
    private long lastFoundTrailTime;

    // Credit to WarriorLost: https://github.com/WarriorLost/meteor-client/tree/master
    private long lastFoundPossibleTrailTime;
    private double targetYaw;
    private int baritoneSetGoalTicks = 0;

    public TrailFollower() {
        super(PathSeeker.Hunting, "TrailFollower", "Automatically follows trails in all dimensions.");
    }

    void resetTrail() {
        baritoneSetGoalTicks = 0;
        followingTrail = false;
        trail = new ArrayDeque<>();
        possibleTrail = new ArrayDeque<>();
    }

    @Override
    public void onActivate() {
        resetTrail();
        pathDistanceActual = pathDistance.get();
        if (followMode == FollowMode.YAWLOCK && flightMode.get() == FlightMode.VANILLA) {
            // *fixed, should only activate in overworld or end
            if (!mc.world.getDimension().hasCeiling()) {
                Modules.get().get(AFKVanillaFly.class).toggle();
            }
        }
        XaeroPlus.EVENT_BUS.register(this);
        if (mc.player != null && mc.world != null) {
            if (!mc.world.getDimension().hasCeiling()) {
                followMode = FollowMode.YAWLOCK;
                info("You are in the overworld or end, basic yaw mode will be used.");
            } else {
                try {
                    Class.forName("baritone.api.BaritoneAPI");
                    followMode = FollowMode.BARITONE;
                    info("You are in the nether, baritone mode will be used.");
                } catch (ClassNotFoundException e) {
                    info("Baritone is required to trail follow in the nether. Disabling TrailFollower");
                    this.toggle();
                    return;
                }
            }

            if (followMode == FollowMode.YAWLOCK) {
                Class<Pitch40Util> pitch40Util = Pitch40Util.class;
                Pitch40Util pitch40UtilModule = Modules.get().get(pitch40Util);
                if (flightMode.get() == FlightMode.PITCH40 && !pitch40UtilModule.isActive()) {
                    pitch40UtilModule.toggle();
                    if (pitch40Firework.get()) {
                        Setting<Boolean> setting = (Setting<Boolean>) pitch40UtilModule.settings.get("Auto Firework");
                        if (setting != null) {
                            info("Auto Firework enabled, if you want to change the velocity threshold or the firework cooldown check the settings under Pitch40Util.");
                            oldAutoFireworkValue = setting.get();
                            setting.set(true);
                        }
                    }
                }
            }
            // set original pos to pathDistance blocks in the direction the player is facing
            Vec3d offset = (new Vec3d(Math.sin(-mc.player.getYaw() * Math.PI / 180), 0, Math.cos(-mc.player.getYaw() * Math.PI / 180)).normalize()).multiply(pathDistance.get());
            Vec3d targetPos = mc.player.getPos().add(offset);
            for (int i = 0; i < (maxTrailLength.get() * startDirectionWeighting.get()); i++) {
                trail.add(targetPos);
            }
            targetYaw = getActualYaw(mc.player.getYaw());
        } else {
            this.toggle();
        }
    }

    @Override
    public void onDeactivate() {
        // do this at the end to free memory
        seenChunksCache = Caffeine.newBuilder()
                .maximumSize(chunkCacheLength.get())
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
        XaeroPlus.EVENT_BUS.unregister(this);
        trail.clear();
        // If follow mode was never set due to baritone not being present, etc.
        if (flightMode.get() == FlightMode.VANILLA) {
            AFKVanillaFly afkFly = Modules.get().get(AFKVanillaFly.class);
            if (afkFly.isActive()) afkFly.toggle();
        }
        switch (followMode) {
            case BARITONE: {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("cancel");
                break;
            }
            case YAWLOCK: {
                Class<Pitch40Util> pitch40Util = Pitch40Util.class;
                Pitch40Util pitch40UtilModule = Modules.get().get(pitch40Util);
                if (flightMode.get() == FlightMode.PITCH40 && pitch40UtilModule.isActive()) {
                    pitch40UtilModule.toggle();
                }
                Setting<Boolean> autoFireworkSetting = (Setting<Boolean>) pitch40UtilModule.settings.get("Auto Firework");
                if (autoFireworkSetting != null) {
                    autoFireworkSetting.set(oldAutoFireworkValue);
                }
            }
        }
    }

    private void circle() {
        if (followMode == FollowMode.BARITONE && baritoneSetGoalTicks == 0) {
            baritoneSetGoalTicks = baritoneUpdateTicks.get();
        } else if (followMode == FollowMode.BARITONE) return;
        mc.player.setYaw(getActualYaw((float) (mc.player.getYaw() + circlingDegPerTick.get())));
        if (mc.player.age % 100 == 0) {
            info("Circling to look for new chunks, abandoning trail in " + (trailTimeout.get() - (System.currentTimeMillis() - lastFoundTrailTime)) / 1000 + " seconds.");
        }
    }
    // add a Nether minimum chunk distance threshold (seperate from maxTrailLength) to decrease number of waypoints in the future if needed
    private void optimizeBaritoneForNether() {
        if (mc.world.getRegistryKey().equals(World.NETHER)) {
            var baritoneSettings = BaritoneAPI.getSettings();
            baritoneSettings.primaryTimeoutMS.value = 500L;
            baritoneSettings.failureTimeoutMS.value = 1000L;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (followingTrail && System.currentTimeMillis() - lastFoundTrailTime > trailTimeout.get()) {
            resetTrail();
            info("Trail timed out, stopping.");
            // TODO: Add options for what to do next
            switch (trailEndBehavior.get()) {
                case DISABLE: {
                    this.toggle();
                    break;
                }
            }
        }
        if (followingTrail && System.currentTimeMillis() - lastFoundTrailTime > chunkFoundTimeout.get()) {
            circle();
            return;
        }
        switch (followMode) {
            case BARITONE: {
                if (baritoneSetGoalTicks > 0) {
                    baritoneSetGoalTicks--;
                } else if (baritoneSetGoalTicks == 0) {
                    optimizeBaritoneForNether();
                    //instead of flying to a calculated offset from the player using pathDistanceActual, will directly set the last trail chunk detected
                    if (mc.world.getRegistryKey().equals(World.NETHER)) {
                        optimizeBaritoneForNether();
                        // reduce waypoint frequency
                        if (baritoneSetGoalTicks > 0)
                        {
                            baritoneSetGoalTicks--;
                            return;
                        }

                        if (!trail.isEmpty()) {
                            Vec3d lastTrailPoint = trail.getLast();
                            int x = (int) lastTrailPoint.x;
                            int z = (int) lastTrailPoint.z;

                            // directly sets a pathfinding goal in Baritone using the internal Java API, rather than having it continuously cancel and re-execute commands, which caused a lot of stuttering. No more freezing!
                            var baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                            // will still set continuous path updates using the existing method
                            baritone.getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));

                            baritoneSetGoalTicks = baritoneUpdateTicks.get();
                        }

                } else {
                        // use average path for overworld
                        Vec3d averagePos = calculateAveragePosition(trail);
                        Vec3d positionVec = averagePos.subtract(mc.player.getPos()).normalize();
                        Vec3d targetPos = mc.player.getPos().add(positionVec.multiply(10));
                        targetYaw = Rotations.getYaw(targetPos);

                        // set Baritone goal in that direction
                        Vec3d baritoneTarget = positionInDirection(mc.player.getPos(), targetYaw, pathDistanceActual);
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                                .setGoalAndPath(new GoalXZ((int) baritoneTarget.x, (int) baritoneTarget.z));

                        targetYaw = Rotations.getYaw(targetPos);
                    }
                    if (autoElytra.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination() == null) {
                        // TODO: Fix this
                        info("The auto elytra mode is broken right now. If it's not working just turn it off and manually use #elytra to start.");
                        BaritoneAPI.getSettings().elytraTermsAccepted.value = true;
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("elytra");
                    }
                }
                break;
            }
            case YAWLOCK: {
                mc.player.setYaw(smoothRotation(getActualYaw(mc.player.getYaw()), targetYaw));
                break;
            }

        }

    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!debug.get()) return;
        Vec3d targetPos = positionInDirection(mc.player.getPos(), targetYaw, 10);
        // target line
        event.renderer.line(mc.player.getX(), mc.player.getY(), mc.player.getZ(), targetPos.x, targetPos.y, targetPos.z, new Color(255, 0, 0));
        // chunk
        if (posDebug != null)
            event.renderer.line(mc.player.getX(), mc.player.getY(), mc.player.getZ(), posDebug.x, targetPos.y, posDebug.z, new Color(0, 0, 255));
    }


    @net.lenni0451.lambdaevents.EventHandler(priority = -1)
    public void onChunkData(ChunkDataEvent event) {
        WorldChunk chunk = event.chunk();
        long chunkLong = chunk.getPos().toLong();

        // if found in the cache then ignore the chunk
        if (seenChunksCache.getIfPresent(chunkLong) != null) return;

        seenChunksCache.put(chunkLong, Byte.MAX_VALUE);
        boolean is119NewChunk = ModuleManager.getModule(PaletteNewChunks.class)
                .isNewChunk(
                        chunk.getPos().x,
                        chunk.getPos().z,
                        chunk.getWorld().getRegistryKey()
                );

        // TODO: Find a better way to do this bc Xaero is already checking the chunk
        boolean is112OldChunk = false;
        ReferenceSet<Block> OVERWORLD_BLOCKS = ReferenceOpenHashSet.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.AMETHYST_BLOCK, Blocks.SMOOTH_BASALT, Blocks.TUFF, Blocks.KELP, Blocks.KELP_PLANT, Blocks.POINTED_DRIPSTONE, Blocks.DRIPSTONE_BLOCK, Blocks.DEEPSLATE, Blocks.AZALEA, Blocks.BIG_DRIPLEAF, Blocks.BIG_DRIPLEAF_STEM, Blocks.SMALL_DRIPLEAF, Blocks.MOSS_BLOCK, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT);
        ReferenceSet<Block> NETHER_BLOCKS = ReferenceOpenHashSet.of(Blocks.ANCIENT_DEBRIS, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM, Blocks.NETHER_GOLD_ORE, Blocks.CHAIN);
        // In the end
        if (!mc.world.getDimension().hasCeiling() && !mc.world.getDimension().bedWorks()) {
            RegistryEntry<Biome> biomeHolder = this.mc.world.getBiome(new BlockPos(ChunkUtils.chunkCoordToCoord(chunk.getPos().x) + 8, 64, ChunkUtils.chunkCoordToCoord(chunk.getPos().z) + 8));
            if (biomeHolder.getKey().filter((biome) -> biome.equals(BiomeKeys.THE_END)).isPresent())
                is112OldChunk = true;
        } else {
            // Not in the end
            is112OldChunk = !ChunkScanner.chunkContainsBlocks(chunk, !mc.world.getDimension().hasCeiling() ? OVERWORLD_BLOCKS : NETHER_BLOCKS, 5);
        }

        // TODO: Add options for following certain types of chunks.

        if (!is119NewChunk || is112OldChunk) {
            Vec3d pos = chunk.getPos().getCenterAtY(0).toCenterPos();
            posDebug = pos;

            if (!followingTrail) {
                if (System.currentTimeMillis() - lastFoundPossibleTrailTime > chunkConsiderationWindow.get() * 1000) {
                    possibleTrail.clear();
                }
                possibleTrail.add(pos);
                lastFoundPossibleTrailTime = System.currentTimeMillis();
                if (possibleTrail.size() > chunksBeforeStarting.get()) {
                    followingTrail = true;
                    lastFoundTrailTime = System.currentTimeMillis();
                    trail.addAll(possibleTrail);
                    possibleTrail.clear();
                }
                return;
            }


            // add chunks to the list

            double chunkAngle = Rotations.getYaw(pos);
            double angleDiff = angleDifference(targetYaw, chunkAngle);

            // Ignore chunks not in the direction of the target
            // This shouldn't be needed assuming the chunk cache works
            // was not able to add this before, but now can successfully filter out most other trails using the most recent chunk for pathing
            if (followingTrail && Math.abs(angleDiff) > maxTrailDeviation.get()) {
                return;
            }
            lastFoundTrailTime = System.currentTimeMillis();

            // free up one spot for a new chunk to be added
            while (trail.size() >= maxTrailLength.get()) {
                trail.pollFirst();
            }

            if (angleDiff > 0 && angleDiff < 90 && directionWeighting.get() == DirectionWeighting.LEFT) {
                // add extra chunks to increase the weighting
                // TODO: Maybe redo this to use a map of chunk pos to weights
                for (int i = 0; i < directionWeightingMultiplier.get() - 1; i++) {
                    trail.pollFirst();
                    trail.add(pos);
                }
                trail.add(pos);
            } else if (angleDiff < 0 && angleDiff > -90 && directionWeighting.get() == DirectionWeighting.RIGHT) {
                for (int i = 0; i < directionWeightingMultiplier.get() - 1; i++) {
                    trail.pollFirst();
                    trail.add(pos);
                }
                trail.add(pos);
            } else {
                trail.add(pos);
            }


            // instead of a calculated average coordinate, will use latest chunk added to trail
            // *fix for overworld smoothing
            if (!trail.isEmpty()) {
                if (!trail.isEmpty()) {
                    if (followMode == FollowMode.YAWLOCK) {
                        Vec3d averagePos = calculateAveragePosition(trail);
                        Vec3d positionVec = averagePos.subtract(mc.player.getPos()).normalize();
                        Vec3d targetPos = mc.player.getPos().add(positionVec.multiply(10));
                        targetYaw = Rotations.getYaw(targetPos);
                    } else {
                        Vec3d lastTrailPoint = trail.getLast();
                        targetYaw = Rotations.getYaw(lastTrailPoint);
                    }
                }
            }
        }
    }

    private Vec3d calculateAveragePosition(ArrayDeque<Vec3d> positions) {
        double sumX = 0, sumZ = 0;
        for (Vec3d pos : positions) {
            sumX += pos.x;
            sumZ += pos.z;
        }
        return new Vec3d(sumX / positions.size(), 0, sumZ / positions.size());
    }

    private float getActualYaw(float yaw) {
        return (yaw % 360 + 360) % 360;
    }

    private float smoothRotation(double current, double target) {
        double difference = angleDifference(target, current);
        return (float) (current + difference * rotateScaling.get());
    }

    private double angleDifference(double target, double current) {
        double diff = (target - current + 180) % 360 - 180;
        return diff < -180 ? diff + 360 : diff;
    }

    private double pathDistanceActual;

    private Vec3d positionInDirection(Vec3d pos, double yaw, double distance) {
        Vec3d offset = (new Vec3d(Math.sin(-yaw * Math.PI / 180), 0, Math.cos(-yaw * Math.PI / 180)).normalize()).multiply(distance);
        return pos.add(offset);
    }

    private enum FollowMode {
        BARITONE,
        YAWLOCK
    }

    public enum DirectionWeighting {
        LEFT,
        NONE,
        RIGHT
    }

    public enum TrailEndBehavior {
        DISABLE,
    }
}