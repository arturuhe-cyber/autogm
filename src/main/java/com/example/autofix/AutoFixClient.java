package com.example.autofix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public class AutoFixClient implements ClientModInitializer {
    private boolean enabled = true;
    private int cooldown = 0;
    private int flyDelay = -1;
    private GameMode prevMode = null;
    private boolean announced = false;

    @Override
    public void onInitializeClient() {
        // команда /autofix включает и выключает мод
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("autofix").executes(ctx -> {
                enabled = !enabled;
                ctx.getSource().sendFeedback(Text.literal(
                        "[AutoFix] " + (enabled ? "ВКЛЮЧЁН" : "ВЫКЛЮЧЕН")));
                return 1;
            }))
        );

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
                player.sendMessage(Text.literal(
                        "[AutoFix] мод загружен, /autofix: вкл/выкл"), false);
                announced = true;
            }

            GameMode mode = im.getCurrentGameMode();
            if (cooldown > 0) cooldown--;

            if (!enabled) {
                prevMode = mode;
                flyDelay = -1;
                return;
            }

            if (flyDelay > 0 && --flyDelay == 0) {
                player.networkHandler.sendChatCommand("fly");
            }

            if (cooldown == 0) {
                boolean becameSpectator = prevMode != null
                        && mode == GameMode.SPECTATOR && prevMode != GameMode.SPECTATOR;

                boolean falling = !player.isOnGround() && !player.isTouchingWater()
                        && !player.isClimbing() && player.getVelocity().y < -0.6
                        && !player.getAbilities().allowFlying;

                if (becameSpectator || falling) {
                    player.networkHandler.sendChatCommand("gm 1");
                    flyDelay = 10;
                    cooldown = 60;
                }
            }

            prevMode = mode;
        });
    }
}
