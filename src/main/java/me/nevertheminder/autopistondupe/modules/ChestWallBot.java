package me.nevertheminder.autopistondupe.modules;

import me.nevertheminder.autopistondupe.AutoPistonDupeAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ChestWallBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> distance = sgGeneral.add(new IntSetting.Builder()
        .name("distance")
        .description("Distance to stand from the wall while building.")
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

    private final Setting<Integer> width = sgGeneral.add(new IntSetting.Builder()
        .name("width")
        .description("How wide of a slice to place at once. Keep small to ensure correct chest rotation.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Color of the target wall sides.")
        .defaultValue(new SettingColor(255, 150, 0, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the target wall lines.")
        .defaultValue(new SettingColor(255, 150, 0, 255))
        .build()
    );

    private BlockPos pos1 = null;
    private BlockPos pos2 = null;

    private boolean isXAxis = true;
    private int targetCoord = 0;
    private boolean movingRight = false;
    private float targetYaw = 0;
    private double standCoord = 0;

    public ChestWallBot() {
        super(AutoPistonDupeAddon.CATEGORY, "chest-wall-bot", "Walks sideways perfectly parallel to the wall. Use .chestbot to set points.");
    }

    public void setPos1(BlockPos p) { this.pos1 = p; }
    public void setPos2(BlockPos p) { this.pos2 = p; }

    @Override
    public void onActivate() {
        if (pos1 == null || pos2 == null) {
            error("Please set pos1 and pos2 using '.chestbot pos1' and '.chestbot pos2' first.");
            toggle();
            return;
        }

        Direction dir = mc.player.getHorizontalFacing();
        
        if (dir == Direction.NORTH) targetYaw = 180;
        else if (dir == Direction.SOUTH) targetYaw = 0;
        else if (dir == Direction.EAST) targetYaw = -90;
        else if (dir == Direction.WEST) targetYaw = 90;

        isXAxis = (dir == Direction.NORTH || dir == Direction.SOUTH);
        
        if (isXAxis) {
            standCoord = mc.player.getZ();
            int targetX1 = pos1.getX();
            int targetX2 = pos2.getX();
            
            // Pick the target that is furthest from us
            targetCoord = (Math.abs(mc.player.getX() - targetX1) > Math.abs(mc.player.getX() - targetX2)) ? targetX1 : targetX2;
            
            if (targetYaw == 0) { // South (+Z), Right is West (-X)
                movingRight = (targetCoord < mc.player.getX());
            } else { // North (-Z), Right is East (+X)
                movingRight = (targetCoord > mc.player.getX());
            }
        } else {
            standCoord = mc.player.getX();
            int targetZ1 = pos1.getZ();
            int targetZ2 = pos2.getZ();
            
            targetCoord = (Math.abs(mc.player.getZ() - targetZ1) > Math.abs(mc.player.getZ() - targetZ2)) ? targetZ1 : targetZ2;
            
            if (targetYaw == 90) { // West (-X), Right is North (-Z)
                movingRight = (targetCoord < mc.player.getZ());
            } else { // East (+X), Right is South (+Z)
                movingRight = (targetCoord > mc.player.getZ());
            }
        }

        mc.player.setYaw(targetYaw);

        // Turn on AutoChestBuilder
        Module autoChestBuilder = Modules.get().get(AutoChestBuilder.class);
        if (autoChestBuilder != null && !autoChestBuilder.isActive()) {
            autoChestBuilder.toggle();
        }

        info("ChestWallBot started! Strafing to target...");
    }

    @Override
    public void onDeactivate() {
        mc.options.rightKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        
        // Turn off AutoChestBuilder
        Module autoChestBuilder = Modules.get().get(AutoChestBuilder.class);
        if (autoChestBuilder != null && autoChestBuilder.isActive()) {
            autoChestBuilder.toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (pos1 == null || pos2 == null) return;

        // Force yaw to perfectly face the wall
        mc.player.setYaw(targetYaw);
        
        // Force player to stay on the exact distance line (prevent drifting)
        if (isXAxis) {
            mc.player.setPosition(mc.player.getX(), mc.player.getY(), standCoord);
        } else {
            mc.player.setPosition(standCoord, mc.player.getY(), mc.player.getZ());
        }

        // Press the movement key
        if (movingRight) {
            mc.options.rightKey.setPressed(true);
            mc.options.leftKey.setPressed(false);
        } else {
            mc.options.leftKey.setPressed(true);
            mc.options.rightKey.setPressed(false);
        }

        // Check if we reached the target coordinate
        boolean reached = false;
        if (isXAxis) {
            if ((targetCoord > pos1.getX() && mc.player.getX() >= targetCoord) ||
                (targetCoord < pos1.getX() && mc.player.getX() <= targetCoord)) {
                reached = true;
            }
        } else {
            if ((targetCoord > pos1.getZ() && mc.player.getZ() >= targetCoord) ||
                (targetCoord < pos1.getZ() && mc.player.getZ() <= targetCoord)) {
                reached = true;
            }
        }

        if (reached) {
            info("Wall finished!");
            toggle();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (pos1 != null && pos2 != null) {
            int minX = Math.min(pos1.getX(), pos2.getX());
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());
            
            event.renderer.box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1, sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
        }
    }
}
