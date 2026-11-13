package dev.emi.emi.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import dev.emi.emi.EmiSyncedRecipeMaps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Parses the vanilla recipe data bundled with the Minecraft client, for display
 * only. It cannot know the real recipe set on a server that sends no recipes.
 * This must not mutate the client's registries or crafting permissions.
 */
public final class VanillaClientRecipeLoader {
    private VanillaClientRecipeLoader() {}

    public static RecipeMap load(RegistryAccess registryAccess) {
        SortedMap<Identifier, Recipe<?>> recipes = new TreeMap<>();
        // Vanilla recipes depend on vanilla item tags. Build a separate
        // serialization lookup using the tags from the vanilla built-in pack,
        // rather than assuming the connected server's tags match vanilla.
        ResourceManager resources = ServerPacksSource.createVanillaPackSource().asResourceManager();
        try {
            RegistryAccess.Frozen frozen = registryAccess.freeze();
            List<Registry.PendingTags<?>> pendingTags =
                    TagLoader.loadTagsForExistingRegistries(resources, frozen);
            List<HolderLookup.RegistryLookup<?>> lookups =
                    TagLoader.buildUpdatedLookups(frozen, pendingTags);
            HolderLookup.Provider provider = HolderLookup.Provider.create(lookups.stream());
            RegistryOps<JsonElement> ops = provider.createSerializationContext(JsonOps.INSTANCE);
            // Minecraft 26.3 removed SimpleJsonResourceReloadListener.scanDirectory.
            // Enumerate the bundled recipe JSON files directly and decode them
            // against the same isolated vanilla-tag registry lookup.
            Map<Identifier, Resource> files = resources.listResources("recipe",
                    id -> id.getPath().startsWith("recipe/") && id.getPath().endsWith(".json"));
            for (Map.Entry<Identifier, Resource> entry : files.entrySet()) {
                Identifier fileId = entry.getKey();
                String path = fileId.getPath();
                String recipePath = path.substring("recipe/".length(), path.length() - ".json".length());
                if (recipePath.isEmpty()) {
                    continue;
                }
                Identifier recipeId = Identifier.fromNamespaceAndPath(fileId.getNamespace(), recipePath);
                try (java.io.Reader reader = entry.getValue().openAsReader()) {
                    Recipe.DIRECT_CODEC.parse(ops, JsonParser.parseReader(reader))
                            .resultOrPartial(message -> EmiLog.warn("Skipping vanilla recipe " + recipeId + ": " + message))
                            .ifPresent(recipe -> recipes.put(recipeId, recipe));
                } catch (Exception | LinkageError e) {
                    EmiLog.warn("Skipping vanilla recipe " + recipeId + ": " + e);
                }
            }
        } catch (Exception | LinkageError e) {
            EmiLog.warn("Unable to parse client-bundled vanilla recipe fallback: " + e);
            return null;
        } finally {
            // Vanilla's resource manager can own open pack resources on some
            // loaders. Close it if it supports the closeable API.
            if (resources instanceof CloseableResourceManager closeable) {
                closeable.close();
            }
        }

        List<RecipeHolder<?>> holders = new ArrayList<>(recipes.size());
        recipes.forEach((id, recipe) -> holders.add(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, id), recipe)));
        EmiLog.info("Loaded " + holders.size() + " bundled vanilla recipe fallbacks");
        return holders.isEmpty() ? null : EmiSyncedRecipeMaps.fromHolders(holders);
    }
}
