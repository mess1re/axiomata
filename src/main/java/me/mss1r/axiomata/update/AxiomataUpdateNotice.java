package me.mss1r.axiomata.update;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
//? if forge {
/*import net.minecraftforge.fml.VersionChecker;
*///?} else {
import net.neoforged.fml.VersionChecker;
//?}

import java.net.URI;
import java.util.Optional;

/** Consumes the loader's check once, without starting another request. */
public final class AxiomataUpdateNotice {
    private boolean finished;

    public boolean finished() {
        return finished;
    }

    public Optional<Component> take(VersionChecker.CheckResult result) {
        if (finished || result.status() == VersionChecker.Status.PENDING) {
            return Optional.empty();
        }
        finished = true;
        if ((result.status() != VersionChecker.Status.OUTDATED
                && result.status() != VersionChecker.Status.BETA_OUTDATED) || result.target() == null) {
            return Optional.empty();
        }

        Component link = Component.translatable("message.axiomata.update.open")
                .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl(result.url()))));
        return Optional.of(Component.translatable("message.axiomata.update.available", result.target().toString())
                .append(" ").append(link));
    }

    private static String downloadUrl(String url) {
        if (url != null) {
            try {
                URI uri = URI.create(url);
                if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                    return url;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "https://modrinth.com/mod/axiomata/versions";
    }
}
