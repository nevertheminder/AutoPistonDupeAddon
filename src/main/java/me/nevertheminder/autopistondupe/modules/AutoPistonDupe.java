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
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
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
import java.util.Comparator;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.MeteorClient;

public class AutoPistonDupe extends Module {
    public enum State {
        DUPING,
        DUMPING
    }

    public enum ChestFillOrder {
        None,
        Closest,
        ColumnsBottomToTop,
        ColumnsTopToBottom,
        LayersBottomToTop,
        LayersTopToBottom,
        LeftToRight,
        RightToLeft
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

    public final Setting<ChestFillOrder> fillOrder = sgGeneral.add(new EnumSetting.Builder<ChestFillOrder>()
        .name("chest-fill-order")
        .description("The order in which to fill chests.")
        .defaultValue(ChestFillOrder.ColumnsBottomToTop)
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

    public final Setting<Integer> interactDelay = sgGeneral.add(new IntSetting.Builder()
        .name("interact-delay")
        .description("Delay in ticks to wait after clicking a chest before moving items. (1 tick = 50ms)")
        .defaultValue(5)
        .min(0)
        .sliderMax(20)
        .build()
    );

    public final Setting<Double> syncDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("ghost-sync-delay")
        .description("Ticks between ghost item checks (lower is faster but sends more packets, supports decimals).")
        .defaultValue(2.0)
        .min(0.0)
        .sliderMax(20.0)
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
    private double syncTimer = 0;
    private final Set<BlockPos> fullChests = new HashSet<>();
    private static final File MEMORY_FILE = new File(MeteorClient.FOLDER, "autopistondupe_chest_memory.json");
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private BlockPos currentChest = null;
    private boolean isPathing = false;
    private int syncSlot = 9;
    private int stuckCounter = 0;

    public AutoPistonDupe() {
        super(AutoPistonDupeAddon.CATEGORY, "auto-piston-dupe", "Automates piston duping and dumping into chests.");
        loadMemory();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton clearBtn = table.add(theme.button("**CLEAR CHEST MEMORY**")).expandX().minWidth(100).widget();
        clearBtn.action = () -> {
            fullChests.clear();
            if (MEMORY_FILE.exists()) MEMORY_FILE.delete();
            info("Persistent chest memory cleared.");
        };
        return table;
    }

    private void saveMemory() {
        try {
            if (!MEMORY_FILE.getParentFile().exists()) MEMORY_FILE.getParentFile().mkdirs();
            List<Long> list = fullChests.stream().map(BlockPos::asLong).collect(Collectors.toList());
            try (FileWriter writer = new FileWriter(MEMORY_FILE)) {
                GSON.toJson(list, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMemory() {
        if (!MEMORY_FILE.exists()) return;
        try (FileReader reader = new FileReader(MEMORY_FILE)) {
            Type type = new TypeToken<List<Long>>(){}.getType();
            List<Long> list = GSON.fromJson(reader, type);
            if (list != null) {
                fullChests.clear();
                for (Long l : list) {
                    fullChests.add(BlockPos.fromLong(l));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivate() {
        state = State.DUPING;
        timer = 0;
        syncTimer = 0;
        currentChest = null;
        isPathing = false;
        syncSlot = 9;
        loadMemory(); // Reload memory just in case it was edited externally
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
            // Anti-ghost GUI: If the server opens a chest late due to ping, close it instantly.
            if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.GenericContainerScreen) {
                mc.player.closeHandledScreen();
            }

            if (!placeOnlyMode.get() && isInventoryFull()) {
                state = State.DUMPING;
                baritone.getPathingBehavior().cancelEverything();
                isPathing = false;
                return;
            }

            // Anti-ghost item background sync
            syncTimer -= 1.0;
            if (syncTimer <= 0) {
                if (syncTimer < -10) syncTimer = 0; // prevent huge negative buildup
                syncTimer += syncDelay.get();
                if (syncDelay.get() == 0) syncTimer = 0; // if 0, run every tick

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
                stuckCounter = 0; // Successfully opened GUI, reset stuck counter
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
                    if (currentChest != null) {
                        fullChests.add(currentChest);
                        saveMemory();
                    }
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

            Vec3d hitVec = new Vec3d(currentChest.getX() + 0.5, currentChest.getY() + 0.5, currentChest.getZ() + 0.5);
            double dist = mc.player.getEyePos().distanceTo(hitVec);
            
            double interactReach = 4.4; // Maximum distance to open the chest (server max is 4.5)
            double pathingReach = 4.0; // Strict distance Baritone must achieve to satisfy the goal
            
            if (dist > interactReach) {
                if (!isPathing || !baritone.getPathingBehavior().isPathing()) {
                    Goal reachGoal = new Goal() {
                        @Override
                        public boolean isInGoal(int x, int y, int z) {
                            double dx = (x + 0.5) - (currentChest.getX() + 0.5);
                            double dz = (z + 0.5) - (currentChest.getZ() + 0.5);
                            double dist2d = Math.sqrt(dx * dx + dz * dz);
                            
                            // Must be horizontally adjacent to the chest column (dist2d <= 1.2)
                            // and player must remain roughly on the same floor level
                            return dist2d <= 1.2 && Math.abs(y - mc.player.getY()) <= 2;
                        }
                        @Override
                        public double heuristic(int x, int y, int z) {
                            double dx = (x + 0.5) - (currentChest.getX() + 0.5);
                            double dz = (z + 0.5) - (currentChest.getZ() + 0.5);
                            return Math.sqrt(dx * dx + dz * dz);
                        }
                    };
                    baritone.getCustomGoalProcess().setGoalAndPath(reachGoal);
                    isPathing = true;
                }
            } else {
                if (isPathing) {
                    baritone.getPathingBehavior().cancelEverything();
                    isPathing = false;
                }
                
                // Calculate which horizontal face is closest to the player
                double dx = mc.player.getX() - (currentChest.getX() + 0.5);
                double dz = mc.player.getZ() - (currentChest.getZ() + 0.5);
                Direction side;
                if (Math.abs(dx) > Math.abs(dz)) {
                    side = dx > 0 ? Direction.EAST : Direction.WEST;
                } else {
                    side = dz > 0 ? Direction.SOUTH : Direction.NORTH;
                }
                
                // Adjust hitVec to point to the actual physical boundary of the chest.
                // A standard chest collision box is not a full 1x1x1 block.
                // It is 14/16 wide/long (0.875) and 14/16 tall. The center offset is 0.4375.
                Vec3d faceHitVec = new Vec3d(
                    currentChest.getX() + 0.5 + side.getOffsetX() * 0.4375,
                    currentChest.getY() + 0.4375,
                    currentChest.getZ() + 0.5 + side.getOffsetZ() * 0.4375
                );
                
                BlockHitResult hitResult = new BlockHitResult(faceHitVec, side, currentChest, false);
                
                Rotations.rotate(Rotations.getYaw(faceHitVec), Rotations.getPitch(faceHitVec), () -> {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                });
                
                stuckCounter++;
                if (stuckCounter > 5) {
                    // Desync detected. Nudge the player slightly forward to force a movement packet sync
                    Vec3d lookVec = mc.player.getRotationVector();
                    mc.player.setPos(mc.player.getX() + lookVec.x * 0.3, mc.player.getY(), mc.player.getZ() + lookVec.z * 0.3);
                    stuckCounter = 0;
                    info("Fixing server desync automatically...");
                }
                
                timer = interactDelay.get(); // Wait for GUI to open based on user setting
            }
        }
    }

    private BlockPos findNearestAvailableChest() {
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

        List<BlockPos> validChests = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (fullChests.contains(p)) continue;

                    net.minecraft.block.BlockState state = mc.world.getBlockState(p);
                    if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
                        validChests.add(p);
                    }
                }
            }
        }
        
        if (validChests.isEmpty()) return null;

        BlockPos playerPos = mc.player.getBlockPos();
        ChestFillOrder order = fillOrder.get();

        if (order != ChestFillOrder.None) {
            validChests.sort((p1, p2) -> {
                if (order == ChestFillOrder.Closest) {
                    return Double.compare(p1.getSquaredDistance(playerPos), p2.getSquaredDistance(playerPos));
                } else if (order == ChestFillOrder.ColumnsBottomToTop || order == ChestFillOrder.ColumnsTopToBottom) {
                    double dist2d1 = Math.pow(p1.getX() - playerPos.getX(), 2) + Math.pow(p1.getZ() - playerPos.getZ(), 2);
                    double dist2d2 = Math.pow(p2.getX() - playerPos.getX(), 2) + Math.pow(p2.getZ() - playerPos.getZ(), 2);
                    if (Double.compare(dist2d1, dist2d2) != 0) return Double.compare(dist2d1, dist2d2);
                    
                    if (order == ChestFillOrder.ColumnsBottomToTop) return Integer.compare(p1.getY(), p2.getY());
                    else return Integer.compare(p2.getY(), p1.getY());
                } else if (order == ChestFillOrder.LayersBottomToTop) {
                    if (p1.getY() != p2.getY()) return Integer.compare(p1.getY(), p2.getY());
                    return Double.compare(p1.getSquaredDistance(playerPos), p2.getSquaredDistance(playerPos));
                } else if (order == ChestFillOrder.LayersTopToBottom) {
                    if (p1.getY() != p2.getY()) return Integer.compare(p2.getY(), p1.getY());
                    return Double.compare(p1.getSquaredDistance(playerPos), p2.getSquaredDistance(playerPos));
                } else if (order == ChestFillOrder.LeftToRight || order == ChestFillOrder.RightToLeft) {
                    Direction dir = mc.player.getHorizontalFacing();
                    Direction right = dir.rotateYClockwise();
                    int offset1 = p1.getX() * right.getOffsetX() + p1.getZ() * right.getOffsetZ();
                    int offset2 = p2.getX() * right.getOffsetX() + p2.getZ() * right.getOffsetZ();
                    
                    if (offset1 != offset2) {
                        if (order == ChestFillOrder.LeftToRight) return Integer.compare(offset1, offset2);
                        else return Integer.compare(offset2, offset1);
                    }
                    
                    // If same column, sort bottom to top
                    if (p1.getY() != p2.getY()) return Integer.compare(p1.getY(), p2.getY());
                    return Double.compare(p1.getSquaredDistance(playerPos), p2.getSquaredDistance(playerPos));
                }
                return 0;
            });
        }

        return validChests.get(0);
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
