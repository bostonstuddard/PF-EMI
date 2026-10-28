package dev.emi.emi.recipe;

import java.util.HashMap;
import java.util.Map;

import dev.emi.emi.EmiPort;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

/**
 * Discovers items with the 26.3 COOKING_FUEL component for all three loaders.
 *
 * Vanilla 26.3 primarily uses registry references instead of literal values.
 * The client recipe browser has no server-side furnace LootContext, so it must
 * not attempt to execute those context-dependent providers. Vanilla's default
 * normal-furnace burn times are mirrored from ContextIntProviders.bootstrap.
 *
 * If a mod or data pack supplies a custom provider, only literal values and
 * references to the known vanilla provider IDs can be displayed accurately
 * without server-authoritative synchronization. Unknown references are omitted
 * rather than being displayed with a fabricated burn time.
 */
public final class EmiFuelDiscovery {
    private EmiFuelDiscovery() {
    }

    public static Map<Item, Integer> getFuelMap() {
        Map<Item, Integer> fuels = new HashMap<>();
        for (Item item : EmiPort.getItemRegistry()) {
            CookingFuel component = new ItemStack(item).get(DataComponents.COOKING_FUEL);
            if (component == null) {
                continue;
            }
            int burnTime = getNormalFurnaceBurnTime(component.burnTime());
            if (burnTime > 0) {
                fuels.put(item, burnTime);
            }
        }
        return fuels;
    }

    /** Returns ticks in a normal furnace (not the reduced fast-furnace duration). */
    static int getNormalFurnaceBurnTime(ResolvableInt burnTime) {
        if (burnTime instanceof ResolvableInt.Constant constant) {
            return Math.max(0, constant.value());
        }
        if (burnTime instanceof ResolvableInt.Reference reference) {
            Identifier id = reference.key().identifier();
            if (!"minecraft".equals(id.getNamespace())) {
                return 0;
            }
            // Minecraft 26.3: ContextIntProviders.bootstrap's COOKING_TIME_* defaults.
            // This includes all default wooden blocks, logs, planks, stairs, slabs,
            // fences, signs, tools, boats, wool, bamboo, roots, and special fuels.
            return switch (id.getPath()) {
                case "cooking/time_bamboo", "cooking/time_wool_slabs" -> 50;
                case "cooking/time_wool_carpets" -> 67;
                case "cooking/time_dry_plants", "cooking/time_wood_items_extra_small",
                     "cooking/time_wool" -> 100;
                case "cooking/time_wood_slabs" -> 150;
                case "cooking/time_wood_items_large" -> 200;
                case "cooking/time_roots", "cooking/time_wood_blocks",
                     "cooking/time_wood_items_small" -> 300;
                case "cooking/time_hanging_signs" -> 800;
                case "cooking/time_boats" -> 1200;
                case "cooking/time_coal" -> 1600;
                case "cooking/time_blaze_rod" -> 2400;
                case "cooking/time_dried_kelp_block" -> 4001;
                case "cooking/time_coal_block" -> 16000;
                case "cooking/time_lava_bucket" -> 20000;
                default -> 0;
            };
        }
        return 0;
    }
}
