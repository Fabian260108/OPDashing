package org.CoreBytes.opdash.client.NameTag;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.CoreBytes.opdash.client.OpdashClient;

public final class NameSymbolUtil {

    private static final String SYMBOL = "\u2726";

    private NameSymbolUtil() {
    }

    public static boolean isSymbolEnabled() {
        return OpdashClient.shardsConfig != null
                && OpdashClient.shardsConfig.isShopNameSymbolOwned()
                && OpdashClient.shardsConfig.isShopNameSymbolEquipped();
    }

    public static Text prependIfMissing(Text original) {
        if (original == null) {
            return null;
        }
        if (original.getString().contains(SYMBOL)) {
            return original;
        }
        MutableText prefix = Text.literal(SYMBOL + " ").formatted(Formatting.AQUA, Formatting.BOLD);
        return prefix.append(original.copy());
    }

    public static Text appendIfMissing(Text original) {
        if (original == null) {
            return null;
        }
        if (original.getString().contains(SYMBOL)) {
            return original;
        }
        return original.copy().append(Text.literal(" " + SYMBOL).formatted(Formatting.AQUA, Formatting.BOLD));
    }
}
