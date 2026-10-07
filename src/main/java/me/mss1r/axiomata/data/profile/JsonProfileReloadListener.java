package me.mss1r.axiomata.data.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import me.mss1r.axiomata.PackPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class JsonProfileReloadListener<T>
        extends SimplePreparableReloadListener<JsonProfileReloadListener.PreparedProfiles<T>> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonProfileReloadListener.class);
    private final String directory;
    private final String label;
    private final Codec<T> codec;
    private final Function<T, Optional<String>> validator;
    private final ProfileCatalog<T> destination;
    private final Function<JsonElement, Optional<String>> formatValidator;

    public JsonProfileReloadListener(String directory, String label, Codec<T> codec,
                                     Function<T, Optional<String>> validator,
                                     ProfileCatalog<T> destination) {
        this(directory, label, codec, validator, destination, json -> Optional.empty());
    }

    public JsonProfileReloadListener(String directory, String label, Codec<T> codec,
                                     Function<T, Optional<String>> validator, ProfileCatalog<T> destination,
                                     Function<JsonElement, Optional<String>> formatValidator) {
        this.directory = directory;
        this.label = label;
        this.codec = codec;
        this.validator = validator;
        this.destination = destination;
        this.formatValidator = formatValidator;
    }

    @Override
    protected PreparedProfiles<T> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, T> profiles = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        List<ResourceLocation> missing = new ArrayList<>();
        Map<ResourceLocation, List<Resource>> resources =
                resourceManager.listResourceStacks(directory, id -> id.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, List<Resource>> entry : resources.entrySet()) {
            ResourceLocation resourceId = entry.getKey();
            ResourceLocation profileId = profileId(resourceId);
            if (profileId == null) {
                errors.add("Invalid " + label + " resource path: " + resourceId);
                missing.add(resourceId);
                continue;
            }

            // The last resource is the winning pack; lower packs can supply a safe startup fallback. Datapacks always
            // rank above the mod jars, which some loaders place on top.
            List<Resource> stack = PackPriority.datapacksOverMods(entry.getValue());
            for (int i = stack.size() - 1; i >= 0; i--) {
                Resource resource = stack.get(i);
                String source = resourceId + " [" + resource.sourcePackId() + "]";
                try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonElement json = JsonParser.parseReader(reader);
                    Optional<String> formatError = formatValidator.apply(json);
                    if (formatError.isPresent()) {
                        errors.add(source + ": " + formatError.get());
                        continue;
                    }
                    List<String> decodeErrors = new ArrayList<>();
                    DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
                    Optional<T> decoded = result.resultOrPartial(decodeErrors::add);
                    if (!decodeErrors.isEmpty()) {
                        decodeErrors.forEach(message -> errors.add(source + ": " + message));
                        continue;
                    }
                    if (decoded.isEmpty()) {
                        errors.add(source + ": codec returned no profile");
                        continue;
                    }
                    T profile = decoded.get();
                    Optional<String> validationError = validator.apply(profile);
                    if (validationError.isPresent()) {
                        errors.add(source + ": " + validationError.get());
                        continue;
                    }
                    profiles.put(profileId, profile);
                    break;
                } catch (Exception exception) {
                    errors.add(source + ": " + exception.getMessage());
                    LOG.debug("Failed to prepare {} from {}", label, resourceId, exception);
                }
            }
            if (!profiles.containsKey(profileId)) {
                missing.add(resourceId);
            }
        }

        return new PreparedProfiles<>(Map.copyOf(profiles), List.copyOf(errors), List.copyOf(missing));
    }

    @Override
    protected void apply(PreparedProfiles<T> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (!prepared.errors().isEmpty()) {
            prepared.errors().forEach(message -> LOG.error("Invalid {}: {}", label, message));
            if (destination.hasSnapshot()) {
                LOG.error("Rejected {} reload with {} error(s); keeping the previous snapshot",
                        label, prepared.errors().size());
                return;
            }
            if (!prepared.missing().isEmpty()) {
                throw new IllegalStateException("No valid " + label + " for " + prepared.missing()
                        + "; fix the datapack errors above before starting this world");
            }
            LOG.error("Starting with lower-priority {} instead of invalid overrides; fix the datapack errors above",
                    label);
        }

        destination.publish(prepared.profiles());
        LOG.info("Loaded {} {}", prepared.profiles().size(), label);
    }

    private ResourceLocation profileId(ResourceLocation resourceId) {
        String prefix = directory + "/";
        String path = resourceId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return null;
        }
        return ResourceLocation.tryBuild(
                resourceId.getNamespace(),
                path.substring(prefix.length(), path.length() - ".json".length())
        );
    }

    public record PreparedProfiles<T>(Map<ResourceLocation, T> profiles, List<String> errors,
                                       List<ResourceLocation> missing) {
    }
}
