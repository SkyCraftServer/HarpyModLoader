package org.agmas.harpymodloader.client;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import java.util.ArrayList;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.modifiers.Modifier;

public class HarpymodloaderClient implements ClientModInitializer {

    public static float rainbowRoleTime = 0;
    public static Role hudRole = null;
    public static ArrayList<Modifier> modifiers = null;

    private static final long BOSSBAR_DURATION_TICKS = 20L * 60L;
    private static long bossBarShowUntilWorldTime = -1L;

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((clientPlayNetworkHandler, packetSender, minecraftClient) -> {
            Harpymodloader.refreshRoles();
            bossBarShowUntilWorldTime = -1L;
        });
        
        ClientTickEvents.END_CLIENT_TICK.register((t) -> {
            rainbowRoleTime += 1;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                bossBarShowUntilWorldTime = -1L;
                return;
            }

            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(client.world);
            Role role = gameWorldComponent.getRole(client.player);

            // 在游戏结束后，Wathe 会清掉 role
            if (role == null) {
                bossBarShowUntilWorldTime = -1L;
                return;
            }

            long worldTime = client.world.getTime();
            if (bossBarShowUntilWorldTime < 0L) {
                // 视为“开局/获得身份”的起点：只在接下来 60 秒内显示
                bossBarShowUntilWorldTime = worldTime + BOSSBAR_DURATION_TICKS;
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(client.world);
            Role role = gameWorldComponent.getRole(client.player);

            if (role == null || bossBarShowUntilWorldTime < 0L) return;

            long worldTime = client.world.getTime();
            if (worldTime >= bossBarShowUntilWorldTime) return;

            // 准备文字
            WorldModifierComponent worldModifierComponent = WorldModifierComponent.KEY.get(client.world);
            ArrayList<Modifier> myModifiers = worldModifierComponent.getModifiers(client.player);

            MutableText roleText = Harpymodloader.getRoleName(role).withColor(role.color());
            Text message;
            if (myModifiers == null || myModifiers.isEmpty()) {
                message = Text.translatable("harpymodloader.hud.role_only", roleText).formatted(Formatting.WHITE);
            } else {
                Text modifiersText = Texts.join(myModifiers, Text.literal(", "), modifier -> modifier.getName(false).withColor(modifier.color));
                message = Text.translatable("harpymodloader.hud.actionbar", roleText, modifiersText).formatted(Formatting.WHITE);
            }

            // 绘制文字 (左下角)
            int windowWidth = client.getWindow().getScaledWidth();
            int windowHeight = client.getWindow().getScaledHeight();
            int textWidth = client.textRenderer.getWidth(message);
            int margin = 6;
            int textX = margin;
            int textY = windowHeight - margin - client.textRenderer.fontHeight;

            drawContext.drawTextWithShadow(client.textRenderer, message, textX, textY, 0xFFFFFF);
        });
    }

}
