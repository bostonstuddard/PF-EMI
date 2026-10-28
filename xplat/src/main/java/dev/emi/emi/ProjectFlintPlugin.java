package dev.emi.emi;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.FluidUnit;
import dev.emi.emi.recipe.ProjectFlintProcessRecipe;
import dev.emi.emi.recipe.ProjectFlintLootRecipe;
import net.minecraft.fluid.Fluids;
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
	public static final String PF_EMI_PATCH_LEVEL = "pf.11";

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

	@Override
	public void register(EmiRegistry registry) {
		if (!hasPfItem("rock")) {
			return;
		}

		ProjectFlintRecipeIndex.reset();

		/*
		 * ProjectFlint progression is intentionally not the vanilla crafting catalog.
		 * Keep only recipes explicitly supplied by PF-EMI. World-interaction entries
		 * survive because every recipe created below has a projectflint:* id.
		 */
		registry.removeRecipes(recipe -> {
			Identifier id = recipe.getId();
			return id == null || !PF.equals(id.getNamespace());
		});

		registerWorldInteractions(registry);
		registerStationRecipes(registry);
		registerLumberProcessing(registry);
		registerWell(registry);
		registerHeatProcessing(registry);
		registerAdvancedKiln(registry);
		registerMissingWorldRoutes(registry);
		registerModernProjectFlintRoutes(registry);
		registerBlockDropRecipes(registry);
		registerMobLootRecipes(registry);

		// Register structure guides from the SAME entrypoint so they cannot vanish
		// because a second EMI plugin was omitted/reordered.
		ProjectFlintMultiblockPlugin.registerAll(registry);

		// Last: make the item index match the recipes/routes that are actually visible.
		registry.removeEmiStacks(ProjectFlintRecipeIndex::shouldHide);
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

		if (clay != null) {
			// Current ClayGrinderRecipeGuideRegistry.
			simple(registry, clay, "clay/bone_club", List.of(pf("filed_bone", 4)), pf("bone_club"));
			simple(registry, clay, "clay/bone_helmet", List.of(pf("filed_bone", 5)), pf("bone_helmet"));
			simple(registry, clay, "clay/bone_chestplate", List.of(pf("filed_bone", 8)), pf("bone_chestplate"));
			simple(registry, clay, "clay/bone_leggings", List.of(pf("filed_bone", 7)), pf("bone_leggings"));
			simple(registry, clay, "clay/bone_boots", List.of(pf("filed_bone", 4)), pf("bone_boots"));
			simple(registry, clay, "clay/clay_mortar", List.of(pf("super_heated_clay", 9)), pf("clay_mortar"));
			simple(registry, clay, "clay/flint_chips", List.of(mc("flint")), pf("flint_chip", 4));
			simple(registry, clay, "clay/flint_from_chips", List.of(pf("flint_chip", 4)), mc("flint"));
			simple(registry, clay, "clay/grinded_wood", List.of(pf("sanded_wood_slab", 2)), pf("grinded_wood_slab"));
			simple(registry, clay, "clay/sanded_reverse", List.of(pf("grinded_wood_slab")), pf("sanded_wood_slab", 2));
			simple(registry, clay, "clay/stone_grindsaw", List.of(pf("crushed_stone", 8)), pf("stone_grindsaw"));
			simple(registry, clay, "clay/crushed_stone", List.of(pf("very_refined_stone", 8)), pf("crushed_stone"));
			simple(registry, clay, "clay/crushed_slab", List.of(pf("crushed_stone")), pf("crushed_stone_slab", 2));
			simple(registry, clay, "clay/crushed_block", List.of(pf("crushed_stone_slab", 2)), pf("crushed_stone"));
			simple(registry, clay, "clay/mortar_slab", List.of(pf("mortar_bricks")), pf("mortar_brick_slab", 2));
			simple(registry, clay, "clay/mortar_block", List.of(pf("mortar_brick_slab", 2)), pf("mortar_bricks"));
			simple(registry, clay, "clay/mortar_kiln_base", List.of(pf("mortar_bricks", 32)), pf("mortar_kiln_base"));
			simple(registry, clay, "clay/sanding_paper", List.of(pf("sugarcane_roll", 4)), pf("sanding_paper"));
			simple(registry, clay, "clay/twig_to_branch", List.of(pf("twig", 4)), pf("branch"));
			simple(registry, clay, "clay/branch_to_twigs", List.of(pf("branch")), pf("twig", 2));
			simple(registry, clay, "clay/unfired_kiln_glass", List.of(pf("glass_shard", 4)), pf("unfired_kiln_glass"));
			simple(registry, clay, "clay/floor_tile", List.of(pf("wattled_daub_bricks")), pf("wattled_daub_floor_tile", 2));
			simple(registry, clay, "clay/wooden_grinder_base", List.of(pf("woven_wood", 12)), pf("wooden_grinder_base"));
			simple(registry, clay, "clay/woven_wood", List.of(pf("woven_wood_slab", 2)), pf("woven_wood"));
			simple(registry, clay, "clay/woven_slab", List.of(pf("woven_wood")), pf("woven_wood_slab", 2));
			simple(registry, clay, "clay/loose_mud_slab", List.of(pf("loose_mud_bricks")), pf("loose_mud_brick_slab", 2));
			simple(registry, clay, "clay/loose_mud_block", List.of(pf("loose_mud_brick_slab", 2)), pf("loose_mud_bricks"));
			simple(registry, clay, "clay/crude_clay_slab", List.of(pf("crude_clay_block")), pf("crude_clay_slab", 2));
			simple(registry, clay, "clay/crude_clay_block", List.of(pf("crude_clay_slab", 2)), pf("crude_clay_block"));
			simple(registry, clay, "clay/fired_clay", List.of(pf("crude_clay_block")), pf("fired_clay", 4));
			simple(registry, clay, "clay/burnt_branch_block", List.of(pf("burnt_branch_slab", 2)), pf("burnt_branch_block"));
			simple(registry, clay, "clay/burnt_branch_slabs", List.of(pf("burnt_branch_block")), pf("burnt_branch_slab", 2));
			simple(registry, clay, "clay/burnt_branches", List.of(pf("burnt_branch_slab")), pf("burnt_branch", 8));

			for (String mold : List.of("axe", "block", "brick", "cutters", "multitool", "pot")) {
				simple(registry, clay, "clay/unfired_" + mold + "_mold",
					List.of(pf("unfired_clay_mold_base")), pf("unfired_clay_" + mold + "_mold"));
			}

			logSlabPair(registry, clay, "oak", "stripped_oak_log", "oak_slab");
			logSlabPair(registry, clay, "spruce", "stripped_spruce_log", "spruce_slab");
			logSlabPair(registry, clay, "birch", "stripped_birch_log", "birch_slab");
			logSlabPair(registry, clay, "jungle", "stripped_jungle_log", "jungle_slab");
			logSlabPair(registry, clay, "acacia", "stripped_acacia_log", "acacia_slab");
			logSlabPair(registry, clay, "dark_oak", "stripped_dark_oak_log", "dark_oak_slab");
			logSlabPair(registry, clay, "mangrove", "stripped_mangrove_log", "mangrove_slab");
			logSlabPair(registry, clay, "cherry", "stripped_cherry_log", "cherry_slab");
			logSlabPair(registry, clay, "pale_oak", "stripped_pale_oak_log", "pale_oak_slab");
			simple(registry, clay, "clay/bamboo_slab", List.of(mc("stripped_bamboo_block")), mc("bamboo_slab", 2));
			simple(registry, clay, "clay/bamboo_block", List.of(mc("bamboo_slab", 2)), mc("stripped_bamboo_block"));
		}

		if (wood != null) {
			EmiIngredient flintFile = pf("flint_file");
			EmiIngredient boneKnife = pf("bone_knife");

			simple(registry, wood, "wood/ground_leather",
				List.of(any(2, pf("raw_leather"), mc("leather")), pf("sanding_paper")), pf("ground_leather"));
			simple(registry, wood, "wood/woven_chest_frame",
				List.of(mc("leather", 3), pf("woven_wood", 6)), pf("woven_chest_frame"));
			simple(registry, wood, "wood/leather_backpack",
				List.of(mc("leather", 4), pf("reed_backpack")), pf("leather_backpack"));
			simple(registry, wood, "wood/backpack_chest",
				List.of(pf("leather_backpack"), pf("wooden_chest", 2)), pf("leather_backpack_with_chest"));
			simple(registry, wood, "wood/wattle_window",
				List.of(pf("wattled_daub_bricks", 2), pf("advanced_kiln_glass")), pf("wattled_daub_window"));
			simple(registry, wood, "wood/woven_fan",
				List.of(pf("sugarcane_roll"), pf("plant_tool_binding")), pf("woven_fan", 2));
			simple(registry, wood, "wood/sanding_paper",
				List.of(pf("sugarcane_roll", 3), pf("plant_tool_binding")), pf("sanding_paper", 2));
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
				List.of(pf("woven_wood", 16), pf("clay_pot")), pf("water_bearing_pot"));
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
				List.of(pf("fired_clay", 4), pf("mud_pile", 16)), pf("loose_mud_bricks"));
			simple(registry, wood, "wood/bricks",
				List.of(pf("loose_mud_bricks", 4), pf("wooden_screw", 2)), mc("bricks"));
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
			process(registry, wood, "wood/unfired_clay_bucket_bone_knife",
				List.of(mc("clay_ball", 3)), List.of(boneKnife), List.of(pf("unfired_clay_bucket")), 0);
			simple(registry, wood, "wood/wheat_seeds",
				List.of(pf("flax_seeds", 9), pf("clay_water_bucket")), mc("wheat_seeds", 3));
			simple(registry, wood, "wood/flour",
				List.of(mc("wheat", 9), pf("clay_water_bucket")), pf("flour", 9));
			simple(registry, wood, "wood/fat",
				List.of(rawMeats(1), pf("clay_water_bucket")), pf("fat"));
			simple(registry, wood, "wood/milk_cups",
				List.of(pf("wooden_cup", 2), pf("clay_milk_bucket")), pf("wooden_cup_raw_milk", 4));
			simple(registry, wood, "wood/utility_vest",
				List.of(pf("tanned_leather_chestplate"), pf("leather_backpack_with_chest")), pf("tanned_utility_vest"));
			simple(registry, wood, "wood/vanilla_bricks",
				List.of(mc("brick", 2), mc("brick", 2)), mc("bricks"));
			simple(registry, wood, "wood/wattled_daub_from_bricks",
				List.of(mc("brick", 2)), pf("wattled_daub_bricks", 12));

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
		}

		if (shelf != null) {
			simple(registry, shelf, "bookshelf/book",
				List.of(pf("sugarcane_roll", 8), mc("leather", 2)), mc("book"));
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
					List.of(pf("bookshelf"), mc("book", 4)), List.of(pf("recipe_parchment")), 0);
			}
			process(registry, shelf, "bookshelf/research/pumpkin_cake",
				List.of(pf("pumpkin_slice", 16), pf("recipe_parchment")),
				List.of(pf("bookshelf")), List.of(pf("recipe_parchment")), 0);
			process(registry, shelf, "bookshelf/research/pumpkin_pie",
				List.of(pf("pumpkin_slice", 16), pf("recipe_parchment")),
				List.of(pf("bookshelf")), List.of(pf("recipe_parchment")), 0);
			process(registry, shelf, "bookshelf/research/butter",
				List.of(pf("wooden_cup_raw_milk", 4), pf("recipe_parchment")),
				List.of(pf("bookshelf")), List.of(pf("recipe_parchment")), 0);

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
					List.of(pf("bookshelf"), mc("book", 4)), List.of(pf("recipe_parchment")), 0);
			}
			process(registry, shelf, "bookshelf/research/modern/cheese",
				List.of(pf("wooden_cup_raw_milk", 4), pf("recipe_parchment")),
				List.of(pf("bookshelf")), List.of(pf("recipe_parchment")), 0);
			process(registry, shelf, "bookshelf/research/bone_broth",
				List.of(pf("filed_bone", 6), pf("recipe_parchment")),
				List.of(pf("bookshelf")), List.of(pf("recipe_parchment")), 0);
		}

		if (stove != null) {
			EmiIngredient parchment = pf("recipe_parchment");
			stoveRecipe(registry, stove, "sugar", List.of(mc("sugar_cane", 4)), parchment, mc("sugar"));
			stoveRecipe(registry, stove, "sweet_berry_jam",
				List.of(mc("sweet_berries", 8), mc("sugar"), pf("wooden_cup", 2)), parchment, pf("sweet_berry_jam", 2));
			stoveRecipe(registry, stove, "sweet_berry_pie",
				List.of(pf("sweet_berry_jam", 2), mc("sugar"), pf("butter")), parchment, pf("sweet_berry_pie", 2));
			stoveRecipe(registry, stove, "pumpkin_pie",
				List.of(pf("pumpkin_slice", 8), mc("sugar", 2)), parchment, mc("pumpkin_pie", 2));
			stoveRecipe(registry, stove, "melon_pie",
				List.of(mc("melon_slice", 8), mc("sugar", 2)), parchment, pf("melon_pie", 2));
			stoveRecipe(registry, stove, "mutton_pie",
				List.of(mc("cooked_mutton", 8), mc("sugar", 2)), parchment, pf("mutton_pie", 2));
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
				List.of(pf("strawberry", 12), mc("sugar"), pf("wooden_cup", 2)), parchment, pf("strawberry_jam", 2));
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
			List.of(pf("wooden_board", 2)), 0);
		process(registry, lumber, "lumber/beams",
			List.of(strippedLog.copy(), water.copy()),
			List.of(pf("lumber_processor"), pf("flint_plant_cutters")),
			List.of(pf("wooden_beam")), 0);
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

		if (campfire != null) registerCommonHeat(registry, campfire, "campfire", 0.90f, false);
		if (baked != null) {
			registerCommonHeat(registry, baked, "baked", 1.0f, true);
			registerKilnOnly(registry, baked, "baked", 1.0f);
		}
		if (mortar != null) {
			registerCommonHeat(registry, mortar, "mortar", 1.25f, true);
			registerKilnOnly(registry, mortar, "mortar", 1.25f);
		}
		if (furnace != null) {
			registerCommonHeat(registry, furnace, "brick_furnace", 1.50f, true);
			registerKilnOnly(registry, furnace, "brick_furnace", 1.50f);
		}
		if (blast != null) {
			registerCommonHeat(registry, blast, "brick_blast_furnace", 1.50f, true);
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
		float speed,
		boolean includeDirectBurns
	) {
		if (includeDirectBurns) {
			heat(registry, category, prefix, "burnt_twig", pf("twig"), pf("burnt_twig"), 0, speed);
			heat(registry, category, prefix, "burnt_branch", pf("branch"), pf("burnt_branch"), 0, speed);
		}
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
		heat(registry, category, prefix, "unfired_brick", pf("unfired_brick"), mc("brick"), 300, speed);
		heat(registry, category, prefix, "fireclay_brick", pf("unfired_fireclay_brick"), pf("fireclay_brick"), 300, speed);

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
		inputs.add(mc("flint", 8));
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
	}

	/** World processes introduced after the original PF-EMI recipe snapshot. */
	private static void registerModernProjectFlintRoutes(EmiRegistry registry) {
		EmiRecipeCategory placement = category(registry, "recent_world_processing", "wood_dust");
		if (placement != null) {
			info(registry, placement, "placement/strawberry_seeds", "Plant Strawberry Seeds",
				pf("strawberry_seeds"), true, List.of(mc("grass_block")), List.of(),
				List.of("Places a Strawberry Plant on supported ground.", "Growth stages 0-3; mature fruit requires cutters."));
			info(registry, placement, "placement/strawberry_harvest", "Strawberry Plant Harvest",
				pf("strawberry"), true, List.of(anyPf(1,"crude_harvesting_tool","flint_plant_cutters","advanced_flint_multitool")),
				List.of(loot(pf("strawberry", 2), "1-2 berries from mature plant; resets to growth stage 1")),
				List.of("Plant naturally generates in forest, birch, flower forest and meadow habitats.", "Mature plants require a plant-cutting tool."));
			process(registry, placement, "basin/tannin",
				List.of(anyMc(1, "stripped_oak_log", "stripped_oak_wood", "stripped_birch_log", "stripped_birch_wood")),
				List.of(pf("basin")), List.of(pf("tannin", 2)), 0);
			process(registry, placement, "basin/wood_dust",
				List.of(anyMc(1, "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks",
					"dark_oak_planks", "mangrove_planks", "cherry_planks", "pale_oak_planks", "bamboo_planks",
					"crimson_planks", "warped_planks")), List.of(pf("basin")), List.of(pf("wood_dust")), 0);
			process(registry, placement, "forge/wood_to_charcoal",
				List.of(anyMc(4, "oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log",
					"dark_oak_log", "mangrove_log", "cherry_log", "pale_oak_log")),
				List.of(pf("fireclay_forge_controller"), anyPf(1,"flint_firestarter","bone_firestarter","wood_firestarter")),
				List.of(mc("charcoal", 4)), 20 * 60);
			process(registry, placement, "forge/hardwood_alloy",
				List.of(pf("hardwood"), mc("charcoal")),
				List.of(pf("fireclay_forge_controller"), anyPf(1,"flint_firestarter","bone_firestarter","wood_firestarter")),
				List.of(pf("hardwood_alloy")), 20 * 60 * 5);
			info(registry, placement, "torch/fat_burning_torch", "Fat-Burning Torch Refueling",
				pf("fat_burning_torch"), true, List.of(pf("fat")), List.of(),
				List.of("Requires Fat charges for lighting and relighting.", "Supports floor and wall mounting; burns for up to 30 minutes."));
		}
		// Hardwood Crafting Table has its own delayed 3x3 crafting grid; do not pretend
		// its recipes are identical to regular vanilla crafting-table behavior.
		EmiRecipeCategory hardwood = category(registry, "hardwood_crafting", "hardwood_crafting_table");
		if (hardwood != null) {
			process(registry, hardwood, "hardwood/material",
				List.of(pf("wood_dust", 2), pf("tannin"), pf("clay_water_bucket")),
				List.of(), List.of(pf("hardwood")), 0);
			process(registry, hardwood, "hardwood/table_assembly",
				List.of(pf("hardwood_alloy", 2), mc("cobblestone")), List.of(pf("lumber_processor")),
				List.of(pf("hardwood_crafting_table")), 0);
			// The exact shaped recipes for the Wooden/Hardwood tool set are native
			// ProjectFlint recipe-manager entries, retained by the namespace filter.
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

		// Additional later-game route: coal remains normal coal from Wooden Pickaxe mining.
		blockDrop(registry, drops, "coal_ore_wooden_pickaxe",
			mc("coal_ore"), List.of(pf("wooden_pickaxe")),
			List.of(loot(mc("coal"), "Normal Coal; charcoal-based lighting remains a separate unlock")),
			List.of("Wooden Pickaxe is permitted on ordinary Coal Ore in v0.31.45-b."));

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
			loot(pf("raw_leather", 2), "Replaces vanilla Leather"),
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
			loot(pf("animal_bones", 3), "2-3 Animal Bones")
		));
		mob(registry, mobs, "brown_bear", pf("brown_bear_spawn_egg"), List.of(
			loot(pf("raw_bear_meat", 2), "1-2; cooked if killed on fire"),
			loot(pf("animal_bones", 3), "2-3 Animal Bones")
		));
		mob(registry, mobs, "turtle", mc("turtle_spawn_egg"), List.of(
			loot(pf("raw_turtle_meat"), "Custom turtle meat; count and conditions require drop-system audit")
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
			List.of("Shows ProjectFlint-modified/default no-Looting ranges.", "Baby Ageable mobs do not receive ProjectFlint's added meat."));
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

	private static void stoveRecipe(
		EmiRegistry registry,
		EmiRecipeCategory category,
		String id,
		List<EmiIngredient> inputs,
		EmiIngredient parchment,
		EmiStack output
	) {
		process(registry, category, "stove/" + id, inputs, List.of(parchment), List.of(output), 20 * 15);
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
		String id,
		String log,
		String slab
	) {
		simple(registry, category, "clay/" + id + "_slab", List.of(mc(log)), mc(slab, 2));
		simple(registry, category, "clay/" + id + "_log", List.of(mc(slab, 2)), mc(log));
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
