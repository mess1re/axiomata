package me.mss1r.axiomata.update;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
//? if forge {
/*import net.minecraftforge.fml.VersionChecker;
*///?} else {
import net.neoforged.fml.VersionChecker;
//?}
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateNoticeTest {
    private static final ComparableVersion TARGET = new ComparableVersion("0.1.0-beta.5");
    private static final String PAGE = "https://modrinth.com/mod/axiomata/versions?g=1.21.1&l=neoforge";

    @Test
    void waitsForTheResultAndShowsOnlyOneShortMessage() {
        var notice = new AxiomataUpdateNotice();
        assertTrue(notice.take(new VersionChecker.CheckResult(
                VersionChecker.Status.PENDING, null, null, null)).isEmpty());
        assertFalse(notice.finished());
        var result = result(VersionChecker.Status.BETA_OUTDATED, PAGE);
        var message = notice.take(result).orElseThrow();
        var text = (TranslatableContents) message.getContents();
        assertEquals("message.axiomata.update.available", text.getKey());
        assertArrayEquals(new Object[] {"0.1.0-beta.5"}, text.getArgs());
        assertEquals(2, message.getSiblings().size());
        ClickEvent click = message.getSiblings().get(1).getStyle().getClickEvent();
        assertEquals(ClickEvent.Action.OPEN_URL, click.getAction());
        assertEquals(PAGE, click.getValue());
        assertTrue(notice.finished());
        assertTrue(notice.take(result).isEmpty());
    }

    @Test
    void stableUpdateAlsoGetsANotice() {
        assertTrue(new AxiomataUpdateNotice().take(result(VersionChecker.Status.OUTDATED, PAGE)).isPresent());
    }

    @Test
    void failedCurrentAndAheadChecksStayQuiet() {
        for (var status : new VersionChecker.Status[] {VersionChecker.Status.FAILED,
                VersionChecker.Status.UP_TO_DATE, VersionChecker.Status.AHEAD, VersionChecker.Status.BETA}) {
            var notice = new AxiomataUpdateNotice();
            assertTrue(notice.take(result(status, PAGE)).isEmpty());
            assertTrue(notice.finished());
        }
        assertTrue(new AxiomataUpdateNotice().take(new VersionChecker.CheckResult(
                VersionChecker.Status.OUTDATED, null, Map.of(), PAGE)).isEmpty());
    }

    @Test
    void invalidLinksUseTheProjectPage() {
        for (String url : new String[] {"file:///etc/passwd", "javascript:alert(1)", "broken url"}) {
            var message = new AxiomataUpdateNotice().take(result(VersionChecker.Status.OUTDATED, url)).orElseThrow();
            ClickEvent click = message.getSiblings().get(1).getStyle().getClickEvent();
            assertEquals("https://www.curseforge.com/projects/1713796", click.getValue());
        }
    }

    private static VersionChecker.CheckResult result(VersionChecker.Status status, String url) {
        return new VersionChecker.CheckResult(status, TARGET, Map.of(TARGET, "CHANGELOG_SENTINEL"), url);
    }
}
