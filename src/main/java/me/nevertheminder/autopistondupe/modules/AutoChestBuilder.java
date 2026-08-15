package me.nevertheminder.autopistondupe.modules;

import me.nevertheminder.autopistondupe.AutoPistonDupeAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoChestBuilder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> width = sgGeneral.add(new IntSetting.Builder()
        .name("width")
        .description("How many chests wide to scan around you.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("How many chests high to build.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Integer> distance = sgGeneral.add(new IntSetting.Builder()
        .name("distance")
        .description("Distance from player to place chests.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 4)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between placing blocks.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many chests to place per tick.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Spoof rotation when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color")
        .description("Color of the target block being placed.")
        .defaultValue(new SettingColor(255, 150, 0, 100))
        .build()
    );

    private int ticks;
    private BlockPos currentTarget = null;

    public AutoChestBuilder() {
        super(AutoPistonDupeAddon.CATEGORY, "auto-chest-builder", "Continuously builds a wall of chests as you walk.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
        currentTarget = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        currentTarget = null;

        if (ticks > 0) {
            ticks--;
            return;
        }

        FindItemResult chest = InvUtils.findInHotbar(Items.CHEST);
        if (!chest.found()) return;

        Direction dir = mc.player.getHorizontalFacing();
        BlockPos center = mc.player.getBlockPos().offset(dir, distance.get());
        Direction rightDir = dir.rotateYClockwise();

        int halfWidth = width.get() / 2;
        int startX = -halfWidth;
        int endX = width.get() - halfWidth - (width.get() % 2 == 0 ? 0 : 1);

        int blocksPlaced = 0;

        // Scan area
        for (int y = 0; y < height.get(); y++) {
            for (int x = startX; x <= endX; x++) {
                BlockPos pos = center.offset(rightDir, x).up(y);

                if (BlockUtils.canPlace(pos, true)) {
                    currentTarget = pos;
                    
                    if (BlockUtils.place(pos, chest, rotate.get(), 50, true)) {
                        blocksPlaced++;
                        if (blocksPlaced >= bpt.get()) {
                            ticks = delay.get();
                            return;
                        }
                    }
                }
            }
        }

        if (blocksPlaced > 0) {
            ticks = delay.get();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (currentTarget != null) {
            event.renderer.box(currentTarget, color.get(), color.get(), ShapeMode.Both, 0);
        }
    }
}
