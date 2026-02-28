package org.CoreBytes.opdash.client.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.CoreBytes.opdash.client.OpdashClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityNameMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void opdash$addShopSymbol(CallbackInfoReturnable<Text> cir) {
        if (OpdashClient.shardsConfig == null) {
            return;
        }
        if (!OpdashClient.shardsConfig.isShopNameSymbolOwned() || !OpdashClient.shardsConfig.isShopNameSymbolEquipped()) {
            return;
        }

        Text original = cir.getReturnValue();
        MutableText symbol = Text.literal("\u272A ").formatted(Formatting.AQUA, Formatting.BOLD);
        cir.setReturnValue(symbol.append(original.copy()));
    }
}


