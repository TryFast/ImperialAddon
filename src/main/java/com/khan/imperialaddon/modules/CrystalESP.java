package com.khan.imperialaddon.modules;

import com.khan.imperialaddon.ImperialAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Box;

public class CrystalESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> crystalColor = sgGeneral.add(new ColorSetting.Builder()
        .name("crystal-color")
        .description("Color for rendering End Crystals when PvP Mode is disabled.")
        .defaultValue(new SettingColor(255, 0, 0, 100))
        .build()
    );

    private final Setting<Boolean> pvpMode = sgGeneral.add(new BoolSetting.Builder()
        .name("pvp-mode")
        .description("Colors crystals based on potential damage relative to your Y level.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maxDistance = sgGeneral.add(new IntSetting.Builder()
        .name("max-distance")
        .description("Maximum distance (in blocks) to render ESP.")
        .defaultValue(12)
        .min(1)
        .max(12)
        .visible(() -> !pvpMode.get())
        .build()
    );

    private final Setting<SettingColor> highDamageColor = sgGeneral.add(new ColorSetting.Builder()
        .name("high-damage-color")
        .description("Color for crystals at or above your Y level (high damage).")
        .defaultValue(new SettingColor(255, 0, 0, 100))
        .visible(() -> pvpMode.get())
        .build()
    );

    private final Setting<SettingColor> mediumDamageColor = sgGeneral.add(new ColorSetting.Builder()
        .name("medium-damage-color")
        .description("Color for crystals 1 block below your Y level (medium damage).")
        .defaultValue(new SettingColor(255, 255, 0, 100))
        .visible(() -> pvpMode.get())
        .build()
    );

    private final Setting<SettingColor> lowDamageColor = sgGeneral.add(new ColorSetting.Builder()
        .name("low-damage-color")
        .description("Color for crystals 2+ blocks below your Y level (low damage).")
        .defaultValue(new SettingColor(0, 255, 0, 100))
        .visible(() -> pvpMode.get())
        .build()
    );

    public CrystalESP() {
        super(ImperialAddon.Main, "crystal-esp", "Highlights End Crystals in the world.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        int currentMaxDistance = pvpMode.get() ? 12 : maxDistance.get();

        for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, mc.player.getBoundingBox().expand(currentMaxDistance), e -> true)) {
            if (mc.player.getPos().distanceTo(crystal.getPos()) > currentMaxDistance) continue;

            Box box = crystal.getBoundingBox();

            if (pvpMode.get()) {
                int playerY = mc.player.getBlockY();
                int crystalY = (int) crystal.getY();
                int yDifference = playerY - crystalY;

                SettingColor color;

                if (yDifference >= 0) {
                    color = highDamageColor.get();
                } else if (yDifference == -1) {
                    color = mediumDamageColor.get();
                } else {
                    color = lowDamageColor.get();
                }

                event.renderer.box(box, color, color, ShapeMode.Both, 0);
            } else {
                event.renderer.box(box, crystalColor.get(), crystalColor.get(), ShapeMode.Both, 0);
            }
        }
    }
}
