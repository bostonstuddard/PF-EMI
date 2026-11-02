package dev.emi.emi.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import dev.emi.emi.EmiSyncedRecipeMaps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
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
        try (CloseableResourceManager resources = new MultiPackResourceManager(
                PackType.SERVER_DATA, List.of(ServerPacksSource.createVanillaPackSource()))) {
            RegistryAccess.Frozen frozen = registryAccess.freeze();
            List<Registry.PendingTags<?>> pendingTags =
                    TagLoader.loadTagsForExistingRegistries(resources, frozen);
            List<HolderLookup.RegistryLookup<?>> lookups =
                    TagLoader.buildUpdatedLookups(frozen, pendingTags);
            HolderLookup.Provider provider = HolderLookup.Provider.create(lookups.stream());
            RegistryOps<JsonElement> ops = provider.createSerializationContext(JsonOps.INSTANCE);
            SimpleJsonResourceReloadListener.scanDirectory(
                    resources,
                    FileToIdConverter.registry(Registries.RECIPE),
                    ops,
                    Recipe.CODEC,
                    recipes);
        } catch (Exception | LinkageError e) {
            EmiLog.warn("Unable to parse client-bundled vanilla recipe fallback: " + e);
            return null;
        }

        List<RecipeHolder<?>> holders = new ArrayList<>(recipes.size());
        recipes.forEach((id, recipe) -> holders.add(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, id), recipe)));
        EmiLog.info("Loaded " + holders.size() + " bundled vanilla recipe fallbacks");
        return holders.isEmpty() ? null : EmiSyncedRecipeMaps.fromHolders(holders);
    }
}
