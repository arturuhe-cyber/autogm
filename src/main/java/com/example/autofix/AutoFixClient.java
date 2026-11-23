package com.example.autofix;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AutoFixClient implements ClientModInitializer {
    private int cooldown = 0;
    private int flyDelay = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null) {
                flyDelay = -1;
                return;
            }

            if (cooldown > 0) cooldown--;

            // отложенная отправка /fly после /gm 1
            if (flyDelay > 0 && --flyDelay == 0) {
                player.networkHandler.sendChatCommand("fly");
            }

            if (cooldown > 0) return;

            // на земле, в воде или на лестнице: не падаем
            if (player.isOnGround() || player.isTouchingWater() || player.isClimbing()) return;

            // падаем достаточно быстро?
            if (player.getVelocity().y >= -0.6) return;

            boolean creative = player.isCreative();
            boolean canFly = player.getAbilities().allowFlying;

            if (player.isSpectator() || (!creative && !canFly)) {
                // падаем в /gm 3 или в выживании: включаем креатив и полёт
                player.networkHandler.sendChatCommand("gm 1");
                flyDelay = 10;   // полсекунды
                cooldown = 60;   // 3 секунды паузы, чтобы не спамить
            } else if (creative && !canFly) {
                // уже в креативе, но полёт отобрали
                player.networkHandler.sendChatCommand("fly");
                cooldown = 60;
            }
        });
    }
}
