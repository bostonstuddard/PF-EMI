package dev.emi.emi.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

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
 * Decode vanilla recipes shipped in the 26.1.2 Minecraft client JAR without
 * installing them into the player's live registries or touching crafting state.
 * This cannot and must not guess server-specific datapack recipes.
 */
public final class VanillaClientRecipeLoader {
    private VanillaClientRecipeLoader() { }

    public static RecipeMap load(RegistryAccess registryAccess) {
        SortedMap<Identifier, Recipe<?>> parsed = new TreeMap<>();
        // Use VANILLA data-pack tags, not arbitrary server tags, when decoding
        // JSON bundled in the client. Pending tags are used only as lookups.
        try (CloseableResourceManager resources = new MultiPackResourceManager(
                PackType.SERVER_DATA, List.of(ServerPacksSource.createVanillaPackSource()))) {
            RegistryAccess.Frozen frozen = registryAccess.freeze();
            List<Registry.PendingTags<?>> vanillaTags =
                    TagLoader.loadTagsForExistingRegistries(resources, frozen);
            List<HolderLookup.RegistryLookup<?>> lookups =
                    TagLoader.buildUpdatedLookups(frozen, vanillaTags);
            HolderLookup.Provider provider = HolderLookup.Provider.create(lookups.stream());
            RegistryOps<JsonElement> ops = provider.createSerializationContext(JsonOps.INSTANCE);
            SimpleJsonResourceReloadListener.scanDirectory(
                    resources, FileToIdConverter.registry(Registries.RECIPE), ops,
                    Recipe.CODEC, parsed);
        } catch (Exception | LinkageError exception) {
            EmiLog.warn("PF-EMI vanilla recipe fallback decode failed: " + exception);
            return null;
        }

        if (parsed.isEmpty()) {
            return null;
        }
        List<RecipeHolder<?>> holders = new ArrayList<>(parsed.size());
        parsed.forEach((id, recipe) -> holders.add(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, id), recipe)));
        EmiLog.info("PF-EMI loaded " + holders.size() + " bundled vanilla recipes for read-only fallback");
        // 26.1.2 RecipeMap.create accepts an Iterable of RecipeHolder<?>.
        // Do NOT copy 26.3's MappedRegistry helper: it is unnecessary here.
        return RecipeMap.create(holders);
    }
}
