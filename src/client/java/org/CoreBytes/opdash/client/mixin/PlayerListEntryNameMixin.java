package org.CoreBytes.opdash.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.CoreBytes.opdash.client.NameTag.NameSymbolUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryNameMixin {

    @Shadow public abstract GameProfile getProfile();

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void opdash$entryFallbackSymbol(CallbackInfoReturnable<Text> cir) {
        if (!NameSymbolUtil.isSymbolEnabled()) {
            return;
        }

        Text original = cir.getReturnValue();
        if (original == null) {
            original = Text.literal(getProfile().getName());
        }

        cir.setReturnValue(NameSymbolUtil.appendIfMissing(original));
    }
}
