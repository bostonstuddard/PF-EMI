package dev.emi.emi.api.recipe;

import java.util.List;

/**
 * Opt-in recipe tree estimates for partially consumed input items.
 *
 * <p>Used only for bill-of-materials calculations. It does not modify crafting,
 * inventory transfer, recipe matching, or any actual item remainder. A recipe
 * can declare that an input representing a reusable resource (such as a spool)
 * has a fixed capacity in units, and that each craft consumes some of those units.
 *
 * <p>The matching {@link #getInputs()} ingredient remains a normal EMI ingredient;
 * use its list position as the input index. Mod developers must provide units
 * themselves. EMI never guesses from NBT, damage, or stack remainder. A partially
 * depleted physical item is not automatically measured from inventory state.
 *
 * <p>Example: one 1000-unit spool, consuming 50 units per craft, is declared as
 * {@code new PartialInput(0, 1000, 50)}. Eight crafts then require one spool in
 * the tree estimate instead of eight. Modded recipes may supply several entries.
 *
 * <p>ABI: entirely additive. Existing {@link EmiRecipe} implementations do not
 * implement this interface and are unaffected.
 */
public interface EmiPartialConsumptionRecipe extends EmiRecipe {

    /**
     * @return an optional description for one or more indices in {@link #getInputs()}.
     *         Indices not listed retain normal EMI semantics.
     */
    default List<PartialInput> getPartialInputs() {
        return List.of();
    }

    /**
     * @param inputIndex index in {@link EmiRecipe#getInputs()}
     * @param capacityUnits units contained in one fresh input item, greater than zero
     * @param usedUnitsPerCraft units consumed across this input entry per recipe craft,
     *                          greater than zero. For multi-count ingredients, this is
     *                          the combined consumption for that recipe input entry.
     */
    record PartialInput(int inputIndex, long capacityUnits, long usedUnitsPerCraft) {
        public PartialInput {
            if (inputIndex < 0 || capacityUnits <= 0 || usedUnitsPerCraft <= 0) {
                throw new IllegalArgumentException("Partial input values must have a nonnegative index and positive units");
            }
        }
    }
}
