package binnie.core.client.ponder;

import net.createmod.ponder.foundation.PonderIndex;

/** Client-only bridge kept behind a mod-presence check so Ponder remains optional. */
public class BinniePonderCompat {
    private static boolean registered;

    private BinniePonderCompat() {
    }

    public static void register() {
        if (!registered) {
            PonderIndex.addPlugin(new BinniePonderPlugin());
            registered = true;
        }
    }
}
