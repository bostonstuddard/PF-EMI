package dev.emi.emi.runtime;

/**
 * EMI profiling hooks. Minecraft 1.21.2 no longer exposes the client profiler
 * through MinecraftClient, so these hooks intentionally become no-ops on this port.
 */
public class EmiProfiler {
	public static void push(String name) {
	}

	public static void pop() {
	}

	public static void swap(String name) {
	}
}
