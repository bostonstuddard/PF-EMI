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
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
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
 * Read-only discovery pages backported from PF-EMI 26.3 v1.1 to 26.2.
 * There are no synthetic gameplay recipes, custom network packets, or
 * guessed server trade prices. All registry lookups use the current world.
 */
public final class EmiVanillaDiscovery {
    private static final EmiRecipeCategory EFFECTS = category("status_effects", Items.POTION, "Status Effects");
    private static final EmiRecipeCategory ENCHANTMENTS = category("enchantments", Items.ENCHANTED_BOOK, "Enchantments");
    private static final EmiRecipeCategory PROFESSIONS = category("villager_professions", Items.LECTERN, "Villager Professions");
    private static final EmiRecipeCategory TRADES = new EmiRecipeCategory(
        EmiPort.id("emi", "villager_trading"), EmiStack.of(Items.EMERALD), EmiStack.of(Items.EMERALD),
        EmiRecipeSorting.compareInputThenOutput()) {
        @Override public Component getName() { return Component.literal("Villager Trading (Potential Offers)"); }
    };

    private EmiVanillaDiscovery() {}

    private static EmiRecipeCategory category(String path, Item icon, String name) {
        return new EmiRecipeCategory(EmiPort.id("emi", path), EmiStack.of(icon)) {
            @Override public Component getName() { return Component.literal(name); }
        };
    }

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
                if (potion.getEffects().isEmpty()) continue;
                Holder<Potion> holder = BuiltInRegistries.POTION.wrapAsHolder(potion);
                List<EmiIngredient> variants = new ArrayList<>();
                for (Item item : List.of(Items.POTION, Items.SPLASH_POTION,
                        Items.LINGERING_POTION, Items.TIPPED_ARROW)) {
                    if (!hiddenItems.contains(item)) {
                        variants.add(EmiStack.of(PotionContents.createItemStack(item, holder)));
                    }
                }
                if (!variants.isEmpty()) {
                    // ListEmiIngredient (not tag compression) retains POTION_CONTENTS.
                    EmiIngredient family = new ListEmiIngredient(List.copyOf(variants), 1);
                    Set<MobEffect> seen = new LinkedHashSet<>();
                    for (MobEffectInstance effect : potion.getEffects()) {
                        if (seen.add(effect.getEffect().value())) {
                            potionsByEffect.computeIfAbsent(effect.getEffect().value(), key -> new ArrayList<>())
                                .add(family);
                        }
                    }
                }
            } catch (RuntimeException | LinkageError e) {
                EmiReloadLog.warn("[EMI Discovery] Could not index potion "
                    + BuiltInRegistries.POTION.getKey(potion) + "; skipping only this potion", e);
            }
        }

        int count = 0;
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (id == null) continue;
            try {
                List<Component> description = new ArrayList<>();
                description.add(effect.getDisplayName().copy().withStyle(ChatFormatting.GOLD));
                String kind = switch (effect.getCategory()) {
                    case BENEFICIAL -> "Beneficial status effect";
                    case HARMFUL -> "Harmful status effect";
                    default -> "Neutral status effect";
                };
                description.add(Component.literal(kind));
                if (effect.isInstantaneous()) description.add(Component.literal("Applies instantly"));
                List<EmiIngredient> potionSources = potionsByEffect.getOrDefault(effect, List.of());
                description.add(Component.literal(potionSources.isEmpty()
                    ? "No registered potion for this effect"
                    : "Registered potion variants: " + potionSources.size()));
                String name = effectDescription(id);
                if (name != null) description.add(Component.literal(name));
                addOptionalDescription(description, "effect." + id.getNamespace() + "." + id.getPath() + ".description",
                    "effect." + id.getNamespace() + "." + id.getPath() + ".desc");
                MobEffectEmiStack effectStack = new MobEffectEmiStack(effect);
                List<EmiIngredient> display = new ArrayList<>();
                display.add(effectStack);
                display.addAll(potionSources);
                registry.addEmiStack(effectStack);
                registry.addRecipe(new EffectInfoRecipe(display, potionSources, effectStack, description,
                    synthetic("effect", id)));
                count++;
            } catch (RuntimeException | LinkageError e) {
                EmiReloadLog.warn("[EMI Discovery] Failed to register effect " + id, e);
            }
        }
        EmiLog.info("[EMI Discovery] Status effects: " + count + "/"
            + BuiltInRegistries.MOB_EFFECT.size() + " registered; " + potionsByEffect.size()
            + " effects have potion sources");
    }

    public static void registerEnchantments(EmiRegistry registry, Set<Item> hiddenItems) {
        Minecraft mc = Minecraft.getInstance();
        Registry<Enchantment> enchantments = null;
        if (mc.getConnection() != null) {
            enchantments = mc.getConnection().registryAccess().lookup(Registries.ENCHANTMENT).orElse(null);
        }
        if (mc.level != null) {
            Registry<Enchantment> world = mc.level.registryAccess().lookup(Registries.ENCHANTMENT).orElse(null);
            if (world != null && (enchantments == null || world.size() > enchantments.size())) {
                enchantments = world;
            }
        }
        // Integrated server data must never be used when connected to a remote server.
        if (mc.getSingleplayerServer() != null) {
            Registry<Enchantment> local = mc.getSingleplayerServer().registryAccess()
                .lookup(Registries.ENCHANTMENT).orElse(null);
            if (local != null && (enchantments == null || local.size() > enchantments.size())) {
                enchantments = local;
            }
        }
        if (enchantments == null || enchantments.size() == 0) {
            registry.addRecipe(new DiscoveryInfoRecipe(ENCHANTMENTS,
                List.of(EmiStack.of(Items.ENCHANTED_BOOK)),
                List.of(Component.literal("Enchantment registry data is unavailable in this world.")),
                EmiPort.id("emi", "/discovery/enchantment/unavailable")));
            EmiLog.warn("[EMI Discovery] Enchantment registry is missing; no entries were guessed");
            return;
        }
        int count = 0;
        for (Holder.Reference<Enchantment> holder : enchantments.listElements().toList()) {
            Identifier id = holder.key().identifier();
            try {
                Enchantment enchantment = holder.value();
                List<EmiStack> books = new ArrayList<>();
                for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
                    try {
                        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                        ItemEnchantments.Mutable contents = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        contents.upgrade(holder, level);
                        book.set(DataComponents.STORED_ENCHANTMENTS, contents.toImmutable());
                        EmiStack variant = EmiStack.of(book).comparison(EmiPort.compareStrict());
                        books.add(variant);
                        try {
                            if (EmiStackList.stacks.stream().noneMatch(
                                    stack -> stack.isEqual(variant, EmiPort.compareStrict()))) {
                                registry.addEmiStack(variant);
                            }
                            registry.addAlias(variant, enchantment.description());
                        } catch (RuntimeException | LinkageError e) {
                            EmiReloadLog.warn("[EMI Discovery] Unable to index enchantment book "
                                + id + " level " + level + "; preserving the information page", e);
                        }
                    } catch (RuntimeException | LinkageError e) {
                        EmiReloadLog.warn("[EMI Discovery] Book variant " + id + " level " + level, e);
                    }
                }
                if (books.isEmpty()) continue;
                List<EmiIngredient> display = new ArrayList<>();
                List<EmiIngredient> compatible = new ArrayList<>();
                display.add(new ListEmiIngredient(List.copyOf(books), 1));
                Map<String, List<EmiIngredient>> groups = new LinkedHashMap<>();
                try {
                    for (Holder<Item> supported : enchantment.getSupportedItems()) {
                        try {
                            Item item = supported.value();
                            if (!hiddenItems.contains(item)) {
                                EmiStack stack = EmiStack.of(item);
                                groups.computeIfAbsent(enchantmentDisplayFamily(supported),
                                    ignored -> new ArrayList<>()).add(stack);
                                compatible.add(stack);
                            }
                        } catch (RuntimeException | LinkageError e) {
                            EmiReloadLog.warn("[EMI Discovery] Bad supported item for " + id, e);
                        }
                    }
                } catch (RuntimeException | LinkageError e) {
                    EmiReloadLog.warn("[EMI Discovery] Couldn't access supported-item set for " + id, e);
                }
                for (List<EmiIngredient> group : groups.values()) {
                    display.add(EmiIngredient.of(group));
                }
                List<Component> description = new ArrayList<>();
                description.add(enchantment.description().copy().withStyle(ChatFormatting.GOLD));
                description.add(Component.literal("Maximum level: " + enchantment.getMaxLevel()));
                description.add(Component.literal("Enchantment weight: " + enchantment.getWeight()));
                if (holder.is(EnchantmentTags.CURSE)) {
                    description.add(Component.literal("Curse").withStyle(ChatFormatting.RED));
                }
                if (enchantment.exclusiveSet().iterator().hasNext()) {
                    description.add(Component.literal("Incompatible with:"));
                    for (Holder<Enchantment> other : enchantment.exclusiveSet()) {
                        description.add(other.value().description().copy().withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
                addOptionalDescription(description, "enchantment." + id.getNamespace() + "." + id.getPath() + ".desc");
                registry.addRecipe(new EnchantmentInfoRecipe(display, compatible, books,
                    description, synthetic("enchantment", id)));
                count++;
            } catch (RuntimeException | LinkageError e) {
                EmiReloadLog.warn("[EMI Discovery] Failed to register enchantment " + id, e);
            }
        }
        EmiLog.info("[EMI Discovery] Enchantments: " + count + "/" + enchantments.size() + " registered");
    }

    public static void registerProfessions(EmiRegistry registry, Set<Item> hiddenItems) {
        int count = 0;
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
            if (id == null || "none".equals(id.getPath()) || "nitwit".equals(id.getPath())) continue;
            try {
                Set<Item> sites = new LinkedHashSet<>();
                for (PoiType poi : BuiltInRegistries.POINT_OF_INTEREST_TYPE) {
                    if (profession.heldJobSite().test(BuiltInRegistries.POINT_OF_INTEREST_TYPE.wrapAsHolder(poi))) {
                        for (var state : poi.matchingStates()) {
                            Item item = state.getBlock().asItem();
                            if (item != Items.AIR && !hiddenItems.contains(item)) sites.add(item);
                        }
                    }
                }
                List<EmiIngredient> display = new ArrayList<>();
                display.add(EmiStack.of(Items.VILLAGER_SPAWN_EGG));
                display.add(EmiStack.of(Items.EMERALD));
                for (Item item : sites) display.add(EmiStack.of(item));
                List<Component> description = new ArrayList<>();
                description.add(profession.name().copy().withStyle(ChatFormatting.GOLD));
                description.add(Component.literal("Job-site workstation(s) shown above"));
                if (sites.isEmpty()) description.add(Component.literal("No job-site block found in this registry"));
                if (!profession.tradeSetsByLevel().isEmpty()) {
                    description.add(Component.literal("Trades depend on the server and profession level"));
                }
                registry.addRecipe(new DiscoveryInfoRecipe(PROFESSIONS, display,
                    description, synthetic("profession", id)));
                count++;
            } catch (RuntimeException | LinkageError e) {
                EmiReloadLog.warn("[EMI Discovery] Failed to register profession " + id, e);
            }
        }
        EmiLog.info("[EMI Discovery] Villager professions: " + count + " registered");
    }

    public static void registerTrades(EmiRegistry registry, Set<Item> hiddenItems) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var tradeSets = mc.level.registryAccess().lookup(Registries.TRADE_SET);
        if (tradeSets.isEmpty() && mc.getSingleplayerServer() != null) {
            tradeSets = mc.getSingleplayerServer().registryAccess().lookup(Registries.TRADE_SET);
        }
        if (tradeSets.isEmpty()) {
            addTradeUnavailable(registry);
            EmiLog.info("[EMI Discovery] Trade sets not present on client; no offers guessed");
            return;
        }
        Registry<TradeSet> sets = tradeSets.get();
        int count = 0;
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            Identifier id = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
            if (id == null) continue;
            for (int level = 1; level <= 5; level++) {
                var key = profession.getTrades(level);
                if (key != null) {
                    var offerSet = sets.getOptional(key);
                    if (offerSet.isPresent()) {
                        count += addTradeSet(registry, offerSet.get(),
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
                    Component.translatable("entity.minecraft.wandering_trader"), hiddenItems);
            }
        }
        if (count == 0) addTradeUnavailable(registry);
        EmiLog.info("[EMI Discovery] Villager trade templates: " + count + " registered");
    }

    private static int addTradeSet(EmiRegistry registry, TradeSet set, String path, int level,
            Component trader, Set<Item> hiddenItems) {
        int count = 0;
        int i = 0;
        for (Holder<VillagerTrade> holder : set.getTrades()) {
            try {
                VillagerTradeAccessor trade = (VillagerTradeAccessor) (Object) holder.value();
                ItemStack output = trade.emi$getGives().create();
                if (!output.isEmpty() && !hiddenItems.contains(output.getItem())
                        && !hiddenItems.contains(trade.emi$getWants().item().value())
                        && trade.emi$getAdditionalWants()
                            .map(c -> !hiddenItems.contains(c.item().value())).orElse(true)) {
                    registry.addRecipe(new EmiVillagerTradeRecipe(TRADES,
                        EmiPort.id("emi", "/discovery/trade/" + path + "/" + i),
                        trade.emi$getWants(), trade.emi$getAdditionalWants().orElse(null),
                        EmiStack.of(output), level, trader));
                    count++;
                }
            } catch (RuntimeException | LinkageError e) {
                EmiReloadLog.warn("[EMI Discovery] Failed to register trade template " + path + "/" + i, e);
            }
            i++;
        }
        return count;
    }

    private static void addTradeUnavailable(EmiRegistry registry) {
        registry.addRecipe(new DiscoveryInfoRecipe(TRADES, List.of(EmiStack.of(Items.EMERALD)),
            List.of(Component.literal("Trade templates are not available from this server. "
                + "PF-EMI does not guess trade offers or prices.")),
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

    private static String enchantmentDisplayFamily(Holder<Item> holder) {
        Item item = holder.value();
        if (item == Items.CARVED_PUMPKIN) return "single/minecraft:carved_pumpkin";
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
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        String name = id.getPath();
        for (String suffix : List.of("helmet", "chestplate", "leggings", "boots", "sword",
                "axe", "spear", "pickaxe", "shovel", "hoe", "horse_armor")) {
            if (name.endsWith("_" + suffix)) return "type/" + suffix;
        }
        if (name.endsWith("_head") || name.endsWith("_skull")) return "type/skull";
        return "single/" + id;
    }

    private static String effectDescription(Identifier id) {
        if (!"minecraft".equals(id.getNamespace())) return null;
        return switch (id.getPath()) {
            case "speed" -> "Increases movement speed; stronger levels increase the boost.";
            case "slowness" -> "Reduces movement speed; stronger levels slow movement further.";
            case "haste" -> "Speeds up mining and repeated attack actions.";
            case "mining_fatigue" -> "Slows block breaking and repeated attacks.";
            case "strength" -> "Increases melee attack damage.";
            case "instant_health" -> "Restores health immediately; harms undead instead.";
            case "instant_damage" -> "Deals immediate damage; heals undead instead.";
            case "jump_boost" -> "Increases jump height and reduces some fall damage.";
            case "nausea" -> "Distorts the screen with a disorienting effect.";
            case "regeneration" -> "Gradually restores health while active.";
            case "resistance" -> "Reduces most incoming damage.";
            case "fire_resistance" -> "Prevents damage from fire, lava and burning hazards.";
            case "water_breathing" -> "Prevents oxygen loss underwater.";
            case "invisibility" -> "Hides the entity, though equipment may still be visible.";
            case "blindness" -> "Limits visibility to nearby surroundings.";
            case "night_vision" -> "Brightens dark areas, including underwater.";
            case "hunger" -> "Causes the hunger meter to drain more quickly.";
            case "weakness" -> "Reduces melee attack damage.";
            case "poison" -> "Damages living entities over time but normally cannot kill.";
            case "wither" -> "Deals repeated damage and can be lethal.";
            case "health_boost" -> "Temporarily increases maximum health.";
            case "absorption" -> "Adds temporary extra hearts that absorb damage.";
            case "saturation" -> "Immediately replenishes hunger and saturation.";
            case "glowing" -> "Outlines the entity so it can be seen through blocks.";
            case "levitation" -> "Causes the entity to rise into the air.";
            case "luck" -> "Raises luck, affecting supported loot rolls.";
            case "unluck" -> "Lowers luck, affecting supported loot rolls.";
            case "slow_falling" -> "Slows falling and prevents ordinary fall damage.";
            case "conduit_power" -> "Improves underwater mining and visibility and supplies water breathing.";
            case "dolphins_grace" -> "Improves swimming speed in water.";
            case "bad_omen" -> "Can turn into a raid or trial omen when an encounter begins.";
            case "hero_of_the_village" -> "Provides villager trade discounts after a successful raid.";
            case "darkness" -> "Periodically darkens the surroundings and reduces visibility.";
            case "raid_omen" -> "Precedes an ominous raid encounter.";
            case "trial_omen" -> "Makes trial spawners use ominous behavior.";
            case "wind_charged" -> "Releases wind when the entity dies.";
            case "weaving" -> "Creates cobwebs when the entity dies.";
            case "oozing" -> "Can spawn slimes when the entity dies.";
            case "infested" -> "Can summon silverfish when the entity is damaged.";
            default -> null;
        };
    }

    /** Only supported items are inputs. Only actual enchanted books are outputs. */
    private static final class EnchantmentInfoRecipe extends EmiInfoRecipe {
        private final List<EmiStack> books;
        private final List<EmiIngredient> supported;
        private EnchantmentInfoRecipe(List<EmiIngredient> display, List<EmiIngredient> compatible,
                List<EmiStack> books, List<Component> description, Identifier id) {
            super(display, description, id);
            this.books = List.copyOf(books);
            this.supported = compatible.isEmpty() ? List.of()
                : List.of(new ListEmiIngredient(List.copyOf(compatible), 1));
        }
        @Override public EmiRecipeCategory getCategory() { return ENCHANTMENTS; }
        @Override public List<EmiIngredient> getInputs() { return supported; }
        @Override public List<EmiStack> getOutputs() { return books; }
    }

    /** Potion sources are indexed as catalysts; the effect is its own output. */
    private static final class EffectInfoRecipe extends EmiInfoRecipe {
        private final EmiStack effect;
        private final List<EmiIngredient> potions;
        private EffectInfoRecipe(List<EmiIngredient> display, List<EmiIngredient> sources,
                EmiStack effect, List<Component> description, Identifier id) {
            super(display, description, id);
            this.effect = effect;
            this.potions = List.copyOf(sources);
        }
        @Override public EmiRecipeCategory getCategory() { return EFFECTS; }
        @Override public List<EmiIngredient> getInputs() { return List.of(); }
        @Override public List<EmiIngredient> getCatalysts() { return potions; }
        @Override public List<EmiStack> getOutputs() { return List.of(effect); }
        @Override public int getDisplayHeight() { return Math.min(144, super.getDisplayHeight()); }
    }

    private static final class DiscoveryInfoRecipe extends EmiInfoRecipe {
        private final EmiRecipeCategory category;
        private DiscoveryInfoRecipe(EmiRecipeCategory category, List<EmiIngredient> items,
                List<Component> description, Identifier id) {
            super(items, description, id);
            this.category = category;
        }
        @Override public EmiRecipeCategory getCategory() { return category; }
    }
}
