package dev.emi.emi;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.Cell;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.Layer;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.Material;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.Output;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.StackRef;
import dev.emi.emi.recipe.ProjectFlintMultiblockRecipe;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Structure-guide helper called directly by ProjectFlintPlugin.
 *
 * This intentionally is NOT a second EMI entrypoint anymore.
 */
public final class ProjectFlintMultiblockPlugin {
	public static final String PF_EMI_MULTIBLOCK_PATCH = "pf.12";

	private ProjectFlintMultiblockPlugin() {
	}

	public static void registerAll(EmiRegistry registry) {
		StackRef iconRef = pf("Lumber Processor", "lumber_processor");
		EmiStack icon = iconRef.resolveExact(1);
		if (icon.isEmpty()) {
			icon = pf("Mortar Bricks", "mortar_bricks").resolveDisplay(1);
		}

		EmiRecipeCategory category = new EmiRecipeCategory(
			EmiPort.id("projectflint", "multiblock"),
			icon
		);
		registry.addCategory(category);
		ProjectFlintRecipeIndex.mark(icon);

		for (ProjectFlintMultiblockDefinition definition : definitions()) {
			ProjectFlintMultiblockRecipe recipe = new ProjectFlintMultiblockRecipe(category, definition);
			registry.addRecipe(recipe);
			ProjectFlintRecipeIndex.mark(recipe);

			EmiStack workstation = definition.representative() == null
				? EmiStack.EMPTY : definition.representative().resolveExact(1);
			if (!workstation.isEmpty()) {
				registry.addWorkstation(category, workstation);
				ProjectFlintRecipeIndex.mark(workstation);
			}
		}
	}

	private static List<ProjectFlintMultiblockDefinition> definitions() {
		return List.of(
			lumberProcessor(),
			well(),
			primitiveCampfire(),
			bakedKiln(),
			mortarKiln(),
			advancedKiln(),
			fireclayForge()
		);
	}

	private static ProjectFlintMultiblockDefinition lumberProcessor() {
		StackRef woven = pf("Woven Wood", "woven_wood");
		StackRef pot = pf("Water Pot", "water_pot");
		StackRef stone = pf("Crushed Stone", "crushed_stone");
		StackRef bound = pf("Bound Wood Processor", "bound_wood_processor");
		StackRef processor = pf("Lumber Processor", "lumber_processor");

		return definition(
			"lumber_processor",
			"Lumber Processor",
			"3x1x2 + support",
			List.of(
				layer("Support", 3, 1,
					support("Any sturdy support"), support("Any sturdy support"), support("Any sturdy support")),
				layer("Machine", 3, 1,
					controller(woven, "Left: form here with Bound Wood Processor"),
					block(pot, "Middle: Water Pot"),
					block(stone, "Right: Crushed Stone")),
				layer("Working space", 3, 1,
					air("Stripped log goes here"),
					air("Water Pot access"),
					air("Finishing work space"))
			),
			List.of(
				material(woven, 1, "Left position"),
				material(pot, 1, "Middle position"),
				material(stone, 1, "Right position"),
				catalyst(bound, 1, "Used on Woven Wood to form")
			),
			List.of(),
			processor,
			List.of(
				"Woven Wood -> Water Pot -> Crushed Stone.",
				"Every base block requires sturdy support below and clear air above.",
				"The formed processor consumes one full bucket of water per log."
			)
		);
	}

	private static ProjectFlintMultiblockDefinition well() {
		StackRef bricks = ProjectFlintMultiblockDefinition.vanilla("Vanilla Bricks", "bricks");
		StackRef shingles = pf("Brick Shingles", "brick_shingles");
		StackRef rope = pf("Rope", "rope");
		StackRef pulley = pf("Well Pulley", "well_pulley");
		StackRef beam = anyPf("Any Wooden Beam",
			"wooden_beam", "acacia_wooden_beam", "birch_wooden_beam", "cherry_wooden_beam",
			"crimson_wooden_beam", "dark_oak_wooden_beam", "jungle_wooden_beam",
			"mangrove_wooden_beam", "pale_oak_wooden_beam", "spruce_wooden_beam", "warped_wooden_beam");

		Cell b = block(bricks, "Vanilla Brick");
		Cell h = air("Hollow shaft center: air/water/replaceable");
		Cell e = Cell.empty();
		Cell s = block(shingles, "Brick Shingle");
		Cell w = block(beam, "Any supported Wooden Beam variant");
		Cell r = block(rope, "Structural Rope");
		Cell p = controller(pulley, "Well Pulley / controller");

		return definition(
			"well",
			"Well",
			"3x3x8",
			List.of(
				// Y0: SOLID 3x3 floor = 9
				layer("Brick floor", 3, 3, b,b,b, b,b,b, b,b,b),
				// Y1-Y3: hollow shell = 8 each
				layer("Shaft 1/3", 3, 3, b,b,b, b,h,b, b,b,b),
				layer("Shaft 2/3", 3, 3, b,b,b, b,h,b, b,b,b),
				layer("Shaft 3/3", 3, 3, b,b,b, b,h,b, b,b,b),
				// Current compact frame from WellMultiblock.java.
				layer("Lower frame", 3, 3,
					s,e,s,
					w,r,w,
					s,e,s),
				layer("Middle frame", 3, 3,
					e,e,e,
					w,r,w,
					e,e,e),
				layer("Pulley frame", 3, 3,
					e,e,e,
					w,p,w,
					e,e,e),
				layer("Roof", 3, 3,
					e,e,e,
					s,s,s,
					e,e,e)
			),
			List.of(
				material(bricks, 33, "9 solid floor + 3 hollow rings of 8"),
				material(shingles, 7, "4 lower corners + 3 roof"),
				material(beam, 6, "Any accepted wooden beam variants"),
				material(rope, 2, "Structural rope blocks"),
				material(pulley, 1, "Controller")
			),
			List.of(
				output(EmiStack.of(Fluids.WATER, dev.emi.emi.config.FluidUnit.BUCKET * 3L), "3 source-water cells", "Refilled each completed cycle"),
				output(mc("clay_ball", 2), "0-2 Clay Balls", "Two quality-based rolls"),
				output(pfStack("flint_chip", 3), "2-3 Flint Chips", "Unless flint-units reach 4"),
				output(mc("flint"), "1 Flint", "Replaces chips when flint-units reach 4")
			),
			pulley,
			List.of(
				"Must be exactly centered on the virtual aquifer center.",
				"Foundation total is 33 vanilla Bricks: 9 + 8 + 8 + 8.",
				"Structural frame is 7 Brick Shingles + 6 Wooden Beams + 2 Rope + 1 Well Pulley.",
				"GUI Rope is optional and boosts quality; it is separate from the 2 structural Rope blocks.",
				"Base cycle work is 60 seconds at quality 100."
			)
		);
	}

	private static ProjectFlintMultiblockDefinition primitiveCampfire() {
		StackRef stone = pf("Campfire Stone", "campfire_stone");
		StackRef twig = pf("Lightly Burnt Twig", "lightly_burnt_twig");
		return definition(
			"primitive_campfire",
			"Permanent Campfire",
			"1 block assembly",
			List.of(layer("Campfire", 1, 1, controller(stone, "Pile Campfire Stones here"))),
			List.of(
				material(stone, 12, "Campfire Stone capacity"),
				material(twig, 12, "Lightly Burnt Twig fuel/frame")
			),
			List.of(),
			stone,
			List.of(
				"Build the permanent campfire from 12 Campfire Stones and 12 Lightly Burnt Twigs.",
				"The completed frame is relightable and uses Permanent Campfire heat recipes."
			)
		);
	}

	private static ProjectFlintMultiblockDefinition bakedKiln() {
		StackRef base = pf("Baked Clay Kiln", "baked_clay_kiln");
		StackRef top = pf("Baked Kiln Top", "baked_kiln_top");
		return definition(
			"baked_kiln",
			"Baked Clay Kiln",
			"1x1x2",
			List.of(
				layer("Base", 1, 1, controller(base, "Processing base")),
				layer("Top", 1, 1, block(top, "Required directly above base"))
			),
			List.of(material(base, 1, "Base"), material(top, 1, "Top")),
			List.of(),
			base,
			List.of("The top must remain directly above the Baked Clay Kiln base.")
		);
	}

	private static ProjectFlintMultiblockDefinition mortarKiln() {
		StackRef base = pf("Mortar Kiln Base", "mortar_kiln_base");
		StackRef glass = pf("Advanced Kiln Glass", "advanced_kiln_glass");
		StackRef kiln = pf("Mortar Kiln", "mortar_kiln");
		return definition(
			"mortar_kiln",
			"Mortar Kiln",
			"1x1 upgrade",
			List.of(layer("Base", 1, 1, controller(base, "Use Advanced Kiln Glass here"))),
			List.of(material(base, 1, "Placed base"), catalyst(glass, 1, "Forms the Mortar Kiln")),
			List.of(),
			kiln,
			List.of("Right-click the Mortar Kiln Base with Advanced Kiln Glass.")
		);
	}

	private static ProjectFlintMultiblockDefinition advancedKiln() {
		StackRef bricks = pf("Mortar Bricks", "mortar_bricks");
		StackRef glass = pf("Advanced Kiln Glass", "advanced_kiln_glass");
		Cell b = block(bricks, "Mortar Brick structure position");
		Cell c = controller(bricks, "Controller corner after formation");

		return definition(
			"advanced_kiln",
			"Advanced Kiln Structure",
			"2x2x2",
			List.of(
				layer("Bottom", 2, 2, c,b, b,b),
				layer("Top", 2, 2, b,b, b,b)
			),
			List.of(
				material(bricks, 8, "Complete 2x2x2 cube"),
				catalyst(glass, 1, "Use on a horizontal face to form")
			),
			List.of(),
			bricks,
			List.of(
				"Build a complete 2x2x2 cube of 8 Mortar Bricks.",
				"Use Advanced Kiln Glass on a horizontal face to form the controller/casings.",
				"Processing recipes are shown separately under Advanced Kiln."
			)
		);
	}

	/** Six-layer Fireclay Forge, validated against FireclayForgeMultiblock.java at v0.31.48-b. */
	private static ProjectFlintMultiblockDefinition fireclayForge() {
		StackRef brick = pf("Fireclay Brick Block", "fireclay_brick_block");
		StackRef stairs = pf("Fireclay Brick Stairs", "fireclay_brick_stairs");
		StackRef slab = pf("Fireclay Brick Slab", "fireclay_brick_slab");
		StackRef controller = pf("Fireclay Forge Controller", "fireclay_forge_controller");
		Cell b = block(brick, "Fireclay brick casing");
		Cell a = air("Internal open chimney");
		Cell e = Cell.empty();
		Cell s = block(stairs, "Fireclay stair corner");
		Cell h = block(slab, "Single fireclay slab cap (not a double slab)");
		Cell c = controller(controller, "Controller: front centre, facing outward");
		return definition(
			"fireclay_forge", "Fireclay Forge", "3x3x6 (+ open chimney)",
			List.of(
				layer("Foundation", 3, 3, b,b,b, b,b,b, b,b,b),
				layer("Controller ring", 3, 3, b,c,b, b,a,b, b,b,b),
				layer("Stair crown", 3, 3, s,b,s, b,a,b, s,b,s),
				layer("Upper ring 1", 3, 3, e,b,e, b,a,b, e,b,e),
				layer("Upper ring 2", 3, 3, e,b,e, b,a,b, e,b,e),
				layer("Slab caps", 3, 3, e,h,e, h,a,h, e,h,e)
			),
			List.of(
				material(brick, 28, "9 foundation + 7 Y1 + 4 Y2 + 4 Y3 + 4 Y4"),
				material(stairs, 4, "Four Y2 corner stairs"),
				material(slab, 4, "Four single slab caps"),
				material(controller, 1, "Front-centre Y1 controller")
			),
			List.of(output(mc("charcoal", 4), "Four Charcoal", "Four logs/wood, 60 s fuel cycle"),
				output(pfStack("hardwood_alloy", 1), "Hardwood Alloy", "One Hardwood, full 5-minute charcoal fire")),
			controller,
			List.of(
				"Only the inner chimney and block above the top centre must be air.",
				"Exterior gaps in Y3-Y5 are ignored; neighboring forges may share normal casing walls.",
				"Four logs/wood fuel a 60-second run; one charcoal produced every 15 seconds.",
				"One charcoal fuels a 5-minute run, with no charcoal byproduct.",
				"Each ignition consumes one firestarter durability point."
			)
		);
	}

	private static ProjectFlintMultiblockDefinition definition(
		String id,
		String title,
		String dimensions,
		List<Layer> layers,
		List<Material> materials,
		List<Output> outputs,
		StackRef representative,
		List<String> notes
	) {
		return new ProjectFlintMultiblockDefinition(
			EmiPort.id("projectflint", "pf_emi/multiblock/" + id),
			title, dimensions, layers, materials, outputs, representative, notes
		);
	}

	private static Layer layer(String name, int width, int depth, Cell... cells) {
		return new Layer(name, width, depth, List.of(cells));
	}

	private static Cell block(StackRef stack, String role) {
		return Cell.block(stack, role);
	}

	private static Cell controller(StackRef stack, String role) {
		return Cell.controller(stack, role);
	}

	private static Cell air(String role) {
		return Cell.air(role);
	}

	private static Cell support(String role) {
		return Cell.support(role);
	}

	private static Material material(StackRef stack, long amount, String note) {
		return new Material(stack, amount, note, false);
	}

	private static Material catalyst(StackRef stack, long amount, String note) {
		return new Material(stack, amount, note, true);
	}

	private static Output output(EmiStack stack, String label, String note) {
		return new Output(stack, label, note);
	}

	private static StackRef pf(String label, String... paths) {
		return ProjectFlintMultiblockDefinition.pf(label, paths);
	}

	private static StackRef anyPf(String label, String... paths) {
		return ProjectFlintMultiblockDefinition.pf(label, paths);
	}

	private static EmiStack pfStack(String path, long amount) {
		Identifier id = EmiPort.id("projectflint", path);
		Item item = Registries.ITEM.get(id);
		if (item == null || item == Items.AIR || !id.equals(Registries.ITEM.getId(item))) return EmiStack.EMPTY;
		return EmiStack.of(item, amount);
	}

	private static EmiStack mc(String path) {
		return mc(path, 1);
	}

	private static EmiStack mc(String path, long amount) {
		Identifier id = EmiPort.id("minecraft", path);
		Item item = Registries.ITEM.get(id);
		if (item == null || item == Items.AIR || !id.equals(Registries.ITEM.getId(item))) return EmiStack.EMPTY;
		return EmiStack.of(item, amount);
	}
}
