package dev.emi.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.Reader;
import java.util.Comparator;
import java.util.stream.Stream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.FluidUnit;
import dev.emi.emi.recipe.ProjectFlintProcessRecipe;
import dev.emi.emi.recipe.ProjectFlintLootRecipe;
import dev.emi.emi.recipe.ProjectFlintBasinRecipe;
import dev.emi.emi.recipe.ProjectFlintGridRecipe;
import dev.emi.emi.recipe.EmiShapedRecipe;
import dev.emi.emi.recipe.EmiShapelessRecipe;
import dev.emi.emi.platform.EmiAgnos;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * PF-EMI's authoritative ProjectFlint integration.
 *
 * ProjectFlint itself does not need or register an EMI plugin. Everything here
 * resolves by registry id so PF-EMI remains independently buildable.
 */
public final class ProjectFlintPlugin implements EmiPlugin {
	public static final String PF_EMI_PATCH_LEVEL = "pf.23";

	private static final String PF = "projectflint";

	private static final List<String> STRIPPED_LOGS = List.of(
		"stripped_oak_log",
		"stripped_spruce_log",
		"stripped_birch_log",
		"stripped_jungle_log",
		"stripped_acacia_log",
		"stripped_dark_oak_log",
		"stripped_mangrove_log",
		"stripped_cherry_log",
		"stripped_pale_oak_log"
	);

	private static final List<String> WOODEN_BEAMS = List.of(
		"wooden_beam",
		"acacia_wooden_beam",
		"birch_wooden_beam",
		"cherry_wooden_beam",
		"crimson_wooden_beam",
		"dark_oak_wooden_beam",
		"jungle_wooden_beam",
		"mangrove_wooden_beam",
		"pale_oak_wooden_beam",
		"spruce_wooden_beam",
		"warped_wooden_beam"
	);


	// Exact JSON recipe ids from PersonalCraftingGuideRegistry and the Hardwood tool
	// definitions. Removing native EMI's generic Crafting display prevents the same
	// item from having inconsistent workstation categories.
	private static final List<String> WOOD_VARIANTS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak");
	private static final Set<String> REMAPPED_GRID_RECIPES = createRemappedRecipeIds();
	// Populated from the live ProjectFlint JSON crafting registry at recipe reload.
	private static final Set<String> AUTO_REMAPPED_RECIPES = new java.util.HashSet<>();
	private static EmiRecipeCategory personalCategory;

	private static Set<String> createRemappedRecipeIds() {
		java.util.Set<String> ids = new java.util.HashSet<>(List.of(
			"leather_sheet_from_tanned", "leather_straps_from_sheet", "leather_repair_kit",
			"crushed_stone_from_cobblestone",
			"basin_crafting", "sticks_from_sheet", "sand_from_flakes",
			"clay_block_from_clay_balls", "dirt_from_loose_soil", "loose_soil_from_dirt",
			"dirt_piles_from_loose_soil", "fat_burning_torch",
			"hardwood_crafting_table",
			"hardwood", "personal_wooden_cups", "personal_wooden_cups_raw_milk",
			"personal_pmi_sheets", "wooden_axe", "wooden_pickaxe",
			"wooden_shovel", "wooden_sword_personal", "wooden_hoe_personal"
		));
		for (String wood : WOOD_VARIANTS) {
			ids.add(wood + "_planks_from_log");
			ids.add(wood + "_planks_from_stripped_log");
			ids.add(wood + "_sheet_from_plank");
			ids.add(wood + "_board_from_plank");
			ids.add(wood + "_door_hinged");
		}
		return Set.copyOf(ids);
	}

	@Override
	public void register(EmiRegistry registry) {
		if (!hasPfItem("rock")) {
			return;
		}

		ProjectFlintRecipeIndex.reset();
		AUTO_REMAPPED_RECIPES.clear();

		/*
		 * ProjectFlint progression is intentionally not the vanilla crafting catalog.
		 * Keep only recipes explicitly supplied by PF-EMI. World-interaction entries
		 * survive because every recipe created below has a projectflint:* id.
		 */
		registry.removeRecipes(recipe -> {
			Identifier id = recipe.getId();
			// Hide vanilla-furnace displays for ProjectFlint's primitive meats.
			// This is VIEWER-ONLY: their native JSON registrations remain in the
			// ProjectFlint gameplay mod until it retires them explicitly.
			if (id != null && PF.equals(id.getNamespace())
				&& (id.getPath().equals("personal_pmi_sheets")
				|| id.getPath().equals("primitive_wooden_sheet_from_stripped_log"))) return true;
			if (id != null && PF.equals(id.getNamespace())
				&& (recipe.getCategory() == VanillaEmiRecipeCategories.SMELTING
					|| recipe.getCategory() == VanillaEmiRecipeCategories.SMOKING
					|| recipe.getCategory() == VanillaEmiRecipeCategories.BLASTING
					|| recipe.getCategory() == VanillaEmiRecipeCategories.CAMPFIRE_COOKING)
				&& id.getPath().matches("raw_(calamari|turtle_meat|fox_meat|frog_meat|goat_meat|horse_meat|llama_meat|dolphin_meat|bear_meat|camel_meat)_(smelting|smoking|blasting|campfire_cooking)")) return true;
			// ProjectFlint's custom workstation recipes must not acquire an
			// extra fake vanilla Crafting entry. Keep non-crafting EMI pages.
			if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING) {
				if (recipe.getOutputs().stream().anyMatch(out ->
					pfId("tanned_utility_vest").equals(out.getId()))) return true;
				// PF's native crafting pages duplicate our workstation-specific grids.
				// New PF recipes are routed by registerOtherProjectFlintCrafting;
				// unknown special types are never misrepresented as vanilla crafts.
				if (id != null && PF.equals(id.getNamespace())) return true;
			}
			return id == null || !PF.equals(id.getNamespace());
		});

		registerWorldInteractions(registry);
		registerStationRecipes(registry);
		registerLumberProcessing(registry);
		registerWell(registry);
		registerHeatProcessing(registry);
		registerAdvancedKiln(registry);
		registerMissingWorldRoutes(registry);
		registerDynamiteBlasting(registry);
		registerFarmingRoutes(registry);
		registerBasinRoutes(registry);
		registerUndergroundHazards(registry);
		registerFireclayForgeRoutes(registry);
		registerPersonalCrafting(registry);
		registerHardwoodCrafting(registry);
		registerBlockDropRecipes(registry);
		registerMobLootRecipes(registry);
		registerStationTransferHandlers(registry);

		// Register structure guides from the SAME entrypoint so they cannot vanish
		// because a second EMI plugin was omitted/reordered.
		ProjectFlintMultiblockPlugin.registerAll(registry);

		// Last: make the item index match the recipes/routes that are actually visible.
		registry.removeEmiStacks(ProjectFlintRecipeIndex::shouldHide);
	}

	/** Register only exact known ProjectFlint workstation layouts.
	 * Output slots, fake previews, and fuel/progress controls are never fill targets.
	 */
	@SuppressWarnings("unchecked")
	private static void registerStationTransferHandlers(EmiRegistry registry) {
		stationHandler(registry, "clay_grinder", "clay_grinder", 0);
		stationHandler(registry, "wooden_grinder", "wooden_grinder", 0, 1);
		stationHandler(registry, "sanding_station", "sanding_station", 0);
		stationHandler(registry, "brick_furnace", "brick_furnace", 1);
		stationHandler(registry, "brick_blast_furnace", "brick_blast_furnace", 1);
	}

	@SuppressWarnings("unchecked")
	private static void stationHandler(EmiRegistry registry, String menuId, String categoryId, int... inputs) {
		var id = pfId(menuId);
		var menu = net.minecraft.registry.Registries.SCREEN_HANDLER.get(id);
		if (menu == null || !id.equals(net.minecraft.registry.Registries.SCREEN_HANDLER.getId(menu))) return;
		registry.addRecipeHandler((net.minecraft.screen.ScreenHandlerType<net.minecraft.screen.ScreenHandler>) menu,
			new dev.emi.emi.handler.ProjectFlintStationRecipeHandler(categoryId, inputs));
	}

	private static void registerWorldInteractions(EmiRegistry registry) {
		EmiIngredient hammer = anyPf(1,
			"crude_stone_hammer", "crude_multitool", "flint_multitool", "advanced_flint_multitool");
		EmiIngredient cutter = anyPf(1,
			"crude_harvesting_tool", "crude_multitool", "flint_multitool", "advanced_flint_multitool");
		EmiIngredient multitool = anyPf(1,
			"crude_multitool", "flint_multitool", "advanced_flint_multitool");
		EmiIngredient firestarter = anyPf(1,
			"flint_firestarter", "bone_firestarter", "wood_firestarter");

		// Rock progression.
		world(registry, "rock_to_refined",
			List.of(pf("rock")), List.of(pf("rock")), pf("refined_rock"));
		world(registry, "rock_to_very_refined",
			List.of(pf("rock")), List.of(pf("crushed_stone")), pf("very_refined_stone"));

		// Earliest physical ground assemblies.
		world(registry, "crude_plant_string",
			List.of(pf("plant_fiber", 2), pf("refined_rock")), List.of(), pf("crude_plant_string"));
		world(registry, "crude_stone_hammer",
			List.of(pf("lightly_burnt_twig", 2), pf("rock"), pf("refined_rock")),
			List.of(), pf("crude_stone_hammer"));
		world(registry, "plant_tool_binding",
			List.of(pf("crude_reed", 4), pf("crude_plant_string", 2)),
			List.of(hammer), pf("plant_tool_binding"));
		world(registry, "crude_plant_cutters",
			List.of(pf("plant_tool_binding"), pf("very_refined_stone", 2), pf("lightly_burnt_twig")),
			List.of(hammer), pf("crude_harvesting_tool"));

		// Exact current crude multitool assembly: NO hammer.
		world(registry, "crude_multitool",
			List.of(pf("burnt_twig", 2), pf("plant_tool_binding", 4), pf("very_refined_stone", 4)),
			List.of(), pf("crude_multitool"));

		// Flint assemblies.
		world(registry, "flint_firestarter",
			List.of(pf("branch", 8), mc("flint", 2)), List.of(), pf("flint_firestarter"));
		world(registry, "flint_multitool",
			List.of(pf("burnt_branch", 2), mc("flint", 2)), List.of(), pf("flint_multitool"));

		// AdvancedToolAssemblyBlock — these are the exact current recipes.
		world(registry, "flint_axe",
			List.of(pf("flint_axe_pieces"), pf("burnt_branch", 2)), List.of(), pf("flint_axe"));
		world(registry, "flint_plant_cutters",
			List.of(pf("flint_cutter_piece"), pf("burnt_branch", 2)), List.of(), pf("flint_plant_cutters"));
		world(registry, "advanced_flint_multitool",
			List.of(pf("refined_flint_multitool_piece"), pf("burnt_branch", 2)),
			List.of(), pf("advanced_flint_multitool"));

		// Woven physical assemblies.
		world(registry, "reed_backpack",
			List.of(pf("woven_fan", 8), pf("plant_tool_binding", 2), pf("woven_wool", 2)),
			List.of(), pf("reed_backpack"));
		world(registry, "bone_brush",
			List.of(pf("woven_fan", 4), pf("filed_bone", 4)), List.of(), pf("bone_brush"));
		world(registry, "crudely_woven_bowl",
			List.of(pf("burnt_twig", 12), pf("dirt_pile", 32)), List.of(), pf("crudely_woven_bowl"));

		// Piles / clay progression.
		world(registry, "loose_dirt",
			List.of(pf("dirt_pile", 4)), List.of(hammer), pf("loose_dirt"));
		worldTimed(registry, "fired_clay",
			List.of(pf("unfired_clay_ball"), pf("twig", 4)), List.of(firestarter),
			pf("fired_clay", 2), 20 * 30);
		world(registry, "crude_clay_block_from_fired_clay",
			List.of(pf("fired_clay", 4)), List.of(hammer), pf("crude_clay_block"));
		world(registry, "clay_grinder_assembly",
			List.of(pf("clay_grinding_base"), pf("very_refined_stone", 12)),
			List.of(), pf("clay_grinder"));

		// Stone working.
		world(registry, "campfire_stone",
			List.of(pf("very_refined_stone")), List.of(pf("very_refined_stone")), pf("campfire_stone"));
		world(registry, "split_burnt_twig",
			List.of(pf("burnt_twig")), List.of(hammer), pf("lightly_burnt_twig", 2));
		world(registry, "knap_flint",
			List.of(pf("flint_chip", 4)), List.of(hammer), mc("flint"));
		world(registry, "reed_roll",
			List.of(pf("crude_reed", 4)), List.of(multitool), pf("reed_roll"));
		world(registry, "sand_ball",
			List.of(pf("sand_flake", 4)), List.of(multitool), pf("sand_ball"));

		// Sugar cane yields differ by tool tier.
		world(registry, "reeds_cutters",
			List.of(mc("sugar_cane")), List.of(pf("crude_harvesting_tool")), pf("crude_reed", 3));
		world(registry, "reeds_crude_multitool",
			List.of(mc("sugar_cane")), List.of(pf("crude_multitool")), pf("crude_reed", 4));
		world(registry, "reeds_flint_multitool",
			List.of(mc("sugar_cane")), List.of(pf("flint_multitool")), pf("crude_reed", 6));
		world(registry, "reeds_advanced_multitool",
			List.of(mc("sugar_cane")), List.of(pf("advanced_flint_multitool")), pf("crude_reed", 8));

		// Wattle / daub.
		world(registry, "wattle",
			List.of(pf("crude_reed", 12), pf("twig", 12)), List.of(), pf("wattle", 4));
		world(registry, "tied_wattle",
			List.of(pf("wattle"), pf("crude_plant_string")), List.of(), pf("tied_wattle"));
		world(registry, "unfired_wattle_and_daub",
			List.of(pf("tied_wattle", 4), pf("wet_daub_pile", 2)),
			List.of(), pf("unfired_wattle_and_daub", 2));

		// Sanding-by-hand / simple item transforms.
		world(registry, "filed_bone",
			List.of(any(1, pf("animal_bones"), mc("bone"))), List.of(pf("sanding_paper")), pf("filed_bone"));
		world(registry, "bone_firestarter",
			List.of(pf("filed_bone"), mc("flint")), List.of(), pf("bone_firestarter"));
		world(registry, "woven_wool",
			List.of(pf("wool", 2)), List.of(pf("sanding_paper")), pf("woven_wool"));
		world(registry, "black_dye_from_ink",
			List.of(mc("ink_sac")), List.of(pf("sanding_paper")), mc("black_dye"));
		world(registry, "woven_fan",
			List.of(pf("plant_tool_binding")), List.of(pf("sanding_paper")), pf("woven_fan"));

		// Water interactions. Open water is catalytic, not consumed.
		EmiIngredient water = EmiStack.of(Fluids.WATER, FluidUnit.BUCKET);
		world(registry, "mud_pile",
			List.of(pf("dirt_pile")), List.of(water), pf("mud_pile"));
		world(registry, "wet_daub_pile",
			List.of(pf("daub_pile")), List.of(water), pf("wet_daub_pile"));
		world(registry, "washed_leather",
			List.of(pf("ground_leather")), List.of(water), mc("leather"));
		world(registry, "sugarcane_roll",
			List.of(pf("reed_roll")), List.of(water), pf("sugarcane_roll"));
		world(registry, "unfired_mortar_bricks",
			List.of(pf("clay_mortar")), List.of(water), pf("unfired_mortar_brick", 4));

		// World block transforms.
		world(registry, "daub_pile",
			List.of(pf("mud_pile"), mc("oak_leaves")), List.of(), pf("daub_pile"));
		world(registry, "sanded_wood_slab_world",
			List.of(anyMc(1, "oak_slab", "spruce_slab", "birch_slab", "jungle_slab",
				"acacia_slab", "dark_oak_slab", "mangrove_slab", "cherry_slab", "pale_oak_slab")),
			List.of(pf("sanding_paper")), pf("sanded_wood_slab"));
		world(registry, "woven_wood_slab_world",
			List.of(pf("grinded_wood_slab")), List.of(pf("woven_fan")), pf("woven_wood_slab"));
		world(registry, "mortar_kiln_formation",
			List.of(pf("mortar_kiln_base"), pf("advanced_kiln_glass")), List.of(), pf("mortar_kiln"));
		world(registry, "wooden_grinder_formation",
			List.of(pf("wooden_grinder_base"), pf("stone_grindsaw")), List.of(), pf("wooden_grinder"));
		world(registry, "woven_chest_upgrade",
			List.of(pf("mortar_chest"), pf("woven_chest_frame")), List.of(), pf("woven_chest"));
		// Place the Wooden Chest Frame against an existing Woven Chest. The frame
		// is consumed, the block is upgraded, and all nine stored items carry over.
		// The Woven Chest is the placed, non-consumed target rather than a crafting ingredient.
		world(registry, "wooden_chest_upgrade",
			List.of(pf("wooden_chest_frame")), List.of(pf("woven_chest")), pf("wooden_chest"));
		world(registry, "loose_dirt_slab",
			List.of(pf("loose_dirt")), List.of(multitool), pf("loose_dirt_slab"));
		world(registry, "unfired_woven_bowl",
			List.of(mc("clay"), pf("crudely_woven_bowl")), List.of(), pf("unfired_woven_bowl"));
		world(registry, "mortar_chest",
			List.of(pf("mortar_bricks"), pf("woven_fan")), List.of(), pf("mortar_chest"));

		// Clay bowl scooping chain.
		world(registry, "fired_clay_bowl_2",
			List.of(mc("clay"), pf("fired_clay_bowl")), List.of(), pf("fired_clay_bowl_2"));
		world(registry, "fired_clay_bowl_4",
			List.of(mc("clay"), pf("fired_clay_bowl_2")), List.of(), pf("fired_clay_bowl_4"));
		world(registry, "fired_clay_bowl_6",
			List.of(mc("clay"), pf("fired_clay_bowl_4")), List.of(), pf("fired_clay_bowl_6"));
		world(registry, "unfired_clay_ball_from_bowl",
			List.of(mc("clay"), pf("fired_clay_bowl_6")), List.of(), pf("unfired_clay_ball"));
		world(registry, "clay_pot_2",
			List.of(mc("clay"), pf("clay_pot")), List.of(), pf("clay_pot_2"));
		world(registry, "unfired_clay_ball_from_pot",
			List.of(mc("clay"), pf("clay_pot_2")), List.of(), pf("unfired_clay_ball"));
		world(registry, "crude_clay_kiln",
			List.of(pf("crude_clay_block")), List.of(cutter), pf("crude_clay_kiln"));
	}

	private static void registerStationRecipes(EmiRegistry registry) {
		EmiRecipeCategory clay = category(registry, "clay_grinder", "clay_grinder");
		EmiRecipeCategory wood = category(registry, "wooden_grinder", "wooden_grinder");
		EmiRecipeCategory sand = category(registry, "sanding_station", "sanding_station");
		EmiRecipeCategory shelf = category(registry, "bookshelf", "bookshelf");
		EmiRecipeCategory stove = category(registry, "primitive_stove", "primitive_stove");

		if (clay != null) registerClayConversions(registry, clay, "clay");
		// Wooden Grinder can also perform all Clay Grinder one-input conversions.
		if (wood != null) registerClayConversions(registry, wood, "wood_clay");

		if (wood != null) {
			EmiIngredient flintFile = pf("flint_file");
			EmiIngredient boneKnife = pf("bone_knife");

			simple(registry, wood, "wood/ground_leather",
				List.of(any(2, pf("raw_leather"), mc("leather")), pf("sanding_paper")), pf("ground_leather"));
			simple(registry, wood, "wood/woven_chest_frame",
				List.of(mc("leather", 2), pf("woven_wood", 4)), pf("woven_chest_frame"));
			simple(registry, wood, "wood/leather_backpack",
				List.of(mc("leather", 4), pf("reed_backpack")), pf("leather_backpack"));
			simple(registry, wood, "wood/backpack_chest",
				List.of(pf("leather_backpack"), pf("wooden_chest", 2)), pf("leather_backpack_with_chest"));
			simple(registry, wood, "wood/wattle_window",
				List.of(pf("wattled_daub_bricks", 4), pf("advanced_kiln_glass")), pf("wattled_daub_window", 3));
			simple(registry, wood, "wood/woven_fan",
				List.of(pf("sugarcane_roll"), pf("plant_tool_binding")), pf("woven_fan", 2));
			simple(registry, wood, "wood/sanding_paper",
				List.of(pf("sugarcane_roll", 3), pf("plant_tool_binding")), pf("sanding_paper", 2));
			// New v0.32.1 later-stage Wattle conversion; early ground assembly is unchanged.
			simple(registry, wood, "wood/wattle_from_cane",
				List.of(mc("sugar_cane", 3), pf("branch", 2)), pf("wattle", 4));
			simple(registry, wood, "wood/tied_wattle",
				List.of(pf("woven_fan"), pf("wattle")), pf("tied_wattle", 3));
			simple(registry, wood, "wood/daub",
				List.of(pf("flint_chip"), pf("mud_pile")), pf("daub_pile", 3));
			simple(registry, wood, "wood/unfired_wattle_daub",
				List.of(pf("tied_wattle", 4), pf("wet_daub_pile", 2)), pf("unfired_wattle_and_daub", 4));
			simple(registry, wood, "wood/torch_holder",
				List.of(pf("mortar_bricks", 8), pf("mud_pile", 16)), pf("torch_holder"));
			simple(registry, wood, "wood/unlit_flint_torch",
				List.of(mc("flint", 12), pf("woven_wood")), pf("unlit_flint_torch"));
			simple(registry, wood, "wood/flint_torch_holder",
				List.of(pf("torch_holder"), pf("unlit_flint_torch")), pf("flint_torch_holder"));
			simple(registry, wood, "wood/sanding_station",
				List.of(pf("sanded_wood_slab", 8), mc("leather", 5)), pf("sanding_station"));
			simple(registry, wood, "wood/water_bearing_pot",
				List.of(pf("woven_wood", 6), pf("clay_pot")), pf("water_bearing_pot"));
			simple(registry, wood, "wood/water_pot",
				List.of(mc("leather", 4), pf("clay_pot")), pf("water_pot"));
			simple(registry, wood, "wood/bound_wood_processor",
				List.of(pf("flint_axe_pieces"), mc("leather", 2)), pf("bound_wood_processor"));
			simple(registry, wood, "wood/wood_firestarter",
				List.of(pf("wooden_board", 2), pf("unlit_flint_torch")), pf("wood_firestarter"));
			simple(registry, wood, "wood/bone_hoe",
				List.of(mc("stick", 2), pf("filed_bone", 3)), pf("bone_hoe"));
			simple(registry, wood, "wood/bookshelf",
				List.of(pf("wooden_sheet", 8), pf("wooden_screw", 2)), pf("bookshelf"));
			simple(registry, wood, "wood/flint_file",
				List.of(pf("flint_file_head"), mc("stick")), pf("flint_file"));
			process(registry, wood, "wood/aquifer_finder",
				List.of(mc("stick", 2)), List.of(flintFile), List.of(pf("aquifer_finder")), 0);
			simple(registry, wood, "wood/brick_shingles",
				List.of(mc("bricks"), pf("wooden_board")), pf("brick_shingles", 3));
			// New Grinder alternatives. The Flint File wears, while the Fan is consumed.
			process(registry, wood, "wood/grinded_slabs_with_file",
				List.of(pf("sanded_wood_slab", 3)), List.of(flintFile),
				List.of(pf("grinded_wood_slab", 2)), 0);
			simple(registry, wood, "wood/woven_wood_from_slabs",
				List.of(pf("grinded_wood_slab", 3), pf("woven_fan", 3)), pf("woven_wood", 2));
			process(registry, wood, "wood/wooden_sheet",
				List.of(pf("wooden_beam")), List.of(flintFile), List.of(pf("wooden_sheet", 4)), 0);
			process(registry, wood, "wood/wooden_screw",
				List.of(pf("wooden_board")), List.of(flintFile), List.of(pf("wooden_screw", 2)), 0);
			process(registry, wood, "wood/limestone_bricks",
				List.of(pf("limestone", 4)), List.of(flintFile), List.of(pf("limestone_bricks", 4)), 0);
			process(registry, wood, "wood/chiseled_limestone",
				List.of(pf("limestone_bricks", 4)), List.of(flintFile), List.of(pf("chiseled_limestone", 4)), 0);
			simple(registry, wood, "wood/door_hinge",
				List.of(pf("wooden_sheet", 8), pf("wooden_screw", 4)), pf("wooden_door_hinge"));
			simple(registry, wood, "wood/chest_frame",
				List.of(pf("wooden_sheet", 4), pf("wooden_screw", 2)), pf("wooden_chest_frame"));
			simple(registry, wood, "wood/wattle_door",
				List.of(pf("wattled_daub_bricks", 16), pf("wooden_door_hinge")), pf("wattled_daub_door"));
			simple(registry, wood, "wood/loose_mud_bricks",
				List.of(pf("fired_clay", 4), pf("mud_pile", 8)), pf("loose_mud_bricks"));
			simple(registry, wood, "wood/bricks",
				List.of(pf("loose_mud_bricks", 2), pf("wooden_screw", 2)), mc("bricks"));
			simple(registry, wood, "wood/primitive_stove",
				List.of(pf("smooth_limestone"), mc("bricks", 2)), pf("primitive_stove"));
			simple(registry, wood, "wood/bone_knife",
				List.of(pf("filed_bone", 2), mc("stick")), pf("bone_knife"));
			process(registry, wood, "wood/wooden_cup",
				List.of(pf("wooden_sheet")), List.of(boneKnife), List.of(pf("wooden_cup", 2)), 0);
			simple(registry, wood, "wood/rope_stage_1",
				List.of(pf("cobweb"), pf("plant_tool_binding", 4)), pf("rope_stage_1"));
			simple(registry, wood, "wood/rope_stage_2",
				List.of(pf("rope_stage_1"), pf("wooden_screw", 4)), pf("rope_stage_2"));
			simple(registry, wood, "wood/rope_stage_3",
				List.of(pf("rope_stage_2"), pf("wooden_sheet", 2)), pf("rope_stage_3"));
			process(registry, wood, "wood/rope",
				List.of(pf("rope_stage_3")), List.of(flintFile), List.of(pf("rope")), 0);


			// ProjectFlint WoodenGrinderRecipeRegistry, as of v0.31.48-b.
			simple(registry, wood, "wood/nest",
				List.of(pf("plant_fiber", 16), mc("string", 2)), pf("nest"));
			simple(registry, wood, "wood/well_pulley",
				List.of(anyMc(16, "stripped_oak_log", "stripped_oak_wood", "stripped_birch_log", "stripped_birch_wood",
					"stripped_spruce_log", "stripped_spruce_wood", "stripped_jungle_log", "stripped_jungle_wood",
					"stripped_acacia_log", "stripped_acacia_wood", "stripped_dark_oak_log", "stripped_dark_oak_wood",
					"stripped_mangrove_log", "stripped_mangrove_wood", "stripped_cherry_log", "stripped_cherry_wood",
					"stripped_pale_oak_log", "stripped_pale_oak_wood", "stripped_crimson_stem", "stripped_crimson_hyphae",
					"stripped_warped_stem", "stripped_warped_hyphae"), pf("rope")), pf("well_pulley"));
			simple(registry, wood, "wood/brick_furnace",
				List.of(pf("primitive_stove"), pf("fireclay_brick_block", 8)), pf("brick_furnace"));
			simple(registry, wood, "wood/fireclay_forge_controller",
				List.of(pf("wooden_sheet", 32), pf("fireclay_brick_block", 32)), pf("fireclay_forge_controller"));
			simple(registry, wood, "wood/unfired_fireclay_brick",
				List.of(pf("unfired_brick", 2), pf("sand_flake")), pf("unfired_fireclay_brick", 2));
			process(registry, wood, "wood/unfired_brick_mold",
				List.of(pf("wooden_sheet", 8)), List.of(boneKnife), List.of(pf("unfired_brick_mold")), 0);
			process(registry, wood, "wood/unfired_bricks",
				List.of(mc("clay_ball", 8)), List.of(pf("unfired_brick_mold")), List.of(pf("unfired_brick", 8)), 0);
			// Bone Knife + 3 Clay Balls moved exclusively to Hardwood Table in 0.32.1.
			// Keep the separate Woven Chest Frame recipe below in the Wooden Grinder.
			simple(registry, wood, "wood/unfired_clay_bucket_frame",
				List.of(mc("clay_ball", 3), pf("woven_chest_frame")), pf("unfired_clay_bucket"));
			// Leather armor from the Wooden Grinder has custom 512-use durability.
			simple(registry, wood, "wood/leather_boots",
				List.of(mc("leather", 4), pf("woven_wool", 4)), maxDamageItem("leather_boots", 512));
			simple(registry, wood, "wood/leather_helmet",
				List.of(mc("leather", 5), pf("woven_wool", 5)), maxDamageItem("leather_helmet", 512));
			simple(registry, wood, "wood/leather_chestplate",
				List.of(mc("leather", 8), pf("woven_wool", 8)), maxDamageItem("leather_chestplate", 512));
			simple(registry, wood, "wood/leather_leggings",
				List.of(mc("leather", 7), pf("woven_wool", 7)), maxDamageItem("leather_leggings", 512));
			// WoodenGrinderMenu returns one empty Clay Bucket for each filled vessel.
			process(registry, wood, "wood/wheat_seeds",
				List.of(pf("flax_seeds", 9), pf("clay_water_bucket")), List.of(),
				List.of(mc("wheat_seeds", 3), pf("clay_bucket")), 0);
			process(registry, wood, "wood/flour",
				List.of(mc("wheat", 9), pf("clay_water_bucket")), List.of(),
				List.of(pf("flour", 9), pf("clay_bucket")), 0);
			process(registry, wood, "wood/fat",
				List.of(rawMeats(1), pf("clay_water_bucket")), List.of(),
				List.of(pf("fat"), pf("clay_bucket")), 0);
			process(registry, wood, "wood/milk_cups",
				List.of(pf("wooden_cup", 2), pf("clay_milk_bucket")), List.of(),
				List.of(pf("wooden_cup_raw_milk", 4), pf("clay_bucket")), 0);
			simple(registry, wood, "wood/utility_vest",
				List.of(pf("tanned_leather_chestplate"), pf("leather_backpack_with_chest")), pf("tanned_utility_vest"));
			simple(registry, wood, "wood/vanilla_bricks",
				List.of(mc("brick", 2), mc("brick", 2)), mc("bricks"));
			simple(registry, wood, "wood/wattled_daub_from_bricks",
				List.of(mc("brick", 2)), pf("wattled_daub_bricks", 12));
			// Remaining WoodenGrinderRecipeRegistry entries previously omitted.
			process(registry, wood, "wood/pumpkin_cake_slice",
				List.of(pf("pumpkin_cake")), List.of(pf("bone_knife")),
				List.of(pf("pumpkin_cake_slice", 4)), 0);
			// Requires a fully undamaged Bone Brush, consumed as an ingredient;
			// produces ProjectFlint's 512-durability vanilla Brush variant.
			simple(registry, wood, "wood/brush_upgrade",
				List.of(pf("wooden_screw", 2), pf("bone_brush")), maxDamageItem("brush", 512));

			EmiIngredient beamAny = anyPf(1, WOODEN_BEAMS.toArray(String[]::new));
			simple(registry, wood, "wood/wooden_cabinet",
				List.of(beamAny, pf("crushed_stone")), pf("wooden_cabinet"));
		}

		if (sand != null) {
			simple(registry, sand, "sand/woven_fan",
				List.of(pf("plant_tool_binding", 2)), pf("woven_fan", 3));
			simple(registry, sand, "sand/advanced_kiln_glass",
				List.of(pf("glass_shard", 3)), pf("advanced_kiln_glass"));
			simple(registry, sand, "sand/leather",
				List.of(pf("raw_leather", 3)), mc("leather", 2));
			simple(registry, sand, "sand/filed_bone",
				List.of(pf("animal_bones", 2)), pf("filed_bone", 3));
			simple(registry, sand, "sand/wood_slab",
				List.of(anyMc(2, "oak_slab", "spruce_slab", "birch_slab", "jungle_slab", "acacia_slab",
					"dark_oak_slab", "mangrove_slab", "cherry_slab", "pale_oak_slab", "bamboo_slab", "bamboo_mosaic_slab")),
				pf("sanded_wood_slab", 3));
			simple(registry, sand, "sand/woven_wool",
				List.of(pf("wool", 3)), pf("woven_wool", 2));
			simple(registry, sand, "sand/sand_ball",
				List.of(pf("sand_flake", 3)), pf("sand_ball"));
			simple(registry, sand, "sand/flint_file_head",
				List.of(pf("flint_cutter_piece")), pf("flint_file_head"));
			simple(registry, sand, "sand/twigs",
				List.of(pf("branch")), pf("twig", 4));
			simple(registry, sand, "sand/cobweb",
				List.of(mc("string", 16)), pf("cobweb"));
			simple(registry, sand, "sand/smooth_limestone",
				List.of(pf("cobbled_limestone", 8)), pf("smooth_limestone"));
			simple(registry, sand, "sand/polished_limestone",
				List.of(pf("limestone_bricks")), pf("polished_limestone"));
			simple(registry, sand, "sand/glazed_tile",
				List.of(pf("checkered_tile")), pf("glazed_checkered_tile"));
			simple(registry, sand, "sand/checkered_tile",
				List.of(pf("glazed_checkered_tile")), pf("checkered_tile"));
			simple(registry, sand, "sand/recipe_parchment",
				List.of(mc("writable_book")), pf("recipe_parchment"));
			// SandingStationRecipeRegistry flower/ink dye transformations.
			for (String[] dye : new String[][] {
				{"dandelion","yellow_dye","2"}, {"wildflowers","yellow_dye","2"},
				{"sunflower","yellow_dye","4"}, {"poppy","red_dye","2"},
				{"red_tulip","red_dye","2"}, {"rose_bush","red_dye","4"},
				{"blue_orchid","light_blue_dye","2"}, {"allium","magenta_dye","2"},
				{"lilac","magenta_dye","4"}, {"azure_bluet","light_gray_dye","2"},
				{"white_tulip","light_gray_dye","2"}, {"oxeye_daisy","light_gray_dye","2"},
				{"orange_tulip","orange_dye","2"}, {"torchflower","orange_dye","2"},
				{"open_eyeblossom","orange_dye","2"}, {"pink_tulip","pink_dye","2"},
				{"pink_petals","pink_dye","2"}, {"cactus_flower","pink_dye","2"},
				{"peony","pink_dye","4"}, {"cornflower","blue_dye","2"},
				{"lily_of_the_valley","white_dye","2"}, {"wither_rose","black_dye","2"},
				{"closed_eyeblossom","gray_dye","2"}, {"pitcher_plant","cyan_dye","4"},
				{"ink_sac","black_dye","2"}
			}) {
				simple(registry, sand, "sand/dye/" + dye[0], List.of(mc(dye[0])), mc(dye[1], Integer.parseInt(dye[2])));
			}

		}

		if (shelf != null) {
			simple(registry, shelf, "bookshelf/book",
				List.of(pf("sugarcane_roll", 8), mc("leather")), mc("book"));
			process(registry, shelf, "bookshelf/writable_book",
				List.of(mc("book"), mc("black_dye", 2)), List.of(pf("bookshelf")),
				List.of(mc("writable_book")), 0);
			// Research pages use the actual Recipe Parchment as the gate/item.
			String[][] research = {
				{"sugar_cane", "16", "sugar"},
				{"sweet_berries", "16", "sweet_berry_jam"},
				{"sweet_berries", "16", "sweet_berry_pie"},
				{"melon_slice", "16", "melon_pie"},
				{"cooked_mutton", "16", "mutton_pie"},
				{"apple", "16", "apples_n_pie"},
				{"cooked_beef", "8", "beef_stew"},
				{"cooked_porkchop", "8", "unfinished_bacon"}
			};
			for (String[] row : research) {
				process(registry, shelf, "bookshelf/research/" + row[2],
					List.of(mc(row[0], Long.parseLong(row[1])), pf("recipe_parchment")),
					List.of(pf("bookshelf"), mc("book", 4)), List.of(boundParchment(row[2].equals("unfinished_bacon") ? "bacon_and_eggs_unfinished" : row[2])), 0);
			}
			process(registry, shelf, "bookshelf/research/pumpkin_cake",
				List.of(pf("pumpkin_slice", 16), pf("recipe_parchment")),
				List.of(pf("bookshelf"), mc("book", 4)), List.of(boundParchment("pumpkin_cake")), 0);
			process(registry, shelf, "bookshelf/research/pumpkin_pie",
				List.of(pf("pumpkin_slice", 16), pf("recipe_parchment")),
				List.of(pf("bookshelf"), mc("book", 4)), List.of(boundParchment("pumpkin_pie")), 0);
			process(registry, shelf, "bookshelf/research/butter",
				List.of(pf("wooden_cup_raw_milk", 4), pf("recipe_parchment")),
				List.of(pf("bookshelf"), mc("book", 4)), List.of(boundParchment("butter")), 0);

			// The real BookshelfRecipeRegistry requires >= 4 inserted Books and a blank parchment.
			String[][] laterResearch = {
				{"strawberry", "16", "strawberry_jam"}, {"strawberry", "16", "strawberry_pie"},
				{"fat", "8", "fat"}, {"flour", "8", "dough"}, {"dough", "8", "sweet_dough"},
				{"dough", "8", "bread"}, {"dough", "8", "rustic_bread"},
				{"sweet_dough", "8", "sweet_bread"}, {"dough", "8", "meat_bread"},
				{"sweet_dough", "8", "pumpkin_bread"}, {"sweet_dough", "8", "apple_bread"},
				{"dough", "8", "cheese_bread"}, {"dough", "8", "egg_bread"},
				{"sweet_dough", "8", "breakfast_bread"}, {"dough", "8", "flatbread"},
				{"dough", "8", "french_bread"}, {"raw_calamari", "8", "sushi_roll"},
				{"bacon_and_eggs", "1", "scrambled_egg_oven"}
			};
			for (String[] row : laterResearch) {
				process(registry, shelf, "bookshelf/research/modern/" + row[2],
					List.of(pf(row[0], Long.parseLong(row[1])), pf("recipe_parchment")),
					List.of(pf("bookshelf"), mc("book", 4)), List.of(boundParchment(row[2])), 0);
			}
			process(registry, shelf, "bookshelf/research/modern/cheese",
				List.of(pf("wooden_cup_raw_milk", 4), pf("recipe_parchment")),
				List.of(pf("bookshelf"), mc("book", 4)), List.of(boundParchment("cheese")), 0);
			// The Bacon & Eggs parchment is a TWO-STAGE Bookshelf recipe.
			// The second step explicitly requires unfinished bacon parchment, NOT blank parchment.
			process(registry, shelf, "bookshelf/research/finish_bacon_and_eggs",
				List.of(pf("scrambled_egg", 8), boundParchment("bacon_and_eggs_unfinished")),
				List.of(pf("bookshelf"), mc("book", 4)),
				List.of(boundParchment("bacon_and_eggs")), 0);
			process(registry, shelf, "bookshelf/research/bone_broth",
				List.of(pf("filed_bone", 6), pf("recipe_parchment")),
				List.of(pf("bookshelf"), mc("book", 4)), List.of(boundParchment("bone_broth")), 0);
		}

		if (stove != null) {
			registerPrimitiveOvenDirectFood(registry, stove);
			EmiIngredient parchment = pf("recipe_parchment");
			stoveRecipe(registry, stove, "sugar", List.of(mc("sugar_cane", 4)), parchment, mc("sugar"));
			stoveRecipe(registry, stove, "sweet_berry_jam",
				List.of(mc("sweet_berries", 8), mc("sugar"), pf("wooden_cup", 2)), parchment, pf("sweet_berry_jam", 4));
			stoveRecipe(registry, stove, "sweet_berry_pie",
				List.of(pf("sweet_berry_jam", 2), mc("sugar"), pf("butter")), parchment, pf("sweet_berry_pie", 2));
			stoveRecipe(registry, stove, "pumpkin_pie",
				List.of(pf("pumpkin_slice", 8), mc("sugar", 2)), parchment, mc("pumpkin_pie", 2));
			stoveRecipe(registry, stove, "melon_pie",
				List.of(mc("melon_slice", 8), mc("sugar", 2)), parchment, pf("melon_pie", 2));
			stoveRecipe(registry, stove, "mutton_pie",
				List.of(mc("cooked_mutton", 4), mc("sugar", 2)), parchment, pf("mutton_pie", 3));
			stoveRecipe(registry, stove, "butter",
				List.of(pf("wooden_cup_raw_milk", 4), mc("sugar")), parchment, pf("butter"));
			stoveRecipe(registry, stove, "pumpkin_cake",
				List.of(mc("pumpkin_pie", 4), pf("butter", 2), mc("sugar")), parchment, pf("pumpkin_cake"));
			stoveRecipe(registry, stove, "apples_n_pie",
				List.of(any(1, pf("melon_pie"), mc("pumpkin_pie")), mc("apple", 4), pf("wooden_cup")),
				parchment, pf("apples_n_pie", 3));
			stoveRecipe(registry, stove, "beef_stew",
				List.of(mc("cooked_beef", 2), pf("butter"), mc("sugar"), pf("wooden_cup", 3)),
				parchment, pf("beef_stew", 3));
			stoveRecipe(registry, stove, "bacon_and_eggs",
				List.of(mc("cooked_porkchop"), pf("scrambled_egg", 3), pf("wooden_cup")),
				parchment, pf("bacon_and_eggs", 3));

			// Source-aligned v0.31.48-b Primitive Oven recipes; parchment is a reusable unlock.
			stoveRecipe(registry, stove, "strawberry_jam",
				List.of(pf("strawberry", 12), mc("sugar"), pf("wooden_cup", 2)), parchment, pf("strawberry_jam", 4));
			stoveRecipe(registry, stove, "strawberry_pie",
				List.of(pf("strawberry_jam", 2), pf("strawberry", 2), mc("sugar"), pf("butter")), parchment, pf("strawberry_pie", 2));
			stoveRecipe(registry, stove, "fat_efficiency",
				List.of(rawMeats(3)), parchment, pf("fat", 5));
			stoveRecipe(registry, stove, "dough",
				List.of(pf("flour", 2), pf("wooden_cup_raw_milk")), parchment, pf("dough"));
			stoveRecipe(registry, stove, "sweet_dough",
				List.of(pf("dough"), mc("sugar")), parchment, pf("sweet_dough"));
			stoveRecipe(registry, stove, "bread",
				List.of(pf("dough", 2), mc("sugar")), parchment, mc("bread"));
			stoveRecipe(registry, stove, "rustic_bread",
				List.of(pf("dough")), parchment, pf("rustic_bread"));
			stoveRecipe(registry, stove, "sweet_bread",
				List.of(pf("sweet_dough")), parchment, pf("sweet_bread"));
			stoveRecipe(registry, stove, "flatbread",
				List.of(pf("dough", 2), pf("fat")), parchment, pf("flatbread", 2));
			stoveRecipe(registry, stove, "french_bread",
				List.of(pf("dough", 3), pf("wooden_cup_raw_milk")), parchment, pf("french_bread", 2));
			stoveRecipe(registry, stove, "meat_bread",
				List.of(pf("dough", 2), cookedMeats(2), pf("fat")), parchment, pf("meat_bread", 2));
			stoveRecipe(registry, stove, "cheese_bread",
				List.of(pf("dough", 2), pf("cheese")), parchment, pf("cheese_bread", 2));
			stoveRecipe(registry, stove, "egg_bread",
				List.of(pf("dough", 2), pf("scrambled_egg", 2)), parchment, pf("egg_bread", 2));
			stoveRecipe(registry, stove, "breakfast_bread",
				List.of(pf("sweet_dough", 2), pf("scrambled_egg", 2), pf("butter")), parchment, pf("breakfast_bread", 2));
			stoveRecipe(registry, stove, "apple_bread",
				List.of(pf("sweet_dough", 2), mc("apple", 2)), parchment, pf("apple_bread", 2));
			stoveRecipe(registry, stove, "pumpkin_bread",
				List.of(pf("sweet_dough", 2), pf("pumpkin_slice", 4)), parchment, pf("pumpkin_bread", 2));
			stoveRecipe(registry, stove, "scrambled_egg_oven",
				List.of(mc("egg")), parchment, pf("scrambled_egg", 3));
			stoveRecipe(registry, stove, "sushi_roll",
				List.of(pf("raw_calamari", 2), pf("flour", 2), mc("sugar")), parchment, pf("sushi_roll", 5));
			stoveRecipe(registry, stove, "cheese",
				List.of(pf("wooden_cup_raw_milk", 2), pf("fat")), parchment, pf("cheese"));
			stoveRecipe(registry, stove, "bone_broth",
				List.of(pf("filed_bone", 4), pf("butter"), pf("wooden_cup", 2)),
				parchment, pf("bone_broth", 4));
		}
	}

	/** ClayGrinderRecipeGuideRegistry is inherited by the Wooden Grinder. */
	/** PrimitiveStoveRecipeRegistry.findDirectFood permits these WITHOUT a parchment.
	 * Special researched meals are separate and do require their bound parchments. */
	private static void registerPrimitiveOvenDirectFood(EmiRegistry registry, EmiRecipeCategory oven) {
		heat(registry, oven, "oven_direct", "scrambled_egg", mc("egg"), pf("scrambled_egg", 2), 300, 1.0f);
		heat(registry, oven, "oven_direct", "plant_seeds", pf("plant_fiber"), pf("plant_seeds"), 375, 1.0f);
		heat(registry, oven, "oven_direct", "clay_bucket", pf("unfired_clay_bucket"), pf("clay_bucket"), 300, 1.0f);
		heat(registry, oven, "oven_direct", "unfired_brick", pf("unfired_brick"), mc("brick"), 300, 1.0f);
		heat(registry, oven, "oven_direct", "fireclay_brick", pf("unfired_fireclay_brick"), pf("fireclay_brick"), 300, 1.0f);
		String[][] meats = {
			{"raw_calamari","cooked_calamari"}, {"raw_turtle_meat","cooked_turtle_meat"},
			{"raw_fox_meat","cooked_fox_meat"}, {"raw_frog_meat","cooked_frog_meat"},
			{"raw_goat_meat","cooked_goat_meat"}, {"raw_horse_meat","cooked_horse_meat"},
			{"raw_llama_meat","cooked_llama_meat"}, {"raw_dolphin_meat","cooked_dolphin_meat"},
			{"raw_bear_meat","cooked_bear_meat"}, {"raw_camel_meat","cooked_camel_meat"}
		};
		for (String[] meat : meats) heat(registry, oven, "oven_direct", meat[1], pf(meat[0]), pf(meat[1]), 300, 1.0f);
		String[][] vanilla = {
			{"beef","cooked_beef"}, {"porkchop","cooked_porkchop"},
			{"chicken","cooked_chicken"}, {"mutton","cooked_mutton"},
			{"rabbit","cooked_rabbit"}, {"cod","cooked_cod"},
			{"salmon","cooked_salmon"}, {"potato","baked_potato"}, {"kelp","dried_kelp"}
		};
		for (String[] entry : vanilla) heat(registry, oven, "oven_direct", entry[1], mc(entry[0]), mc(entry[1]), 300, 1.0f);
	}

	private static void registerClayConversions(EmiRegistry registry, EmiRecipeCategory target, String prefix) {

			// Current ClayGrinderRecipeGuideRegistry.
			simple(registry, target, prefix + "/bone_club", List.of(pf("filed_bone", 4)), pf("bone_club"));
			simple(registry, target, prefix + "/bone_helmet", List.of(pf("filed_bone", 5)), pf("bone_helmet"));
			simple(registry, target, prefix + "/bone_chestplate", List.of(pf("filed_bone", 8)), pf("bone_chestplate"));
			simple(registry, target, prefix + "/bone_leggings", List.of(pf("filed_bone", 7)), pf("bone_leggings"));
			simple(registry, target, prefix + "/bone_boots", List.of(pf("filed_bone", 4)), pf("bone_boots"));
			// ClayGrinderRecipeGuideRegistry: newer tanning, brick, and fireclay conversions.
			simple(registry, target, prefix + "/tanned_helmet", List.of(pf("tanned_leather", 5)), pf("tanned_leather_helmet"));
			simple(registry, target, prefix + "/tanned_chestplate", List.of(pf("tanned_leather", 8)), pf("tanned_leather_chestplate"));
			simple(registry, target, prefix + "/tanned_leggings", List.of(pf("tanned_leather", 7)), pf("tanned_leather_leggings"));
			simple(registry, target, prefix + "/tanned_boots", List.of(pf("tanned_leather", 4)), pf("tanned_leather_boots"));
			simple(registry, target, prefix + "/fireclay_brick_block", List.of(pf("fireclay_brick", 4)), pf("fireclay_brick_block"));
			simple(registry, target, prefix + "/fireclay_brick_slab", List.of(pf("fireclay_brick_block")), pf("fireclay_brick_slab", 2));
			simple(registry, target, prefix + "/fireclay_brick_stairs", List.of(pf("fireclay_brick_block")), pf("fireclay_brick_stairs"));
			simple(registry, target, prefix + "/fireclay_slab_to_block", List.of(pf("fireclay_brick_slab", 2)), pf("fireclay_brick_block"));
			simple(registry, target, prefix + "/unfired_brick_recycle", List.of(pf("unfired_brick")), pf("unfired_clay_ball", 2));
			simple(registry, target, prefix + "/loose_dirt_from_slabs", List.of(pf("loose_dirt_slab", 2)), pf("loose_dirt"));
			simple(registry, target, prefix + "/clay_mortar", List.of(pf("super_heated_clay", 9)), pf("clay_mortar"));
			simple(registry, target, prefix + "/flint_chips", List.of(mc("flint")), pf("flint_chip", 4));
			simple(registry, target, prefix + "/flint_from_chips", List.of(pf("flint_chip", 4)), mc("flint"));
			simple(registry, target, prefix + "/grinded_wood", List.of(pf("sanded_wood_slab", 2)), pf("grinded_wood_slab"));
			simple(registry, target, prefix + "/sanded_reverse", List.of(pf("grinded_wood_slab")), pf("sanded_wood_slab", 2));
			simple(registry, target, prefix + "/stone_grindsaw", List.of(pf("crushed_stone", 8)), pf("stone_grindsaw"));
			simple(registry, target, prefix + "/crushed_stone", List.of(pf("very_refined_stone", 8)), pf("crushed_stone"));
			simple(registry, target, prefix + "/crushed_slab", List.of(pf("crushed_stone")), pf("crushed_stone_slab", 2));
			simple(registry, target, prefix + "/crushed_block", List.of(pf("crushed_stone_slab", 2)), pf("crushed_stone"));
			simple(registry, target, prefix + "/mortar_slab", List.of(pf("mortar_bricks")), pf("mortar_brick_slab", 2));
			simple(registry, target, prefix + "/mortar_block", List.of(pf("mortar_brick_slab", 2)), pf("mortar_bricks"));
			simple(registry, target, prefix + "/mortar_kiln_base", List.of(pf("mortar_bricks", 24)), pf("mortar_kiln_base"));
			simple(registry, target, prefix + "/sanding_paper", List.of(pf("sugarcane_roll", 4)), pf("sanding_paper"));
			simple(registry, target, prefix + "/twig_to_branch", List.of(pf("twig", 4)), pf("branch"));
			simple(registry, target, prefix + "/branch_to_twigs", List.of(pf("branch")), pf("twig", 2));
			simple(registry, target, prefix + "/unfired_kiln_glass", List.of(pf("glass_shard", 4)), pf("unfired_kiln_glass"));
			simple(registry, target, prefix + "/floor_tile", List.of(pf("wattled_daub_bricks")), pf("wattled_daub_floor_tile", 2));
			simple(registry, target, prefix + "/wooden_grinder_base", List.of(pf("woven_wood", 12)), pf("wooden_grinder_base"));
			simple(registry, target, prefix + "/woven_wood", List.of(pf("woven_wood_slab", 2)), pf("woven_wood"));
			simple(registry, target, prefix + "/woven_slab", List.of(pf("woven_wood")), pf("woven_wood_slab", 2));
			simple(registry, target, prefix + "/loose_mud_slab", List.of(pf("loose_mud_bricks")), pf("loose_mud_brick_slab", 2));
			simple(registry, target, prefix + "/loose_mud_block", List.of(pf("loose_mud_brick_slab", 2)), pf("loose_mud_bricks"));
			simple(registry, target, prefix + "/crude_clay_slab", List.of(pf("crude_clay_block")), pf("crude_clay_slab", 2));
			simple(registry, target, prefix + "/crude_clay_block", List.of(pf("crude_clay_slab", 2)), pf("crude_clay_block"));
			simple(registry, target, prefix + "/fired_clay", List.of(pf("crude_clay_block")), pf("fired_clay", 4));
			simple(registry, target, prefix + "/burnt_branch_block", List.of(pf("burnt_branch_slab", 2)), pf("burnt_branch_block"));
			simple(registry, target, prefix + "/burnt_branch_slabs", List.of(pf("burnt_branch_block")), pf("burnt_branch_slab", 2));
			simple(registry, target, prefix + "/burnt_branches", List.of(pf("burnt_branch_slab")), pf("burnt_branch", 8));

			for (String mold : List.of("axe", "block", "brick", "cutters", "multitool", "pot")) {
				simple(registry, target, prefix + "/unfired_" + mold + "_mold",
					List.of(pf("unfired_clay_mold_base")), pf("unfired_clay_" + mold + "_mold"));
			}

			logSlabPair(registry, target, prefix, "oak", "stripped_oak_log", "oak_slab");
			logSlabPair(registry, target, prefix, "spruce", "stripped_spruce_log", "spruce_slab");
			logSlabPair(registry, target, prefix, "birch", "stripped_birch_log", "birch_slab");
			logSlabPair(registry, target, prefix, "jungle", "stripped_jungle_log", "jungle_slab");
			logSlabPair(registry, target, prefix, "acacia", "stripped_acacia_log", "acacia_slab");
			logSlabPair(registry, target, prefix, "dark_oak", "stripped_dark_oak_log", "dark_oak_slab");
			logSlabPair(registry, target, prefix, "mangrove", "stripped_mangrove_log", "mangrove_slab");
			logSlabPair(registry, target, prefix, "cherry", "stripped_cherry_log", "cherry_slab");
			logSlabPair(registry, target, prefix, "pale_oak", "stripped_pale_oak_log", "pale_oak_slab");
			simple(registry, target, prefix + "/bamboo_slab", List.of(mc("stripped_bamboo_block")), mc("bamboo_slab", 2));
	}

	private static void registerLumberProcessing(EmiRegistry registry) {
		EmiRecipeCategory lumber = category(registry, "lumber_processing", "lumber_processor");
		if (lumber == null) {
			return;
		}

		// Formation route is a world interaction so the Lumber Processor item has a recipe.
		world(registry, "lumber_processor_formation",
			List.of(pf("woven_wood"), pf("water_pot"), pf("crushed_stone"), pf("bound_wood_processor")),
			List.of(), pf("lumber_processor"));

		List<EmiStack> logs = new ArrayList<>();
		for (String path : STRIPPED_LOGS) {
			EmiStack stack = mc(path);
			if (!stack.isEmpty()) {
				logs.add(stack);
			}
		}
		EmiIngredient strippedLog = EmiIngredient.of(logs);
		EmiIngredient water = EmiStack.of(Fluids.WATER, FluidUnit.BUCKET);

		process(registry, lumber, "lumber/boards",
			List.of(strippedLog, water),
			List.of(pf("lumber_processor"), pf("flint_plant_cutters")),
			List.of(pf("wooden_board", 2)), 290);
		process(registry, lumber, "lumber/beams",
			List.of(strippedLog.copy(), water.copy()),
			List.of(pf("lumber_processor"), pf("flint_plant_cutters")),
			List.of(pf("wooden_beam")), 290);
	}

	private static void registerWell(EmiRegistry registry) {
		EmiRecipeCategory well = category(registry, "well", "well_pulley");
		if (well == null) {
			return;
		}

		/*
		 * Actual WellPulleyBlockEntity output behavior:
		 * - refill 3 center shaft cells with source water
		 * - 0-2 clay balls
		 * - 2-3 flint chips, OR 1 vanilla flint when the roll reaches 4 units
		 * Rope in the GUI is an optional quality booster, not a consumed recipe input.
		 */
		process(registry, well, "well/cycle",
			List.of(),
			List.of(pf("well_pulley")),
			List.of(
				EmiStack.of(Fluids.WATER, FluidUnit.BUCKET * 3L),
				mc("clay_ball", 2),
				pf("flint_chip", 3),
				mc("flint")
			),
			20 * 60);
	}

	private static void registerHeatProcessing(EmiRegistry registry) {
		EmiRecipeCategory campfire = category(registry, "permanent_campfire", "campfire_stone");
		EmiRecipeCategory baked = category(registry, "baked_clay_kiln", "baked_clay_kiln");
		EmiRecipeCategory mortar = category(registry, "mortar_kiln", "mortar_kiln");
		EmiRecipeCategory furnace = category(registry, "brick_furnace", "brick_furnace");
		EmiRecipeCategory blast = category(registry, "brick_blast_furnace", "brick_blast_furnace");

		if (campfire != null) registerCommonHeat(registry, campfire, "campfire", 0.90f);
		if (baked != null) {
			registerCommonHeat(registry, baked, "baked", 1.0f);
			registerKilnOnly(registry, baked, "baked", 1.0f);
		}
		if (mortar != null) {
			registerCommonHeat(registry, mortar, "mortar", 1.25f);
			registerKilnOnly(registry, mortar, "mortar", 1.25f);
		}
		if (furnace != null) {
			registerCommonHeat(registry, furnace, "brick_furnace", 1.50f);
			registerKilnOnly(registry, furnace, "brick_furnace", 1.50f);
			// BrickFurnaceRecipeRegistry: exclusive conversions not valid in kiln/campfire.
			heat(registry, furnace, "brick_furnace", "tanned_leather", mc("leather"), pf("tanned_leather"), 300, 1.50f);
			heat(registry, furnace, "brick_furnace", "glass", mc("sand"), mc("glass"), 600, 1.50f);
			heat(registry, furnace, "brick_furnace", "stone_from_cobblestone", mc("cobblestone"), mc("stone"), 300, 1.50f);
			// Exclusive Brick Furnace recipe: fixed 4,000-tick (200-second) cycle.
			heat(registry, furnace, "brick_furnace", "fine_fuse_powder", pf("fuse_ready_dust"), pf("fine_fuse_powder"), 4000, 1.0f);
		}
		if (blast != null) {
			registerCommonHeat(registry, blast, "brick_blast_furnace", 1.50f);
			registerKilnOnly(registry, blast, "brick_blast_furnace", 1.50f);
		}

		// Crude kiln bake step.
		EmiRecipeCategory crude = category(registry, "crude_clay_kiln", "crude_clay_kiln");
		if (crude != null) {
			process(registry, crude, "heat/crude/bake",
				List.of(pf("crude_clay_kiln"), pf("branch", 8)),
				List.of(anyPf(1, "flint_firestarter", "bone_firestarter", "wood_firestarter")),
				List.of(pf("baked_clay_kiln")), 20 * 30);
		}
	}

	private static void registerCommonHeat(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String prefix,
		float speed
	) {
		// Both CAMPFIRE and KILN accept these instant/direct burns.
		heat(registry, category, prefix, "burnt_twig", pf("twig"), pf("burnt_twig"), 0, speed);
		heat(registry, category, prefix, "burnt_branch", pf("branch"), pf("burnt_branch"), 0, speed);
		// PrimitiveProcessingRegistry flags bucket/brick/fireclay firing as BOTH.
		heat(registry, category, prefix, "clay_bucket", pf("unfired_clay_bucket"), pf("clay_bucket"), 300, speed);
		heat(registry, category, prefix, "brick", pf("unfired_brick"), mc("brick"), 300, speed);
		heat(registry, category, prefix, "fireclay_brick", pf("unfired_fireclay_brick"), pf("fireclay_brick"), 300, speed);
		heat(registry, category, prefix, "scrambled_egg", mc("egg"), pf("scrambled_egg", 2), 300, speed);
		heat(registry, category, prefix, "plant_seeds", pf("plant_fiber"), pf("plant_seeds"), 375, speed);
		heat(registry, category, prefix, "fired_clay_bowl", pf("unfired_woven_bowl"), pf("fired_clay_bowl"), 300, speed);
		heat(registry, category, prefix, "wattle_bricks", pf("unfired_wattle_and_daub"), pf("wattled_daub_bricks", 3), 300, speed);

		String[][] customMeat = {
			{"raw_calamari", "cooked_calamari"},
			{"raw_turtle_meat", "cooked_turtle_meat"},
			{"raw_fox_meat", "cooked_fox_meat"},
			{"raw_frog_meat", "cooked_frog_meat"},
			{"raw_goat_meat", "cooked_goat_meat"},
			{"raw_horse_meat", "cooked_horse_meat"},
			{"raw_llama_meat", "cooked_llama_meat"},
			{"raw_dolphin_meat", "cooked_dolphin_meat"},
			{"raw_bear_meat", "cooked_bear_meat"},
			{"raw_camel_meat", "cooked_camel_meat"}
		};
		for (String[] pair : customMeat) {
			heat(registry, category, prefix, pair[1], pf(pair[0]), pf(pair[1]), 300, speed);
		}

		String[][] vanilla = {
			{"beef", "cooked_beef"},
			{"porkchop", "cooked_porkchop"},
			{"chicken", "cooked_chicken"},
			{"mutton", "cooked_mutton"},
			{"rabbit", "cooked_rabbit"},
			{"cod", "cooked_cod"},
			{"salmon", "cooked_salmon"},
			{"potato", "baked_potato"},
			{"kelp", "dried_kelp"}
		};
		for (String[] pair : vanilla) {
			heat(registry, category, prefix, pair[1], mc(pair[0]), mc(pair[1]), 300, speed);
		}
	}

	private static void registerKilnOnly(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String prefix,
		float speed
	) {
		heat(registry, category, prefix, "advanced_kiln_glass", pf("unfired_kiln_glass"), pf("advanced_kiln_glass"), 300, speed);
		heat(registry, category, prefix, "unfired_mold_base", pf("crude_clay_block"), pf("unfired_clay_mold_base"), 300, speed);
		heat(registry, category, prefix, "super_heated_clay", pf("unfired_clay_ball"), pf("super_heated_clay"), 80, speed);
		heat(registry, category, prefix, "mortar_bricks", pf("unfired_mortar_brick"), pf("mortar_bricks"), 300, speed);
		heat(registry, category, prefix, "limestone", pf("smooth_limestone"), pf("limestone"), 300, speed);
		heat(registry, category, prefix, "glass_shard", pf("sand_ball"), pf("glass_shard"), 300, speed);

		for (String mold : List.of("axe", "brick", "cutters", "multitool", "pot", "block")) {
			heat(registry, category, prefix, "fired_" + mold + "_mold",
				pf("unfired_clay_" + mold + "_mold"),
				pf("fired_clay_" + mold + "_mold"), 300, speed);
		}
	}

	private static void registerAdvancedKiln(EmiRegistry registry) {
		EmiRecipeCategory advanced = category(registry, "advanced_kiln", "advanced_kiln_glass");
		if (advanced == null) {
			return;
		}

		// Fired Clay Block Mold is retained.
		process(registry, advanced, "advanced/burnt_branch_block",
			List.of(pf("branch", 4), mc("flint", 4)),
			List.of(pf("fired_clay_block_mold")),
			List.of(pf("burnt_branch_block")), 20 * 15);
		process(registry, advanced, "advanced/crude_clay_block",
			List.of(pf("burnt_branch", 8), pf("unfired_clay_ball", 8)),
			List.of(pf("fired_clay_block_mold")),
			List.of(pf("crude_clay_block", 3)), 20 * 20);

		advancedCast(registry, advanced, "axe", "fired_clay_axe_mold", pf("flint_axe_pieces"), false);
		advancedCast(registry, advanced, "bricks", "fired_clay_brick_mold", pf("mortar_bricks", 6), false);
		advancedCast(registry, advanced, "cutters", "fired_clay_cutters_mold", pf("flint_cutter_piece"), false);
		advancedCast(registry, advanced, "multitool", "fired_clay_multitool_mold", pf("refined_flint_multitool_piece"), false);
		advancedCast(registry, advanced, "pot", "fired_clay_pot_mold", pf("clay_pot"), true);
	}

	private static void advancedCast(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String id,
		String mold,
		EmiStack output,
		boolean consumesMold
	) {
		List<EmiIngredient> inputs = new ArrayList<>();
		inputs.add(pf("burnt_branch", 8));
		inputs.add(mc("flint", 6));
		inputs.add(pf("fired_clay", 4));
		List<EmiIngredient> catalysts = new ArrayList<>();
		if (consumesMold) inputs.add(pf(mold));
		else catalysts.add(pf(mold));

		process(registry, category, "advanced/" + id, inputs, catalysts, List.of(output), 20 * 60);
	}


	private static void registerMissingWorldRoutes(EmiRegistry registry) {
		EmiRecipeCategory placement = category(registry, "world_placement", "sleeping_bag");
		if (placement == null) return;

		// WovenWoolItem: eight Woven Wool place the supported two-block Sleeping Bag.
		process(registry, placement, "placement/sleeping_bag",
			List.of(pf("woven_wool", 8)), List.of(), List.of(pf("sleeping_bag")), 0);

		// SuperHeatedClayItem: the base is placed directly from eight Super Heated Clay.
		process(registry, placement, "placement/clay_grinding_base",
			List.of(pf("super_heated_clay", 8)), List.of(), List.of(pf("clay_grinding_base")), 0);

		// WaterBearingPotItem source-water interaction.
		process(registry, placement, "placement/fill_water_bearing_pot",
			List.of(pf("water_bearing_pot"), EmiStack.of(Fluids.WATER, FluidUnit.BUCKET / 2L)),
			List.of(), List.of(pf("water_bearing_pot_filled")), 0);
		process(registry, placement, "placement/fill_water_pot",
			List.of(pf("water_bearing_pot_filled")),
			List.of(pf("water_pot")),
			List.of(pf("water_bearing_pot")), 0);
		// A single Clay Water Bucket fills an EMPTY placed Water Pot completely.
		// A partially filled Pot refuses this interaction; the vessel returns empty.
		process(registry, placement, "placement/fill_empty_water_pot_clay_bucket",
			List.of(pf("clay_water_bucket")), List.of(pf("water_pot")),
			List.of(pf("clay_bucket")), 0);

		// Bone Brush sustained world processing.
		process(registry, placement, "placement/brush_sand",
			List.of(anyMc(1, "sand", "red_sand")),
			List.of(pf("bone_brush")),
			List.of(pf("sand_flake"), mc("clay")), 20 * 7 / 2);
		process(registry, placement, "placement/brush_gravel",
			List.of(mc("gravel")),
			List.of(pf("bone_brush")),
			List.of(mc("flint")), 20 * 7 / 2);

		// SweetBerrySeedsItem has no bush item output, so show it as a placement information page.
		info(registry, placement, "placement/sweet_berry_bush",
			"Plant Sweet Berry Bush",
			pf("sweet_berry_seeds"),
			true,
			List.of(mc("grass_block")),
			List.of(),
			List.of("Use on the top of Grass Block.", "Places a vanilla Sweet Berry Bush."));

		info(registry, placement, "placement/recover_string", "Pick Up Placed String",
			mc("string"), true, List.of(), List.of(),
			List.of("Break placed String by hand to pick it back up."));

		// Bone Hoe turns supported wet ProjectFlint soil into farmland and can find Flax Seeds.
		info(registry, placement, "placement/bone_hoe_farmland",
			"Bone Hoe: Wet Soil",
			pf("bone_hoe"),
			true,
			List.of(any(1, mc("grass_block"), pf("loose_dirt"), pf("loose_soil"))),
			List.of(new ProjectFlintLootRecipe.LootOutput(pf("flax_seeds"), "10% chance")),
			List.of("Soil must directly border water.", "Transforms the block into Farmland."));

		// Bone Brush shears an adult sheep for ProjectFlint Wool.
		info(registry, placement, "placement/bone_brush_sheep",
			"Bone Brush: Sheep",
			pf("bone_brush"),
			true,
			List.of(mc("white_wool")),
			List.of(new ProjectFlintLootRecipe.LootOutput(pf("wool", 4), "Drops 3-4 ProjectFlint Wool")),
			List.of("Use the Bone Brush on a sheep that is ready for shearing."));
		// The upgraded 512-durability Project Flint Brush obtains 4-5 Wool per sheep.
		info(registry, placement, "placement/projectflint_brush_sheep", "Project Flint Brush: Sheep",
			maxDamageItem("brush", 512), true, List.of(mc("white_wool")),
			List.of(loot(pf("wool", 5), "4–5 Wool per adult sheep")),
			List.of("Use the 512-durability Brush on a sheep ready for shearing."));
		// Clay Bucket collects a water SOURCE, then returns empty after pouring.
		info(registry, placement, "bucket/collect_water", "Clay Bucket: Collect Water",
			pf("clay_bucket"), true, List.of(mc("water_bucket")),
			List.of(loot(pf("clay_water_bucket"), "Collect one water source block")),
			List.of("Use on a water source block, not flowing water.",
				"Placing its water returns the empty Clay Bucket."));

		// ProjectFlint v0.31.55-b Axle is placeable infrastructure only, not a power transmitter.
		info(registry, placement, "placement/axle", "Place Axle",
			pf("axle"), true, List.of(), List.of(),
			List.of("Place the Axle to orient its shaft along the chosen axis.",
				"Recover it by hand in five ticks. Axles do not transmit power yet."));

		// Pure Mica Insulated Glass is transparent and only returns intact when
		// recovered with the Tanned Utility Vest equipped (0.31.60/0.31.61).
		info(registry, placement, "placement/pure_mica_glass_recovery", "Pure Mica Glass Recovery",
			pf("pure_mica_insulated_glass"), true, List.of(pf("tanned_utility_vest")), List.of(),
			List.of("Wear the Tanned Utility Vest to recover intact by hand.",
				"This glass lets light through like regular Glass."));

		info(registry, placement, "packed_mud/speed", "Packed Mud",
			mc("packed_mud"), true, List.of(), List.of(),
			List.of("Walking on Packed Mud gives a 10% speed boost."));

		info(registry, placement, "torch/fat_burning_torch", "Fat-Burning Torch Refueling",
			pf("fat_burning_torch"), true, List.of(pf("fat")), List.of(),
			List.of("Crafted torches start with zero Fat charges.", "After placing, add Fat, then ignite with a Firestarter.",
				"Each Fat charge lasts 50 minutes. Cannot be relit below Y=0."));

	}

	/** World processes introduced after the original PF-EMI recipe snapshot. */
	/** World crop planting/harvesting is NOT a machine recipe. */
	private static void registerFarmingRoutes(EmiRegistry registry) {
		EmiRecipeCategory farming = categoryIcon(registry, "farming", "strawberry_seeds");
		if (farming == null) return;
		info(registry, farming, "farming/strawberry_planting", "Plant Strawberry Seeds",
			pf("strawberry_seeds"), true, List.of(mc("grass_block")), List.of(),
			List.of("Use Strawberry Seeds on grass to plant.", "Plant grows through four stages (0-3)."));
		info(registry, farming, "farming/strawberry_harvest", "Strawberry Plant Harvest",
			pf("strawberry_seeds"), false,
			List.of(anyPf(1, "crude_harvesting_tool", "crude_multitool", "flint_multitool", "flint_plant_cutters", "advanced_flint_multitool")),
			List.of(loot(pf("strawberry", 2), "1-2 Strawberries, only when mature; resets age to 1")),
			List.of("Cutters are required at mature growth stages.", "Tool durability is consumed when harvesting."));
	}

	/**
	 * Keep recipe-viewer probabilities aligned with the installed gameplay mod.
	 * 0.31.69 uses +25% independent Basin yields, 0.31.68 used +20%,
	 * and the earlier explosive used +50%. All over-100% rolls keep their
	 * guaranteed items and independent fractional bonus rolls.
	 */
	private static final double DYNAMITE_YIELD_MULTIPLIER_03169 = 1.25;
	private static final double DYNAMITE_YIELD_MULTIPLIER_03168 = 1.20;
	private static final double LEGACY_DYNAMITE_YIELD_MULTIPLIER = 1.50;
	private static Integer detectedProjectFlintPatch;

	/** The gameplay mod is authoritative; do not advertise future rules early. */
	private static int projectFlintPatch() {
		if (detectedProjectFlintPatch != null) return detectedProjectFlintPatch;
		int patchVersion = -1;
		try {
			Class<?> loaderApi = Class.forName("net.fabricmc.loader.api.FabricLoader");
			Object loader = loaderApi.getMethod("getInstance").invoke(null);
			Object container = loaderApi.getMethod("getModContainer", String.class).invoke(loader, PF);
			if (container instanceof java.util.Optional<?> found && found.isPresent()) {
				Class<?> modApi = Class.forName("net.fabricmc.loader.api.ModContainer");
				Class<?> metadataApi = Class.forName("net.fabricmc.loader.api.metadata.ModMetadata");
				Class<?> versionApi = Class.forName("net.fabricmc.loader.api.Version");
				Object metadata = modApi.getMethod("getMetadata").invoke(found.get());
				Object version = metadataApi.getMethod("getVersion").invoke(metadata);
				String value = String.valueOf(versionApi.getMethod("getFriendlyString").invoke(version));
				var match = java.util.regex.Pattern.compile("^(?:v)?(\\d+)\\.(\\d+)\\.(\\d+)").matcher(value);
				if (match.find()) {
					int major = Integer.parseInt(match.group(1));
					int minor = Integer.parseInt(match.group(2));
					int patch = Integer.parseInt(match.group(3));
					patchVersion = major > 0 || minor > 31 ? 1000 : minor == 31 ? patch : -1;
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
			// Unknown build: don't guess its physics, crafting patterns, or permissions.
		}
		detectedProjectFlintPatch = patchVersion;
		return patchVersion;
	}

	private static boolean projectFlintAtLeast03168() { return projectFlintPatch() >= 68; }
	private static boolean projectFlintAtLeast03169() { return projectFlintPatch() >= 69; }

	private static void registerDynamiteBlasting(EmiRegistry registry) {
		EmiRecipeCategory blasting = categoryIcon(registry, "dynamite_blasting", "dynamite");
		if (blasting == null) return;
		info(registry, blasting, "dynamite/placement", "Weak Dynamite",
			pf("dynamite"), true,
			List.of(any(1, mc("flint_and_steel"), anyPf(1, "flint_firestarter", "bone_firestarter", "wood_firestarter"))),
			List.of(), projectFlintAtLeast03169()
				? List.of("3-second fuse; up to 4 ores and 2 stone blocks.",
					"Ore: 3 blocks. Stone: 2.25 blocks; 65% break chance.")
				: projectFlintAtLeast03168()
					? List.of("4.5-second fuse; up to 2 ores and 1 stone block.",
						"Ore: 1.75 blocks. Stone: 1.25 blocks; 50% break chance.")
					: List.of("Fuse: 4.5 seconds. Use a Firestarter offhand."));
		blasting(registry, blasting, "stone", mc("stone"), List.of(
			br(pf("stone_dust"), .66), br(pf("stone_dust"), .44), br(pf("stone_dust"), .1333)));
		blasting(registry, blasting, "cobblestone", mc("cobblestone"), List.of(
			br(pf("stone_dust"), .66), br(pf("stone_dust"), .44), br(pf("stone_dust"), .1333)));
		for (String rock : List.of("andesite", "granite")) {
			blasting(registry, blasting, rock, mc(rock), List.of(
				br(pf("fertilizer"), 1), br(pf("fertilizer"), .5), br(pf("stone_dust"), .33)));
		}
		blasting(registry, blasting, "diorite", mc("diorite"), List.of(
			br(pf("stone_dust"), 1), br(pf("stone_dust"), .5)));
		blasting(registry, blasting, "iron_ore", mc("iron_ore"), List.of(
			br(pf("iron_dust"), 1), br(pf("iron_dust"), .75),
			br(pf("slag"), .5), br(pf("gold_dust"), .03)));
		blasting(registry, blasting, "copper_ore", mc("copper_ore"), List.of(
			br(pf("copper_dust"), 1), br(pf("copper_dust"), .83),
			br(pf("copper_dust"), .24), br(pf("nickel_dust"), .10),
			br(pf("silver_dust"), .02), br(pf("slag"), .125)));
		blasting(registry, blasting, "coal_ore", mc("coal_ore"), List.of(
			br(pf("sulfur_dust"), .45), br(pf("stone_dust"), .43),
			br(pf("stone_dust"), .23), br(pf("carbon_dust"), .024)));
	}

	/** These numbers affect EMI's presentation, never ProjectFlint's explosions. */
	private static double dynamiteYieldMultiplier() {
		if (projectFlintAtLeast03169()) return DYNAMITE_YIELD_MULTIPLIER_03169;
		if (projectFlintAtLeast03168()) return DYNAMITE_YIELD_MULTIPLIER_03168;
		return LEGACY_DYNAMITE_YIELD_MULTIPLIER;
	}

	private record BlastRoll(EmiStack item, double normalChance) {}
	private static BlastRoll br(EmiStack item, double chance) { return new BlastRoll(item, chance); }
	private static void blasting(EmiRegistry registry, EmiRecipeCategory category,
		String path, EmiIngredient block, List<BlastRoll> rolls) {
		List<ProjectFlintLootRecipe.LootOutput> outputs = new ArrayList<>();
		for (BlastRoll roll : rolls) {
			if (roll.item().isEmpty()) return;
			double boosted = roll.normalChance() * dynamiteYieldMultiplier();
			// A roll above 100% guarantees its integer part, then rolls the
			// fractional remainder once. Do not clamp 120% down to 100%.
			long guaranteed = (long) Math.floor(boosted);
			if (guaranteed > 0) outputs.add(loot(roll.item().copy().setAmount(guaranteed),
				guaranteed == 1 ? "Guaranteed" : guaranteed + " guaranteed"));
			double remainder = boosted - guaranteed;
			if (remainder > 0.00001) {
				String odds = java.math.BigDecimal.valueOf(remainder * 100)
					.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
				outputs.add(loot(roll.item().copy(), odds + "% chance"));
			}
		}
		boolean rock = List.of("stone", "cobblestone", "andesite", "granite", "diorite").contains(path);
		List<String> notes = projectFlintAtLeast03169()
			? rock ? List.of("65% break chance within 2.25 blocks; max 2 rocks.")
				: List.of("Within 3 blocks; max 4 ore blocks per blast.")
			: projectFlintAtLeast03168()
				? rock ? List.of("50% break chance within 1.25 blocks; max 1 rock.")
					: List.of("Within 1.75 blocks; max 2 ore blocks per blast.")
				: List.of("Drops roll when this block breaks.");
		info(registry, category, "dynamite/blast/" + path, "Blast: " + pretty(path),
			block, true, List.of(pf("dynamite")), outputs, notes);
	}

	/**
	 * Basin entries mirror ProjectFlint 0.31.67's processing rolls. Each output
	 * corresponds to ONE independent roll, even for duplicate dusts.
	 * Percentages live on output tooltips,
	 * not in paragraphs printed across the player's recipe screen.
	 */
	private static void registerBasinRoutes(EmiRegistry registry) {
		EmiRecipeCategory basin = category(registry, "basin", "basin");
		if (basin == null) return;
		EmiIngredient planks = EmiIngredient.of(TagKey.of(RegistryKeys.ITEM, id("minecraft", "planks")));
		basin(registry, basin, "wood_dust", planks, 2, List.of(
			loot(pf("wood_dust"), "Always: 1 Wood Dust")));
		basin(registry, basin, "tannin", anyMc(1, "stripped_oak_log", "stripped_oak_wood", "stripped_birch_log", "stripped_birch_wood"), 3,
			List.of(loot(pf("tannin", 2), "Always: 2 Tannin")));
		basin(registry, basin, "raw_iron", mc("raw_iron"), 3, List.of(
			loot(pf("iron_dust"), "Always: 1 Iron Dust"),
			loot(pf("iron_dust"), "+1 Iron Dust: 75%"),
			loot(pf("slag"), "+1 Slag: 50%"),
			loot(pf("gold_dust"), "+1 Gold Dust: 3%")));
		basin(registry, basin, "raw_copper", mc("raw_copper"), 3, List.of(
			loot(pf("copper_dust"), "Always: 1 Copper Dust"),
			loot(pf("copper_dust"), "+1 Copper Dust: 83%"),
			loot(pf("copper_dust"), "+1 Copper Dust: 24%"),
			loot(pf("nickel_dust"), "+1 Nickel Dust: 10%"),
			loot(pf("silver_dust"), "+1 Silver Dust: 2%"),
			loot(pf("slag"), "+1 Slag: 12.5%")));
		basin(registry, basin, "andesite", mc("andesite"), 3, List.of(
			loot(pf("fertilizer"), "1 guaranteed"),
			loot(pf("fertilizer"), "+1 Fertilizer: 50%"),
			loot(pf("stone_dust"), "+1 Stone Dust: 33%")));
		basin(registry, basin, "granite", mc("granite"), 3, List.of(
			loot(pf("fertilizer"), "1 guaranteed"),
			loot(pf("fertilizer"), "+1 Fertilizer: 50%"),
			loot(pf("stone_dust"), "+1 Stone Dust: 33%")));
		for (String stone : List.of("stone", "cobblestone")) {
			basin(registry, basin, stone, mc(stone), 3, List.of(
				loot(pf("stone_dust"), "+1 Stone Dust: 66%"),
				loot(pf("stone_dust"), "+1 Stone Dust: 44%"),
				loot(pf("stone_dust"), "+1 Stone Dust: 13.33%")));
		}
		basin(registry, basin, "diorite", mc("diorite"), 3, List.of(
			loot(pf("stone_dust"), "1 guaranteed"),
			loot(pf("stone_dust"), "+1 Stone Dust: 50%")));
		basin(registry, basin, "stone_dust", pf("stone_dust"), 2,
			List.of(loot(pf("slag"), "Slag: 50% (otherwise nothing)")));
		basin(registry, basin, "gravel", mc("gravel"), 2, List.of(
			loot(mc("flint"), "Always: 1 Flint"),
			loot(pf("quartz_dust"), "+1 Quartz Dust: 20%"),
			loot(pf("iron_dust"), "+1 Iron Dust: 1%"),
			loot(pf("rare_earth_dust"), "+1 Rare Earth Dust: 4%")));
		basin(registry, basin, "sand", mc("sand"), 2, List.of(
			loot(pf("potassium_dust"), "Always: 1 Potassium Dust"),
			loot(pf("tin_dust"), "+1 Tin Dust: 15%"),
			loot(pf("mica_dust"), "+1 Mica Dust: 5%"),
			loot(pf("gold_dust"), "+1 Gold Dust: 1%"),
			loot(pf("rare_earth_dust"), "+1 Rare Earth Dust: 4%")));
		basin(registry, basin, "clay", mc("clay"), 3, List.of(
			loot(pf("mica_dust"), "Mica Dust: 5%"),
			loot(pf("rare_earth_dust"), "Rare Earth Dust: 4%"),
			loot(pf("silver_dust"), "Silver Dust: 0.33%"),
			loot(pf("gold_dust"), "Gold Dust: 2%")));
		basin(registry, basin, "rare_earth_dust", pf("rare_earth_dust"), 3, List.of(
			loot(pf("lithium_dust"), "Lithium Dust: 22.3%"),
			loot(pf("iron_dust"), "Iron Dust: 15%"),
			loot(pf("aluminum_dust"), "Aluminum Dust: 4.8%"),
			loot(pf("gallium_dust"), "Gallium Dust: 0.85%")));
		basin(registry, basin, "slag", pf("slag"), 3, List.of(
			loot(pf("iron_dust"), "Iron Dust: 33%"),
			loot(pf("copper_dust"), "Copper Dust: 33%"),
			loot(pf("silicon_dust"), "Silicon Dust: 33%"),
			loot(pf("rare_earth_dust"), "Rare Earth Dust: 1.8%")));
		basin(registry, basin, "charcoal", mc("charcoal"), 2, List.of(
			loot(pf("sulfur_dust"), "Sulfur Dust: 20%"),
			loot(pf("wood_dust"), "Wood Dust: 15%")));
		basin(registry, basin, "coal", mc("coal"), 3, List.of(
			loot(pf("sulfur_dust"), "Sulfur Dust: 45%"),
			loot(pf("stone_dust"), "Stone Dust: 43%"),
			loot(pf("stone_dust"), "+1 Stone Dust: 23%"),
			loot(pf("carbon_dust"), "Carbon Dust: 2.4%")));
	}

	private static void basin(EmiRegistry registry, EmiRecipeCategory category, String path,
		EmiIngredient input, int stomps, List<ProjectFlintLootRecipe.LootOutput> outputs) {
		if (input.isEmpty() || outputs.stream().anyMatch(o -> o.stack().isEmpty())) return;
		ProjectFlintBasinRecipe recipe = new ProjectFlintBasinRecipe(
			category, pfId("pf_emi/basin/" + path), input, pf("basin"), outputs, stomps);
		registry.addRecipe(recipe);
		ProjectFlintRecipeIndex.mark(recipe);
	}

	/** Server-side environmental effects are conditions, not item crafts/drops. */
	private static void registerUndergroundHazards(EmiRegistry registry) {
		EmiStack icon = mc("deepslate");
		if (icon.isEmpty()) return;
		EmiRecipeCategory environment = new EmiRecipeCategory(pfId("environmental_hazards"), icon);
		registry.addCategory(environment);
		info(registry, environment, "environment/speluncaphobia", "Speluncaphobia: Below Y=0",
			icon, false, List.of(), List.of(), List.of(
				"Below Y=0 while not in direct daytime sunlight or block light 13+.",
				"Stacks every 60 seconds, up to level IV; increases hunger use above 6 food points.",
				"Also slows natural healing. Does not cause direct lethal damage.",
				"Bright daytime open sky without rain OR block light >=13 removes the effect."));
		info(registry, environment, "environment/claustrophobia", "Claustrophobia: Below Y=-40",
			icon, false, List.of(), List.of(), List.of(
				"Below Y=-40 without sunlight or block light 13+.",
				"Movement speed -7%; attack damage -1; combines with Speluncaphobia.",
				"Strong light or sunlight removes both underground effects."));
	}

	/** FireclayForgeBlockEntity is a machine, not world placement. */
	private static void registerFireclayForgeRoutes(EmiRegistry registry) {
		EmiRecipeCategory forge = category(registry, "fireclay_forge", "fireclay_forge_controller");
		if (forge == null) return;
		process(registry, forge, "forge/wood_to_charcoal",
			List.of(anyMc(4, "oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log",
				"dark_oak_log", "mangrove_log", "cherry_log", "pale_oak_log",
				"oak_wood", "birch_wood", "spruce_wood", "jungle_wood", "acacia_wood",
				"dark_oak_wood", "mangrove_wood", "cherry_wood", "pale_oak_wood",
				"stripped_oak_log", "stripped_birch_log", "stripped_spruce_log", "stripped_jungle_log",
				"stripped_acacia_log", "stripped_dark_oak_log", "stripped_mangrove_log", "stripped_cherry_log",
				"stripped_pale_oak_log", "stripped_oak_wood", "stripped_birch_wood", "stripped_spruce_wood",
				"stripped_jungle_wood", "stripped_acacia_wood", "stripped_dark_oak_wood",
				"stripped_mangrove_wood", "stripped_cherry_wood", "stripped_pale_oak_wood")),
			List.of(pf("fireclay_forge_controller"), anyPf(1, "flint_firestarter", "bone_firestarter", "wood_firestarter")),
			List.of(mc("charcoal", 4)), 20 * 60);
		process(registry, forge, "forge/hardwood_alloy",
			List.of(pf("hardwood"), mc("charcoal")),
			List.of(pf("fireclay_forge_controller"), anyPf(1, "flint_firestarter", "bone_firestarter", "wood_firestarter")),
			List.of(pf("hardwood_alloy")), 20 * 60 * 5);
		process(registry, forge, "forge/mica_insulated_glass",
			List.of(pf("mbssig_dust"), mc("charcoal")),
			List.of(pf("fireclay_forge_controller"), anyPf(1, "flint_firestarter", "bone_firestarter", "wood_firestarter")),
			List.of(pf("pure_mica_insulated_glass")), 20 * 60 * 5);
		// Carbon refining spans two consecutive valid charcoal charges (400 seconds total).
		process(registry, forge, "forge/pure_carbon_dust",
			List.of(pf("carbon_dust"), mc("charcoal", 2)),
			List.of(pf("fireclay_forge_controller"), anyPf(1, "flint_firestarter", "bone_firestarter", "wood_firestarter")),
			List.of(pf("pure_carbon_dust")), 400 * 20);
	}

	/** Explicit 2x2 JSONs. Use a real grid view, not a generic new-features category. */
	private static void registerPersonalCrafting(EmiRegistry registry) {
		EmiRecipeCategory personal = category(registry, "personal_crafting", "tanned_utility_vest");
		if (personal == null) return;
		personalCategory = personal;
		grid(registry, personal, "leather_sheet_from_tanned", 2, true,
			List.of(pf("tanned_leather"), pf("bone_knife")), pf("leather_sheet"));
		grid(registry, personal, "leather_straps_from_sheet", 2, true,
			List.of(pf("leather_sheet"), pf("bone_knife")), pf("leather_strap", 2), true);
		grid(registry, personal, "leather_repair_kit", 2, true,
			List.of(pf("leather_strap"), mc("string"), mc("string")), pf("repair_kit"));
		// 0.31.64 restored first-wood tools in the personal 2x2 grid.
		grid(registry, personal, "wooden_pickaxe", 2, true,
			List.of(mc("stick"), mc("string"), pf("leather_strap"), pf("wooden_sheet")), mc("wooden_pickaxe"));
		grid(registry, personal, "wooden_axe", 2, true,
			List.of(pf("tannin"), pf("leather_sheet"), mc("stick"), pf("wooden_sheet")), mc("wooden_axe"));
		grid(registry, personal, "wooden_shovel", 2, true,
			List.of(mc("oak_planks"), mc("stick"), pf("leather_strap")), mc("wooden_shovel"));
		grid(registry, personal, "wooden_sword_personal", 2, true,
			List.of(pf("wooden_sheet"), pf("wooden_sheet"), mc("stick")), mc("wooden_sword"));
		grid(registry, personal, "wooden_hoe_personal", 2, true,
			List.of(pf("wooden_sheet"), pf("wooden_sheet"), mc("stick"), pf("plant_tool_binding")), mc("wooden_hoe"));
		// Real 2x2 cup recipes (Hardwood Table has higher-output equivalents).
		grid(registry, personal, "personal_wooden_cups", 2, true,
			List.of(pf("wooden_sheet"), pf("bone_knife")), pf("wooden_cup", 3), true);
		grid(registry, personal, "personal_wooden_cups_raw_milk", 2, true,
			List.of(pf("wooden_cup"), pf("wooden_cup"), pf("clay_milk_bucket")),
			pf("wooden_cup_raw_milk", 5));
		for (String kind : WOOD_VARIANTS) {
			EmiStack planks = mc(kind + "_planks");
			EmiStack log = mc(kind + "_log");
			EmiStack stripped = mc("stripped_" + kind + "_log");
			EmiIngredient cuttingAxe = any(1, mc("wooden_axe"), pf("hardwood_axe"));
			grid(registry, personal, kind + "_planks_from_log", 2, true,
				List.of(log, cuttingAxe), mc(kind + "_planks", 2));
			grid(registry, personal, kind + "_planks_from_stripped_log", 2, true,
				List.of(stripped, cuttingAxe.copy()), mc(kind + "_planks", 2));
			grid(registry, personal, kind + "_sheet_from_plank", 2, true,
				List.of(planks, pf("bone_knife")), pf("wooden_sheet", 3));
			grid(registry, personal, kind + "_board_from_plank", 2, true,
				List.of(planks, pf("flint_file")), pf("wooden_board"));
			// Restored in ProjectFlint 0.31.55-b: genuine early 2x2 hinged doors.
			grid(registry, personal, kind + "_door_hinged", 2, true,
				List.of(pf("wooden_door_hinge"), planks.copy(), planks.copy(), planks.copy()), mc(kind + "_door"));
		}
		grid(registry, personal, "crushed_stone_from_cobblestone", 2, true, List.of(mc("cobblestone")), pf("crushed_stone"));
		grid(registry, personal, "basin_crafting", 2, true,
			List.of(pf("leather_sheet"), pf("wooden_sheet"), mc("cobblestone"), mc("cobblestone")), pf("basin"));
		grid(registry, personal, "sticks_from_sheet", 2, true, List.of(pf("wooden_sheet")), mc("stick", 4));
		grid(registry, personal, "sand_from_flakes", 2, true,
			List.of(pf("sand_flake"), pf("sand_flake"), pf("sand_flake"), pf("sand_flake")), mc("sand"));
		grid(registry, personal, "clay_block_from_clay_balls", 2, true,
			List.of(mc("clay_ball"), mc("clay_ball"), mc("clay_ball"), mc("clay_ball")), mc("clay"));
		grid(registry, personal, "dirt_from_loose_soil", 2, true, List.of(pf("loose_soil")), mc("dirt"));
		grid(registry, personal, "loose_soil_from_dirt", 2, true, List.of(mc("dirt")), pf("loose_soil"));
		grid(registry, personal, "dirt_piles_from_loose_soil", 2, true,
			List.of(pf("loose_soil"), mc("wooden_shovel")), pf("dirt_pile", 4));
		grid(registry, personal, "fat_burning_torch", 2, true,
			List.of(mc("stick"), mc("string"), mc("charcoal")), pf("fat_burning_torch"));
		// Vanilla/native recipes remain authoritative for conditional repair behavior.
		// Avoid inventing an unconditional repair output for any damaged armor item.
		// Only actual 2x2 recipes belong here; the station-only table assembly
		// was moved to projectflint:hardwood_crafting in 0.31.51-b.
		grid(registry, personal, "hardwood", 2, true,
			List.of(pf("clay_water_bucket"), pf("wood_dust"), pf("wood_dust"), pf("tannin")), pf("hardwood"));
		// The first Hardwood Table must be craftable BEFORE owning a table.
		grid(registry, personal, "hardwood_crafting_table", 2, true,
			List.of(pf("hardwood_alloy"), pf("hardwood_alloy"), mc("cobblestone"), pf("bound_wood_processor")),
			pf("hardwood_crafting_table"));
	}

	/**
	 * ProjectFlint 0.31.51+ moved its Hardwood Table recipes to a dedicated
	 * recipe type. The previous fixed six-tool list was both incomplete and wrong
	 * (it missed Leather Straps and most of the 3x3 progression).
	 *
	 * Read the registered station-only type instead of guessing from output IDs.
	 * Neither vanilla crafting nor the 2x2 vest recipes own these recipes.
	 */
	@SuppressWarnings("unchecked")
	private static void registerHardwoodCrafting(EmiRegistry registry) {
		EmiRecipeCategory hardwood = category(registry, "hardwood_crafting", "hardwood_crafting_table");
		if (hardwood == null) return;

		RecipeType<?> rawType = Registries.RECIPE_TYPE.get(pfId("hardwood_crafting"));
		if (rawType != null && pfId("hardwood_crafting").equals(Registries.RECIPE_TYPE.getId(rawType))) {
			RecipeType<CraftingRecipe> hardwoodType = (RecipeType<CraftingRecipe>) rawType;
			int count = 0;
			Set<String> unresolved = new java.util.HashSet<>();
			for (var entry : EmiAgnos.getAllRecipesOfType(registry.getRecipeManager(), hardwoodType)) {
				Identifier id = entry.id().getValue();
				if (id == null || !PF.equals(id.getNamespace())) continue;
				CraftingRecipe wrapped = entry.value();
				CraftingRecipe delegate = unwrapHardwoodCrafting(wrapped);
				if (delegate == null) { unresolved.add(id.getPath()); continue; }

				// Read the current Hardwood pattern from the gameplay mod instead
				// of copying an obsolete or hypothetical shaped recipe into EMI.
				boolean shapeless = delegate instanceof ShapelessRecipe;
				List<EmiIngredient> cells = hardwoodCells(delegate);
				if (cells.isEmpty()) { unresolved.add(id.getPath()); continue; }
				ItemStack result = wrapped.getDisplays().isEmpty()
					? ItemStack.EMPTY : EmiPort.getOutput(wrapped);
				if (result.isEmpty() && !delegate.getDisplays().isEmpty()) result = EmiPort.getOutput(delegate);
				EmiStack output = EmiStack.of(result);
				if (output.isEmpty()) { unresolved.add(id.getPath()); continue; }
				Identifier serializer = Registries.RECIPE_SERIALIZER.getId(wrapped.getSerializer());
				boolean durable = pfId("hardwood_durable").equals(serializer);
				grid(registry, hardwood, "hardwood/type/" + id.getPath(), 3, shapeless, cells, output, durable);
				count++;
			}
			// This is intentionally not a fabricated '100% complete' assertion: a
			// server which fails to synchronize custom recipes must be diagnosed.
			if (count == 0) {
				dev.emi.emi.runtime.EmiLog.warn("PF-EMI: No synchronized hardwood recipes. Reading bundled ProjectFlint data instead.");
				count = registerBundledHardwoodRecipes(registry, hardwood, null);
			} else if (!unresolved.isEmpty()) {
				// Do not restore arbitrary server-disabled recipes: supplement only live
				// custom-wrapped recipes that cannot be decoded as shaped/shapeless.
				count += registerBundledHardwoodRecipes(registry, hardwood, unresolved);
			}
			dev.emi.emi.runtime.EmiLog.info("PF-EMI: Registered " + count + " Hardwood Table recipes");
		} else {
			// This also handles older servers or custom recipe synchronization gaps.
			int bundled = registerBundledHardwoodRecipes(registry, hardwood, null);
			dev.emi.emi.runtime.EmiLog.warn("PF-EMI: No synchronized Hardwood recipe type; registered " + bundled + " bundled recipes");
		}
		// In ProjectFlint 0.31.69+, Hardwood made from Wood Dust must be
		// imported from the real Hardwood recipe: DDD / DWD / TTT.
		// D=Wood Dust x5, W=Water Bucket x1, T=Tannin x3; output Hardwood.
		// Do not create a fake recipe here. The synchronized type or bundled
		// gameplay JSON above supplies the real grid, amount and Bucket remainder.
		registerOtherProjectFlintCrafting(registry, hardwood);
	}


	/**
	 * Fallback for serverless/un-synchronized custom recipes: read the actual JSON
	 * files inside the installed ProjectFlint mod. No hardcoded outputs and no
	 * ProjectFlint class imports. Minecraft recipe types still win when present.
	 *
	 * This client-side fallback is intentionally only invoked when the custom
	 * type supplies no recipes. It cannot account for server datapack removals.
	 */
	private static int registerBundledHardwoodRecipes(EmiRegistry registry, EmiRecipeCategory hardwood, Set<String> onlyIds) {
		int added = 0;
		try {
			Class<?> loaderApi = Class.forName("net.fabricmc.loader.api.FabricLoader");
			Object loader = loaderApi.getMethod("getInstance").invoke(null);
			Object maybeMod = loaderApi.getMethod("getModContainer", String.class).invoke(loader, PF);
			if (!(maybeMod instanceof java.util.Optional<?> opt) || opt.isEmpty()) return 0;
			Class<?> modApi = Class.forName("net.fabricmc.loader.api.ModContainer");
			Object rootPaths = modApi.getMethod("getRootPaths").invoke(opt.get());
			if (!(rootPaths instanceof List<?> roots)) return 0;
			for (Object root : roots) {
				if (!(root instanceof Path rootPath)) continue;
				Path recipeDirectory = rootPath.resolve("data/projectflint/recipe");
				if (!Files.isDirectory(recipeDirectory)) continue;
				try (Stream<Path> files = Files.list(recipeDirectory)) {
					for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json"))
						.sorted(Comparator.comparing(f -> f.getFileName().toString())).toList()) {
						try (Reader reader = Files.newBufferedReader(file)) {
							JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
							String type = json.has("type") ? json.get("type").getAsString() : "";
							if (!type.equals("projectflint:hardwood_shaped")
								&& !type.equals("projectflint:hardwood_shapeless")
								&& !type.equals("projectflint:hardwood_durable")) continue;
							String filename = file.getFileName().toString();
							String recipeId = filename.substring(0, filename.length() - 5);
							if (onlyIds != null && !onlyIds.contains(recipeId)) continue;
							EmiStack output = bundledOutput(json.get("result"));
							if (output.isEmpty()) continue;
							List<EmiIngredient> cells = bundledCells(json, type);
							if (cells.isEmpty() || cells.stream().anyMatch(EmiIngredient::isEmpty)) {
								// For shaped recipes empty grid cells are represented by EmiStack.EMPTY.
								if (cells.isEmpty() || !type.equals("projectflint:hardwood_shaped")) continue;
							}
							grid(registry, hardwood, "hardwood/bundled/" + recipeId, 3,
								!type.equals("projectflint:hardwood_shaped"), cells, output,
								type.equals("projectflint:hardwood_durable"));
							added++;
						} catch (RuntimeException | java.io.IOException ex) {
							dev.emi.emi.runtime.EmiLog.warn("PF-EMI: Skipping invalid bundled recipe " + file.getFileName() + ": " + ex.getMessage());
						}
					}
				}
			}
		} catch (ReflectiveOperationException | LinkageError | java.io.IOException ex) {
			dev.emi.emi.runtime.EmiLog.warn("PF-EMI: Could not index bundled ProjectFlint recipes: " + ex.getMessage());
		}
		return added;
	}

	private static EmiStack bundledOutput(JsonElement result) {
		if (result == null || !result.isJsonObject()) return EmiStack.EMPTY;
		JsonObject data = result.getAsJsonObject();
		if (!data.has("id")) return EmiStack.EMPTY;
		String id = data.get("id").getAsString();
		int count = data.has("count") ? data.get("count").getAsInt() : 1;
		return bundledItem(id, count);
	}

	private static EmiStack bundledItem(String id, int count) {
		Identifier identifier = Identifier.tryParse(id);
		if (identifier == null || count <= 0) return EmiStack.EMPTY;
		return stack(identifier.getNamespace(), identifier.getPath(), count);
	}

	private static EmiIngredient bundledIngredient(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) return EmiStack.EMPTY;
		String id = element.getAsString();
		if (id.startsWith("#")) {
			Identifier tagId = Identifier.tryParse(id.substring(1));
			return tagId == null ? EmiStack.EMPTY : EmiIngredient.of(TagKey.of(RegistryKeys.ITEM, tagId));
		}
		return bundledItem(id, 1);
	}

	private static List<EmiIngredient> bundledCells(JsonObject json, String type) {
		List<EmiIngredient> cells = new ArrayList<>();
		if (type.equals("projectflint:hardwood_shaped")) {
			if (!json.has("pattern") || !json.has("key")) return List.of();
			var pattern = json.getAsJsonArray("pattern");
			JsonObject key = json.getAsJsonObject("key");
			if (pattern.size() > 3) return List.of();
			for (int y = 0; y < 3; y++) {
				String row = y < pattern.size() ? pattern.get(y).getAsString() : "";
				if (row.length() > 3) return List.of();
				for (int x = 0; x < 3; x++) {
					String letter = x < row.length() ? row.substring(x, x + 1) : " ";
					if (letter.equals(" ")) {
						cells.add(EmiStack.EMPTY);
					} else {
						EmiIngredient ingredient = bundledIngredient(key.get(letter));
						if (ingredient.isEmpty()) return List.of();
						cells.add(ingredient);
					}
				}
			}
		} else {
			if (json.has("ingredients")) {
				for (JsonElement item : json.getAsJsonArray("ingredients")) cells.add(bundledIngredient(item));
			}
			if (type.equals("projectflint:hardwood_durable") && json.has("tool")) {
				cells.add(bundledIngredient(json.get("tool")));
			}
		}
		return cells.size() > 9 ? List.of() : cells;
	}

	/** Unwrap ProjectFlint's public CraftingRecipe interface without linking to its JAR. */
	private static CraftingRecipe unwrapHardwoodCrafting(CraftingRecipe recipe) {
		for (int i = 0; i < 4 && recipe != null; i++) {
			if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) return recipe;
			try {
				Field delegateField = recipe.getClass().getDeclaredField("delegate");
				delegateField.setAccessible(true);
				Object value = delegateField.get(recipe);
				if (!(value instanceof CraftingRecipe next) || value == recipe) return null;
				recipe = next;
			} catch (ReflectiveOperationException | SecurityException ex) {
				return null;
			}
		}
		return null;
	}

	/** Preserve the exact 3x3 arrangement or ingredient alternatives of the delegate. */
	private static List<EmiIngredient> hardwoodCells(CraftingRecipe recipe) {
		if (recipe instanceof ShapedRecipe shaped) {
			List<EmiIngredient> cells = new ArrayList<>(9);
			var ingredients = shaped.getIngredients();
			for (int y = 0; y < 3; y++) {
				for (int x = 0; x < 3; x++) {
					int index = y * shaped.getWidth() + x;
					if (x >= shaped.getWidth() || y >= shaped.getHeight() || index >= ingredients.size()) {
						cells.add(EmiStack.EMPTY);
						continue;
					}
					var ingredient = ingredients.get(index);
					cells.add(ingredient.isPresent() ? EmiIngredient.of(ingredient.get()) : EmiStack.EMPTY);
				}
			}
			return cells;
		}
		if (recipe instanceof ShapelessRecipe shapeless) {
			return shapeless.getIngredientPlacement().getIngredients().stream()
				.map(EmiIngredient::of).toList();
		}
		return List.of();
	}

	/**
	 * Catch any new native ProjectFlint crafting JSON missed by the hardcoded
	 * release catalog. Route by actual grid requirements: 2x2 vs 3x3.
	 * Recipes with specialized non-vanilla types remain under their own categories.
	 */
	private static void registerOtherProjectFlintCrafting(EmiRegistry registry, EmiRecipeCategory hardwood) {
		for (var entry : EmiAgnos.getAllRecipesOfType(registry.getRecipeManager(), RecipeType.CRAFTING)) {
			CraftingRecipe recipe = entry.value();
			Identifier id = EmiPort.getId(recipe);
			if (id == null || !PF.equals(id.getNamespace()) || REMAPPED_GRID_RECIPES.contains(id.getPath())) continue;
			if (id.getPath().equals("primitive_wooden_sheet_from_stripped_log")
				|| id.getPath().equals("personal_pmi_sheets")) continue;
			// The Tanned Utility Vest is NEVER a crafting-table recipe.
			if (id.getPath().equals("tanned_utility_vest") || id.getPath().equals("personal_pmi_sheets")) continue;
			int size;
			boolean shapeless;
			List<EmiIngredient> cells;
			if (recipe instanceof ShapedRecipe shaped) {
				size = shaped.getWidth() <= 2 && shaped.getHeight() <= 2 ? 2 : 3;
				List<EmiIngredient> nativeCells = new EmiShapedRecipe(shaped).getInputs();
				cells = size == 2 ? List.of(nativeCells.get(0), nativeCells.get(1), nativeCells.get(3), nativeCells.get(4))
					: nativeCells;
				shapeless = false;
			} else if (recipe instanceof ShapelessRecipe sh) {
				cells = new EmiShapelessRecipe(sh).getInputs();
				size = cells.size() <= 4 ? 2 : 3;
				shapeless = true;
			} else continue;
			EmiRecipeCategory category = size == 2 ? personalCategory : hardwood;
			if (category == null) continue;
			EmiStack output = EmiStack.of(EmiPort.getOutput(recipe));
			if (output.isEmpty()) continue;
			AUTO_REMAPPED_RECIPES.add(id.getPath());
			grid(registry, category, "synced/" + id.getPath(), size, shapeless, cells, output);
		}
	}

	private static void grid(EmiRegistry registry, EmiRecipeCategory category,
		String path, int size, boolean shapeless, List<EmiIngredient> cells, EmiStack output) {
		grid(registry, category, path, size, shapeless, cells, output, false);
	}

	private static void grid(EmiRegistry registry, EmiRecipeCategory category,
		String path, int size, boolean shapeless, List<EmiIngredient> cells, EmiStack output, boolean durable) {
		if (output.isEmpty() || cells.stream().anyMatch(cell -> cell == null)) return;
		// Missing (unregistered) real ingredients invalidate the route; empty grid cells are OK.
		for (EmiIngredient cell : cells) {
			if (cell.isEmpty() && cell != EmiStack.EMPTY) return;
		}
		markCraftingRemainders(cells);
		ProjectFlintGridRecipe recipe = new ProjectFlintGridRecipe(category, pfId("pf_emi/grid/" + path), cells, output, size, shapeless, durable);
		registry.addRecipe(recipe);
		ProjectFlintRecipeIndex.mark(recipe);
	}


	/** Source-defined, unchanged vessel/form remainders for Hardwood station crafts. */
	private static void markCraftingRemainders(List<EmiIngredient> cells) {
		for (EmiIngredient ingredient : cells) {
			for (EmiStack option : ingredient.getEmiStacks()) {
				if (id("minecraft", "water_bucket").equals(option.getId())) {
					option.setRemainder(mc("bucket"));
				} else if (pfId("clay_water_bucket").equals(option.getId())) {
					option.setRemainder(pf("clay_bucket"));
				} else if (pfId("clay_milk_bucket").equals(option.getId())) {
					option.setRemainder(pf("clay_bucket"));
				} else if (pfId("unfired_brick_mold").equals(option.getId())) {
					option.setRemainder(pf("unfired_brick_mold"));
				} else if (pfId("bone_knife").equals(option.getId())
					|| pfId("flint_file").equals(option.getId())
					|| pfId("flint_plant_cutters").equals(option.getId())
					|| pfId("hardwood_axe").equals(option.getId())
					|| id("minecraft", "wooden_axe").equals(option.getId())) {
					option.setRemainder(option.copy());
				}
			}
		}
	}

	private static EmiIngredient rawMeats(long count) {
		return any(count, anyMc(count, "beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon",
			"tropical_fish", "pufferfish"), anyPf(count, "raw_calamari", "raw_turtle_meat", "raw_fox_meat",
			"raw_frog_meat", "raw_goat_meat", "raw_horse_meat", "raw_llama_meat",
			"raw_dolphin_meat", "raw_bear_meat", "raw_camel_meat"));
	}

	private static EmiIngredient cookedMeats(long count) {
		return any(count, anyMc(count, "cooked_beef", "cooked_porkchop", "cooked_chicken", "cooked_mutton",
			"cooked_rabbit", "cooked_cod", "cooked_salmon"),
			anyPf(count,"cooked_calamari", "cooked_turtle_meat", "cooked_fox_meat", "cooked_frog_meat",
				"cooked_goat_meat", "cooked_horse_meat", "cooked_llama_meat", "cooked_dolphin_meat",
				"cooked_bear_meat", "cooked_camel_meat"));
	}

	private static void registerBlockDropRecipes(EmiRegistry registry) {
		EmiRecipeCategory drops = category(registry, "block_drops", "advanced_flint_multitool");
		if (drops == null) return;

		EmiIngredient flintTier = anyPf(1, "flint_multitool", "advanced_flint_multitool");
		EmiIngredient workDismantle = flintTier;
		EmiIngredient wellWood = anyPf(1, "flint_axe", "flint_plant_cutters");
		EmiIngredient plantTools = anyPf(1,
			"crude_harvesting_tool", "crude_multitool", "flint_multitool",
			"flint_plant_cutters", "advanced_flint_multitool");

		// v0.31.55 OreMiningRules: ALL Deepslate is inaccessible, including Coal Ore.
		// Stone-family timings: pickaxes 270 ticks; Hardwood Hammer 240 ticks.
		EmiIngredient earlyPickaxes = any(1, mc("wooden_pickaxe"), pf("hardwood_pickaxe"));
		blockDrop(registry, drops, "stone/crude_hammer_processing",
			mc("stone"), List.of(anyPf(1, "crude_stone_hammer", "crude_multitool", "flint_multitool", "advanced_flint_multitool")),
			List.of(loot(pf("rock", 4), "4 Rocks")),
			List.of("Stone becomes Crushed Stone. Crude Hammer: about 11.7 seconds."));
		blockDrop(registry, drops, "ore/coal_ore/pickaxe",
			mc("coal_ore"), List.of(earlyPickaxes),
			List.of(loot(mc("coal"), "1 Coal; Wooden/Hardwood Pickaxe, 13.5 seconds")),
			List.of("Higher-tier compatible vanilla Pickaxes obey vanilla timings."));
		blockDrop(registry, drops, "ore/coal_ore/hammer",
			mc("coal_ore"), List.of(pf("hardwood_hammer")),
			List.of(loot(mc("coal", 2), "2 Coal; Hardwood Hammer, 12 seconds")),
			List.of("Unqualified tools: 45 seconds and no loot."));
		blockDrop(registry, drops, "ore/iron_hardwood_hammer",
			mc("iron_ore"), List.of(pf("hardwood_hammer")),
			List.of(loot(mc("raw_iron"), "Exactly 1 Raw Iron; Hardwood Hammer, 13.5 seconds")),
			List.of("Deepslate Iron Ore is completely inaccessible."));
		blockDrop(registry, drops, "ore/copper_hardwood_hammer",
			mc("copper_ore"), List.of(pf("hardwood_hammer")),
			List.of(loot(mc("raw_copper"), "Exactly 1 Raw Copper; Hardwood Hammer, 13.5 seconds")),
			List.of("Deepslate Copper Ore is completely inaccessible."));
		for (String stone : List.of("stone", "cobblestone", "andesite", "granite", "diorite")) {
			blockDrop(registry, drops, "stone/" + stone + "/early_pickaxe",
				mc(stone), List.of(earlyPickaxes),
				List.of(loot(mc(stone.equals("stone") ? "cobblestone" : stone), "13.5 seconds with Wooden/Hardwood Pickaxe")),
				List.of("Stone-family blocks require an appropriate held tool; the Utility Vest alone is insufficient."));
			blockDrop(registry, drops, "stone/" + stone + "/hammer",
				mc(stone), List.of(pf("hardwood_hammer")),
				List.of(loot(mc(stone.equals("stone") ? "cobblestone" : stone), "12 seconds with Hardwood Hammer")),
				List.of("Hands, food and unrelated tools cannot break the vanilla stone family."));
		}
		info(registry, drops, "drops/locked_deepslate", "Deepslate: Locked Progression",
			anyMc(1, "deepslate", "cobbled_deepslate", "polished_deepslate", "deepslate_bricks",
				"deepslate_tiles", "deepslate_coal_ore", "deepslate_copper_ore", "deepslate_iron_ore",
				"deepslate_gold_ore", "deepslate_redstone_ore", "deepslate_lapis_ore",
				"deepslate_diamond_ore", "deepslate_emerald_ore"), true,
			List.of(), List.of(),
			List.of("No current survival tool can break Deepslate or any Deepslate Ore.",
				"No drops are obtainable, including with higher-tier vanilla Pickaxes."));

		// v0.31.68+: no early-game drop recipes for advanced geology.
		// Later-tier vanilla pickaxes are separate; do not label these unobtainable forever.
		if (projectFlintAtLeast03168()) {
			info(registry, drops, "drops/advanced_geology", "Advanced Geology",
				anyMc(1, "dripstone_block", "pointed_dripstone", "tuff", "calcite",
					"basalt", "smooth_basalt", "blackstone", "amethyst_block"), true,
				List.of(), List.of(), List.of("Requires a later mining tool."));
		}

		// Explicit recoverable decorations; no generic 'wear a Vest' salvage bypass.
		for (String decoration : List.of("fat_burning_torch", "flint_torch", "nest", "axle")) {
			blockDrop(registry, drops, "decoration/" + decoration, pf(decoration), List.of(),
				List.of(loot(pf(decoration), "Recovered intact by hand (5 ticks)")),
				List.of(decoration.equals("nest") ? "Stored Nest eggs are ejected on recovery."
					: decoration.equals("axle") ? "Axle is orientation-aware but does not transmit mechanical power yet."
					: "No Utility Vest or mining tool is required."));
		}
		for (String torch : List.of("torch", "soul_torch", "redstone_torch")) {
			blockDrop(registry, drops, "decoration/vanilla_" + torch, mc(torch), List.of(),
				List.of(loot(mc(torch), "Recovered by hand (5 ticks); wall variant also supported")), List.of());
		}

		// SleepingBagBlock: normal salvage vs advanced-tool full recovery.
		blockDrop(registry, drops, "sleeping_bag_salvage",
			pf("sleeping_bag"), List.of(), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("woven_wool", 2), "Normal break salvage")
			), List.of("Breaking either half removes the full two-block Sleeping Bag."));
		blockDrop(registry, drops, "sleeping_bag_full",
			pf("sleeping_bag"), List.of(pf("advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("sleeping_bag"), "Advanced Flint Multitool recovers the full bag")
			), List.of());

		// Wooden beams return themselves plus their screw with the dedicated Well wood tools.
		for (String beam : WOODEN_BEAMS) {
			blockDrop(registry, drops, "beam/" + beam,
				pf(beam), List.of(wellWood), List.of(
					new ProjectFlintLootRecipe.LootOutput(pf(beam), "Recovered beam"),
					new ProjectFlintLootRecipe.LootOutput(pf("wooden_screw"), "1 Wooden Screw")
				), List.of());
		}

		blockDrop(registry, drops, "crushed_stone_block",
			pf("crushed_stone"), List.of(pf("advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("crushed_stone"), "Player-placed Crushed Stone only")
			), List.of());

		// v0.31.69+ treats Packed Mud and Mud Bricks as recoverable
		// pickaxe masonry, at the same early tier as Fireclay Bricks.
		// Do not imply the new mining permission exists on older game builds.
		if (projectFlintAtLeast03169()) {
			EmiIngredient masonryPickaxes = any(1, mc("wooden_pickaxe"), pf("hardwood_pickaxe"));
			for (String mud : List.of("packed_mud", "mud_bricks")) {
				blockDrop(registry, drops, "masonry/" + mud,
					mc(mud), List.of(masonryPickaxes),
					List.of(loot(mc(mud), "Recovered intact")),
					List.of("Use a Wooden or Hardwood Pickaxe."));
			}
		}

		blockDrop(registry, drops, "loose_mud_bricks",
			pf("loose_mud_bricks"), List.of(flintTier), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("loose_mud_bricks"), "Recovered intact")
			), List.of());
		blockDrop(registry, drops, "loose_mud_brick_slab",
			pf("loose_mud_brick_slab"), List.of(flintTier), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("loose_mud_brick_slab"), "1 per slab; double slabs return 2")
			), List.of());

		// Limestone family source-audited recovery.
		String[][] limestone = {
			{"limestone", "cobbled_limestone"},
			{"cobbled_limestone", "cobbled_limestone"},
			{"smooth_limestone", "smooth_limestone"},
			{"limestone_bricks", "limestone_bricks"},
			{"chiseled_limestone", "chiseled_limestone"},
			{"polished_limestone", "polished_limestone"},
			{"checkered_tile", "checkered_tile"},
			{"glazed_checkered_tile", "glazed_checkered_tile"}
		};
		for (String[] pair : limestone) {
			blockDrop(registry, drops, "limestone/" + pair[0],
				pf(pair[0]), List.of(pf("advanced_flint_multitool")), List.of(
					new ProjectFlintLootRecipe.LootOutput(pf(pair[1]), "Advanced Flint Multitool")
				), List.of());
		}

		blockDrop(registry, drops, "sanding_station",
			pf("sanding_station"), List.of(pf("flint_axe")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("sanding_station"), "Preserves stored sanding durability")
			), List.of());
		blockDrop(registry, drops, "bookshelf",
			pf("bookshelf"), List.of(pf("flint_axe")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("bookshelf"), "Preserves installed-book count")
			), List.of());
		blockDrop(registry, drops, "woven_chest",
			pf("woven_chest"), List.of(pf("flint_axe")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("woven_chest"), "Stored contents spill normally")
			), List.of());
		blockDrop(registry, drops, "wooden_grinder",
			pf("wooden_grinder"), List.of(pf("flint_axe")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("wooden_grinder"), "Recovered intact")
			), List.of());
		blockDrop(registry, drops, "mortar_kiln",
			pf("mortar_kiln"), List.of(pf("advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("mortar_kiln"), "Stored Branches/input are also returned")
			), List.of());
		blockDrop(registry, drops, "mortar_kiln_base",
			pf("mortar_kiln_base"), List.of(flintTier), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("mortar_kiln_base"), "Recovered intact")
			), List.of());
		blockDrop(registry, drops, "wooden_grinder_base",
			pf("wooden_grinder_base"), List.of(flintTier), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("wooden_grinder_base"), "Recovered intact")
			), List.of());

		blockDrop(registry, drops, "advanced_kiln",
			pf("mortar_bricks", 8), List.of(workDismantle), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("mortar_bricks", 8), "Recovered from the formed 2x2x2 kiln"),
				new ProjectFlintLootRecipe.LootOutput(pf("advanced_kiln_glass"), "Returned on dismantle")
			), List.of("Applies to Advanced Kiln Controller/Casing blocks."));

		// Wattle/Daub recovery is tool-dependent.
		for (String block : List.of("wattled_daub_bricks", "wattled_daub_window")) {
			blockDrop(registry, drops, block + "_intact",
				pf(block), List.of(anyPf(1, "flint_multitool", "advanced_flint_multitool")), List.of(
					new ProjectFlintLootRecipe.LootOutput(pf(block), "Flint Multitool or Advanced Flint Multitool")
				), List.of());
			blockDrop(registry, drops, block + "_salvage",
				pf(block), List.of(), List.of(
					new ProjectFlintLootRecipe.LootOutput(pf("crude_reed"), "Without the required multitool"),
					new ProjectFlintLootRecipe.LootOutput(pf("twig", 2), "Without the required multitool")
				), List.of());
		}

		blockDrop(registry, drops, "wattle_floor_tile",
			pf("wattled_daub_floor_tile"), List.of(), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("wattled_daub_floor_tile"), "Recovered intact")
			), List.of());
		blockDrop(registry, drops, "mortar_bricks",
			pf("mortar_bricks"), List.of(anyPf(1, "crude_stone_hammer", "crude_multitool", "flint_multitool", "advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("mortar_bricks"), "Hammer-like tool")
			), List.of());
		blockDrop(registry, drops, "mortar_brick_slab",
			pf("mortar_brick_slab"), List.of(flintTier), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("mortar_brick_slab"), "1 per slab; double slabs return 2")
			), List.of());
		blockDrop(registry, drops, "crude_clay_block",
			pf("crude_clay_block"), List.of(), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("crude_clay_block"), "Recovered intact")
			), List.of());
		blockDrop(registry, drops, "crushed_stone_slab",
			pf("crushed_stone_slab"), List.of(flintTier), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("crushed_stone_slab"), "1 per slab; double slabs return 2")
			), List.of());
		blockDrop(registry, drops, "crude_clay_slab",
			pf("crude_clay_slab"), List.of(flintTier), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("crude_clay_slab"), "1 per slab; double slabs return 2")
			), List.of());

		blockDrop(registry, drops, "baked_kiln_full",
			pf("baked_clay_kiln"), List.of(pf("advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("baked_clay_kiln"), "Kiln base"),
				new ProjectFlintLootRecipe.LootOutput(pf("baked_kiln_top"), "Returned when top is present")
			), List.of());
		blockDrop(registry, drops, "baked_kiln_salvage",
			pf("baked_clay_kiln"), List.of(pf("flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("fired_clay", 2), "Non-advanced dismantle salvage")
			), List.of());
		blockDrop(registry, drops, "clay_grinder_full",
			pf("clay_grinder"), List.of(pf("advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("clay_grinder"), "Recovered intact")
			), List.of());
		blockDrop(registry, drops, "clay_grinder_salvage",
			pf("clay_grinder"), List.of(pf("flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("super_heated_clay", 4), "Non-advanced dismantle salvage")
			), List.of());

		// 0.31.51-b: natural Stone takes 270 ticks with approved wooden-tier
		// tools and drops Cobblestone (not a zero-tool vanilla quick break).
		blockDrop(registry, drops, "stone/wooden_tier",
			mc("stone"), List.of(any(1, mc("wooden_pickaxe"), pf("hardwood_pickaxe"), pf("hardwood_hammer"))),
			List.of(loot(mc("cobblestone"), "1 Cobblestone; 13.5-second natural Stone harvest")),
			List.of("Unqualified tools and bare hands cannot break ores."));

		// Vanilla blocks whose normal drops are replaced by ProjectFlint progression.
		blockDrop(registry, drops, "stone_hammer",
			mc("stone"), List.of(anyPf(1, "crude_stone_hammer", "crude_multitool", "flint_multitool", "advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("rock", 4), "Stone becomes Crushed Stone in-world and drops four Rocks")
			), List.of());
		blockDrop(registry, drops, "gravel_crude",
			mc("gravel"), List.of(pf("crude_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("flint_chip"), "Crude Multitool")
			), List.of());
		blockDrop(registry, drops, "gravel_flint",
			mc("gravel"), List.of(pf("flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("flint_chip", 2), "Flint Multitool")
			), List.of());
		blockDrop(registry, drops, "gravel_advanced",
			mc("gravel"), List.of(pf("advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(mc("flint"), "Guaranteed"),
				new ProjectFlintLootRecipe.LootOutput(pf("flint_chip"), "Plus one Flint Chip")
			), List.of());

		blockDrop(registry, drops, "short_grass_binding",
			mc("short_grass"), List.of(pf("flint_plant_cutters")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("plant_tool_binding"), "Dedicated Plant Cutters upgrade")
			), List.of());
		blockDrop(registry, drops, "grass_string",
			anyMc(1, "short_grass", "tall_grass"), List.of(plantTools), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("crude_plant_string", 3), "Yield depends on tool tier; Tall Grass doubles it")
			), List.of("Base/Flint/Advanced yields are encoded by the actual tool tier."));
		blockDrop(registry, drops, "leaves_flint_cutters",
			anyMc(1, "oak_leaves", "birch_leaves", "spruce_leaves", "jungle_leaves",
				"acacia_leaves", "dark_oak_leaves", "mangrove_leaves", "cherry_leaves", "pale_oak_leaves"),
			List.of(pf("flint_plant_cutters")),
			List.of(loot(pf("branch", 5), "5 Branches each time")),
			List.of("Saplings (25%), Sticks (10%), and rare Oak apples may also drop."));
		blockDrop(registry, drops, "leaf_litter",
			mc("leaf_litter"), List.of(plantTools), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("plant_fiber", 6), "Maximum full-layer yield; scales by tool tier/layers")
			), List.of());
		blockDrop(registry, drops, "grass_scrape",
			mc("grass_block"), List.of(), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("plant_fiber"), "58% chance when hand-scraping")
			), List.of("Grass Block becomes Dirt."));

		blockDrop(registry, drops, "dirt_crude",
			any(1, mc("dirt"), mc("grass_block"), pf("loose_soil")), List.of(pf("crude_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("dirt_pile"), "1 Dirt Pile")
			), List.of());
		blockDrop(registry, drops, "dirt_flint",
			any(1, mc("dirt"), mc("grass_block"), pf("loose_soil")), List.of(pf("flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("dirt_pile", 2), "2 Dirt Piles")
			), List.of());
		blockDrop(registry, drops, "dirt_advanced",
			any(1, mc("dirt"), mc("grass_block"), pf("loose_soil")), List.of(pf("advanced_flint_multitool")), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("dirt_pile", 3), "3 Dirt Piles")
			), List.of());

		// Pumpkin/Melon specialized recovery.
		blockDrop(registry, drops, "pumpkin_slice",
			mc("pumpkin"), List.of(plantTools), List.of(
				new ProjectFlintLootRecipe.LootOutput(pf("pumpkin_slice"), "Plant-cutting tool")
			), List.of());
		blockDrop(registry, drops, "melon_slice",
			mc("melon"), List.of(plantTools), List.of(
				new ProjectFlintLootRecipe.LootOutput(mc("melon_slice"), "Plant-cutting tool")
			), List.of());
		blockDrop(registry, drops, "pumpkin_seeds",
			mc("pumpkin"), List.of(pf("flint_axe")), List.of(
				new ProjectFlintLootRecipe.LootOutput(mc("pumpkin_seeds"), "Flint Axe")
			), List.of());
		blockDrop(registry, drops, "melon_seeds",
			mc("melon"), List.of(pf("flint_axe")), List.of(
				new ProjectFlintLootRecipe.LootOutput(mc("melon_seeds"), "Flint Axe")
			), List.of());

		// Raw log stripping routes.
		String[][] logs = {
			{"oak_log", "stripped_oak_log"},
			{"spruce_log", "stripped_spruce_log"},
			{"birch_log", "stripped_birch_log"},
			{"jungle_log", "stripped_jungle_log"},
			{"acacia_log", "stripped_acacia_log"},
			{"dark_oak_log", "stripped_dark_oak_log"},
			{"mangrove_log", "stripped_mangrove_log"},
			{"cherry_log", "stripped_cherry_log"},
			{"pale_oak_log", "stripped_pale_oak_log"},
			{"crimson_stem", "stripped_crimson_stem"},
			{"warped_stem", "stripped_warped_stem"},
			{"bamboo_block", "stripped_bamboo_block"}
		};
		for (String[] pair : logs) {
			blockDrop(registry, drops, "strip/" + pair[0],
				mc(pair[0]), List.of(pf("flint_axe")), List.of(
					new ProjectFlintLootRecipe.LootOutput(mc(pair[1]), "Flint Axe strips and recovers the log")
				), List.of());
		}
	}

	private static void registerMobLootRecipes(EmiRegistry registry) {
		EmiRecipeCategory mobs = category(registry, "mob_drops", "animal_bones");
		if (mobs == null) return;

		// The Clay Bucket is a cow interaction, NOT an item dropped by killing a cow.
		// Keep it indexed here as players naturally inspect cow loot to find milk.
		info(registry, mobs, "mob/cow_milking", "Cow: Milk with Clay Bucket",
			pf("clay_bucket"), true, List.of(mc("cow_spawn_egg")),
			List.of(loot(pf("clay_milk_bucket"), "Cow must be an adult")),
			List.of("Right-click a living adult cow with an empty Clay Bucket.",
				"The cow survives. This is milking, not a death drop."));

		mob(registry, mobs, "chicken", mc("chicken_spawn_egg"), List.of(
			loot(mc("chicken"), "Vanilla base drop"),
			loot(mc("feather", 2), "0-2 Feathers"),
			loot(pf("animal_bones"), "Exactly 1 Animal Bones")
		));
		mob(registry, mobs, "pig", mc("pig_spawn_egg"), List.of(
			loot(mc("porkchop", 3), "Vanilla base range"),
			loot(pf("animal_bones", 2), "1-2 Animal Bones")
		));
		mob(registry, mobs, "sheep", mc("sheep_spawn_egg"), List.of(
			loot(mc("mutton", 2), "Vanilla base range"),
			loot(pf("wool", 2), "Vanilla Wool is replaced with 1-2 ProjectFlint Wool"),
			loot(pf("animal_bones", 3), "1-3 Animal Bones")
		));
		mob(registry, mobs, "cow", mc("cow_spawn_egg"), List.of(
			loot(mc("beef", 3), "Vanilla base range"),
			loot(pf("raw_leather", 2), "At least 1 Raw Leather from adult cows"),
			loot(pf("animal_bones", 3), "2-3 Animal Bones")
		));
		mob(registry, mobs, "squid", mc("squid_spawn_egg"), List.of(
			loot(mc("ink_sac", 3), "Vanilla base range"),
			loot(pf("raw_calamari"), "Always adds 1 Raw Calamari on the default loot table"),
			loot(pf("animal_bones", 3), "1-3 Animal Bones")
		));
		mob(registry, mobs, "horse", mc("horse_spawn_egg"), List.of(
			loot(pf("raw_horse_meat", 3), "2-3 Raw Horse Meat; cooked if killed on fire"),
			loot(pf("raw_leather"), "50% independent chance; vanilla Leather removed"),
			loot(pf("animal_bones"), "50% independent chance")
		));
		mob(registry, mobs, "fox", mc("fox_spawn_egg"), List.of(
			loot(pf("raw_fox_meat"), "1; cooked if killed on fire")
		));
		mob(registry, mobs, "frog", mc("frog_spawn_egg"), List.of(
			loot(pf("raw_frog_meat"), "1; cooked if killed on fire")
		));
		mob(registry, mobs, "goat", mc("goat_spawn_egg"), List.of(
			loot(pf("raw_goat_meat", 2), "1-2; cooked if killed on fire")
		));
		mob(registry, mobs, "llama", mc("llama_spawn_egg"), List.of(
			loot(pf("raw_llama_meat", 3), "2-3; cooked if killed on fire")
		));
		mob(registry, mobs, "dolphin", mc("dolphin_spawn_egg"), List.of(
			loot(pf("raw_dolphin_meat", 2), "1-2; cooked if killed on fire")
		));
		mob(registry, mobs, "polar_bear", mc("polar_bear_spawn_egg"), List.of(
			loot(pf("raw_bear_meat", 4), "2-4; cooked if killed on fire")
		));
		mob(registry, mobs, "black_bear", pf("black_bear_spawn_egg"), List.of(
			loot(pf("raw_bear_meat", 2), "1-2; cooked if killed on fire"),
			loot(pf("raw_leather", 3), "2-3 extra Raw Leather from adults"),
			loot(pf("animal_bones", 3), "2-3 Animal Bones")
		));
		mob(registry, mobs, "brown_bear", pf("brown_bear_spawn_egg"), List.of(
			loot(pf("raw_bear_meat", 2), "1-2; cooked if killed on fire"),
			loot(pf("raw_leather", 3), "2-3 extra Raw Leather from adults"),
			loot(pf("animal_bones", 3), "2-3 Animal Bones")
		));
		mob(registry, mobs, "turtle", mc("turtle_spawn_egg"), List.of(
			loot(pf("raw_turtle_meat"), "1; cooked if killed on fire")
		));
		mob(registry, mobs, "camel", mc("camel_spawn_egg"), List.of(
			loot(pf("raw_camel_meat", 4), "3-4; cooked if killed on fire")
		));
	}

	private static void blockDrop(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String id,
		EmiIngredient subject,
		List<EmiIngredient> catalysts,
		List<ProjectFlintLootRecipe.LootOutput> outputs,
		List<String> notes
	) {
		info(registry, category, "drops/" + id, "Block Drop / Recovery", subject, true, catalysts, outputs, notes);
	}

	private static void mob(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String id,
		EmiIngredient spawnEgg,
		List<ProjectFlintLootRecipe.LootOutput> outputs
	) {
		info(registry, category, "mob/" + id, "Mob Loot: " + pretty(id), spawnEgg, false, List.of(), outputs,
			List.of());
	}

	private static ProjectFlintLootRecipe.LootOutput loot(EmiStack stack, String note) {
		return new ProjectFlintLootRecipe.LootOutput(stack, note);
	}

	private static void info(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String id,
		String title,
		EmiIngredient subject,
		boolean subjectIsInput,
		List<EmiIngredient> catalysts,
		List<ProjectFlintLootRecipe.LootOutput> outputs,
		List<String> notes
	) {
		if (subject.isEmpty()
			|| catalysts.stream().anyMatch(EmiIngredient::isEmpty)
			|| outputs.stream().anyMatch(output -> output.stack().isEmpty())) {
			return;
		}
		ProjectFlintLootRecipe recipe = new ProjectFlintLootRecipe(
			category,
			pfId("pf_emi/" + id),
			title,
			subject,
			subjectIsInput,
			catalysts,
			outputs,
			notes
		);
		registry.addRecipe(recipe);
		ProjectFlintRecipeIndex.mark(recipe);
		if (!subjectIsInput) {
			// Visual-only subjects such as spawn eggs intentionally remain hidden.
		}
	}

	private static String pretty(String id) {
		StringBuilder result = new StringBuilder();
		for (String part : id.split("_")) {
			if (!result.isEmpty()) result.append(' ');
			if (!part.isEmpty()) {
				result.append(Character.toUpperCase(part.charAt(0)));
				if (part.length() > 1) result.append(part.substring(1));
			}
		}
		return result.toString();
	}

	/** Exact ProjectFlint RecipeParchmentItem.boundStack implementation (CUSTOM_DATA
	 * tag ProjectFlintRecipe, plus the unfinished-bacon model selector). */
	private static EmiStack maxDamageItem(String path, int maximum) {
		Identifier id = id("minecraft", path);
		Item item = Registries.ITEM.get(id);
		if (item == null || item == Items.AIR || !id.equals(Registries.ITEM.getId(item))) return EmiStack.EMPTY;
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponentTypes.MAX_DAMAGE, maximum);
		stack.set(DataComponentTypes.DAMAGE, 0);
		return EmiStack.of(stack);
	}

	private static EmiStack boundParchment(String id) {
		if (!hasPfItem("recipe_parchment")) return EmiStack.EMPTY;
		ItemStack stack = new ItemStack(Registries.ITEM.get(pfId("recipe_parchment")));
		NbtCompound data = new NbtCompound();
		data.putString("ProjectFlintRecipe", "projectflint:" + id);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
		if (id.equals("bacon_and_eggs_unfinished")) {
			stack.set(DataComponentTypes.CUSTOM_MODEL_DATA,
				new CustomModelDataComponent(List.of(), List.of(), List.of("incomplete"), List.of()));
		}
		return EmiStack.of(stack);
	}

	private static void stoveRecipe(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String id,
		List<EmiIngredient> inputs,
		EmiIngredient parchment,
		EmiStack output
	) {
		// The server checks the bound recipe id, not merely the parchment item type.
		String recipeId = id.equals("fat_efficiency") ? "fat" : id;
		process(registry, category, "stove/" + id, inputs,
			List.of(boundParchment(recipeId)), List.of(output), 20 * 15);
	}

	private static void heat(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String prefix,
		String id,
		EmiIngredient input,
		EmiStack output,
		int baseTicks,
		float speed
	) {
		int ticks = baseTicks <= 0 ? 0 : (int) Math.ceil(baseTicks / speed);
		process(registry, category, "heat/" + prefix + "/" + id,
			List.of(input), List.of(), List.of(output), ticks);
	}

	private static void logSlabPair(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String prefix,
		String id,
		String log,
		String slab
	) {
		simple(registry, category, prefix + "/" + id + "_slab", List.of(mc(log)), mc(slab, 2));
	}

	private static EmiRecipeCategory category(
		EmiRegistry registry,
		String path,
		String workstationPath
	) {
		EmiStack workstation = pf(workstationPath);
		if (workstation.isEmpty()) {
			return null;
		}
		EmiRecipeCategory category = new EmiRecipeCategory(pfId(path), workstation);
		registry.addCategory(category);
		registry.addWorkstation(category, workstation);
		ProjectFlintRecipeIndex.mark(workstation);
		return category;
	}

	/** Informational category (crops, block events): icon is not a crafting station. */
	private static EmiRecipeCategory categoryIcon(EmiRegistry registry, String path, String iconPath) {
		EmiStack icon = pf(iconPath);
		if (icon.isEmpty()) return null;
		EmiRecipeCategory category = new EmiRecipeCategory(pfId(path), icon);
		registry.addCategory(category);
		ProjectFlintRecipeIndex.mark(icon);
		return category;
	}

	private static void simple(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String id,
		List<EmiIngredient> inputs,
		EmiStack output
	) {
		process(registry, category, id, inputs, List.of(), List.of(output), 0);
	}

	private static void process(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String path,
		List<? extends EmiIngredient> inputs,
		List<? extends EmiIngredient> catalysts,
		List<EmiStack> outputs,
		int ticks
	) {
		List<EmiIngredient> safeInputs = new ArrayList<>(inputs);
		List<EmiIngredient> safeCatalysts = new ArrayList<>(catalysts);
		if (safeInputs.stream().anyMatch(EmiIngredient::isEmpty)
			|| safeCatalysts.stream().anyMatch(EmiIngredient::isEmpty)
			|| outputs.isEmpty()
			|| outputs.stream().anyMatch(EmiStack::isEmpty)) {
			return;
		}
		ProjectFlintProcessRecipe recipe = new ProjectFlintProcessRecipe(
			category, pfId("pf_emi/" + path), safeInputs, safeCatalysts, outputs, ticks
		);
		registry.addRecipe(recipe);
		ProjectFlintRecipeIndex.mark(recipe);
	}

	private static void worldTimed(
		EmiRegistry registry,
		String path,
		List<EmiIngredient> consumed,
		List<EmiIngredient> catalysts,
		EmiStack output,
		int ticks
	) {
		// WorldInteractionRecipe itself has no timer widget, but keeping this method
		// separate documents that the physical process is timed in gameplay.
		world(registry, path, consumed, catalysts, output);
	}

	private static void world(
		EmiRegistry registry,
		String path,
		List<EmiIngredient> consumed,
		List<EmiIngredient> catalysts,
		EmiStack output
	) {
		if (consumed.isEmpty() || output.isEmpty()
			|| consumed.stream().anyMatch(EmiIngredient::isEmpty)
			|| catalysts.stream().anyMatch(EmiIngredient::isEmpty)) {
			return;
		}

		EmiWorldInteractionRecipe.Builder builder = EmiWorldInteractionRecipe.builder()
			.id(pfId("pf_emi/world/" + path));

		if (!catalysts.isEmpty()) {
			for (EmiIngredient input : consumed) builder.leftInput(input);
			for (EmiIngredient catalyst : catalysts) builder.rightInput(catalyst, true);
		} else if (consumed.size() == 1) {
			// Builder requires both sides. A one-input physical transform is represented
			// with its own placed/held stack as a catalyst only when no better tool exists.
			builder.leftInput(consumed.getFirst());
			builder.rightInput(consumed.getFirst().copy(), true);
		} else {
			for (int i = 0; i < consumed.size() - 1; i++) builder.leftInput(consumed.get(i));
			builder.rightInput(consumed.getLast(), false);
		}
		builder.output(output);

		try {
			EmiWorldInteractionRecipe recipe = builder.build();
			registry.addRecipe(recipe);
			ProjectFlintRecipeIndex.mark(recipe);
		} catch (IllegalStateException ignored) {
		}
	}

	private static EmiIngredient anyPf(long amount, String... paths) {
		List<EmiStack> stacks = new ArrayList<>();
		for (String path : paths) {
			EmiStack stack = pf(path);
			if (!stack.isEmpty()) stacks.add(stack);
		}
		return EmiIngredient.of(stacks, amount);
	}

	private static EmiIngredient anyMc(long amount, String... paths) {
		List<EmiStack> stacks = new ArrayList<>();
		for (String path : paths) {
			EmiStack stack = mc(path);
			if (!stack.isEmpty()) stacks.add(stack);
		}
		return EmiIngredient.of(stacks, amount);
	}

	private static EmiIngredient any(long amount, EmiIngredient... ingredients) {
		return EmiIngredient.of(List.of(ingredients), amount);
	}

	private static boolean hasPfItem(String path) {
		Identifier wanted = pfId(path);
		Item item = Registries.ITEM.get(wanted);
		return item != null && item != Items.AIR && wanted.equals(Registries.ITEM.getId(item));
	}

	private static EmiStack pf(String path) {
		return pf(path, 1);
	}

	private static EmiStack pf(String path, long amount) {
		return stack(PF, path, amount);
	}

	private static EmiStack mc(String path) {
		return mc(path, 1);
	}

	private static EmiStack mc(String path, long amount) {
		return stack("minecraft", path, amount);
	}

	private static EmiStack stack(String namespace, String path, long amount) {
		Identifier wanted = id(namespace, path);
		Item item = Registries.ITEM.get(wanted);
		if (item == null || item == Items.AIR || !wanted.equals(Registries.ITEM.getId(item))) {
			return EmiStack.EMPTY;
		}
		EmiStack stack = EmiStack.of(item, amount);
		ProjectFlintRecipeIndex.mark(stack);
		return stack;
	}

	private static Identifier pfId(String path) {
		return id(PF, path);
	}

	private static Identifier id(String namespace, String path) {
		return EmiPort.id(namespace, path);
	}
}
