package com.khan.imperialaddon.modules;

import com.khan.imperialaddon.ImperialAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ImpY extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> distance = sgGeneral.add(new IntSetting.Builder()
        .name("distance")
        .description("How many blocks away from you to build.")
        .defaultValue(2)
        .min(1)
        .sliderMax(10)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards placed blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically switches to obsidian.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Ticks between block placements.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );

    // Pattern representing the vertical Y
    // Width = 5, Height = 6

    private final int[][] PATTERN = {
        {0, 0, 1, 0, 0},
        {0, 0, 1, 0, 0},
        {0, 0, 1, 0, 0},
        {0, 1, 1, 1, 0},
        {1, 1, 0, 1, 1},
        {1, 0, 0, 0, 1}
    };

    private int ticksWaited = 0;
    private Direction facing;
    private int currentBlock = 0;
    private int totalBlocks = 0;

    public ImpY() {
        super(ImperialAddon.Main, "ImpY", "The Heart Of The Imperials!");
    }

    @Override
    public void onActivate() {
        facing = mc.player.getHorizontalFacing();
        ticksWaited = 0;
        currentBlock = 0;

        // Count the total number of blocks to place
        totalBlocks = 0;
        for (int[] row : PATTERN) {
            for (int block : row) {
                if (block == 1) totalBlocks++;
            }
        }

        info("Building vertical Y pattern with " + totalBlocks + " obsidian blocks.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Wait for delay
        if (ticksWaited < placeDelay.get()) {
            ticksWaited++;
            return;
        } else {
            ticksWaited = 0;
        }

        // Find obsidian in hotbar
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.found()) {
            error("No obsidian found in hotbar!");
            toggle();
            return;
        }

        // Get player position
        BlockPos playerPos = mc.player.getBlockPos();
        Direction playerFacing = mc.player.getHorizontalFacing();

        // Calculate base position (in front of player)
        BlockPos basePos = playerPos.offset(playerFacing, distance.get());

        boolean placedBlock = false;

        // Iterate through the pattern and place blocks
        for (int y = 0; y < PATTERN.length; y++) {
            for (int x = 0; x < PATTERN[y].length; x++) {
                if (PATTERN[y][x] == 1) {
                    // Calculate position in world
                    // x-2 centers the pattern (since width is 5, offset by 2)
                    Direction right = playerFacing.rotateYClockwise();

                    BlockPos placePos = basePos
                        .offset(right, x - 2)  // Horizontal offset (centered)
                        .up(y);                // Vertical position (bottom to top)

                    // Try to place block
                    if (BlockUtils.canPlace(placePos)) {
                        if (autoSwitch.get()) InvUtils.swap(obsidian.slot(), false);

                        if (BlockUtils.place(placePos, obsidian, rotate.get(), 50)) {
                            currentBlock++;
                            placedBlock = true;
                            break; // Place one block per tick
                        }
                    }
                }
            }
            if (placedBlock) break;
        }

        // Check if we're done
        if (currentBlock >= totalBlocks || !placedBlock) {
            info("Pattern completed!");
            toggle();
        }
    }
}
