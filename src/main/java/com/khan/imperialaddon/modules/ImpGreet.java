package com.khan.imperialaddon.modules;

import com.khan.imperialaddon.ImperialAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;

import java.awt.Desktop;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImpGreet extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
        .name("greeting-message")
        .description("Customizable greeting message with {player} placeholder.")
        .defaultValue("{player} Hail The Imperials!")
        .build()
    );

    private final Setting<Boolean> commandMode = sgGeneral.add(new BoolSetting.Builder()
        .name("command-mode")
        .description("Send a command instead of a chat message.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("Command to execute when a player joins. Use {player} as a placeholder.")
        .defaultValue("/msg {player} Hail The Imperials!")
        .visible(() -> commandMode.get())
        .build()
    );

    private final Setting<Integer> messageDelay = sgGeneral.add(new IntSetting.Builder()
        .name("message-delay")
        .description("Cooldown in seconds between greeting messages.")
        .defaultValue(5)
        .min(0)
        .sliderMax(60)
        .build()
    );

    private List<String> blockedUsers = new ArrayList<>();
    private final Path blocklistPath = Paths.get("imperialaddon", "greet_blocklist.txt");

    private final AtomicBoolean isOnCooldown = new AtomicBoolean(false);

    public ImpGreet() {
        super(ImperialAddon.Main, "imp-greet", "Greets players when they join the server.");
        ensureBlocklistExists();
        loadBlocklist();
    }

    @Override
    public void onActivate() {
        loadBlocklist();
        isOnCooldown.set(false);
    }

    private void ensureBlocklistExists() {
        try {
            if (!Files.exists(blocklistPath.getParent())) {
                Files.createDirectories(blocklistPath.getParent());
            }

            if (!Files.exists(blocklistPath)) {
                Files.createFile(blocklistPath);
                Files.write(blocklistPath, List.of("# Add player names to ignore (one per line)"));
            }
        } catch (IOException e) {
            error("Failed to create blocklist file: " + e.getMessage());
        }
    }

    public void loadBlocklist() {
        blockedUsers.clear();
        try {
            ensureBlocklistExists();

            List<String> lines = Files.readAllLines(blocklistPath);

            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    blockedUsers.add(line);
                }
            }

            info("Loaded " + blockedUsers.size() + " blocked users.");
        } catch (IOException e) {
            error("Failed to load blocklist: " + e.getMessage());
        }
    }

    public void openBlocklist() {
        try {
            ensureBlocklistExists();

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(blocklistPath.toFile());
                info("Opened blocklist file.");
            } else {
                error("Desktop not supported - cannot open file automatically.");
                info("Blocklist located at: " + blocklistPath.toAbsolutePath());
            }
        } catch (IOException e) {
            error("Failed to open blocklist file: " + e.getMessage());
        }
    }

    public boolean isPlayerBlocked(String playerName) {
        return blockedUsers.contains(playerName);
    }

    public int getBlockedUsersCount() {
        return blockedUsers.size();
    }

    public void listBlockedPlayers() {
        if (blockedUsers.isEmpty()) {
            info("Blocklist is empty.");
        } else {
            info("Blocked users (" + blockedUsers.size() + "):");
            for (String player : blockedUsers) {
                info(" - " + player);
            }
        }
    }

    private void sendGreeting(String playerName) {
        if (isOnCooldown.get()) {
            info("Greeting for " + playerName + " skipped (on cooldown)");
            return;
        }

        if (commandMode.get()) {
            String formattedCommand = command.get().replace("{player}", playerName);
            if (formattedCommand.startsWith("/")) {
                formattedCommand = formattedCommand.substring(1);
            }
            MinecraftClient.getInstance().getNetworkHandler().sendCommand(formattedCommand);
            info("Sent command for " + playerName);
        } else {
            String formattedMessage = message.get().replace("{player}", playerName);
            MinecraftClient.getInstance().getNetworkHandler().sendChatMessage(formattedMessage);
            info("Sent greeting for " + playerName);
        }

        int delaySeconds = messageDelay.get();
        if (delaySeconds > 0) {
            isOnCooldown.set(true);

            Thread cooldownThread = new Thread(() -> {
                try {
                    Thread.sleep(delaySeconds * 500);
                    isOnCooldown.set(false);
                    info("Greeting cooldown expired, ready to greet next player");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            cooldownThread.setDaemon(true);
            cooldownThread.start();
        }
    }

    @EventHandler
    private void onPlayerJoin(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerListS2CPacket packet && packet.getActions().contains(Action.ADD_PLAYER)) {
            packet.getEntries().forEach(entry -> {
                String playerName = entry.profile().getName();

                if (blockedUsers.contains(playerName)) {
                    info("Skipping greeting for blocked user: " + playerName);
                    return;
                }

                MinecraftClient.getInstance().execute(() -> sendGreeting(playerName));
            });
        }
    }

    public Path getBlocklistPath() {
        return blocklistPath;
    }
}
