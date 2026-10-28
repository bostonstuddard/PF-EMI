package dev.emi.emi.mixin;

/**
 * Legacy source-path shim for the pre-1.21.2 status-effect mixin.
 *
 * <p>Minecraft 1.21.2 removed AbstractInventoryScreen and moved this logic to
 * StatusEffectsDisplay. The active implementation is StatusEffectsDisplayMixin.
 * This empty class intentionally remains so an overlay-style source patch also
 * neutralizes the old source file instead of requiring a manual delete.</p>
 */
@Deprecated(forRemoval = true)
final class AbstractInventoryScreenMixin {
	private AbstractInventoryScreenMixin() {
	}
}
