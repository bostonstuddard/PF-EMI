package dev.emi.emi.recipe;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.emi.stack.MobEffectEmiStack;
import dev.emi.emi.stack.serializer.MobEffectEmiStackSerializer;
import net.minecraft.world.item.Item;

/**
 * Built-in 26.3 information pages. Registered by EmiReloadManager after the
 * vanilla plugin on every loader, without depending on the vanilla recipe
 * translation pass completing successfully.
 *
 * This intentionally uses EMI's public plugin/registry APIs, not a second
 * recipe index or loader-specific recipe registration path.
 */
public final class EmiDiscoveryPlugin implements EmiPlugin {
    @Override
    public void initialize(EmiInitRegistry registry) {
        registry.addIngredientSerializer(MobEffectEmiStack.class, new MobEffectEmiStackSerializer());
    }

    @Override
    public void register(EmiRegistry registry) {
        EmiVanillaDiscovery.registerCategories(registry);
        Set<Item> hiddenItems = Stream.concat(
            EmiTagKey.of(EmiPort.getItemRegistry(), EmiTags.HIDDEN_FROM_RECIPE_VIEWERS).stream(),
            EmiPort.getDisabledItems()
        ).collect(Collectors.toSet());
        EmiLog.info("[EMI Discovery] Starting built-in 26.3 information plugin");
        safely("status effects", () -> EmiVanillaDiscovery.registerEffects(registry, hiddenItems));
        safely("enchantments", () -> EmiVanillaDiscovery.registerEnchantments(registry, hiddenItems));
        safely("villager professions", () -> EmiVanillaDiscovery.registerProfessions(registry, hiddenItems));
        safely("villager trades", () -> EmiVanillaDiscovery.registerTrades(registry, hiddenItems));
    }

    private static void safely(String section, Runnable task) {
        try {
            task.run();
        } catch (RuntimeException | LinkageError e) {
            EmiReloadLog.warn("[EMI Discovery] Failed to register " + section +
                "; remaining sections will still be processed", e);
        }
    }
}
