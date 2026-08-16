package me.nevertheminder.autopistondupe.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import me.nevertheminder.autopistondupe.AutoPistonDupeAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoPistonDupe extends Module {
    public enum State {
        DUPING,
        DUMPING
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<BlockPos> targetBlock1 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("target-block-1")
        .description("First exact block to place the shulker.")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    public final Setting<BlockPos> targetBlock2 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("target-block-2")
        .description("Second exact block to place the shulker.")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    public final Setting<BlockPos> targetBlock3 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("target-block-3")
        .description("Third exact block to place the shulker.")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    public final Setting<BlockPos> targetBlock4 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("target-block-4")
        .description("Fourth exact block to place the shulker.")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    public final Setting<BlockPos> targetBlock5 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("target-block-5")
        .description("Fifth exact block to place the shulker.")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    public final Setting<BlockPos> targetBlock6 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("target-block-6")
        .description("Sixth exact block to place the shulker.")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    public final Setting<BlockPos> standPosition = sgGeneral.add(new BlockPosSetting.Builder()
        .name("stand-position")
        .description("The exact block where the player should stand.")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );

    public enum ChestAreaMode {
        Radius,
        TwoPoints
    }

    public final Setting<ChestAreaMode> chestMode = sgGeneral.add(new EnumSetting.Builder<ChestAreaMode>()
        .name("chest-area-mode")
        .description("How to define the chest area.")
        .defaultValue(ChestAreaMode.Radius)
        .build()
    );

    public final Setting<BlockPos> chestOrigin = sgGeneral.add(new BlockPosSetting.Builder()
        .name("chest-origin")
        .description("The center block to search for chests from.")
        .defaultValue(new BlockPos(0, 0, 0))
        .visible(() -> chestMode.get() == ChestAreaMode.Radius)
        .build()
    );

    public final Setting<Integer> chestRadius = sgGeneral.add(new IntSetting.Builder()
        .name("chest-radius")
        .description("Radius to search for chests.")
        .defaultValue(10)
        .min(1)
        .sliderMax(30)
        .visible(() -> chestMode.get() == ChestAreaMode.Radius)
        .build()
    );

    public final Setting<BlockPos> chestPos1 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("chest-pos-1")
        .description("First corner of the chest area.")
        .defaultValue(new BlockPos(0, 0, 0))
        .visible(() -> chestMode.get() == ChestAreaMode.TwoPoints)
        .build()
    );

    public final Setting<BlockPos> chestPos2 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("chest-pos-2")
        .description("Second corner of the chest area.")
        .defaultValue(new BlockPos(0, 0, 0))
        .visible(() -> chestMode.get() == ChestAreaMode.TwoPoints)
        .build()
    );

    public final Setting<Integer> keepAmount = sgGeneral.add(new IntSetting.Builder()
        .name("keep-amount")
        .description("How many shulker boxes to keep in inventory.")
        .defaultValue(1)
        .min(0)
        .sliderMax(5)
        .build()
    );

    public final Setting<Boolean> placeOnlyMode = sgGeneral.add(new BoolSetting.Builder()
        .name("place-only-mode")
        .description("Only places shulkers. Disables walking and dumping to chests.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Double> chestReach = sgGeneral.add(new DoubleSetting.Builder()
        .name("chest-reach")
        .description("How far you can reach to open a chest.")
        .defaultValue(4.0)
        .min(1.0)
        .sliderMax(10.0)
        .build()
    );

    public final Setting<Double> placeDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-delay")
        .description("Delay in ticks before placing the next shulker (supports decimals like 1.5).")
        .defaultValue(2.0)
        .min(0.0)
        .sliderMax(20.0)
        .build()
    );

    public final Setting<Integer> dumpDelay = sgGeneral.add(new IntSetting.Builder()
        .name("dump-delay")
        .description("Delay in ticks between moving items to chest.")
        .defaultValue(2)
        .min(0)
        .sliderMax(10)
        .build()
    );

    public final Setting<Integer> syncDelay = sgGeneral.add(new IntSetting.Builder()
        .name("ghost-sync-delay")
        .description("Ticks between ghost item checks (lower is faster but sends more packets).")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );

    // Render settings
    private final Setting<Boolean> renderLines = sgRender.add(new BoolSetting.Builder()
        .name("render-lines")
        .description("Renders lines to targets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderChestArea = sgRender.add(new BoolSetting.Builder()
        .name("render-chest-area")
        .description("Renders a square box around the chest searching area.")
        .defaultValue(true)
        .build()
    );

    private final Setting<meteordevelopment.meteorclient.utils.render.color.SettingColor> areaSideColor = sgRender.add(new ColorSetting.Builder()
        .name("area-side-color")
        .description("The side color for the chest area.")
        .defaultValue(new meteordevelopment.meteorclient.utils.render.color.SettingColor(255, 255, 255, 50))
        .visible(renderChestArea::get)
        .build()
    );

    private final Setting<meteordevelopment.meteorclient.utils.render.color.SettingColor> areaLineColor = sgRender.add(new ColorSetting.Builder()
        .name("area-line-color")
        .description("The line color for the chest area.")
        .defaultValue(new meteordevelopment.meteorclient.utils.render.color.SettingColor(255, 255, 255, 255))
        .visible(renderChestArea::get)
        .build()
    );

    private State state = State.DUPING;
    private double timer = 0;
    private final Set<BlockPos> fullChests = new HashSet<>();
    private BlockPos currentChest = null;
    private boolean isPathing = false;
    private int syncSlot = 9;

    public AutoPistonDupe() {
        super(AutoPistonDupeAddon.CATEGORY, "auto-piston-dupe", "Automates piston duping and dumping into chests.");
    }

    @Override
    public void onActivate() {
        state = State.DUPING;
        timer = 0;
        fullChests.clear();
        currentChest = null;
        isPathing = false;
        syncSlot = 9;
    }

    @Override
    public void onDeactivate() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (baritone != null) {
            baritone.getPathingBehavior().cancelEverything();
        }
    }

    private boolean isShulker(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof ShulkerBoxBlock;
    }

    private int getShulkerCount() {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (isShulker(mc.player.getInventory().main.get(i))) {
                count++;
            }
        }
        return count;
    }

    private boolean isInventoryFull() {
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (mc.player.getInventory().main.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        
        if (timer > 0) {
            timer -= 1.0;
            return;
        }

        int shulkers = getShulkerCount();

        if (state == State.DUPING) {
            if (!placeOnlyMode.get() && isInventoryFull()) {
                state = State.DUMPING;
                baritone.getPathingBehavior().cancelEverything();
                isPathing = false;
                return;
            }

            // Anti-ghost item background sync
            if (syncDelay.get() == 0 || mc.player.age % syncDelay.get() == 0) { // Configurable sync delay
                for (int count = 0; count < 36; count++) {
                    syncSlot++;
                    if (syncSlot > 44) syncSlot = 9;
                    if (mc.player.playerScreenHandler.getSlot(syncSlot).getStack().isEmpty()) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, syncSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, syncSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                    }
                }
            }

            BlockPos standPos = standPosition.get();
            
            // Check if we are exactly at standPos
            if (!placeOnlyMode.get() && !mc.player.getBlockPos().equals(standPos)) {
                if (!isPathing || !baritone.getPathingBehavior().isPathing()) {
                    baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(standPos));
                    isPathing = true;
                }
                return;
            } else {
                if (isPathing) {
                    baritone.getPathingBehavior().cancelEverything();
                    isPathing = false;
                }
            }

            // Place shulkers
            boolean placedAny = false;
            BlockPos[] targets = new BlockPos[]{targetBlock1.get(), targetBlock2.get(), targetBlock3.get(), targetBlock4.get(), targetBlock5.get(), targetBlock6.get()};
            for (BlockPos target : targets) {
                if (target == null || target.equals(BlockPos.ORIGIN)) continue;
                
                if (mc.world.getBlockState(target).isReplaceable()) {
                    FindItemResult shulkerRes = InvUtils.findInHotbar(itemStack -> isShulker(itemStack));
                    if (shulkerRes.found()) {
                        BlockUtils.place(target, shulkerRes, false, 50, true, true, true);
                        placedAny = true;
                    } else {
                        // Try to move from inventory to hotbar
                        FindItemResult invShulker = InvUtils.find(itemStack -> isShulker(itemStack));
                        if (invShulker.found()) {
                            InvUtils.move().from(invShulker.slot()).toHotbar(mc.player.getInventory().selectedSlot);
                            timer = 5;
                            break;
                        } else {
                            // Fix ghost items by clicking slots
                            int slotToClick = syncSlot; 
                            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slotToClick, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slotToClick, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player);
                            syncSlot++;
                            if (syncSlot > 44) syncSlot = 9;
                            timer = 5; // Wait 5 ticks before trying the next slot
                            break;
                        }
                    }
                }
            }
            if (placedAny) {
                // Prevent huge negative buildup if we haven't placed in a while
                if (timer < -10) timer = 0;
                timer += placeDelay.get();
            }

        } else if (state == State.DUMPING) {
            if (shulkers <= keepAmount.get()) {
                state = State.DUPING;
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    mc.player.closeHandledScreen();
                }
                baritone.getPathingBehavior().cancelEverything();
                isPathing = false;
                currentChest = null;
                return;
            }

            // We are dumping. 
            // If screen is open, dump.
            if (mc.currentScreen instanceof GenericContainerScreen) {
                GenericContainerScreenHandler handler = ((GenericContainerScreen) mc.currentScreen).getScreenHandler();
                
                // Find first shulker in player inventory
                int containerSize = handler.getInventory().size(); // e.g. 27 for small chest, 54 for large chest
                
                // Check if chest is full
                boolean chestFull = true;
                for (int i = 0; i < containerSize; i++) {
                    if (handler.getSlot(i).getStack().isEmpty()) {
                        chestFull = false;
                        break;
                    }
                }

                if (chestFull) {
                    if (currentChest != null) fullChests.add(currentChest);
                    mc.player.closeHandledScreen();
                    currentChest = null;
                    return;
                }

                // Dump
                for (int i = containerSize; i < handler.slots.size(); i++) {
                    Slot slot = handler.getSlot(i);
                    if (isShulker(slot.getStack())) {
                        if (getShulkerCount() <= keepAmount.get()) break;
                        
                        // Shift click
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                        timer = dumpDelay.get();
                        if (timer > 0) return; // Only return if we have a delay. Otherwise, keep dumping!
                    }
                }
                return;
            }

            // Find chest
            if (currentChest == null) {
                currentChest = findNearestAvailableChest();
                if (currentChest == null) {
                    error("No available chests found in radius!");
                    toggle();
                    return;
                }
            }

            double dist = mc.player.getBlockPos().getSquaredDistance(currentChest);
            if (dist > chestReach.get() * chestReach.get()) {
                if (!isPathing || !baritone.getPathingBehavior().isPathing()) {
                    baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(currentChest, (int)Math.max(1, chestReach.get() - 1)));
                    isPathing = true;
                }
            } else {
                if (isPathing) {
                    baritone.getPathingBehavior().cancelEverything();
                    isPathing = false;
                }
                
                // Open chest
                Vec3d hitVec = new Vec3d(currentChest.getX() + 0.5, currentChest.getY() + 0.5, currentChest.getZ() + 0.5);
                BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, currentChest, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                timer = 10; // Wait a bit for GUI to open
            }
        }
    }

    private BlockPos findNearestAvailableChest() {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        int minX, minY, minZ, maxX, maxY, maxZ;

        if (chestMode.get() == ChestAreaMode.Radius) {
            BlockPos origin = chestOrigin.get();
            int r = chestRadius.get();
            minX = origin.getX() - r;
            minY = origin.getY() - r;
            minZ = origin.getZ() - r;
            maxX = origin.getX() + r;
            maxY = origin.getY() + r;
            maxZ = origin.getZ() + r;
        } else {
            BlockPos p1 = chestPos1.get();
            BlockPos p2 = chestPos2.get();
            minX = Math.min(p1.getX(), p2.getX());
            minY = Math.min(p1.getY(), p2.getY());
            minZ = Math.min(p1.getZ(), p2.getZ());
            maxX = Math.max(p1.getX(), p2.getX());
            maxY = Math.max(p1.getY(), p2.getY());
            maxZ = Math.max(p1.getZ(), p2.getZ());
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (fullChests.contains(p)) continue;

                    net.minecraft.block.BlockState state = mc.world.getBlockState(p);
                    if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
                        double d = mc.player.getBlockPos().getSquaredDistance(p);
                        if (d < bestDist) {
                            bestDist = d;
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        BlockPos[] targets = new BlockPos[]{targetBlock1.get(), targetBlock2.get(), targetBlock3.get(), targetBlock4.get(), targetBlock5.get(), targetBlock6.get()};
        for (BlockPos target : targets) {
            if (target != null && !target.equals(BlockPos.ORIGIN)) {
                event.renderer.box(target, meteordevelopment.meteorclient.utils.render.color.Color.GREEN, meteordevelopment.meteorclient.utils.render.color.Color.GREEN, ShapeMode.Lines, 0);
            }
        }

        if (standPosition.get() != null) {
            event.renderer.box(standPosition.get(), meteordevelopment.meteorclient.utils.render.color.Color.BLUE, meteordevelopment.meteorclient.utils.render.color.Color.BLUE, ShapeMode.Lines, 0);
        }

        if (renderChestArea.get()) {
            if (chestMode.get() == ChestAreaMode.Radius && chestOrigin.get() != null) {
                BlockPos o = chestOrigin.get();
                int r = chestRadius.get();
                Box box = new Box(o.getX() - r, o.getY() - r, o.getZ() - r, o.getX() + r + 1, o.getY() + r + 1, o.getZ() + r + 1);
                event.renderer.box(box, areaSideColor.get(), areaLineColor.get(), ShapeMode.Both, 0);
            } else if (chestMode.get() == ChestAreaMode.TwoPoints && chestPos1.get() != null && chestPos2.get() != null) {
                BlockPos p1 = chestPos1.get();
                BlockPos p2 = chestPos2.get();
                int minX = Math.min(p1.getX(), p2.getX());
                int minY = Math.min(p1.getY(), p2.getY());
                int minZ = Math.min(p1.getZ(), p2.getZ());
                int maxX = Math.max(p1.getX(), p2.getX());
                int maxY = Math.max(p1.getY(), p2.getY());
                int maxZ = Math.max(p1.getZ(), p2.getZ());
                Box box = new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
                event.renderer.box(box, areaSideColor.get(), areaLineColor.get(), ShapeMode.Both, 0);
            }
        }

        if (renderLines.get()) {
            if (state == State.DUPING) {
                for (BlockPos target : targets) {
                    if (target != null && !target.equals(BlockPos.ORIGIN)) {
                        event.renderer.line(mc.player.getPos().x, mc.player.getPos().y, mc.player.getPos().z, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, meteordevelopment.meteorclient.utils.render.color.Color.GREEN);
                    }
                }
            } else if (state == State.DUMPING && currentChest != null) {
                event.renderer.line(mc.player.getPos().x, mc.player.getPos().y, mc.player.getPos().z, currentChest.getX() + 0.5, currentChest.getY() + 0.5, currentChest.getZ() + 0.5, meteordevelopment.meteorclient.utils.render.color.Color.RED);
            }
        }
    }
}
