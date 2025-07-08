package dev.journey.PathSeeker.modules.utility;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;


import static dev.journey.PathSeeker.utils.PathSeekerUtil.*;

public class ElytraFlyPlusPlus extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgObstaclePasser = settings.createGroup("Obstacle Passer");

    private final Setting<Boolean> bounce = sgGeneral.add(new BoolSetting.Builder()
            .name("Bounce")
            .description("Automatically does bounce efly.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> lockPitch = sgGeneral.add(new BoolSetting.Builder()
            .name("Lock Pitch")
            .description("Whether to lock your pitch when bounce is enabled.")
            .defaultValue(true)
            .visible(bounce::get)
            .build()
    );

    private final Setting<Double> pitch = sgGeneral.add(new DoubleSetting.Builder()
            .name("Pitch")
            .description("The pitch to set when bounce is enabled.")
            .defaultValue(90.0)
            .visible(() -> bounce.get() && lockPitch.get())
            .build()
    );

    private final Setting<Boolean> lockYaw = sgGeneral.add(new BoolSetting.Builder()
            .name("Lock Yaw")
            .description("Whether to lock your yaw when bounce is enabled.")
            .defaultValue(false)
            .visible(bounce::get)
            .build()
    );

    private final Setting<Boolean> useCustomYaw = sgGeneral.add(new BoolSetting.Builder()
            .name("Use Custom Yaw")
            .description("Enable this if you want to use a yaw that isn't a factor of 45.")
            .defaultValue(false)
            .visible(bounce::get)
            .build()
    );

    private final Setting<Double> yaw = sgGeneral.add(new DoubleSetting.Builder()
            .name("Yaw")
            .description("The yaw to set when bounce is enabled. This is auto set to the closest 45 deg angle to you unless Use Custom Yaw is enabled.")
            .defaultValue(0.0)
            .visible(() -> bounce.get() && useCustomYaw.get())
            .build()
    );

    private final Setting<Boolean> highwayObstaclePasser = sgObstaclePasser.add(new BoolSetting.Builder()
            .name("Highway Obstacle Passer")
            .description("Uses baritone to pass obstacles.")
            .defaultValue(false)
            .visible(bounce::get)
            .build()
    );

    private final Setting<Boolean> useCustomStartPos = sgObstaclePasser.add(new BoolSetting.Builder()
            .name("Use Custom Start Position")
            .description("Enable and set this ONLY if you are on a ringroad or don't want to be locked to a highway. Otherwise (0, 0) is the start position and will be automatically used.")
            .defaultValue(false)
            .visible(() -> bounce.get() && highwayObstaclePasser.get())
            .build()
    );

    private final Setting<BlockPos> startPos = sgObstaclePasser.add(new BlockPosSetting.Builder()
            .name("Start Position")
            .description("The start position to use when using a custom start position.")
            .defaultValue(new BlockPos(0,0,0))
            .visible(() -> bounce.get() && highwayObstaclePasser.get() && useCustomStartPos.get())
            .build()
    );

    private final Setting<Boolean> awayFromStartPos = sgObstaclePasser.add(new BoolSetting.Builder()
            .name("Away From Start Position")
            .description("If true, will go away from the start position instead of towards it. The start pos is (0,0) if it is not set to a custom start pos.")
            .defaultValue(true)
            .visible(() -> bounce.get() && highwayObstaclePasser.get())
            .build()
    );

    private final Setting<Double> distance = sgObstaclePasser.add(new DoubleSetting.Builder()
            .name("Distance")
            .description("The distance to set the baritone goal for path realignment.")
            .defaultValue(10.0)
            .visible(() -> bounce.get() && highwayObstaclePasser.get())
            .build()
    );

    private final Setting<Integer> targetY = sgObstaclePasser.add(new IntSetting.Builder()
            .name("Y Level")
            .description("The Y level to bounce at.")
            .defaultValue(120)
            .visible(() -> bounce.get() && highwayObstaclePasser.get())
            .build()
    );

    private final Setting<Boolean> avoidPortalTraps = sgObstaclePasser.add(new BoolSetting.Builder()
            .name("Avoid Portal Traps")
            .description("Will attempt to detect portal traps on chunk load and avoid them.")
            .defaultValue(false)
            .visible(() -> bounce.get() && highwayObstaclePasser.get())
            .build()
    );

    private final Setting<Double> portalAvoidDistance = sgObstaclePasser.add(new DoubleSetting.Builder()
            .name("Portal Avoid Distance")
            .description("The distance to a portal trap where the obstacle passer will takeover and go around it.")
            .defaultValue(20)
            .min(0)
            .sliderMax(50)
            .visible(() -> bounce.get() && highwayObstaclePasser.get() && avoidPortalTraps.get())
            .build()
    );

    private final Setting<Integer> portalScanWidth = sgObstaclePasser.add(new IntSetting.Builder()
            .name("Portal Scan Width")
            .description("The width on the axis of the highway that will be scanned for portal traps.")
            .defaultValue(5)
            .min(3)
            .sliderMax(10)
            .visible(() -> bounce.get() && highwayObstaclePasser.get() && avoidPortalTraps.get())
            .build()
    );

    private final Setting<Boolean> toggleElytra = sgGeneral.add(new BoolSetting.Builder()
            .name("Toggle Elytra")
            .description("Equips an elytra on activate, and a chestplate on deactivate.")
            .defaultValue(false)
            .build()
    );

    public ElytraFlyPlusPlus() {
        super(
                PathSeeker.Utility,
                "ElytraFlyPlusPlus",
                "Elytra fly with some more features."
        );
    }

    private boolean startSprinting;
    private BlockPos portalTrap = null;
    private boolean paused = false;

    private boolean elytraToggled = false;

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event)
    {
        if (event.packet instanceof PlayerPositionLookS2CPacket packet)
        {
            onActivate();
        }
    }

    @Override
    public void onActivate()
    {
        if (mc.player == null || mc.player.getAbilities().allowFlying) return;

        startSprinting = mc.player.isSprinting();
        tempPath = null;
        portalTrap = null;
        paused = false;
        waitingForChunksToLoad = false;
        elytraToggled = false;

        // I don't know any other way to fix this stupid shit
        if (bounce.get() && mc.player.getPos().multiply(1, 0, 1).length() >= 100)
        {
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination() == null)
            {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
            }

            if (!useCustomStartPos.get())
            {
                startPos.set(new BlockPos(0, 0, 0));
            }

            if (!useCustomYaw.get())
            {
                // If less than 100 blocks from the start pos, angle calculation may be wrong, so just use players yaw
                if (mc.player.getBlockPos().getSquaredDistance(startPos.get()) < 10_000 || !highwayObstaclePasser.get())
                {
                    double playerAngleNormalized = angleOnAxis(mc.player.getYaw());
                    yaw.set(playerAngleNormalized);
                }
                else
                {
                    // Otherwise use the angle from the starting position to the players position
                    BlockPos directionVec = mc.player.getBlockPos().subtract(startPos.get());
                    double angle = Math.toDegrees(Math.atan2(-directionVec.getX(), directionVec.getZ()));
                    double angleNormalized = angleOnAxis(angle);
                    if (!awayFromStartPos.get())
                    {
                        angleNormalized += 180;
                    }

                    yaw.set(angleNormalized);
                }
            }
        }
    }

    @Override
    public void onDeactivate()
    {
        if (mc.player == null) return;

        if (bounce.get())
        {
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination() == null)
            {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
            }
        }

        mc.player.setSprinting(startSprinting);

        if (toggleElytra.get())
        {
            if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().toString().contains("chestplate")) {
                Modules.get().get(ChestSwap.class).swap();
            }
        }
    }

    // 5 chunks forwards
    private final double maxDistance = 16 * 5;

    // a path used when there are no valid blocks in range.
    // it will instead path to this and then when it gets close it will look for a valid block again
    private BlockPos tempPath = null;

    private boolean waitingForChunksToLoad;

    @EventHandler
    private void onTick(TickEvent.Pre event)
    {
        if (mc.player == null || mc.player.getAbilities().allowFlying) return;

        if (toggleElytra.get() && !elytraToggled)
        {
            if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)))
            {
                Modules.get().get(ChestSwap.class).swap();
            }
            else
            {
                elytraToggled = true;
            }
        }

        if (enabled()) mc.player.setSprinting(true);
        if (bounce.get())
        {
            if (tempPath != null && mc.player.getBlockPos().getSquaredDistance(tempPath) < 500)
            {
                tempPath = null;
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(null);
            }
            else if (tempPath != null)
            {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(tempPath));
                return;
            }

            // if still pathing, wait for that to complete
            if (highwayObstaclePasser.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal() != null)
            {
                return;
            }

            // Length check to fix weird issue where goal gets set to 0 0 when going through queue, even though it gets reset. Likely due to bad connection.
            if (highwayObstaclePasser.get() && mc.player.getPos().length() > 100 && (mc.player.getY() < targetY.get()
                    || mc.player.getY() > targetY.get() + 2
                    || mc.player.horizontalCollision)
                    || portalTrap != null && portalTrap.getSquaredDistance(mc.player.getBlockPos()) < portalAvoidDistance.get() * portalAvoidDistance.get()
                    || waitingForChunksToLoad)
            {
                waitingForChunksToLoad = false;
                paused = true;
                BlockPos goal = mc.player.getBlockPos();
                double currDistance = distance.get(); // Keep checking farther distances until a goal is found that has a block beneath it

                if (portalTrap != null) {
                    currDistance += mc.player.getPos().distanceTo(portalTrap.toCenterPos());
                    portalTrap = null;
                    info("Pathing around portal.");
                }

                do
                {
                    if (currDistance > maxDistance)
                    {
                        tempPath = goal;
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
                        return;
                    }
                    Vec3d unitYawVec = yawToDirection(yaw.get());
                    Vec3d travelVec = mc.player.getPos().subtract(startPos.get().toCenterPos());

                    double parallelCurrPosDot = travelVec.multiply(new Vec3d(1, 0, 1)).dotProduct(unitYawVec);
                    Vec3d parallelCurrPosComponent = unitYawVec.multiply(parallelCurrPosDot);

                    Vec3d pos = startPos.get().toCenterPos().add(parallelCurrPosComponent);
                    pos = positionInDirection(pos, yaw.get(), currDistance);

                    goal = new BlockPos((int)(Math.floor(pos.x)), targetY.get(), (int)Math.floor(pos.z));
                    currDistance++;

                    // Blocks in unloaded chunks are void air, for some reason checking if the chunk is loaded was always true, so I check this instead
                    if (mc.world.getBlockState(goal).getBlock() == Blocks.VOID_AIR)
                    {
                        waitingForChunksToLoad = true;
                        return;
                    }
                }
                // avoid pathing on air cause baritone freaks out, and dont path into portals in case a mod is avoiding portals
                while (!mc.world.getBlockState(goal.down()).isSolidBlock(mc.world, goal.down()) ||
                        mc.world.getBlockState(goal).getBlock() == Blocks.NETHER_PORTAL ||
                        !mc.world.getBlockState(goal).isAir());
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
            }
            else
            {
                // keep jumping
                paused = false;
                if (!enabled()) return;
                if (mc.player.isOnGround())
                {
                    mc.player.jump();
                }

                // set yaw and pitch
                if (lockYaw.get())
                {
                    mc.player.setYaw(yaw.get().floatValue());
                }
                if (lockPitch.get())
                {
                    mc.player.setPitch(pitch.get().floatValue());
                }
            }
        }

        if (enabled())
        {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                    mc.player,
                    ClientCommandC2SPacket.Mode.START_FALL_FLYING
            ));
        }
    }

    public boolean enabled()
    {
        return this.isActive() && !paused && mc.player != null && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().toString().contains("elytra");
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event)
    {
        if (!avoidPortalTraps.get()) return;
        ChunkPos pos = event.chunk().getPos();

        BlockPos centerPos = pos.getCenterAtY(targetY.get());

        // Check if chunk is on the players path
        Vec3d moveDir = yawToDirection(yaw.get());
        double distanceToHighway = distancePointToDirection(Vec3d.of(centerPos), moveDir, mc.player.getPos());

        if (distanceToHighway > 21) return;

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                for (int y = targetY.get(); y < targetY.get() + 3; y++)
                {
                    BlockPos position = new BlockPos(pos.x * 16 + x, y, pos.z * 16 + z);

                    if (distancePointToDirection(Vec3d.of(position), moveDir, mc.player.getPos()) > portalScanWidth.get()) continue;

                    if (mc.world.getBlockState(position).getBlock().equals(Blocks.NETHER_PORTAL)) // TODO: This position could be unloaded
                    {
                        BlockPos posBehind = new BlockPos((int)Math.floor(position.getX() + moveDir.x), position.getY(), (int) Math.floor(position.getZ() + moveDir.z));

                        // Trap is detected when a portal has a solid block or another portal behind it
                        if (mc.world.getBlockState(posBehind).isSolidBlock(mc.world, posBehind) ||
                                mc.world.getBlockState(posBehind).getBlock() == Blocks.NETHER_PORTAL)
                        {
                            if (portalTrap == null || (
                                    portalTrap.getSquaredDistance(posBehind) > 100 &&
                                            mc.player.getBlockPos().getSquaredDistance(posBehind) < mc.player.getBlockPos().getSquaredDistance(portalTrap))
                            )
                            {
                                portalTrap = posBehind;
                            }
                        }
                    }
                }
            }
        }
    }
}