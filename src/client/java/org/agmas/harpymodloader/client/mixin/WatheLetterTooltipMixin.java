package org.agmas.harpymodloader.client.mixin;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.modifiers.Modifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class WatheLetterTooltipMixin {

    private static final Identifier WATHE_LETTER_ID = Identifier.of("wathe", "letter");

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void harpymodloader$appendRoleTooltip(@Coerce Object tooltipType, @Coerce Object context, Consumer<Text> tooltip, @Coerce Object data, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!Registries.ITEM.getId(stack.getItem()).equals(WATHE_LETTER_ID)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(client.world);
        Role role = gameWorldComponent.getRole(client.player);
        if (role == null) {
            return;
        }

        WorldModifierComponent worldModifierComponent = WorldModifierComponent.KEY.get(client.world);
        ArrayList<Modifier> myModifiers = worldModifierComponent.getModifiers(client.player);

        MutableText roleText = Harpymodloader.getRoleName(role).withColor(role.color());
        Text message;
        if (myModifiers == null || myModifiers.isEmpty()) {
            message = Text.translatable("harpymodloader.hud.role_only", roleText);
        } else {
            Text modifiersText = Texts.join(myModifiers, Text.literal(", "), modifier -> modifier.getName(false).withColor(modifier.color));
            message = Text.translatable("harpymodloader.hud.actionbar", roleText, modifiersText);
        }

        tooltip.accept(message);
    }
}
