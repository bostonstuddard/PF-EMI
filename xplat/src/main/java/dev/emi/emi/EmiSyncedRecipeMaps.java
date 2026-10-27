package dev.emi.emi;

import java.util.Collection;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Minecraft 26.3 keeps reloadable recipes outside the client's ordinary
 * RegistryAccess.  Fabric and Forge already send the authoritative recipe
 * objects, so create a temporary HolderLookup from those actual objects
 * instead of querying the missing client minecraft:recipe registry.
 *
 * Keep this lookup detached from the game's registries: joining another
 * server (or reloading datapacks) replaces the cached map atomically.
 */
public final class EmiSyncedRecipeMaps {
    private EmiSyncedRecipeMaps() {}

    public static RecipeMap fromHolders(Collection<RecipeHolder<?>> recipes) {
        MappedRegistry<Recipe<?>> lookup = new MappedRegistry<>(Registries.RECIPE, Lifecycle.stable());
        for (RecipeHolder<?> holder : recipes) {
            lookup.register(holder.id(), holder.value(), RegistrationInfo.BUILT_IN);
        }
        lookup.freeze();
        return RecipeMap.create(lookup);
    }
}
