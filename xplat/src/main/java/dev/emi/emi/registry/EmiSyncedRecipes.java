package dev.emi.emi.registry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Client-side copy of the server recipe entries used by EMI on Minecraft 1.21.3.
 *
 * <p>Vanilla 1.21.2 no longer exposes the server's full recipe objects through the
 * client {@code RecipeManager}; it exposes recipe display data instead. EMI still
 * needs the actual recipe objects to build its plugin/index model, so PF-EMI syncs
 * the entries from a PF-EMI server and keeps them here.</p>
 */
public final class EmiSyncedRecipes {
	private static volatile List<RecipeEntry<?>> recipes = List.of();

	private EmiSyncedRecipes() {
	}

	public static void setRecipes(Collection<RecipeEntry<?>> entries) {
		recipes = List.copyOf(entries);
	}

	public static void clear() {
		recipes = List.of();
	}

	public static List<RecipeEntry<?>> values() {
		return recipes;
	}

	public static RecipeEntry<?> get(Identifier id) {
		if (id == null) {
			return null;
		}
		for (RecipeEntry<?> entry : recipes) {
			if (entry.id().getValue().equals(id)) {
				return entry;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Recipe<?>> List<RecipeEntry<T>> getAllOfType(RecipeType<T> type) {
		return recipes.stream()
			.filter(entry -> entry.value().getType() == type)
			.map(entry -> (RecipeEntry<T>) entry)
			.toList();
	}

	public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeEntry<T>> getAllMatches(
			RecipeType<T> type, I input, World world) {
		return getAllOfType(type).stream()
			.filter(entry -> entry.value().matches(input, world))
			.toList();
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeEntry<T>> getFirstMatch(
			RecipeType<T> type, I input, World world) {
		return getAllOfType(type).stream()
			.filter(entry -> entry.value().matches(input, world))
			.findFirst();
	}
}
