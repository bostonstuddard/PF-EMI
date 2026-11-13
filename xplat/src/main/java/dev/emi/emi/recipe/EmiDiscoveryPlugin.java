package dev.emi.emi.recipe;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiClientRecipeFallback;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.runtime.EmiTagKey;
import dev.emi.emi.stack.MobEffectEmiStack;
import dev.emi.emi.stack.serializer.MobEffectEmiStackSerializer;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;

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
        // A remote vanilla server can expose a smaller creative-tab index than
        // singleplayer. Do not hide registered vanilla items merely because
        // a creative tab omitted them. Only append missing entries while using
        // the read-only client fallback; server-supplied maps are unaffected.
        safely("vanilla fallback index", () -> ensureCompleteVanillaIndex(registry, hiddenItems));
        safely("status effects", () -> EmiVanillaDiscovery.registerEffects(registry, hiddenItems));
        safely("enchantments", () -> EmiVanillaDiscovery.registerEnchantments(registry, hiddenItems));
        safely("villager professions", () -> EmiVanillaDiscovery.registerProfessions(registry, hiddenItems));
        safely("villager trades", () -> EmiVanillaDiscovery.registerTrades(registry, hiddenItems));
    }

    private static void ensureCompleteVanillaIndex(EmiRegistry registry, Set<Item> hiddenItems) {
        if (!EmiClientRecipeFallback.isUsingFallback()) {
            return;
        }

        // Reuse EMI's strict stack comparison so different potion variants
        // remain distinct while duplicates from creative tabs are avoided.
        Set<EmiStack> indexed = new ObjectOpenCustomHashSet<>(new EmiStackList.StrictHashStrategy());
        indexed.addAll(EmiStackList.stacks);
        int addedItems = 0;
        int addedPotions = 0;

        for (Item item : EmiPort.getItemRegistry()) {
            if (item == Items.AIR || hiddenItems.contains(item)) {
                continue;
            }
            EmiStack stack = EmiStack.of(item);
            if (!stack.isEmpty() && "minecraft".equals(stack.getId().getNamespace()) && indexed.add(stack)) {
                registry.addEmiStack(stack);
                addedItems++;
            }
        }

        // Items alone are not a complete browser: bottled potions and tipped
        // arrows represent distinct searchable vanilla item variants.
        for (Holder<Potion> holder : EmiPort.getPotionRegistry().listElements().toList()) {
            Potion potion = holder.value();
            for (Item item : new Item[] { Items.POTION, Items.SPLASH_POTION,
                    Items.LINGERING_POTION, Items.TIPPED_ARROW }) {
                if (hiddenItems.contains(item)) {
                    continue;
                }
                ItemStack variant = EmiPort.setPotion(new ItemStack(item), potion);
                EmiStack stack = EmiStack.of(variant);
                if (!stack.isEmpty() && indexed.add(stack)) {
                    registry.addEmiStack(stack);
                    addedPotions++;
                }
            }
        }
        EmiLog.info("[EMI] Vanilla serverless index: " + addedItems
                + " missing base items and " + addedPotions + " potion variants restored ("
                + indexed.size() + " indexed stacks before filtering)");
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
