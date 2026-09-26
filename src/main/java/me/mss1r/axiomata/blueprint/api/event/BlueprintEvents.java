package me.mss1r.axiomata.blueprint.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public final class BlueprintEvents {
    public static final Event<RecipeCheck> RECIPE_CHECK = EventFactory.createLoop();
    public static final Event<CreationCheck> CREATION_CHECK = EventFactory.createLoop();
    public static final Event<Created> CREATED = EventFactory.createLoop();
    public static final Event<Used> USED = EventFactory.createLoop();

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
}
