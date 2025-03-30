package com.khan.imperialaddon.modules;

import com.khan.imperialaddon.ImperialAddon;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.HashSet;
import java.util.Set;

public class BloodKillEffect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> playerMode = sgGeneral.add(new BoolSetting.Builder()
        .name("player-mode")
        .description("players emit blood particles.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> particleMultiplier = sgGeneral.add(new IntSetting.Builder()
        .name("particle-multiplier")
        .description("Number of particles to spawn on death.")
        .defaultValue(128)
        .min(1)
        .max(256)
        .sliderMin(1)
        .sliderMax(256)
        .build()
    );

    private final Set<Integer> deadEntities = new HashSet<>();

    public BloodKillEffect() {
        super(ImperialAddon.Main, "blood-kill-effect", "Renders a redstone block particle effect when a living entity dies.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;

            LivingEntity living = (LivingEntity) entity;
            int entityId = entity.getId();

            if (shouldSpawnParticles(living)) {
                spawnDeathParticles(living);
                deadEntities.add(entityId);
            }
        }

        deadEntities.removeIf(id -> mc.world.getEntityById(id) == null);
    }

    private boolean shouldSpawnParticles(LivingEntity entity) {
        if (deadEntities.contains(entity.getId())) return false;

        boolean isDead = entity.isDead() || entity.getHealth() <= 0;
        if (!isDead) return false;

        if (playerMode.get()) {
            return entity instanceof PlayerEntity;
        }
        else {
            return !(entity instanceof PlayerEntity);
        }
    }

    private void spawnDeathParticles(LivingEntity entity) {
        double x = entity.getX();
        double y = entity.getY() + entity.getEyeHeight(entity.getPose());
        double z = entity.getZ();
        BlockState blockState = Blocks.REDSTONE_BLOCK.getDefaultState();

        for (int i = 0; i < particleMultiplier.get(); i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetY = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;

            mc.world.addParticle(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState),
                x + offsetX, y + offsetY, z + offsetZ,
                offsetX * 0.5, offsetY * 0.5, offsetZ * 0.5
            );
        }
    }
}
