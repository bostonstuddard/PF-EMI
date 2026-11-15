package dev.emi.emi.runtime;

import java.io.File;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.EmiPort;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.mixin.accessor.GameRendererAccessor;
import dev.emi.emi.mixin.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class EmiScreenshotRecorder {
	private static final String SCREENSHOTS_DIRNAME = "screenshots";

	/**
	 * Saves a screenshot to the game's `screenshots` directory, doing the appropriate setup so that anything rendered in renderer will be captured
	 * and saved.
	 * <p>
	 * <b>Note:</b> the path can have <code>/</code> characters, indicating subdirectories. Java handles these correctly on Windows. The path should
	 * <b>not</b> contain the <code>.png</code> extension, as that will be added after checking for duplicates. If a file with this path already
	 * exists, then path will be suffixed with a <code>_#</code>, before adding the <code>.png</code> extension, where <code>#</code> represents an
	 * increasing number to avoid conflicts.
	 * <p>
	 * <b>Note 2:</b> The width and height parameters are reflected in the viewport when rendering. But the EMI-config
	 * <code>ui.recipe-screenshot-scale</code> value causes the resulting image to be scaled.
	 *
	 * @param path     the path to save the screenshot to, without extension.
	 * @param width    the width of the screenshot, not counting EMI-config scale.
	 * @param height   the height of the screenshot, not counting EMI-config scale.
	 * @param renderer a function to render the things being screenshotted.
	 */
	public static void saveScreenshot(String path, int width, int height, Consumer<DrawContext> renderer) {
		if (!RenderSystem.isOnRenderThread()) {
			MinecraftClient.getInstance().execute(() -> saveScreenshotInner(path, width, height, renderer));
		} else {
			saveScreenshotInner(path, width, height, renderer);
		}
	}

	private static void saveScreenshotInner(String path, int width, int height, Consumer<DrawContext> renderer) {
		MinecraftClient client = MinecraftClient.getInstance();

		int scale;
		if (EmiConfig.recipeScreenshotScale < 1) {
			scale = EmiPort.getGuiScale(client);
		} else {
			scale = EmiConfig.recipeScreenshotScale;
		}

		Framebuffer previousFramebuffer = client.getFramebuffer();
		Framebuffer framebuffer = new SimpleFramebuffer("emi_recipe_screenshot", width * scale, height * scale, true);
		MinecraftClientAccessor accessor = (MinecraftClientAccessor) (Object) client;

		// Flush anything queued for the normal window before redirecting GUI draws.
		client.getBufferBuilders().getEntityVertexConsumers().draw();
		accessor.emi$setFramebuffer(framebuffer);
		if (framebuffer.getColorAttachment() != null && framebuffer.getDepthAttachment() != null) {
			RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
				framebuffer.getColorAttachment(), 0x00000000, framebuffer.getDepthAttachment(), 1.0);
		}

		GameRenderer gameRenderer = accessor.emi$getGameRenderer();
		GameRendererAccessor gameRendererAccessor = (GameRendererAccessor) (Object) gameRenderer;
		GuiRenderState guiState = gameRendererAccessor.emi$getGuiState();
		GuiRenderer guiRenderer = gameRendererAccessor.emi$getGuiRenderer();

		// GUI rendering became deferred in 1.21.6. Build an isolated frame in the
		// vanilla GuiRenderState, then explicitly render it into our temporary framebuffer.
		guiState.clear();
		DrawContext context = new DrawContext(client, guiState, 0, 0);
		context.getMatrices().scale(
			client.getWindow().getScaledWidth() / (float) width,
			client.getWindow().getScaledHeight() / (float) height
		);

		try {
			renderer.accept(context);
			context.drawDeferredElements();
			guiRenderer.render(gameRendererAccessor.emi$getFogRenderer().getFogBuffer(FogRenderer.FogType.NONE));
			client.getBufferBuilders().getEntityVertexConsumers().draw();
		} finally {
			guiState.clear();
			accessor.emi$setFramebuffer(previousFramebuffer);
		}

		saveScreenshotInner(client.runDirectory, path, framebuffer,
			message -> client.execute(() -> client.inGameHud.getChatHud().addMessage(message)));
	}

	private static void saveScreenshotInner(File gameDirectory, String suggestedPath, Framebuffer framebuffer, Consumer<Text> messageReceiver) {
		net.minecraft.client.util.ScreenshotRecorder.takeScreenshot(framebuffer, nativeImage -> {
			framebuffer.delete();
			writeScreenshot(gameDirectory, suggestedPath, nativeImage, messageReceiver);
		});
	}

	private static void writeScreenshot(File gameDirectory, String suggestedPath, NativeImage nativeImage, Consumer<Text> messageReceiver) {
		File screenshots = new File(gameDirectory, SCREENSHOTS_DIRNAME);
		screenshots.mkdir();

		String filename = getScreenshotFilename(screenshots, suggestedPath);
		File file = new File(screenshots, filename);

		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}

		Util.getIoWorkerExecutor().execute(() -> {
			try {
				nativeImage.writeTo(file);

				Text text = EmiPort.literal(filename,
					Style.EMPTY.withUnderline(true).withClickEvent(new ClickEvent.OpenFile(file.getAbsolutePath())));
				messageReceiver.accept(EmiPort.translatable("screenshot.success", text));
			} catch (Throwable e) {
				EmiLog.error("Failed to write screenshot", e);
				messageReceiver.accept(EmiPort.translatable("screenshot.failure", e.getMessage()));
			} finally {
				nativeImage.close();
			}
		});
	}

	private static String getScreenshotFilename(File directory, String path) {
		int i = 1;
		while ((new File(directory, path + (i == 1 ? "" : "_" + i) + ".png")).exists()) {
			++i;
		}
		return path + (i == 1 ? "" : "_" + i) + ".png";
	}
}
