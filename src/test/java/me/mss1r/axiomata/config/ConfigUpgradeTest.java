package me.mss1r.axiomata.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import me.mss1r.axiomata.Axiomata;
import me.mss1r.axiomata.blueprint.config.BlueprintServerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUpgradeTest {
    @Test
    void unversionedConstructionConfigKeepsEveryValue() {
        CommentedConfig old = CommentedConfig.inMemory();
        old.set("construction.hitsPerItem", 2.5D);
        old.set("construction.minStageHits", 7);
        old.set("construction.maxStageHits", 110);
        old.set("construction.hitCooldownTicks", 48);
        BlueprintServerConfig.SPEC.correct(old);
        assertEquals(Integer.valueOf(Axiomata.CONFIG_FORMAT_VERSION), old.get("configVersion"));
        assertEquals(Double.valueOf(2.5D), old.get("construction.hitsPerItem"));
        assertEquals(Integer.valueOf(7), old.get("construction.minStageHits"));
        assertEquals(Integer.valueOf(110), old.get("construction.maxStageHits"));
        assertEquals(Integer.valueOf(48), old.get("construction.hitCooldownTicks"));
        assertTrue(BlueprintServerConfig.SPEC.isCorrect(old));
    }

    @Test
    void noticePreferenceSurvivesAnotherLoad() {
        CommentedConfig config = CommentedConfig.inMemory();
        AxiomataClientConfig.SPEC.correct(config);
        assertEquals(Integer.valueOf(Axiomata.CONFIG_FORMAT_VERSION), config.get("configVersion"));
        assertEquals(Boolean.TRUE, config.get("updates.showNotice"));
        config.set("updates.showNotice", false);
        AxiomataClientConfig.SPEC.correct(config);
        assertEquals(Boolean.FALSE, config.get("updates.showNotice"));
        assertTrue(AxiomataClientConfig.SPEC.isCorrect(config));
    }
}
