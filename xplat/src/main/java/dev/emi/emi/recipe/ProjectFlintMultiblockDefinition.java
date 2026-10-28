package dev.emi.emi.recipe;

import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Reusable structure definition consumed by the PF-EMI 3D multiblock viewer. PF_EMI_pf8.
 */
public final class ProjectFlintMultiblockDefinition {
	public enum CellKind {
		EMPTY,
		BLOCK,
		AIR,
		SUPPORT
	}

	public record StackRef(List<Identifier> candidates, Identifier fallback, String label) {
		public StackRef {
			candidates = List.copyOf(candidates);
		}

		public EmiStack resolveExact(long amount) {
			for (Identifier id : candidates) {
				Item item = Registries.ITEM.get(id);
				if (item != null && item != Items.AIR && id.equals(Registries.ITEM.getId(item))) {
					return EmiStack.of(item, amount);
				}
			}
			return EmiStack.EMPTY;
		}

		public EmiStack resolveDisplay(long amount) {
			EmiStack exact = resolveExact(amount);
			if (!exact.isEmpty()) {
				return exact;
			}
			if (fallback != null) {
				Item item = Registries.ITEM.get(fallback);
				if (item != null && item != Items.AIR && fallback.equals(Registries.ITEM.getId(item))) {
					return EmiStack.of(item, amount);
				}
			}
			return EmiStack.of(Items.BARRIER, amount);
		}

		public boolean isResolvedExactly() {
			return !resolveExact(1).isEmpty();
		}
	}

	public record Cell(CellKind kind, StackRef stack, String role, boolean controller) {
		public static Cell empty() {
			return new Cell(CellKind.EMPTY, null, "", false);
		}

		public static Cell air(String role) {
			return new Cell(CellKind.AIR, null, role, false);
		}

		public static Cell support(String role) {
			return new Cell(CellKind.SUPPORT, null, role, false);
		}

		public static Cell block(StackRef stack, String role) {
			return new Cell(CellKind.BLOCK, stack, role, false);
		}

		public static Cell controller(StackRef stack, String role) {
			return new Cell(CellKind.BLOCK, stack, role, true);
		}
	}

	public record Layer(String name, int width, int depth, List<Cell> cells) {
		public Layer {
			if (width <= 0 || depth <= 0) {
				throw new IllegalArgumentException("Layer dimensions must be positive");
			}
			if (cells.size() != width * depth) {
				throw new IllegalArgumentException(
					"Layer " + name + " expected " + (width * depth) + " cells but got " + cells.size()
				);
			}
			cells = List.copyOf(cells);
		}

		public Cell cell(int x, int z) {
			if (x < 0 || x >= width || z < 0 || z >= depth) {
				return Cell.empty();
			}
			return cells.get(z * width + x);
		}
	}

	public record Material(StackRef stack, long amount, String note, boolean catalyst) {
	}

	public record Output(EmiStack stack, String label, String note) {
		public Output {
			stack = stack.copy();
		}
	}

	private final Identifier id;
	private final String title;
	private final String dimensions;
	private final List<Layer> layers;
	private final List<Material> materials;
	private final List<Output> outputs;
	private final StackRef representative;
	private final List<String> notes;

	public ProjectFlintMultiblockDefinition(
		Identifier id,
		String title,
		String dimensions,
		List<Layer> layers,
		List<Material> materials,
		List<Output> outputs,
		StackRef representative,
		List<String> notes
	) {
		this.id = id;
		this.title = title;
		this.dimensions = dimensions;
		this.layers = List.copyOf(layers);
		this.materials = List.copyOf(materials);
		this.outputs = List.copyOf(outputs);
		this.representative = representative;
		this.notes = List.copyOf(notes);
		if (layers.isEmpty()) {
			throw new IllegalArgumentException("Multiblock definition must contain at least one layer");
		}
	}

	public Identifier id() {
		return id;
	}

	public String title() {
		return title;
	}

	public String dimensions() {
		return dimensions;
	}

	public List<Layer> layers() {
		return layers;
	}

	public List<Material> materials() {
		return materials;
	}

	public List<Output> outputs() {
		return outputs;
	}

	public StackRef representative() {
		return representative;
	}

	public List<String> notes() {
		return notes;
	}

	public static StackRef pf(String label, String... paths) {
		return candidates(label, null, "projectflint", paths);
	}

	public static StackRef pfFallback(
		String label,
		String fallbackNamespace,
		String fallbackPath,
		String... paths
	) {
		return candidates(
			label,
			EmiPort.id(fallbackNamespace, fallbackPath),
			"projectflint",
			paths
		);
	}

	public static StackRef vanilla(String label, String path) {
		Identifier id = EmiPort.id("minecraft", path);
		return new StackRef(List.of(id), id, label);
	}

	private static StackRef candidates(
		String label,
		Identifier fallback,
		String namespace,
		String... paths
	) {
		return new StackRef(
			java.util.Arrays.stream(paths).map(path -> EmiPort.id(namespace, path)).toList(),
			fallback,
			label
		);
	}
}
