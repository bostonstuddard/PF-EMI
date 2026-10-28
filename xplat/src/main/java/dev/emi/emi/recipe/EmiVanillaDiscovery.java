package dev.emi.emi.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeSorting;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.mixin.accessor.VillagerTradeAccessor;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.stack.MobEffectEmiStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;

/**
 * Browseable, read-only information categories for Minecraft 26.3 data.
 *
 * These pages use EMI's normal registry and recipe UI. They do not add a second
 * item index, create gameplay recipes, or mutate Minecraft's trading system.
 * The extra information categories are internal; no new public EMI API is needed.
 */
public final class EmiVanillaDiscovery {
    private static final EmiRecipeCategory EFFECTS = category("status_effects", Items.POTION);
    private static final EmiRecipeCategory ENCHANTMENTS = category("enchantments", Items.ENCHANTED_BOOK);
    private static final EmiRecipeCategory PROFESSIONS = category("villager_professions", Items.LECTERN);
    private static final EmiRecipeCategory TRADES = new EmiRecipeCategory(
        EmiPort.id("emi", "villager_trading"), EmiStack.of(Items.EMERALD),
        EmiStack.of(Items.EMERALD), EmiRecipeSorting.compareInputThenOutput());

    private EmiVanillaDiscovery() {
    }

    private static EmiRecipeCategory category(String path, Item icon) {
        return new EmiRecipeCategory(EmiPort.id("emi", path), EmiStack.of(icon));
    }

    /** Called once, before recipes are registered, including when trade data is unavailable. */
    public static void registerCategories(EmiRegistry registry) {
        registry.addCategory(EFFECTS);
        registry.addCategory(ENCHANTMENTS);
        registry.addCategory(PROFESSIONS);
        registry.addCategory(TRADES);
        registry.addWorkstation(EFFECTS, EmiStack.of(Items.POTION));
        registry.addWorkstation(EFFECTS, EmiStack.of(Items.SPLASH_POTION));
        registry.addWorkstation(EFFECTS, EmiStack.of(Items.LINGERING_POTION));
        registry.addWorkstation(ENCHANTMENTS, EmiStack.of(Items.ENCHANTING_TABLE));
        registry.addWorkstation(ENCHANTMENTS, EmiStack.of(Items.ENCHANTED_BOOK));
        registry.addWorkstation(PROFESSIONS, EmiStack.of(Items.VILLAGER_SPAWN_EGG));
        registry.addWorkstation(PROFESSIONS, EmiStack.of(Items.LECTERN));
        registry.addWorkstation(TRADES, EmiStack.of(Items.EMERALD));
        registry.addWorkstation(TRADES, EmiStack.of(Items.VILLAGER_SPAWN_EGG));
        registry.addWorkstation(TRADES, EmiStack.of(Items.WANDERING_TRADER_SPAWN_EGG));
    }

    public static void registerEffects(EmiRegistry registry, Set<Item> hiddenItems) {
        Map<MobEffect, List<EmiIngredient>> potionsByEffect = new LinkedHashMap<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            try {
                // Potion metadata itself can fail to resolve during loader/data-pack
                // synchronization. Never lose the whole status-effect category
                // because one potion did not finish loading.
                if (potion.getEffects().isEmpty()) {
                    continue;
                }
                Holder<Potion> holder = BuiltInRegistries.POTION.wrapAsHolder(potion);
                List<EmiIngredient> variants = new ArrayList<>();
                for (Item item : List.of(Items.POTION, Items.SPLASH_POTION,
                        Items.LINGERING_POTION, Items.TIPPED_ARROW)) {
                    if (!hiddenItems.contains(item)) {
                        variants.add(EmiStack.of(PotionContents.createItemStack(item, holder)));
                    }
                }
                // Keep the exact POTION_CONTENTS for every display variant. EMI's
                // generic EmiIngredient.of(...) may recognize a matching item tag
                // here and replace the four component-bearing stacks with a tag,
                // discarding the potion identity and producing incorrect links.
                if (!variants.isEmpty()) {
                    EmiIngredient family = new ListEmiIngredient(List.copyOf(variants), 1);
                    Set<MobEffect> indexed = new LinkedHashSet<>();
                    for (MobEffectInstance instance : potion.getEffects()) {
                        if (indexed.add(instance.getEffect().value())) {
                            potionsByEffect.computeIfAbsent(instance.getEffect().value(), e -> new ArrayList<>())
                                .add(family);
                        }
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                EmiReloadLog.warn("Failed to index potion " + BuiltInRegistries.POTION.getKey(potion)
                    + "; remaining potions and all independent effects will still register", e);
            }
        }

        // The potion registry covers only a subset of status effects. Register ALL
        // effects, even those that have no brewable potion (e.g. Darkness or Haste).
        int count = 0;
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (id == null) {
                continue;
            }
            try {
                List<Component> description = new ArrayList<>();
                description.add(effect.getDisplayName().copy().withStyle(ChatFormatting.GOLD));
                String classification = switch (effect.getCategory()) {
                    case BENEFICIAL -> "beneficial";
                    case HARMFUL -> "harmful";
                    default -> "neutral";
                };
                description.add(EmiPort.translatable("emi.discovery.effect." + classification));
                if (effect.isInstantaneous()) {
                    description.add(EmiPort.translatable("emi.discovery.effect.instant"));
                }
                List<EmiIngredient> stacks = potionsByEffect.getOrDefault(effect, List.of());
                if (stacks.isEmpty()) {
                    description.add(EmiPort.translatable("emi.discovery.effect.no_potion"));
                } else {
                    description.add(EmiPort.translatable("emi.discovery.effect.potions", stacks.size()));
                }
                String key = "effect." + id.getNamespace() + "." + id.getPath();
                // Let addon/modpack descriptions override our independently written
                // vanilla fallback. Do not reproduce JEED's protected text database.
                addOptionalDescription(description, key + ".description", key + ".desc",
                    "emi.discovery.effect.description." + id.getNamespace() + "." + id.getPath());
                // Potion stacks are related sources, not the effect's identity.
                // Non-brewable effects otherwise have no searchable result, and
                // multiple effects on a potion cannot be indexed individually.
                MobEffectEmiStack effectStack = new MobEffectEmiStack(effect);
                List<EmiIngredient> display = new ArrayList<>();
                display.add(effectStack);
                display.addAll(stacks);
                registry.addEmiStack(effectStack);
                registry.addRecipe(new EffectInfoRecipe(display, stacks, effectStack,
                    description, synthetic("effect", id)));
                count++;
            } catch (RuntimeException | LinkageError e) {
                EmiReloadLog.warn("Failed to register effect page " + id, e);
            }
        }
        int expected = BuiltInRegistries.MOB_EFFECT.size();
        EmiLog.info("[EMI Discovery] Status effects: " + count + "/" + expected
            + " registered; " + potionsByEffect.size() + " effects have matching potion variants");
        if (count != expected) {
            EmiReloadLog.warn("Status effect discovery incomplete: " + count + "/" + expected
                + " effect entries registered; see preceding registration errors");
        }
    }

    public static void registerEnchantments(EmiRegistry registry, Set<Item> hiddenItems) {
        Minecraft minecraft = Minecraft.getInstance();
        // Use the current connection's synchronized registry first, so client
        // ItemStack/Holder references belong to the same registry as EMI's UI.
        // In singleplayer, fall back to the integrated server registry if the
        // client registry is absent or incomplete. Never use a stale server
        // registry when connected to a remote server.
        Registry<Enchantment> enchantments = null;
        String registrySource = "unavailable";
        if (minecraft.getConnection() != null) {
            enchantments = minecraft.getConnection().registryAccess()
                .lookup(Registries.ENCHANTMENT).orElse(null);
            if (enchantments != null) {
                registrySource = "client connection";
            }
        }
        if (minecraft.level != null) {
            Registry<Enchantment> worldRegistry = minecraft.level.registryAccess()
                .lookup(Registries.ENCHANTMENT).orElse(null);
            if (worldRegistry != null && (enchantments == null || worldRegistry.size() > enchantments.size())) {
                enchantments = worldRegistry;
                registrySource = "client world";
            }
        }
        if (minecraft.getSingleplayerServer() != null) {
            Registry<Enchantment> serverRegistry = minecraft.getSingleplayerServer().registryAccess()
                .lookup(Registries.ENCHANTMENT).orElse(null);
            if (serverRegistry != null && (enchantments == null || serverRegistry.size() > enchantments.size())) {
                enchantments = serverRegistry;
                registrySource = "integrated server";
            }
        }
        EmiLog.info("[EMI Discovery] Enchantment registry source: " + registrySource
            + ", entries=" + (enchantments == null ? 0 : enchantments.size()));
        if (enchantments == null || enchantments.size() == 0) {
            EmiReloadLog.warn("No enchantment registry entries available for EMI discovery; "
                + "the Enchantments category will show an unavailable-data message");
            registry.addRecipe(new DiscoveryInfoRecipe(ENCHANTMENTS,
                List.of(EmiStack.of(Items.ENCHANTED_BOOK)),
                List.of(EmiPort.translatable("emi.discovery.enchantment.unavailable")),
                EmiPort.id("emi", "/discovery/enchantment/unavailable")));
            return;
        }

        int expected = enchantments.size();
        int count = 0;
        for (Holder.Reference<Enchantment> holder : enchantments.listElements().toList()) {
            Identifier id = holder.key().identifier();
            try {
                Enchantment enchantment = holder.value();
                // Every level is a distinct book variant. Previously indexing
                // only the maximum level made most of the actual enchanted books
                // undiscoverable and did not match EMI Enchanting's behavior.
                List<EmiStack> books = new ArrayList<>();
                for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
                    try {
                        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        builder.upgrade(holder, level);
                        book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
                        EmiStack variant = EmiStack.of(book).comparison(EmiPort.compareStrict());
                        books.add(variant);
                        // A loader may expose fewer creative-tab book levels.
                        // If index/alias insertion fails, retain the recipe itself.
                        try {
                            if (EmiStackList.stacks.stream().noneMatch(
                                    st -> st.isEqual(variant, EmiPort.compareStrict()))) {
                                registry.addEmiStack(variant);
                            }
                            registry.addAlias(variant, enchantment.description());
                        } catch (RuntimeException | LinkageError e) {
                            EmiReloadLog.warn("Failed to index enchantment book variant " + id
                                + " level " + level + "; keeping the info recipe", e);
                        }
                    } catch (RuntimeException | LinkageError e) {
                        EmiReloadLog.warn("Failed to create enchantment book variant " + id
                            + " level " + level, e);
                    }
                }
                if (books.isEmpty()) {
                    EmiReloadLog.warn("No valid book variants for enchantment " + id
                        + "; cannot create a searchable recipe");
                    continue;
                }
                // Use the registry's declared supported-item holders rather than
                // scanning every registered item and invoking ItemStack checks.
                // One invalid/missing holder must not remove an enchantment page.
                List<EmiIngredient> display = new ArrayList<>();
                List<EmiIngredient> compatible = new ArrayList<>();
                // Keep one rotating book slot and one rotating slot per equipment
                // family. Do not put each helmet/sword/etc. in a separate slot.
                // Unlike a broad "all armor" group, these preserve the item type.
                display.add(new ListEmiIngredient(List.copyOf(books), 1));
                Map<String, List<EmiIngredient>> families = new LinkedHashMap<>();
                try {
                    for (Holder<Item> supportedHolder : enchantment.getSupportedItems()) {
                        try {
                            Item item = supportedHolder.value();
                            if (!hiddenItems.contains(item)) {
                                EmiStack supported = EmiStack.of(item);
                                String family = enchantmentDisplayFamily(supportedHolder);
                                families.computeIfAbsent(family, ignored -> new ArrayList<>())
                                    .add(supported);
                                // Preserve the exact ungrouped membership for
                                // recipe lookups. Display grouping changes no
                                // enchantment compatibility or supported item.
                                compatible.add(supported);
                            }
                        } catch (RuntimeException | LinkageError e) {
                            EmiReloadLog.warn("Failed to index supported item of enchantment " + id, e);
                        }
                    }
                } catch (RuntimeException | LinkageError e) {
                    EmiReloadLog.warn("Could not resolve supported-item set for " + id
                        + "; still registering its book and information", e);
                }
                for (List<EmiIngredient> family : families.values()) {
                    // EMI will use an existing item tag where it exactly matches,
                    // otherwise it renders a rotating list of the actual items.
                    display.add(EmiIngredient.of(family));
                }
                List<Component> description = new ArrayList<>();
                description.add(enchantment.description().copy().withStyle(ChatFormatting.GOLD));
                description.add(EmiPort.translatable("emi.discovery.enchantment.max_level", enchantment.getMaxLevel()));
                description.add(EmiPort.translatable("emi.discovery.enchantment.weight", enchantment.getWeight()));
                if (holder.is(EnchantmentTags.CURSE)) {
                    description.add(EmiPort.translatable("emi.discovery.enchantment.curse").withStyle(ChatFormatting.RED));
                }
                if (enchantment.exclusiveSet().iterator().hasNext()) {
                    description.add(EmiPort.translatable("emi.discovery.enchantment.exclusive"));
                    for (Holder<Enchantment> other : enchantment.exclusiveSet()) {
                        // Incompatible enchantments are secondary information, but the
                        // vanilla light gray (#AAAAAA) is illegible against EMI's
                        // light recipe background. Use Minecraft dark gray (#555555).
                        description.add(other.value().description().copy().withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
                addOptionalDescription(description, "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc");
                registry.addRecipe(new EnchantmentInfoRecipe(display, compatible, books, description,
                    synthetic("enchantment", id)));
                count++;
            } catch (RuntimeException | LinkageError e) {
                EmiReloadLog.warn("Failed to register enchantment page " + id, e);
            }
        }
        EmiLog.info("[EMI Discovery] Enchantments: " + count + "/" + expected + " registered");
        if (count != expected) {
            EmiReloadLog.warn("Enchantment discovery incomplete: " + count + "/" + expected
                + " enchantments registered; see preceding registration errors");
        }
    }

    public static void registerProfessions(EmiRegistry registry, Set<Item> hiddenItems) {
        // In 26.3 the POI types and professions are built-in registries, not the
        // client-level data pack registry. Do not require a nullable world lookup.
        int count = 0;
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
            if (id == null || "none".equals(id.getPath()) || "nitwit".equals(id.getPath())) {
                continue;
            }
            try {
                Set<Item> sites = new LinkedHashSet<>();
                for (PoiType poi : BuiltInRegistries.POINT_OF_INTEREST_TYPE) {
                    if (profession.heldJobSite().test(BuiltInRegistries.POINT_OF_INTEREST_TYPE.wrapAsHolder(poi))) {
                        for (var state : poi.matchingStates()) {
                            Item item = state.getBlock().asItem();
                            if (item != Items.AIR && !hiddenItems.contains(item)) {
                                sites.add(item);
                            }
                        }
                    }
                }
                List<EmiIngredient> stacks = new ArrayList<>();
                stacks.add(EmiStack.of(Items.VILLAGER_SPAWN_EGG));
                stacks.add(EmiStack.of(Items.EMERALD));
                for (Item item : sites) {
                    stacks.add(EmiStack.of(item));
                }
                List<Component> description = new ArrayList<>();
                description.add(profession.name().copy().withStyle(ChatFormatting.GOLD));
                description.add(EmiPort.translatable("emi.discovery.profession.sites"));
                if (sites.isEmpty()) {
                    description.add(EmiPort.translatable("emi.discovery.profession.no_site"));
                }
                if (!profession.tradeSetsByLevel().isEmpty()) {
                    description.add(EmiPort.translatable("emi.discovery.profession.trade_levels"));
                }
                registry.addRecipe(new DiscoveryInfoRecipe(PROFESSIONS, stacks, description, synthetic("profession", id)));
                count++;
            } catch (RuntimeException e) {
                EmiReloadLog.warn("Failed to register villager profession page " + id, e);
            }
        }
        EmiLog.info("[EMI Discovery] Villager professions: " + count + " registered");
    }

    public static void registerTrades(EmiRegistry registry, Set<Item> hiddenItems) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var tradeSets = minecraft.level.registryAccess().lookup(Registries.TRADE_SET);
        if (tradeSets.isEmpty() && minecraft.getSingleplayerServer() != null) {
            // Integrated servers may hold registries omitted from the client view.
            // Never use a local world's registry while connected to another server.
            tradeSets = minecraft.getSingleplayerServer().registryAccess().lookup(Registries.TRADE_SET);
        }
        if (tradeSets.isEmpty()) {
            addTradeUnavailablePage(registry);
            EmiLog.info("[EMI Discovery] Trade sets unavailable on client; no offers guessed");
            return;
        }

        Registry<TradeSet> sets = tradeSets.get();
        int count = 0;
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
            if (id == null) {
                continue;
            }
            for (int level = 1; level <= 5; level++) {
                var key = profession.getTrades(level);
                if (key != null) {
                    var set = sets.getOptional(key);
                    if (set.isPresent()) {
                        count += addTradeSet(registry, set.get(),
                            id.getNamespace() + "/" + id.getPath() + "/" + level,
                            level, profession.name(), hiddenItems);
                    }
                }
            }
        }
        for (var entry : sets.entrySet()) {
            Identifier id = entry.getKey().identifier();
            if (id.getPath().startsWith("wandering_trader/")) {
                count += addTradeSet(registry, entry.getValue(),
                    id.getNamespace() + "/" + id.getPath(), 0,
                    EmiPort.translatable("entity.minecraft.wandering_trader"), hiddenItems);
            }
        }
        if (count == 0) {
            addTradeUnavailablePage(registry);
        }
        EmiLog.info("[EMI Discovery] Villager trade templates: " + count + " registered");
    }

    private static int addTradeSet(EmiRegistry registry, TradeSet set, String path, int level,
            Component traderName, Set<Item> hiddenItems) {
        int count = 0;
        int index = 0;
        for (Holder<VillagerTrade> holder : set.trades()) {
            try {
                VillagerTradeAccessor trade = (VillagerTradeAccessor) (Object) holder.value();
                var output = trade.emi$getGives().create();
                if (!output.isEmpty() && !hiddenItems.contains(output.getItem()) &&
                        !hiddenItems.contains(trade.emi$getWants().item().value()) &&
                        trade.emi$getAdditionalWants().map(c -> !hiddenItems.contains(c.item().value())).orElse(true)) {
                    registry.addRecipe(new EmiVillagerTradeRecipe(TRADES,
                        EmiPort.id("emi", "/discovery/trade/" + path + "/" + index),
                        trade.emi$getWants(), trade.emi$getAdditionalWants().orElse(null),
                        EmiStack.of(output), level, traderName));
                    count++;
                }
            } catch (RuntimeException e) {
                EmiReloadLog.warn("Failed to register trade template " + path + "/" + index, e);
            }
            index++;
        }
        return count;
    }

    private static void addTradeUnavailablePage(EmiRegistry registry) {
        registry.addRecipe(new DiscoveryInfoRecipe(TRADES, List.of(EmiStack.of(Items.EMERALD)),
            List.of(EmiPort.translatable("emi.discovery.trade.unavailable")),
            EmiPort.id("emi", "/discovery/trade/unavailable")));
    }

    private static void addOptionalDescription(List<Component> description, String... keys) {
        for (String key : keys) {
            if (Language.getInstance().has(key)) {
                description.add(Component.translatable(key));
                return;
            }
        }
    }

    private static Identifier synthetic(String type, Identifier id) {
        return EmiPort.id("emi", "/discovery/" + type + "/" + id.getNamespace() + "/" + id.getPath());
    }

    /**
     * Returns a stable display family for an already-confirmed supported item.
     * Prefer Minecraft's item-type tags (including contributions by mods);
     * the suffix fallback covers older or incomplete modded tag registrations.
     * Unknown items stay separate instead of being grouped by material, or
     * worse, being displayed as acceptable when they are not.
     */
    private static String enchantmentDisplayFamily(Holder<Item> holder) {
        // Special headwear is a separate entry, not a material variant of a
        // helmet. Skulls get their own family below.
        if (holder.value() == Items.CARVED_PUMPKIN) {
            return "single/minecraft:carved_pumpkin";
        }
        // These are Minecraft's specific equipment/item-type tags, not the
        // broad enchantable tags which mix multiple equipment types together.
        // Give tagged and untagged variants the same family key so, for
        // example, a modded helmet doesn't form a second helmet group.
        if (holder.is(ItemTags.SKULLS)) return "type/skull";
        if (holder.is(ItemTags.HEAD_ARMOR)) return "type/helmet";
        if (holder.is(ItemTags.CHEST_ARMOR)) return "type/chestplate";
        if (holder.is(ItemTags.LEG_ARMOR)) return "type/leggings";
        if (holder.is(ItemTags.FOOT_ARMOR)) return "type/boots";
        if (holder.is(ItemTags.SWORDS)) return "type/sword";
        if (holder.is(ItemTags.AXES)) return "type/axe";
        if (holder.is(ItemTags.SPEARS)) return "type/spear";
        if (holder.is(ItemTags.PICKAXES)) return "type/pickaxe";
        if (holder.is(ItemTags.SHOVELS)) return "type/shovel";
        if (holder.is(ItemTags.HOES)) return "type/hoe";
        Identifier id = BuiltInRegistries.ITEM.getKey(holder.value());
        String path = id.getPath();
        for (String suffix : List.of("helmet", "chestplate", "leggings", "boots",
                "sword", "axe", "spear", "pickaxe", "shovel", "hoe", "horse_armor")) {
            if (path.endsWith("_" + suffix)) {
                return "type/" + suffix;
            }
        }
        if (path.endsWith("_head") || path.endsWith("_skull")) {
            return "type/skull";
        }
        return "single/" + id;
    }

    /**
     * An information page about an enchantment, not a recipe that creates all
     * compatible items. Keep the supported-item examples in the EMI widgets,
     * but only index the enchanted book as a result. This prevents EMI's
     * disabled-ingredient filter from removing otherwise valid pages.
     */
    private static final class EnchantmentInfoRecipe extends EmiInfoRecipe {
        private final List<EmiStack> books;
        private final List<EmiIngredient> supportedItems;

        private EnchantmentInfoRecipe(List<EmiIngredient> display,
                List<EmiIngredient> compatible, List<EmiStack> books,
                List<Component> description, Identifier id) {
            super(display, description, id);
            this.books = List.copyOf(books);
            // A single alternative ingredient is a lookup relationship, not
            // a claim that every supported item is consumed by a recipe.
            this.supportedItems = compatible.isEmpty() ? List.of()
                : List.of(new ListEmiIngredient(List.copyOf(compatible), 1));
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return ENCHANTMENTS;
        }

        @Override
        public List<EmiIngredient> getInputs() {
            return supportedItems;
        }

        @Override
        public List<EmiStack> getOutputs() {
            return books;
        }
    }

    /** An effect is a standalone searchable result; potions are related sources. */
    private static final class EffectInfoRecipe extends EmiInfoRecipe {
        private final EmiStack effect;
        private final List<EmiIngredient> potions;

        private EffectInfoRecipe(List<EmiIngredient> display,
                List<EmiIngredient> potions, EmiStack effect,
                List<Component> description, Identifier id) {
            super(display, description, id);
            this.effect = effect;
            this.potions = List.copyOf(potions);
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return EFFECTS;
        }

        @Override
        public List<EmiIngredient> getInputs() {
            return List.of();
        }

        @Override
        public List<EmiIngredient> getCatalysts() {
            return potions;
        }

        @Override
        public List<EmiStack> getOutputs() {
            return List.of(effect);
        }

        @Override
        public int getDisplayHeight() {
            return Math.min(144, super.getDisplayHeight());
        }
    }

    /** Reuse EMI's existing information-page renderer without changing its public ABI. */
    private static final class DiscoveryInfoRecipe extends EmiInfoRecipe {
        private final EmiRecipeCategory category;

        private DiscoveryInfoRecipe(EmiRecipeCategory category, List<EmiIngredient> items,
                List<Component> description, Identifier id) {
            super(items, description, id);
            this.category = category;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return category;
        }

        @Override
        public int getDisplayHeight() {
            // The normal Info recipe is allowed to grow for arbitrarily long
            // descriptions. Keep effect pages inside a typical recipe screen;
            // EmiInfoRecipe's built-in pager handles any overflow.
            return category == EFFECTS ? Math.min(144, super.getDisplayHeight()) : super.getDisplayHeight();
        }
    }
}
