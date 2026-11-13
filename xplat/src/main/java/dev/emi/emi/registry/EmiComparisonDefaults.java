package dev.emi.emi.registry;

import java.util.Map;
import java.util.Set;

import dev.emi.emi.api.stack.Comparison;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class EmiComparisonDefaults {
	public static Map<Object, Comparison> comparisons = Map.of();

	// Item variants such as Iron Jetpacks tiers share an item ID but have
	// different component data. Their recipes must be indexed by components,
	// otherwise clicking a later tier opens the first tier's recipe.
	private static final Set<String> COMPONENT_AWARE_IRON_JETPACKS = Set.of(
		"jetpack", "cell", "thruster", "capacitor");

	public static Comparison get(Object obj) {
		if (comparisons.containsKey(obj)) {
			return comparisons.get(obj);
		}
		if (obj instanceof Item item) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id != null && "ironjetpacks".equals(id.getNamespace())
					&& COMPONENT_AWARE_IRON_JETPACKS.contains(id.getPath())) {
				return Comparison.compareComponents();
			}
		}
		return Comparison.DEFAULT_COMPARISON;
	}
}
