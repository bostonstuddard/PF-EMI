# PF-EMI 1.21.2 compile pass 3

This patch contains actual Java source changes for the compiler errors from the 2026-09-05 build log.

Main changes:
- Add EmiDrawContext overloads to the 1.21.2 DrawContext texture compatibility bridge.
- Port TooltipComponent to the 1.21.2 TextRenderer-aware height/drawItems API.
- Port TagKey/Registry access to registryRef(), iterateEntries(), and 1.21.2 tag streams.
- Port shader selection to ShaderProgramKeys.
- Port framebuffer/projection APIs and Toast API.
- Port ItemRenderer model lookup, VertexBuffer GlUsage, stack-overlay rendering, and recipe handler generics.
- Keep custom tag-model rendering on a safe first-stack fallback for 1.21.2.
- Keep EMI profiling hooks as no-ops because MinecraftClient no longer exposes getProfiler() in 1.21.2.

Extract this ZIP over the PF-EMI 1.21.2 project root and replace files.
