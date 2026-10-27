package dev.emi.emi.recipe;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiLog;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

/** Adapt 26.3 datapack brewing recipes for EMI across all three loaders. */
public final class EmiBrewingRecipeDiscovery {
    private EmiBrewingRecipeDiscovery() {}

    public static void register(EmiRegistry registry) {
        RecipeMap recipes = EmiPort.getRecipeMap();
        if (recipes == null) {
            return;
        }
        for (RecipeHolder<BrewingRecipe> holder : recipes.byType(RecipeType.BREWING)) {
            try {
                BrewingRecipe recipe = holder.value();
                PotionIngredient input = recipe.getInput();
                PotionIngredient reagent = recipe.getReagent();
                ItemStack output = recipe.getOutput().create();
                if (output.isEmpty()) {
                    continue;
                }
                // Both input and reagent can restrict potion contents in 26.3.
                // A plain Ingredient would lose reagent potion predicates.
                EmiIngredient reagentDisplay = displayIngredient(reagent);
                if (reagentDisplay.isEmpty()) {
                    EmiLog.warn("Skipping brewing display without valid reagent variants: " + holder.id());
                    continue;
                }
                int variant = 0;
                for (Holder<Item> item : input.ingredient().items().toList()) {
                    ItemStack base = new ItemStack(item.value());
                    if (input.potions().isPresent()) {
                        // Match the exact component predicate of each datapack recipe.
                        // This avoids inventing potion input types that the recipe does not accept.
                        for (Holder<Potion> potion : EmiPort.getPotionRegistry().listElements().toList()) {
                            ItemStack withPotion = EmiPort.setPotion(base.copy(), potion.value());
                            if (input.test(withPotion)) {
                                add(registry, holder, withPotion, reagentDisplay, output, variant++);
                            }
                        }
                    } else if (input.test(base)) {
                        add(registry, holder, base, reagentDisplay, output, variant++);
                    }
                }
            } catch (Exception e) {
                EmiLog.error("Failed to import Minecraft 26.3 datapack brewing recipe " + holder.id().identifier(), e);
            }
        }
    }

    private static EmiIngredient displayIngredient(PotionIngredient ingredient) {
        if (ingredient.potions().isEmpty()) {
            return EmiIngredient.of(ingredient.ingredient());
        }
        List<EmiStack> variants = new ArrayList<>();
        for (Holder<Item> item : ingredient.ingredient().items().toList()) {
            ItemStack plain = new ItemStack(item.value());
            if (ingredient.test(plain)) {
                variants.add(EmiStack.of(plain));
            }
            for (Holder<Potion> potion : EmiPort.getPotionRegistry().listElements().toList()) {
                ItemStack withPotion = EmiPort.setPotion(plain.copy(), potion.value());
                if (ingredient.test(withPotion)) {
                    variants.add(EmiStack.of(withPotion));
                }
            }
        }
        return EmiIngredient.of(variants);
    }

    private static void add(EmiRegistry registry, RecipeHolder<BrewingRecipe> holder, ItemStack input,
            EmiIngredient reagent, ItemStack output, int variant) {
        Identifier id = EmiPort.id("emi", "/brewing/datapack/" + EmiUtil.subId(holder.id().identifier()) + "/" + variant);
        registry.addRecipe(new EmiBrewingRecipe(EmiStack.of(input),
                reagent, EmiStack.of(output.copy()), id));
    }
}
