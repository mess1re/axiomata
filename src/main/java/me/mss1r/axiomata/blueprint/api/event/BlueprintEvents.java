package me.mss1r.axiomata.blueprint.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public final class BlueprintEvents {
    public static final Event<RecipeCheck> RECIPE_CHECK = EventFactory.createLoop();
    public static final Event<CreationCheck> CREATION_CHECK = EventFactory.createLoop();
    public static final Event<Created> CREATED = EventFactory.createLoop();
    public static final Event<Used> USED = EventFactory.createLoop();
    public static final Event<Built> BUILT = EventFactory.createLoop();
    public static final Event<Work> WORK = EventFactory.createLoop();
    public static final Event<StageBuilt> STAGE_BUILT = EventFactory.createLoop();
    public static final Event<PlacementCheck> PLACEMENT_CHECK = EventFactory.createLoop();

    private BlueprintEvents() {
    }

    @FunctionalInterface
    public interface RecipeCheck {
        void check(BlueprintRecipeCheckEvent event);
    }

    @FunctionalInterface
    public interface CreationCheck {
        void check(BlueprintCreationCheckEvent event);
    }

    @FunctionalInterface
    public interface Created {
        void created(BlueprintCreatedEvent event);
    }

    @FunctionalInterface
    public interface Used {
        void used(BlueprintUsedEvent event);
    }

    @FunctionalInterface
    public interface Built {
        void built(BlueprintBuiltEvent event);
    }

    @FunctionalInterface
    public interface Work {
        void work(BlueprintWorkEvent event);
    }

    @FunctionalInterface
    public interface StageBuilt {
        void built(BlueprintStageBuiltEvent event);
    }

    @FunctionalInterface
    public interface PlacementCheck {
        void check(BlueprintPlacementCheckEvent event);
    }
}
