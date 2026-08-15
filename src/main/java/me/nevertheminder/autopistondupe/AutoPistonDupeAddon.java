package me.nevertheminder.autopistondupe;


import me.nevertheminder.autopistondupe.modules.AutoPistonDupe;
import me.nevertheminder.autopistondupe.modules.AutoChestBuilder;
import me.nevertheminder.autopistondupe.modules.ChestWallBot;
import me.nevertheminder.autopistondupe.commands.ChestBotCommand;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AutoPistonDupeAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("PistonDupe");
    public static final HudGroup HUD_GROUP = new HudGroup("PistonDupe");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Auto Piston Dupe Addon");

        // Modules
        Modules.get().add(new AutoPistonDupe());
        Modules.get().add(new AutoChestBuilder());
        Modules.get().add(new ChestWallBot());
        
        // Commands
        Commands.add(new ChestBotCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "me.nevertheminder.autopistondupe";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
