package dev.emi.emi.search;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiTagKey;
import net.minecraft.world.item.BlockItem;

public class TagQuery extends Query {
	private final Set<Object> valid;

	public TagQuery(String name) {
		String lowerName = name.toLowerCase();
		valid = Stream.<EmiTagKey<?>>concat(
			EmiTags.TAGS.stream(),
			EmiTagKey.fromRegistry(dev.emi.emi.EmiPort.getBlockRegistry())
		).filter(t -> {
			if (t.hasTranslation() && t.getTagName().getString().toLowerCase().contains(lowerName)) {
				return true;
			}
			return t.id().toString().contains(lowerName);
		}).flatMap(EmiTagKey::stream).collect(Collectors.toSet());
	}

	@Override
	public boolean matches(EmiStack stack) {
		if (stack.getKey() instanceof BlockItem item && valid.contains(item.getBlock())) {
			return true;
		}
		return valid.contains(stack.getKey());
	}
}
