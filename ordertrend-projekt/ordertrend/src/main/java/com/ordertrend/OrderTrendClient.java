package com.ordertrend;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class OrderTrendClient implements ClientModInitializer {
    private static KeyBinding openKey;
    private static KeyBinding scanKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        Store.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("ordertrend", "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ordertrend.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, category));
        scanKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ordertrend.scan", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new TrendScreen());
                }
            }
            while (scanKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    OrderScanner.start();
                }
            }
            OrderScanner.tick(client);
        });
    }
}
