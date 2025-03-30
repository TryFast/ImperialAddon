package com.khan.imperialaddon.modules;

import com.khan.imperialaddon.ImperialAddon;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ImperialChat extends Module {
    private static final String COMMAND_PREFIX = "?ic";
    private static final String MESSAGE_PREFIX = "IC-";
    private static final String IMPERIAL_CHAT_PREFIX = "§8[§dImperial§8] ";

    // Create a fixed-length encryption key using SHA-256
    private static final byte[] ENCRYPTION_KEY;

    // Initialize the encryption key
    static {
        try {
            // Create a MessageDigest for SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Generate a fixed 32-byte key (suitable for AES-256)
            ENCRYPTION_KEY = digest.digest("ImperialAddonKey".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption key", e);
        }
    }

    // Settings
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> enableEncryption = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-encryption")
        .description("Enable sending encrypted messages")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> enableDecryption = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-decryption")
        .description("Enable receiving and decrypting messages")
        .defaultValue(true)
        .build()
    );

    public ImperialChat() {
        super(ImperialAddon.Main, "imperial-chat", "{BETA} Encrypted chat for Imperial addon users.");
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        if (!enableEncryption.get()) return;

        String message = event.message;

        if (message.startsWith(COMMAND_PREFIX) && message.length() > COMMAND_PREFIX.length() + 1) {
            event.cancel();

            try {
                String chatMessage = message.substring(COMMAND_PREFIX.length()).trim();

                if (chatMessage.isEmpty()) {
                    info("Message cannot be empty.");
                    return;
                }

                String playerName = mc.player.getName().getString();

                String encrypted = encryptMessage(playerName + ":" + chatMessage);

                mc.getNetworkHandler().sendChatMessage(MESSAGE_PREFIX + encrypted);

                displayImperialMessage(playerName, chatMessage);

            } catch (Exception e) {
                error("Failed to send encrypted message: " + e.getMessage());
            }
        }
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!enableDecryption.get()) return;

        String message = event.getMessage().getString();

        // Improved message detection to handle potential formatting codes
        if (message.contains(MESSAGE_PREFIX)) {
            try {
                // Find the prefix position and extract encrypted content
                int prefixIndex = message.indexOf(MESSAGE_PREFIX);
                String encrypted = message.substring(prefixIndex + MESSAGE_PREFIX.length());

                // Remove any trailing spaces or formatting that might be present
                encrypted = encrypted.trim();

                // Try to decrypt the message
                String decrypted = decryptMessage(encrypted);

                int colonIndex = decrypted.indexOf(":");
                if (colonIndex > 0) {
                    String senderName = decrypted.substring(0, colonIndex);
                    String chatMessage = decrypted.substring(colonIndex + 1);

                    // Cancel the original message to hide it from others
                    event.cancel();

                    // Display the decrypted message only to this client
                    displayImperialMessage(senderName, chatMessage);
                }
            } catch (Exception e) {
                // Only show errors when in development mode
                // error("Failed to decrypt message: " + e.getMessage() + " for: " + message);
                // We silently ignore decryption errors as it might be an unrelated message
            }
        }
    }

    private void displayImperialMessage(String playerName, String message) {
        String formattedMessage = IMPERIAL_CHAT_PREFIX +
            Formatting.LIGHT_PURPLE + playerName +
            Formatting.WHITE + ": " +
            Formatting.GRAY + message;

        mc.inGameHud.getChatHud().addMessage(Text.of(formattedMessage));
    }

    private String encryptMessage(String message) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decryptMessage(String encryptedMessage) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(ENCRYPTION_KEY, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decoded = Base64.getDecoder().decode(encryptedMessage);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
