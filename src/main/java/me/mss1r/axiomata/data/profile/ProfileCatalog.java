package me.mss1r.axiomata.data.profile;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class ProfileCatalog<T> {
    private final T fallback;
    private final AtomicReference<Map<ResourceLocation, T>> snapshot =
            new AtomicReference<>(Map.of());
    private final List<Runnable> publishListeners = new CopyOnWriteArrayList<>();
    private volatile boolean published;

    public ProfileCatalog(T fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    public T forEntity(EntityType<?> entityType) {
        return get(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
    }

    public T get(ResourceLocation id) {
        return snapshot.get().getOrDefault(id, fallback);
    }

    public boolean contains(ResourceLocation id) {
        return snapshot.get().containsKey(id);
    }

    public Map<ResourceLocation, T> snapshot() {
        return snapshot.get();
    }

    public boolean hasSnapshot() {
        return published;
    }

    public void reset() {
        snapshot.set(Map.of());
        published = false;
    }

    public void onPublish(Runnable listener) {
        publishListeners.add(listener);
    }

    public void publish(Map<ResourceLocation, T> profiles) {
        snapshot.set(Map.copyOf(profiles));
        published = true;
        publishListeners.forEach(Runnable::run);
    }

    /** Applies the server's profiles on the client. Publish listeners are skipped; they only matter on the server. */
    public void acceptFromServer(Map<ResourceLocation, T> profiles) {
        snapshot.set(Map.copyOf(profiles));
    }
}
