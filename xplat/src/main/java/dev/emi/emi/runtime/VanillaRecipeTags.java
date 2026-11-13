package dev.emi.emi.runtime;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;

/**
 * A separate snapshot of bundled vanilla item/block/fluid tags for EMI's
 * read-only recipe browser. Remote server registries are never modified.
 * This is deliberately scoped to vanilla data; modded server tags still use
 * the actual synchronized registry when the fallback is not active.
 */
public final class VanillaRecipeTags {
    private static volatile Map<TagKey<?>, List<?>> tags = Map.of();

    private VanillaRecipeTags() {}

    public static void clear() {
        tags = Map.of();
    }

    public static void load(ResourceManager resources) {
        Map<TagKey<?>, List<?>> loaded = new LinkedHashMap<>();
        loadGroup(resources, "tags/item", BuiltInRegistries.ITEM, loaded);
        loadGroup(resources, "tags/block", BuiltInRegistries.BLOCK, loaded);
        loadGroup(resources, "tags/fluid", BuiltInRegistries.FLUID, loaded);
        tags = Map.copyOf(loaded);
        EmiLog.info("Loaded " + tags.size() + " bundled vanilla tags for client recipe display");
    }

    /** Null means this tag does not exist in bundled vanilla resources. */
    @SuppressWarnings("unchecked")
    public static <T> List<T> get(TagKey<T> key) {
        return (List<T>) tags.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> Stream<TagKey<T>> keys(Registry<T> registry) {
        return tags.keySet().stream()
                .filter(key -> key.isFor(registry.key()))
                .map(key -> (TagKey<T>) key);
    }

    private static <T> void loadGroup(ResourceManager resources, String directory, Registry<T> registry,
            Map<TagKey<?>, List<?>> destination) {
        Map<Identifier, List<Entry>> definitions = new LinkedHashMap<>();
        Map<Identifier, Resource> files = resources.listResources(directory,
                id -> id.getPath().startsWith(directory + "/") && id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> resource : files.entrySet()) {
            Identifier file = resource.getKey();
            String filePath = file.getPath();
            String name = filePath.substring(directory.length() + 1, filePath.length() - 5);
            if (name.isEmpty()) {
                continue;
            }
            Identifier tag = Identifier.fromNamespaceAndPath(file.getNamespace(), name);
            try (Reader reader = resource.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray values = json.getAsJsonArray("values");
                if (values == null) {
                    continue;
                }
                List<Entry> entries = new ArrayList<>();
                for (JsonElement element : values) {
                    try {
                        String value;
                        boolean required = true;
                        if (element.isJsonPrimitive()) {
                            value = element.getAsString();
                        } else if (element.isJsonObject()) {
                            JsonObject object = element.getAsJsonObject();
                            value = object.get("id").getAsString();
                            if (object.has("required")) {
                                required = object.get("required").getAsBoolean();
                            }
                        } else {
                            continue;
                        }
                        boolean reference = value.startsWith("#");
                        entries.add(new Entry(Identifier.parse(reference ? value.substring(1) : value), reference, required));
                    } catch (RuntimeException malformedEntry) {
                        EmiLog.warn("Skipping malformed vanilla tag entry in " + tag + ": " + malformedEntry);
                    }
                }
                definitions.put(tag, entries);
            } catch (Exception e) {
                EmiLog.warn("Unable to read bundled vanilla tag " + tag + ": " + e);
            }
        }

        Map<Identifier, List<T>> resolved = new HashMap<>();
        for (Identifier id : definitions.keySet()) {
            List<T> elements = resolve(id, registry, definitions, resolved, new HashSet<>());
            destination.put(TagKey.create(registry.key(), id), elements);
        }
        EmiLog.info("Vanilla " + directory + " tags: " + definitions.size());
    }

    private static <T> List<T> resolve(Identifier id, Registry<T> registry,
            Map<Identifier, List<Entry>> definitions, Map<Identifier, List<T>> cache, Set<Identifier> visiting) {
        List<T> cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        List<Entry> entries = definitions.get(id);
        if (entries == null || !visiting.add(id)) {
            // Missing tag or cyclic tag reference. Do not recurse indefinitely.
            return List.of();
        }
        Set<T> values = new LinkedHashSet<>();
        for (Entry entry : entries) {
            if (entry.reference()) {
                values.addAll(resolve(entry.id(), registry, definitions, cache, visiting));
            } else {
                registry.get(ResourceKey.create(registry.key(), entry.id()))
                        .map(Holder::value).ifPresent(values::add);
            }
        }
        visiting.remove(id);
        List<T> result = List.copyOf(values);
        cache.put(id, result);
        return result;
    }

    private record Entry(Identifier id, boolean reference, boolean required) {}
}
