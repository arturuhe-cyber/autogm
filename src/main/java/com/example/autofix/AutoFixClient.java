package com.example.autofix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public class AutoFixClient implements ClientModInitializer {
    private int cooldown = 0;
    private int flyDelay = -1;
    private GameMode prevMode = null;
    private boolean announced = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            var im = client.interactionManager;

            if (player == null || im == null) {
                prevMode = null;
                flyDelay = -1;
                announced = false;
                return;
            }

            if (!announced) {
                player.sendMessage(Text.literal("[AutoFix] мод загружен"), false);
                announced = true;
            }

            GameMode mode = im.getCurrentGameMode();
            if (cooldown > 0) cooldown--;

            if (flyDelay > 0 && --flyDelay == 0) {
                player.networkHandler.sendChatCommand("fly");
                player.sendMessage(Text.literal("[AutoFix] отправил /fly"), false);
            }

            if (cooldown == 0) {
                boolean becameSpectator = prevMode != null
                        && mode == GameMode.SPECTATOR && prevMode != GameMode.SPECTATOR;

                boolean falling = !player.isOnGround() && !player.isTouchingWater()
                        && !player.isClimbing() && player.getVelocity().y < -0.6
                        && !player.getAbilities().allowFlying;

                if (becameSpectator || falling) {
                    player.sendMessage(Text.literal("[AutoFix] сработало: "
                            + (becameSpectator ? "/gm 3" : "падение")), false);
                    player.networkHandler.sendChatCommand("gm 1");
                    flyDelay = 10;
                    cooldown = 60;
                }
            }

            prevMode = mode;
        });
    }
}
