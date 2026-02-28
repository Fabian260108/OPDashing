package org.CoreBytes.opdash.client.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.CoreBytes.opdash.client.NameTag.NameSymbolUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDisplayNameMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void opdash$forceNametagSymbol(CallbackInfoReturnable<Text> cir) {
        if (!NameSymbolUtil.isSymbolEnabled()) {
            return;
        }

        if (!((Object) this instanceof PlayerEntity)) {
            return;
        }

        Text original = cir.getReturnValue();
        cir.setReturnValue(NameSymbolUtil.prependIfMissing(original));
    }
}
