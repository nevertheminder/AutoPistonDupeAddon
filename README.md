# AutoPistonDupe Addon for Meteor Client

<div align="center">
  <a href="https://discord.gg/BnCrDsN5ZZ">
    <img src="https://img.shields.io/badge/Discord-Join%20Our%20Community-7289da?style=for-the-badge&logo=discord&logoColor=white" alt="Discord">
  </a>
  <img src="https://img.shields.io/github/v/release/nevertheminder/AutoPistonDupeAddon?style=for-the-badge&color=success" alt="Release">
  <img src="https://img.shields.io/github/downloads/nevertheminder/AutoPistonDupeAddon/total?style=for-the-badge&color=blue" alt="Downloads">
  <img src="https://img.shields.io/github/stars/nevertheminder/AutoPistonDupeAddon?style=for-the-badge&color=yellow" alt="Stars">
  <img src="https://img.shields.io/github/issues/nevertheminder/AutoPistonDupeAddon?style=for-the-badge&color=red" alt="Issues">
</div>

An automated shulker duping addon for Meteor Client, designed to place shulkers in front of pistons and seamlessly store them in nearby chests when your inventory fills up.

## Video Tutorial & Showcase
[![YouTube Guide](https://img.youtube.com/vi/Sf_1_7O-i8o/maxresdefault.jpg)](https://www.youtube.com/watch?v=Sf_1_7O-i8o)
*(Click the image to watch the full setup and usage guide!)*

## Features
- **Auto Placement**: Automatically places shulkers from your hotbar or inventory in up to 6 configurable locations.
- **Anti-Ghost Item**: Advanced background synchronization that constantly refreshes your inventory so you never get stuck on invisible shulkers.
- **Auto Storing (Dumping)**: When your inventory gets full, the addon automatically uses Baritone to walk to nearby chests and offload the shulkers, keeping a configurable amount to continue duping.
- **Smart Chest Memory**: Remembers which chests are full and skips them automatically.
- **Flexible Area Modes**: Define your chest storage using a simple Radius or by setting two corner points (TwoPoints mode) for precise storage rooms.
- **Place-Only Mode**: A toggle for users who just want to stand still and place shulkers at lightning speed without pathing to chests.
- **Reach Support**: Fully configurable reach distance so you don't have to hug the chests to open them.
- **Customizable Visuals**: Renders your placement targets and chest storage area with customizable colors.

## Installation / Download
You can download the ready-to-use mod file directly from the [Releases](https://github.com/nevertheminder/AutoPistonDupeAddon/releases) page!

**For beginners:**
1. Go to the [Releases](https://github.com/nevertheminder/AutoPistonDupeAddon/releases) page and download `autopistondupe-1.0.0.jar`.
2. Move the downloaded `.jar` file into your `.minecraft/mods` folder.
3. Make sure you also have the following installed in your `mods` folder:
   - **Meteor Client** (Fabric 1.21.4 version): [Download here](https://meteorclient.com/)
   - **Baritone** (Fabric 1.21.4 version): [Download here](https://github.com/cabaletta/baritone)
   - **Fabric API** (1.21.4 version)
4. Launch the game and open the Meteor GUI (usually Right Shift).
5. You will find the module under the **PistonDupe** category!

## Building Manually
If you want to build the code yourself:
1. Clone this repository.
2. Run `.\gradlew build` (Windows) or `./gradlew build` (Linux/Mac).
3. The compiled jar will be located in the `build/libs/` folder.

## Community & Support
Join our Discord server to get help, report bugs, and stay updated on new features!
💬 **[Join the Discord](https://discord.gg/BnCrDsN5ZZ)**

## Contributors
- **[nevertheminder](https://github.com/nevertheminder)** - Lead Developer & Creator

*Feel free to open a Pull Request if you want to contribute to the project!*
