package dev.emi.emi.widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.Cell;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.CellKind;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.Layer;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.Material;
import dev.emi.emi.recipe.ProjectFlintMultiblockDefinition.Output;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import org.joml.Matrix3x2fStack;

/**
 * GTNH-style ProjectFlint structure viewer.
 *
 * The structure owns almost the entire recipe canvas. Required materials live
 * in a narrow left rail, outputs in a right rail, and the center is reserved
 * for a dynamically-scaled isometric model.
 */
public final class ProjectFlintMultiblockWidget extends Widget {
	public static final String PF_EMI_MULTIBLOCK_WIDGET = "pf.11-viewport-fit-3d";

	private static final int COLOR_BG = 0xB00E0E0E;
	private static final int COLOR_VIEWPORT = 0xB0161616;
	private static final int COLOR_RAIL = 0xA0202020;
	private static final int COLOR_BORDER = 0xFF606060;
	private static final int COLOR_CONTROLLER = 0xFFFFB347;
	private static final int COLOR_AIR = 0x5577BDEE;
	private static final int COLOR_SUPPORT = 0x66808080;
	private static final int COLOR_BUTTON = 0xCC353535;
	private static final int COLOR_BUTTON_HOVER = 0xCC666666;
	private static final int COLOR_TEXT = 0xFFFFFFFF;
	private static final int COLOR_MUTED = 0xFFB8B8B8;

	private static final int BASE_ITEM = 16;
	private static final int ISO_X = 11;
	private static final int ISO_Z = 6;
	private static final int ISO_Y = 12;
	private static final int RAIL_WIDTH = 29;
	private static final int HEADER_HEIGHT = 17;
	private static final int CONTROL_HEIGHT = 19;

	private final int x;
	private final int y;
	private final int width;
	private final int height;
	private final ProjectFlintMultiblockDefinition definition;

	private int rotation;
	private int topLayer;

	public ProjectFlintMultiblockWidget(
		int x,
		int y,
		int width,
		int height,
		ProjectFlintMultiblockDefinition definition
	) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.definition = definition;
		this.topLayer = definition.layers().size() - 1;
	}

	@Override
	public Bounds getBounds() {
		return new Bounds(x, y, width, height);
	}

	@Override
	public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(draw);
		context.fill(x, y, width, height, COLOR_BG);

		renderHeader(context);
		renderRailBackgrounds(context);
		renderMaterialRail(context, draw, delta);
		renderOutputRail(context, draw, delta);

		Bounds viewport = viewportBounds();
		context.fill(viewport.x(), viewport.y(), viewport.width(), viewport.height(), COLOR_VIEWPORT);
		border(context, viewport, COLOR_BORDER);

		draw.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
		for (RenderedCell rendered : projectedCells()) {
			renderProjectedCell(context, draw, rendered, delta);
		}
		draw.disableScissor();

		renderControls(context, mouseX, mouseY);
	}

	private void renderHeader(EmiDrawContext context) {
		// Do not center a long title underneath the dimensions label.
		String dimensions = definition.dimensions();
		int maxTitleChars = Math.max(3, (width - dimensions.length() * 6 - 18) / 6);
		String title = definition.title();
		if (title.length() > maxTitleChars) title = title.substring(0, maxTitleChars - 1) + "…";
		context.drawText(EmiPort.literal(title), x + 4, y + 4, COLOR_TEXT);

		context.drawText(
			EmiPort.literal(dimensions),
			x + width - dimensions.length() * 6 - 4,
			y + 4,
			COLOR_MUTED
		);
	}

	private void renderRailBackgrounds(EmiDrawContext context) {
		Bounds left = leftRailBounds();
		Bounds right = rightRailBounds();

		context.fill(left.x(), left.y(), left.width(), left.height(), COLOR_RAIL);
		context.fill(right.x(), right.y(), right.width(), right.height(), COLOR_RAIL);
		border(context, left, COLOR_BORDER);
		border(context, right, COLOR_BORDER);

		context.drawCenteredText(
			EmiPort.literal("IN"),
			left.x() + left.width() / 2,
			left.y() + 3,
			COLOR_MUTED
		);

		context.drawCenteredText(
			EmiPort.literal("OUT"),
			right.x() + right.width() / 2,
			right.y() + 3,
			COLOR_MUTED
		);
	}

	private void renderMaterialRail(
		EmiDrawContext context,
		DrawContext draw,
		float delta
	) {
		Bounds rail = leftRailBounds();
		int slotX = rail.x() + 5;
		int slotY = rail.y() + 14;

		for (Material material : definition.materials()) {
			if (slotY + 18 > rail.bottom() - 2) break;
			drawRailSlot(context, slotX, slotY);
			material.stack().resolveDisplay(material.amount())
				.render(draw, slotX + 1, slotY + 1, delta);
			slotY += 20;
		}
	}

	private void renderOutputRail(
		EmiDrawContext context,
		DrawContext draw,
		float delta
	) {
		Bounds rail = rightRailBounds();
		int slotX = rail.x() + 5;
		int slotY = rail.y() + 14;

		if (definition.outputs().isEmpty()) {
			context.drawCenteredText(
				EmiPort.literal("-"),
				rail.x() + rail.width() / 2,
				slotY + 4,
				COLOR_MUTED
			);
			return;
		}

		for (Output output : definition.outputs()) {
			if (slotY + 18 > rail.bottom() - 2) break;
			drawRailSlot(context, slotX, slotY);
			output.stack().render(draw, slotX + 1, slotY + 1, delta);
			slotY += 20;
		}
	}

	private void drawRailSlot(EmiDrawContext context, int sx, int sy) {
		context.fill(sx, sy, 18, 18, COLOR_VIEWPORT);
		context.fill(sx, sy, 18, 1, COLOR_BORDER);
		context.fill(sx, sy + 17, 18, 1, COLOR_BORDER);
		context.fill(sx, sy, 1, 18, COLOR_BORDER);
		context.fill(sx + 17, sy, 1, 18, COLOR_BORDER);
	}

	private void renderControls(
		EmiDrawContext context,
		int mouseX,
		int mouseY
	) {
		renderButton(context, rotateLeftBounds(), "<", mouseX, mouseY);
		renderButton(context, rotateRightBounds(), ">", mouseX, mouseY);
		renderButton(context, lowerLayerBounds(), "-", mouseX, mouseY);
		renderButton(context, raiseLayerBounds(), "+", mouseX, mouseY);
		renderButton(context, allLayersBounds(), "ALL", mouseX, mouseY);
		renderButton(context, infoBounds(), "i", mouseX, mouseY);

		String layer = (topLayer + 1) + "/" + definition.layers().size();
		context.drawText(
			EmiPort.literal(layer),
			allLayersBounds().right() + 5,
			y + height - 14,
			COLOR_MUTED
		);
	}

	private void renderProjectedCell(
		EmiDrawContext context,
		DrawContext draw,
		RenderedCell rendered,
		float delta
	) {
		Cell cell = rendered.cell();
		int sx = rendered.screenX();
		int sy = rendered.screenY();
		int renderSize = rendered.renderSize();

		if (cell.kind() == CellKind.AIR) {
			drawMarker(context, sx, sy, renderSize, COLOR_AIR, "A");
			return;
		}

		if (cell.kind() == CellKind.SUPPORT) {
			drawMarker(context, sx, sy, renderSize, COLOR_SUPPORT, "S");
			return;
		}

		if (cell.kind() != CellKind.BLOCK || cell.stack() == null) {
			return;
		}

		EmiStack stack = cell.stack().resolveDisplay(1);
		if (stack.isEmpty()) {
			return;
		}

		Matrix3x2fStack matrices = draw.getMatrices();
		matrices.pushMatrix();
		matrices.translate(sx, sy);
		matrices.scale(rendered.scale(), rendered.scale());
		stack.render(draw, 0, 0, delta);
		matrices.popMatrix();

		if (cell.controller()) {
			border(
				context,
				new Bounds(sx - 2, sy - 2, renderSize + 4, renderSize + 4),
				COLOR_CONTROLLER
			);
		}
	}

	private void drawMarker(
		EmiDrawContext context,
		int sx,
		int sy,
		int size,
		int color,
		String text
	) {
		int inset = Math.max(2, size / 5);
		context.fill(
			sx + inset,
			sy + inset,
			Math.max(6, size - inset * 2),
			Math.max(6, size - inset * 2),
			color
		);
		context.drawCenteredText(
			EmiPort.literal(text),
			sx + size / 2,
			sy + size / 2 - 4,
			COLOR_TEXT
		);
	}

	private List<RenderedCell> projectedCells() {
		List<RawCell> raw = new ArrayList<>();

		for (int layerIndex = 0; layerIndex <= topLayer; layerIndex++) {
			Layer layer = definition.layers().get(layerIndex);

			for (int z = 0; z < layer.depth(); z++) {
				for (int cellX = 0; cellX < layer.width(); cellX++) {
					Cell cell = layer.cell(cellX, z);
					if (cell.kind() == CellKind.EMPTY) continue;

					RotatedPoint point = rotate(
						cellX,
						z,
						layer.width(),
						layer.depth()
					);

					int rawX = (point.x() - point.z()) * ISO_X;
					int rawY = (point.x() + point.z()) * ISO_Z
						- layerIndex * ISO_Y;

					int depthKey =
						(point.x() + point.z()) * 1000
						+ layerIndex;

					raw.add(new RawCell(
						cell,
						layerIndex,
						rawX,
						rawY,
						depthKey
					));
				}
			}
		}

		if (raw.isEmpty()) return List.of();

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (RawCell cell : raw) {
			minX = Math.min(minX, cell.rawX());
			minY = Math.min(minY, cell.rawY());
			maxX = Math.max(maxX, cell.rawX() + BASE_ITEM);
			maxY = Math.max(maxY, cell.rawY() + BASE_ITEM);
		}

		int rawWidth = Math.max(1, maxX - minX);
		int rawHeight = Math.max(1, maxY - minY);
		Bounds viewport = viewportBounds();

		float fitX = (viewport.width() - 12.0F) / rawWidth;
		float fitY = (viewport.height() - 12.0F) / rawHeight;
		float scale = Math.max(0.55F, Math.min(2.85F, Math.min(fitX, fitY)));

		int scaledWidth = Math.round(rawWidth * scale);
		int scaledHeight = Math.round(rawHeight * scale);
		int originX = viewport.x() + (viewport.width() - scaledWidth) / 2;
		int originY = viewport.y() + (viewport.height() - scaledHeight) / 2;
		int renderSize = Math.round(BASE_ITEM * scale);

		List<RenderedCell> rendered = new ArrayList<>();
		for (RawCell cell : raw) {
			int sx = originX + Math.round((cell.rawX() - minX) * scale);
			int sy = originY + Math.round((cell.rawY() - minY) * scale);

			rendered.add(new RenderedCell(
				cell.cell(),
				cell.layerIndex(),
				sx,
				sy,
				renderSize,
				scale,
				cell.depthKey()
			));
		}

		rendered.sort(Comparator.comparingInt(RenderedCell::depthKey));
		return rendered;
	}

	private RotatedPoint rotate(
		int px,
		int pz,
		int layerWidth,
		int layerDepth
	) {
		return switch (rotation) {
			case 1 -> new RotatedPoint(layerDepth - 1 - pz, px);
			case 2 -> new RotatedPoint(
				layerWidth - 1 - px,
				layerDepth - 1 - pz
			);
			case 3 -> new RotatedPoint(pz, layerWidth - 1 - px);
			default -> new RotatedPoint(px, pz);
		};
	}

	private void renderButton(
		EmiDrawContext context,
		Bounds bounds,
		String text,
		int mouseX,
		int mouseY
	) {
		int color = bounds.contains(mouseX, mouseY)
			? COLOR_BUTTON_HOVER
			: COLOR_BUTTON;

		context.fill(
			bounds.x(),
			bounds.y(),
			bounds.width(),
			bounds.height(),
			color
		);
		border(context, bounds, COLOR_BORDER);
		context.drawCenteredText(
			EmiPort.literal(text),
			bounds.x() + bounds.width() / 2,
			bounds.y() + 2,
			COLOR_TEXT
		);
	}

	private void border(
		EmiDrawContext context,
		Bounds bounds,
		int color
	) {
		context.fill(bounds.x(), bounds.y(), bounds.width(), 1, color);
		context.fill(bounds.x(), bounds.bottom() - 1, bounds.width(), 1, color);
		context.fill(bounds.x(), bounds.y(), 1, bounds.height(), color);
		context.fill(bounds.right() - 1, bounds.y(), 1, bounds.height(), color);
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0) return false;

		if (rotateLeftBounds().contains(mouseX, mouseY)) {
			rotation = Math.floorMod(rotation - 1, 4);
			return true;
		}
		if (rotateRightBounds().contains(mouseX, mouseY)) {
			rotation = Math.floorMod(rotation + 1, 4);
			return true;
		}
		if (lowerLayerBounds().contains(mouseX, mouseY)) {
			topLayer = Math.max(0, topLayer - 1);
			return true;
		}
		if (raiseLayerBounds().contains(mouseX, mouseY)) {
			topLayer = Math.min(definition.layers().size() - 1, topLayer + 1);
			return true;
		}
		if (allLayersBounds().contains(mouseX, mouseY)) {
			topLayer = definition.layers().size() - 1;
			return true;
		}
		return infoBounds().contains(mouseX, mouseY);
	}

	@Override
	public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
		if (rotateLeftBounds().contains(mouseX, mouseY)) return tooltip("Rotate 90 degrees left");
		if (rotateRightBounds().contains(mouseX, mouseY)) return tooltip("Rotate 90 degrees right");
		if (lowerLayerBounds().contains(mouseX, mouseY)) return tooltip("Hide highest visible layer");
		if (raiseLayerBounds().contains(mouseX, mouseY)) return tooltip("Show next layer");
		if (allLayersBounds().contains(mouseX, mouseY)) return tooltip("Show all structure layers");

		if (infoBounds().contains(mouseX, mouseY)) {
			List<TooltipComponent> lines = new ArrayList<>();
			lines.add(component(definition.title()));
			lines.add(component("Size: " + definition.dimensions()));
			lines.add(component("A = required air / hollow space"));
			lines.add(component("S = required sturdy support"));
			lines.add(component("Gold outline = controller / interaction point"));
			for (String note : definition.notes()) lines.add(component(note));
			return lines;
		}

		List<TooltipComponent> railTip = railTooltip(mouseX, mouseY);
		if (!railTip.isEmpty()) return railTip;

		List<RenderedCell> cells = projectedCells();
		for (int i = cells.size() - 1; i >= 0; i--) {
			RenderedCell cell = cells.get(i);
			if (new Bounds(
				cell.screenX(),
				cell.screenY(),
				cell.renderSize(),
				cell.renderSize()
			).contains(mouseX, mouseY)) {
				return cellTooltip(cell);
			}
		}

		return List.of();
	}

	private List<TooltipComponent> railTooltip(int mouseX, int mouseY) {
		Bounds left = leftRailBounds();
		int slotX = left.x() + 5;
		int slotY = left.y() + 14;

		for (Material material : definition.materials()) {
			if (slotY + 18 > left.bottom() - 2) break;
			if (new Bounds(slotX, slotY, 18, 18).contains(mouseX, mouseY)) {
				List<TooltipComponent> lines = new ArrayList<>();
				lines.add(component(material.stack().label()));
				lines.add(component("Required: " + material.amount()));
				if (material.catalyst()) lines.add(component("Formation / tool item"));
				if (!material.note().isBlank()) lines.add(component(material.note()));
				return lines;
			}
			slotY += 20;
		}

		Bounds right = rightRailBounds();
		slotX = right.x() + 5;
		slotY = right.y() + 14;

		for (Output output : definition.outputs()) {
			if (slotY + 18 > right.bottom() - 2) break;
			if (new Bounds(slotX, slotY, 18, 18).contains(mouseX, mouseY)) {
				List<TooltipComponent> lines = new ArrayList<>();
				lines.add(component(output.label()));
				if (!output.note().isBlank()) lines.add(component(output.note()));
				return lines;
			}
			slotY += 20;
		}

		return List.of();
	}

	private List<TooltipComponent> cellTooltip(RenderedCell rendered) {
		Cell cell = rendered.cell();
		List<TooltipComponent> lines = new ArrayList<>();

		lines.add(component(
			"Layer " + (rendered.layerIndex() + 1)
				+ ": "
				+ definition.layers().get(rendered.layerIndex()).name()
		));

		if (cell.kind() == CellKind.AIR) {
			lines.add(component(cell.role().isBlank() ? "Required air" : cell.role()));
			return lines;
		}
		if (cell.kind() == CellKind.SUPPORT) {
			lines.add(component(cell.role().isBlank() ? "Required sturdy support" : cell.role()));
			return lines;
		}
		if (cell.stack() != null) lines.add(component(cell.stack().label()));
		if (cell.controller()) lines.add(component("Controller / interaction position"));
		if (!cell.role().isBlank()) lines.add(component(cell.role()));
		return lines;
	}

	private List<TooltipComponent> tooltip(String line) {
		return List.of(component(line));
	}

	private TooltipComponent component(String line) {
		return TooltipComponent.of(EmiPort.ordered(EmiPort.literal(line)));
	}

	private Bounds leftRailBounds() {
		return new Bounds(
			x + 2,
			y + HEADER_HEIGHT,
			RAIL_WIDTH,
			height - HEADER_HEIGHT - 2
		);
	}

	private Bounds rightRailBounds() {
		return new Bounds(
			x + width - RAIL_WIDTH - 2,
			y + HEADER_HEIGHT,
			RAIL_WIDTH,
			height - HEADER_HEIGHT - 2
		);
	}

	private Bounds viewportBounds() {
		return new Bounds(
			x + RAIL_WIDTH + 6,
			y + HEADER_HEIGHT + 2,
			width - RAIL_WIDTH * 2 - 12,
			height - HEADER_HEIGHT - CONTROL_HEIGHT - 5
		);
	}

	private int controlY() {
		return y + height - 17;
	}

	private Bounds rotateLeftBounds() {
		return new Bounds(x + RAIL_WIDTH + 9, controlY(), 15, 13);
	}

	private Bounds rotateRightBounds() {
		return new Bounds(x + RAIL_WIDTH + 27, controlY(), 15, 13);
	}

	private Bounds lowerLayerBounds() {
		return new Bounds(x + RAIL_WIDTH + 48, controlY(), 15, 13);
	}

	private Bounds raiseLayerBounds() {
		return new Bounds(x + RAIL_WIDTH + 66, controlY(), 15, 13);
	}

	private Bounds allLayersBounds() {
		return new Bounds(x + RAIL_WIDTH + 87, controlY(), 29, 13);
	}

	private Bounds infoBounds() {
		return new Bounds(x + width - RAIL_WIDTH - 23, controlY(), 15, 13);
	}

	private record RotatedPoint(int x, int z) {
	}

	private record RawCell(
		Cell cell,
		int layerIndex,
		int rawX,
		int rawY,
		int depthKey
	) {
	}

	private record RenderedCell(
		Cell cell,
		int layerIndex,
		int screenX,
		int screenY,
		int renderSize,
		float scale,
		int depthKey
	) {
	}
}
