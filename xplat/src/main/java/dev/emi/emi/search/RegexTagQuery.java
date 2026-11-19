package dev.emi.emi.search;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiTagKey;
import net.minecraft.world.item.BlockItem;

public class RegexTagQuery extends Query {
	private final Set<Object> valid;

	public RegexTagQuery(String name) {
		Pattern p;
		try {
			p = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
		} catch (Exception e) {
			p = null;
		}
		if (p == null) {
			valid = Set.of();
		} else {
			final Pattern pat = p;
			valid = Stream.<EmiTagKey<?>>concat(
				EmiTags.TAGS.stream(),
				EmiTagKey.fromRegistry(dev.emi.emi.EmiPort.getBlockRegistry())
			).filter(t -> {
				if (t.hasTranslation() && pat.matcher(t.getTagName().getString()).find()) {
					return true;
				}
				return pat.matcher(t.id().toString()).find();
			}).flatMap(EmiTagKey::stream).collect(Collectors.toSet());
		}
	}

	@Override
	public boolean matches(EmiStack stack) {
		if (stack.getKey() instanceof BlockItem item && valid.contains(item.getBlock())) {
			return true;
		}
		return valid.contains(stack.getKey());
	}
}
