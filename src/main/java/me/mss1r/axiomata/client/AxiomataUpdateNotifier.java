package me.mss1r.axiomata.client;

import dev.architectury.event.events.client.ClientTickEvent;
import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.config.AxiomataClientConfig;
import me.mss1r.axiomata.update.AxiomataUpdateNotice;
import net.minecraft.client.Minecraft;
//? if forge {
/*import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;
*///?} else {
import net.neoforged.fml.ModList;
import net.neoforged.fml.VersionChecker;
//?}

public final class AxiomataUpdateNotifier {
    private static final AxiomataUpdateNotice NOTICE = new AxiomataUpdateNotice();
    private static int ticksUntilCheck = 100;

    private AxiomataUpdateNotifier() {
    }

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(AxiomataUpdateNotifier::tick);
    }

    private static void tick(Minecraft minecraft) {
        if (NOTICE.finished() || minecraft.player == null || minecraft.screen != null
                || !AxiomataClientConfig.showUpdateNotice()) {
            return;
        }
        if (--ticksUntilCheck > 0) {
            return;
        }
        ticksUntilCheck = 100;
        ModList.get().getModContainerById(Axiomata.MOD_ID).ifPresent(container ->
                NOTICE.take(VersionChecker.getResult(container.getModInfo()))
                        .ifPresent(message -> minecraft.gui.getChat().addMessage(message)));
    }
}
