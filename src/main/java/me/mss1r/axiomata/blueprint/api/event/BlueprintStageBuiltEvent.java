package me.mss1r.axiomata.blueprint.api.event;

import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** The server's last blow of a zero-based stage. Rebuilding a dismantled stage is not its first completion. */
public record BlueprintStageBuiltEvent(UnderConstruction result, String recipeId, @Nullable LivingEntity worker,
                                       int stage, boolean firstCompletion, boolean finished) {}
