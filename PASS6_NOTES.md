# PF-EMI 1.21.2 compile pass 6

Fixes the Fabric `IdentifiableResourceReloadListener` compile failure introduced by Minecraft 1.21.2's `ResourceReloader` signature change.

- Removed the obsolete `Profiler` parameters from `EmiClientFabric.reload(...)`.
- Removed the obsolete `net.minecraft.util.profiler.Profiler` import.
- Forwards the 1.21.2 four-argument reload call directly to EMI's registered resource reloader.

Apply this archive directly over the existing PF-EMI 1.21.2 project root.
