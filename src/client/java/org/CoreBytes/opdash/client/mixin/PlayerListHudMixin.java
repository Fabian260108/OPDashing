package org.CoreBytes.opdash.client.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.CoreBytes.opdash.client.NameTag.NameSymbolUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    private void opdash$forceTabSymbol(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!NameSymbolUtil.isSymbolEnabled()) {
            return;
        }

        Text base = entry.getDisplayName();
        if (base == null) {
            base = Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().getName()));
        }

        if (entry.getGameMode() == GameMode.SPECTATOR) {
            base = base.copy().formatted(Formatting.ITALIC);
        }

        cir.setReturnValue(NameSymbolUtil.appendIfMissing(base));
    }
}
