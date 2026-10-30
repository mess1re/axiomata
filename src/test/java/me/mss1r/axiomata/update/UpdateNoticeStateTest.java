package me.mss1r.axiomata.update;

import net.minecraft.network.chat.ClickEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpdateNoticeStateTest {
    @Test void pendingDoesNotConsumeTheNotice() {
        var notice = new UpdateNoticeState();
        assertTrue(notice.take(true, false, null, null).isEmpty());
        assertFalse(notice.finished());
        assertTrue(notice.take(false, true, "0.1.0-beta.10", "https://example.org/download").isPresent());
        assertTrue(notice.finished());
        assertTrue(notice.take(false, true, "0.1.0-beta.11", "https://example.org/download").isEmpty());
    }
    @Test void missingOrCurrentReleaseDoesNotCreateAMessage() {
        var notice = new UpdateNoticeState();
        assertTrue(notice.take(false, false, null, null).isEmpty());
        assertTrue(notice.finished());
        assertTrue(new UpdateNoticeState().take(false, true, null, null).isEmpty());
    }
    @Test void invalidDownloadUrlUsesTheOfficialProject() {
        var message = new UpdateNoticeState().take(false, true, "0.1.0", "file:///private/file").orElseThrow();
        var click = message.getSiblings().get(1).getStyle().getClickEvent();
        assertEquals(ClickEvent.Action.OPEN_URL, click.getAction());
        assertEquals("https://www.curseforge.com/projects/1713796", click.getValue());
    }
}
