package dev.emi.emi.recipe;

import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.emi.emi.EmiPort;
import dev.emi.emi.VanillaPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.special.EmiSmithingTrimRecipe;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.runtime.EmiClientRecipeFallback;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.runtime.EmiTagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

/**
 * Recovery for partial vanilla EMI plugin registration on remote servers.
 * A failing recipe must not remove unrelated categories. Only fill missing
 * recipe IDs; never change the server's actual crafting rules.
 */
public final class EmiVanillaRecipeRecovery {
    private EmiVanillaRecipeRecovery() {}

    public static void register(EmiRegistry registry) {
        if (!EmiClientRecipeFallback.isUsingFallback()) {
            return;
        }
        RecipeMap map = registry.getRecipeMap();
        if (map == null) {
            return;
        }

        // The vanilla plugin may have stopped partway through a recipe loop.
        // Read its current registration rather than registering duplicates.
        List<EmiRecipe> registered = registeredRecipes();
        if (registered == null) {
            return;
        }
        Set<Identifier> known = registered.stream()
                .filter(r -> r != null && r.getId() != null)
                .map(EmiRecipe::getId).collect(Collectors.toCollection(HashSet::new));
        int starting = registered.size();

        recover(registry, map, RecipeType.CRAFTING, known, EmiVanillaRecipeRecovery::crafting);
        recover(registry, map, RecipeType.SMELTING, known,
                r -> new EmiCookingRecipe(r, SMELTING, 1, false));
        recover(registry, map, RecipeType.BLASTING, known,
                r -> new EmiCookingRecipe(r, BLASTING, 2, false));
        recover(registry, map, RecipeType.SMOKING, known,
                r -> new EmiCookingRecipe(r, SMOKING, 2, false));
        recover(registry, map, RecipeType.CAMPFIRE_COOKING, known,
                r -> new EmiCookingRecipe(r, CAMPFIRE_COOKING, 1, true));
        recover(registry, map, RecipeType.STONECUTTING, known,
                EmiStonecuttingRecipe::new);
        recover(registry, map, RecipeType.SMITHING, known,
                EmiVanillaRecipeRecovery::smithing);

        // The original plugin's synthetic sections are private. Invoke their
        // existing implementation only when that category has no recipes; this
        // avoids creating an incomplete imitation of composting/fuels/etc.
        Set<Item> hidden = Stream.concat(
                EmiTagKey.of(EmiPort.getItemRegistry(), EmiTags.HIDDEN_FROM_RECIPE_VIEWERS).stream(),
                EmiPort.getDisabledItems()).collect(Collectors.toSet());
        if (!hasCategory(registered, ANVIL_REPAIRING) && !hasCategory(registered, GRINDING)) {
            invokeVanilla("repair / grindstone", "addRepair",
                    new Class<?>[] { EmiRegistry.class, Set.class }, registry, hidden);
        }
        if (!hasCategory(registered, BREWING)) {
            safely("brewing", () -> EmiAgnos.addBrewingRecipes(registry));
        }
        if (!hasCategory(registered, WORLD_INTERACTION)) {
            List<Item> dyeable = EmiTagKey.of(ItemTags.CAULDRON_CAN_REMOVE_DYE).getList();
            invokeVanilla("world interactions", "addWorldInteraction",
                    new Class<?>[] { EmiRegistry.class, Set.class, List.class }, registry, hidden, dyeable);
        }
        if (!hasCategory(registered, FUEL)) {
            invokeVanilla("fuels", "addFuel",
                    new Class<?>[] { EmiRegistry.class, Set.class }, registry, hidden);
        }
        if (!hasCategory(registered, COMPOSTING)) {
            invokeVanilla("composting", "addComposting",
                    new Class<?>[] { EmiRegistry.class, Set.class }, registry, hidden);
        }
        if (!hasCategory(registered, VanillaPlugin.TAG)) {
            safely("vanilla tags", () -> {
                for (EmiTagKey<?> key : EmiTags.TAGS) {
                    if (new dev.emi.emi.api.stack.TagEmiIngredient(key.raw(), 1).getEmiStacks().size() > 1) {
                        registry.addRecipe(new EmiTagRecipe(key.raw()));
                    }
                }
            });
        }
        EmiLog.info("[EMI Recovery] Vanilla fallback recipe registrations: "
                + starting + " before, " + registered.size() + " after ("
                + (registered.size() - starting) + " recovered). Tag keys: " + EmiTags.TAGS.size());
    }

    private static EmiRecipe crafting(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            if (shaped.getWidth() > 3 || shaped.getHeight() > 3) return null;
            return new EmiShapedRecipe(shaped);
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            return new EmiShapelessRecipe(shapeless);
        }
        if (recipe instanceof CustomRecipe) {
            // Special crafting recipes need the vanilla plugin's specialized
            // representations; do not display invented or incorrect inputs.
            return null;
        }
        List<EmiIngredient> inputs = recipe.placementInfo().ingredients().stream()
                .map(EmiIngredient::of).toList();
        if (inputs.isEmpty()) return null;
        return new EmiCraftingRecipe(inputs, EmiStack.of(EmiPort.getOutput(recipe)),
                EmiPort.getId(recipe), true);
    }

    private static EmiRecipe smithing(SmithingRecipe recipe) {
        if (recipe instanceof SmithingTrimRecipe trim) {
            return new EmiSmithingTrimRecipe(
                    trim.templateIngredient().map(EmiIngredient::of).orElse(EmiStack.EMPTY),
                    EmiIngredient.of(trim.baseIngredient()),
                    trim.additionIngredient().map(EmiIngredient::of).orElse(EmiStack.EMPTY),
                    EmiStack.of(EmiPort.getOutput(recipe)), recipe);
        }
        return new EmiSmithingRecipe(
                recipe.templateIngredient().map(EmiIngredient::of).orElse(EmiStack.EMPTY),
                EmiIngredient.of(recipe.baseIngredient()),
                recipe.additionIngredient().map(EmiIngredient::of).orElse(EmiStack.EMPTY),
                EmiStack.of(EmiPort.getOutput(recipe)), EmiPort.getId(recipe));
    }

    private static <I extends RecipeInput, T extends Recipe<I>> void recover(
            EmiRegistry registry, RecipeMap map, RecipeType<T> type,
            Set<Identifier> known, Function<T, EmiRecipe> convert) {
        int expected = map.byType(type).size();
        int recovered = 0;
        int failed = 0;
        for (var holder : map.byType(type)) {
            Identifier id = holder.id().identifier();
            if (known.contains(id)) continue;
            try {
                EmiRecipe translated = convert.apply(holder.value());
                if (translated != null) {
                    registry.addRecipe(translated);
                    known.add(id);
                    recovered++;
                }
            } catch (Throwable t) {
                failed++;
                EmiReloadLog.warn("[EMI Recovery] Could not translate " + id, t);
            }
        }
        EmiLog.info("[EMI Recovery] " + type + ": " + expected
                + " source recipes, " + recovered + " restored, " + failed + " failed");
    }

    @SuppressWarnings("unchecked")
    private static List<EmiRecipe> registeredRecipes() {
        try {
            Field field = EmiRecipes.class.getDeclaredField("recipes");
            field.setAccessible(true);
            return (List<EmiRecipe>) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            EmiReloadLog.warn("[EMI Recovery] Cannot inspect current registrations; skipping recovery to avoid duplicates", e);
            return null;
        }
    }

    private static boolean hasCategory(List<EmiRecipe> recipes, EmiRecipeCategory category) {
        return recipes.stream().anyMatch(r -> r != null && r.getCategory() == category);
    }

    private static void invokeVanilla(String section, String method, Class<?>[] types, Object... args) {
        safely(section, () -> {
            try {
                Method m = VanillaPlugin.class.getDeclaredMethod(method, types);
                m.setAccessible(true);
                m.invoke(null, args);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot restore " + section, e);
            }
        });
    }

    private static void safely(String section, Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            EmiReloadLog.warn("[EMI Recovery] " + section + " failed; other categories will continue", t);
        }
    }
}
