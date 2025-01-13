package dev.journey.PathSeeker.commands;

import com.google.gson.Gson;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.journey.PathSeeker.PathSeeker;
import dev.journey.PathSeeker.utils.ApiHandler;
import dev.journey.PathSeeker.utils.PathSeekerUtil;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class Stats2b2t extends Command {
    private static final String API_ENDPOINT = "/stats/player?playerName=";
    private final PathSeekerUtil pathSeekerUtil;

    public Stats2b2t() {
        super("stats", "Fetch stats for a 2b2t player from api.2b2t.vc.", "2b2tstats");
        this.pathSeekerUtil = new PathSeekerUtil();
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", StringArgumentType.word()).executes(ctx -> {
            MeteorExecutor.execute(() -> {
                ClientPlayerEntity player = mc.player;

                if (player == null) return;

                String playerString = ctx.getArgument("player", String.class);
                String requestString = ApiHandler.API_2B2T_URL + API_ENDPOINT + playerString.trim();
                String response = new ApiHandler().fetchResponse(requestString);

                if (response == null) return;

                if (response.equals("204 Undocumented")) {
                    player.sendMessage(
                            Text.of(
                                    "§8<" + PathSeekerUtil.randomColorCode() + "§o✨" + "§r§8> §4§oPlayer not found§7..."
                            )
                    );
                } else {
                    try {
                        Gson gson = new Gson();
                        String colorCode = PathSeekerUtil.randomColorCode();
                        PlayerStats stats = gson.fromJson(response, PlayerStats.class);

                        pathSeekerUtil.updateTimeInfo(
                                stats.lastSeen,
                                stats.firstSeen,
                                stats.playtimeSeconds
                        );

                        String formattedFirstSeen = pathSeekerUtil.getFormattedFirstSeen();
                        String formattedLastSeen = pathSeekerUtil.getFormattedLastSeen();
                        String formattedPlaytime = pathSeekerUtil.getFormattedPlaytime();
                        String formattedPlaytimeInMonth = PathSeekerUtil.formatPlaytime(stats.playtimeSecondsMonth);

                        String kdRatio = String.format("%.2f", (float) stats.killCount / Math.max(1, stats.deathCount));

                        player.sendMessage(
                                Text.of(
                                        "§8<" + colorCode + "§o✨" + "§r§8> §7§oStats for " + colorCode + "§o" + playerString + "§7§o:\n" +
                                                "    §7Joins: " + colorCode + "§o" + stats.joinCount + "\n" +
                                                "    §7Leaves: " + colorCode + "§o" + stats.leaveCount + "\n" +
                                                "    §7K/D Ratio: " + colorCode + "§o" + kdRatio + "\n" +
                                                "    §7Chats: " + colorCode + "§o" + stats.chatsCount + "\n" +
                                                "    §7Prio: " + colorCode + "§o" + stats.prio + "\n" +
                                                "    §7First Seen: " + colorCode + "§o" + formattedFirstSeen + "\n" +
                                                "    §7Last Seen: " + colorCode + "§o" + formattedLastSeen + "\n" +
                                                "    §7Playtime: " + colorCode + "§o" + formattedPlaytime + "\n" +
                                                "    §7Playtime in last month: " + colorCode + "§o" + formattedPlaytimeInMonth
                                )
                        );
                    } catch (Exception err) {
                        PathSeeker.LOG.error("[Stats2b2t] Failed to deserialize JSON: {}", err.getMessage());
                        error("§7Failed to deserialize response from the server§4..!");
                    }
                }
            });
            return SINGLE_SUCCESS;
        }));
    }

    private record PlayerStats(
            int joinCount,
            int leaveCount,
            int deathCount,
            int killCount,
            String firstSeen,
            String lastSeen,
            long playtimeSeconds,
            long playtimeSecondsMonth,
            int chatsCount,
            boolean prio
    ) {
    }
}