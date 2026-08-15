package me.nevertheminder.autopistondupe.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.nevertheminder.autopistondupe.modules.ChestWallBot;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ChestBotCommand extends Command {
    public ChestBotCommand() {
        super("chestbot", "Sets coordinates for ChestWallBot.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("pos1")
                .executes(context -> {
                    ChestWallBot module = Modules.get().get(ChestWallBot.class);
                    if (module == null) {
                        error("Module not found");
                        return SINGLE_SUCCESS;
                    }
                    if (mc.player != null) {
                        module.setPos1(mc.player.getBlockPos());
                        info("Pos1 set to " + mc.player.getBlockPos().toShortString());
                    }
                    return SINGLE_SUCCESS;
                }))
                .then(literal("pos2")
                .executes(context -> {
                    ChestWallBot module = Modules.get().get(ChestWallBot.class);
                    if (module == null) {
                        error("Module not found");
                        return SINGLE_SUCCESS;
                    }
                    if (mc.player != null) {
                        module.setPos2(mc.player.getBlockPos());
                        info("Pos2 set to " + mc.player.getBlockPos().toShortString());
                    }
                    return SINGLE_SUCCESS;
                }));
    }
}
