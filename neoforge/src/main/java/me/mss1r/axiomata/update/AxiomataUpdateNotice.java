package me.mss1r.axiomata.update;
import net.neoforged.fml.VersionChecker;
import net.minecraft.network.chat.Component;
import java.util.Optional;

/** Preserves the native update-check API while sharing notice behavior across loaders. */
public final class AxiomataUpdateNotice {
    private final UpdateNoticeState state = new UpdateNoticeState();
    public boolean finished() { return state.finished(); }
    public Optional<Component> take(VersionChecker.CheckResult result) {
        return state.take(result.status() == VersionChecker.Status.PENDING,
                result.status() == VersionChecker.Status.OUTDATED || result.status() == VersionChecker.Status.BETA_OUTDATED,
                result.target() == null ? null : result.target().toString(), result.url());
    }
}
