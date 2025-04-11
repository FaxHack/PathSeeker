# PathSeeker

*A Minecraft utility mod built on the foundation of TrouserStreak, reimagined and enhanced for advanced anarchy server
gameplay. Whether you're hunting bases, exploring new lands, or managing your resources, PathSeeker provides the tools
to stay ahead.*

> [!IMPORTANT]  
> Make sure to install
> Baritone, [1.21.1](https://maven.meteordev.org/snapshots/meteordevelopment/baritone/1.21.1-SNAPSHOT/baritone-1.21.1-20240826.213754-1.jar) [1.21.4](https://maven.meteordev.org/snapshots/meteordevelopment/baritone/1.21.4-SNAPSHOT/baritone-1.21.4-20250105.184728-1.jar)
> else it will crash.

---

## Features

PathSeeker comes packed with powerful features designed for Minecraft explorers and thrill-seekers:

- **Base Finder**  
  Uncover hidden bases and dungeons effortlessly, giving you the edge in anarchy gameplay.

- **Pitch40Util**
  Used alongside Meteor's pitch 40. Auto sets min and max bounds so that you continue to gain height. It also has an
  auto firework mode for when you lose
  velocity. [Jeff-Mod](https://github.com/miles352/meteor-stashhunting-addon/blob/1.21.1/src/main/java/com/stash/hunt/modules/Pitch40Util.java)

- **TrailFollower** (Requires XaeroPlus)
  Follows trails in all dimensions using either pitch40 or baritone. It may break on path splits or other
  cases. [Jeff-Mod](https://github.com/miles352/meteor-stashhunting-addon/blob/1.21.1/src/main/java/com/stash/hunt/modules/TrailFollower.java)

- **StackedMinecartDetector**
  Detects if a stacked spawner is in the area.

- **EntityClusterESP**
  Highlights the centre point of entity clusters.

- **DroppedItemESP**
  Highlights important dropped items.

- **BetterStashFinder:** An upgraded stash finder with enhanced filtering
  options. [Jeff-Mod](https://github.com/miles352/meteor-stashhunting-addon/blob/1.21.1/src/main/java/com/stash/hunt/modules/BetterStashFinder.java)

- **AutoEnchanter:** Automates enchanting at anvils, saving time and effort.

- **GrimEfly:** Vanilla efly uses a chest-plate to ensure that elytra do not consume
  durability. [Jeff-Mod](https://github.com/miles352/meteor-stashhunting-addon/blob/1.21.1/src/main/java/com/stash/hunt/modules/GrimEfly.java)

- **Trident Dupe:** Facilitates trident duplication (note: patched on most servers).

- **AutoFirework:** Automatically activates firework use for added flair.

- **ScreenshotFolderCommand**
  Open the Minecraft screenshot folder.

- **MeteorFolderCommand**
  Open meteor-client folder.

> [!TIP]  
> Use Base Finder with NewerNewChunks to drastically improve your chances of locating uncharted bases.

> [!WARNING]  
> Make sure your predefined item lists are accurate. Incorrect configurations may result in missing valuable loot.

- **GrimDuraFirework**  
  A clever fix for fireworks issues on the Mio client when using Grim Durability ElytraFly with a chestplate. Fly
  smoothly without a
  hitch! [Credits to meteor-mod](https://github.com/miles352/meteor-stashhunting-addon/blob/master/src/main/java/com/example/addon/modules/GrimDuraFirework.java)

- **MobGearESP**  
  Detects mobs carrying or wearing player
  equipment. [Credits to windoid](https://github.com/windoid/MobGearESP/blob/master/src/main/java/com/mobgear/addon/modules/MobGearESP.java)

- **SignHistorian**  
  Records & restores broken or modified
  signs. [Credits to Stardust](https://github.com/0xTas/stardust/blob/64cd499c62d30be8e479b084a613e0c05b77c8d9/src/main/java/dev/stardust/modules/SignHistorian.java)

- **NewerNewChunks**  
  Take control of chunk tracing! Manage chunk data across servers and dimensions with crash-resilient saving, easy
  sharing options, and pinpoint accuracy.

> [!IMPORTANT]  
> To share your chunk data with friends, copy the `PathSeeker/NewChunks` folder and send it directly.

- **Hole/Tunnel/StairsESP**  
  Spot 1x1 holes, horizontal tunnels, and staircase tunnels like a pro. By default, it skips passable blocks like
  torches or water but can be configured to detect only air. (Credits to Meteor Client and etianl)

- **PotESP**  
  Detect Decorated Pots with unnatural contents and find out what's inside! Easily locate suspicious pots in your
  environment. [Credits to etianl](https://github.com/etianl/Trouser-Streak/blob/1.21.1/src/main/java/pwn/noobs/trouserstreak/modules/PotESP.java)

- **PortalPatternFinder**  
  Scan for shapes of broken or removed Nether Portals within cave air blocks. Great for tracking portal skips in the
  Nether for 1.13+
  chunks. [Credits to etianl](https://github.com/etianl/Trouser-Streak/blob/1.21.1/src/main/java/pwn/noobs/trouserstreak/modules/PortalPatternFinder.java)

> [!NOTE]  
> PortalPatternFinder works best when paired with advanced chunk detection tools.

- **CaveDisturbanceDetector**  
  Hunt for single air blocks hidden within cave air, helping you identify disturbances in 1.13+ underground
  structures. [Credits to etianl](https://github.com/etianl/Trouser-Streak/blob/1.21.1/src/main/java/pwn/noobs/trouserstreak/modules/PortalPatternFinder.java)

> [!CAUTION]  
> Scanning large areas with CaveDisturbanceDetector may increase resource usage. Adjust your settings accordingly.

---

## Why Choose This Mod?

PathSeeker brings the famed TrouserStreak to entirely new heights. With innovative features, refined optimisations and
an emphasis on unmatched stability and performance, this mod is the ideal companion for anarchy server gameplay. Whether
you're base raiding, chunk tracing or treasure hunting, PathSeeker empowers your journey.

---

## Installation

Getting started is simple:

1. **Download** the latest release from [Releases](https://github.com/FaxHack/PathSeeker/releases/).
2. **Place** the mod `.jar` file in your Minecraft `mods` folder.
3. **Launch** Minecraft and dive into the chaos!

> [!NOTE]  
> Ensure you’re using the correct version of Minecraft (1.21.x or compatible) for the best experience.

---

## Requirements

- **Minecraft 1.21.x** or compatible versions.
- [Baritone](https://modrinth.com/mod/xaeros-minimap/versions) --> [1.21.1](https://maven.meteordev.org/snapshots/meteordevelopment/baritone/1.21.1-SNAPSHOT/baritone-1.21.1-20240826.213754-1.jar), [1.21.4](https://maven.meteordev.org/snapshots/meteordevelopment/baritone/1.21.4-SNAPSHOT/baritone-1.21.4-20250105.184728-1.jar)
- [**ViaFabricPlus**](https://modrinth.com/mod/viafabricplus/versions?g=1.21.1&l=fabric&c=release) *(optional for
  cross-version compatibility)*.
- [**XearosPlus**](https://modrinth.com/mod/xaeroplus/versions)
- [**XearosWorldMap**](https://modrinth.com/mod/xaeros-world-map/versions)
- [**XearosMiniMap**](https://modrinth.com/mod/xaeros-minimap/versions)

---

## Roadmap

PathSeeker is always evolving! Here's what's planned:

- [x] Core features for the initial release.
- [x] New hunting modules for advanced gameplay.
- [x] Enhanced automation tools for loot and base management.

---

## Credits

A big shoutout to the creators and projects that inspired PathSeeker:

- [**TrouserStreak**](https://github.com/etianl/Trouser-Streak) for laying the groundwork.
- [**meteor-mod**](https://github.com/miles352/meteor-stashhunting-addon) for hunting modules.
- [**Stardust**](https://github.com/0xTas/stardust) for additional features.

---

## Contributing

We welcome contributions! If you have ideas, fixes, or new features to share, feel free to open issues or submit pull
requests.

> [!TIP]  
> Review our coding guidelines for smoother collaboration before submitting a pull request.

---

## License

Licensed under **Apache-2.0**.

---

## Support

Need help or have questions?

- Join our **[Discord](https://discord.gg/SdH8ZF96mD)** community.
- Open an issue directly on **GitHub**.  
## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=FaxHack/PathSeeker&type=Date)](https://www.star-history.com/#FaxHack/PathSeeker&Date)
