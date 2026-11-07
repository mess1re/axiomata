package me.mss1r.axiomata.ballistics.profile;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.data.profile.JsonProfileReloadListener;
import me.mss1r.axiomata.data.profile.ProfileCatalog;
import net.minecraft.server.packs.PackType;

public final class ImpactProfiles {
    public static final ProfileCatalog<ImpactRulesProfile> RULES = new ProfileCatalog<>(ImpactRulesProfile.DEFAULT);
    public static final ProfileCatalog<ArmorImpactProfile> ARMOR = new ProfileCatalog<>(ArmorImpactProfile.DEFAULT);
    private ImpactProfiles() {}
    public static ImpactRulesProfile rules() { return RULES.get(ResourceIds.id("axiomata", "default")); }
    public static void initialize() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>("definitions/structure_damage",
                "structure damage profiles", me.mss1r.axiomata.ballistics.damage.StructureDamageProfile.CODEC,
                me.mss1r.axiomata.ballistics.damage.StructureDamageProfile::validationError,
                me.mss1r.axiomata.ballistics.damage.StructureDamage.PROFILES,
                me.mss1r.axiomata.ballistics.damage.StructureDamageProfile::formatError), ResourceIds.id("axiomata", "structure_damage"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>("definitions/impact_rules",
                "impact rules", ImpactRulesProfile.CODEC, ImpactRulesProfile::validationError, RULES),
                ResourceIds.id("axiomata", "impact_rules"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>("definitions/armor_impacts",
                "armor impact profiles", ArmorImpactProfile.CODEC, ArmorImpactProfile::validationError, ARMOR),
                ResourceIds.id("axiomata", "armor_impacts"));
        LifecycleEvent.SERVER_STOPPED.register(server -> { RULES.reset(); ARMOR.reset();
            me.mss1r.axiomata.ballistics.damage.StructureDamage.PROFILES.reset(); });
    }
}
