package dev.emi.emi.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Maps;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.registry.EmiTags;

/** EMI tag wrapper with a read-only vanilla snapshot on serverless connections. */
public class EmiTagKey<T> {
    public static final Map<TagKey<?>, EmiTagKey<?>> CACHE = Maps.newHashMap();
    private final TagKey<T> raw;
    private List<T> cached;

    private EmiTagKey(TagKey<T> raw) {
        this.raw = raw;
        recalculate();
    }

    public void recalculate() {
        cached = stream().toList();
    }

    public TagKey<T> raw() {
        return raw;
    }

    public boolean isOf(Registry<?> registry) {
        return raw.isFor(registry.key());
    }

    public Identifier id() {
        return raw.location();
    }

    @SuppressWarnings("unchecked")
    public Registry<T> registry() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            Optional<? extends Registry<T>> current = client.level.registryAccess().lookup(raw.registry());
            if (current.isPresent()) {
                return current.get();
            }
        }
        // Some remote connections do not include static registries in their
        // world-specific registry lookup. These registries are still built in.
        if (raw.registry().equals(Registries.ITEM)) {
            return (Registry<T>) BuiltInRegistries.ITEM;
        }
        if (raw.registry().equals(Registries.BLOCK)) {
            return (Registry<T>) BuiltInRegistries.BLOCK;
        }
        if (raw.registry().equals(Registries.FLUID)) {
            return (Registry<T>) BuiltInRegistries.FLUID;
        }
        return null;
    }

    public Stream<T> stream() {
        Registry<T> registry = registry();
        if (registry == null) {
            return Stream.empty();
        }
        if (EmiClientRecipeFallback.isUsingFallback()) {
            List<T> vanilla = VanillaRecipeTags.get(raw);
            if (vanilla != null) {
                return filterFluids(registry, vanilla.stream());
            }
        }
        Optional<Named<T>> opt = registry.get(raw);
        if (opt.isEmpty()) {
            return Stream.empty();
        }
        return filterFluids(registry, opt.get().stream().map(Holder::value));
    }

    private Stream<T> filterFluids(Registry<T> registry, Stream<T> values) {
        if (registry == EmiPort.getFluidRegistry()) {
            return values.filter(item -> {
                Fluid fluid = (Fluid) item;
                return fluid.isSource(fluid.defaultFluidState());
            });
        }
        return values;
    }

    public List<T> getList() {
        return cached;
    }

    public Set<T> getSet() {
        return stream().collect(Collectors.toSet());
    }

    public Component getTagName() {
        String s = getTagTranslationKey();
        if (s == null) {
            return EmiPort.literal("#" + this.id());
        }
        return EmiPort.translatable(s);
    }

    public boolean hasTranslation() {
        return getTagTranslationKey() != null;
    }

    private @Nullable String getTagTranslationKey() {
        Identifier registry = raw.registry().identifier();
        if (registry.getNamespace().equals("minecraft")) {
            String s = translatePrefix("tag." + registry.getPath().replace("/", ".") + ".", this.id());
            if (s != null) {
                return s;
            }
        } else {
            String s = translatePrefix("tag." + registry.getNamespace() + "." + registry.getPath().replace("/", ".") + ".", this.id());
            if (s != null) {
                return s;
            }
        }
        return translatePrefix("tag.", this.id());
    }

    private static @Nullable String translatePrefix(String prefix, Identifier id) {
        String s = EmiUtil.translateId(prefix, id);
        if (net.minecraft.locale.Language.getInstance().has(s)) {
            return s;
        }
        if (id.getNamespace().equals("forge")) {
            s = EmiUtil.translateId(prefix, EmiPort.id("c", id.getPath()));
            if (net.minecraft.locale.Language.getInstance().has(s)) {
                return s;
            }
        }
        return null;
    }

    public @Nullable Identifier getCustomModel() {
        Identifier rid = this.id();
        if (rid.getNamespace().equals("forge") && !EmiTags.MODELED_TAGS.containsKey(raw())) {
            return EmiTagKey.of(registry(), EmiPort.id("c", rid.getPath())).getCustomModel();
        }
        return EmiTags.MODELED_TAGS.get(raw());
    }

    public boolean hasCustomModel() {
        return getCustomModel() != null;
    }

    @Override
    public int hashCode() {
        return raw().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EmiTagKey other && raw().equals(other.raw());
    }

    @SuppressWarnings("unchecked")
    public static <T> EmiTagKey<T> of(TagKey<T> raw) {
        return (EmiTagKey<T>) CACHE.computeIfAbsent(raw, k -> new EmiTagKey<>(k));
    }

    public static <T> EmiTagKey<T> of(Registry<T> registry, Identifier id) {
        return of(TagKey.create(registry.key(), id));
    }

    public static <T> Stream<EmiTagKey<T>> fromRegistry(Registry<T> registry) {
        Stream<EmiTagKey<T>> actual = registry.getTags().map(named -> EmiTagKey.of(named.key()));
        if (EmiClientRecipeFallback.isUsingFallback()) {
            return Stream.concat(actual, VanillaRecipeTags.keys(registry).map(EmiTagKey::of)).distinct();
        }
        return actual;
    }

    public static void reload() {
        for (EmiTagKey<?> key : CACHE.values()) {
            key.recalculate();
        }
    }
}
