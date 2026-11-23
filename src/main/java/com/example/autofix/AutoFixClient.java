package com.example.autofix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.GameMode;

public class AutoFixClient implements ClientModInitializer {
    private GameMode prevMode = null;
    private boolean prevCanFly = false;
    private int flyDelay = -1;
    private int cooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            var im = client.interactionManager;

            if (player == null || im == null) {
                prevMode = null;
                return;
            }

            GameMode mode = im.getCurrentGameMode();
            boolean canFly = player.getAbilities().allowFlying;

            if (cooldown > 0) cooldown--;

            if (flyDelay > 0 && --flyDelay == 0) {
                player.networkHandler.sendChatCommand("fly");
            }

            if (prevMode != null && cooldown == 0) {
                if (mode == GameMode.SPECTATOR && prevMode != GameMode.SPECTATOR) {
                    player.networkHandler.sendChatCommand("gm 1");
                    flyDelay = 10;
                    cooldown = 40;
                } else if (mode == prevMode && mode != GameMode.SPECTATOR
                        && prevCanFly && !canFly) {
                    player.networkHandler.sendChatCommand("fly");
                    cooldown = 40;
                }
            }

            prevMode = mode;
            prevCanFly = canFly;
        });
    }
}
