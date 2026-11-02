# PF-EMI pf.16 — JEI compile-warning diagnosis and patch notes

## User build output

- Both `:xplat:compileJava` and `:fabric:compileJava` completed in the supplied excerpt.
- The twelve javac `removal` warnings refer to deprecated JEI interface contracts implemented by three PF-EMI JEMI adapter classes.
- `Architectury Loom is in beta` is a build-tool notice; the remapper's `Cannot remap modifiers` is a remapping diagnostic and should be investigated only if it correlates with a build/runtime problem.
- `Problems report is available` does not indicate failure. The excerpt ends without the final build status.

## Fix scope

1. Implement modern ingredient entrypoints directly and retain deprecated JEI override methods as thin adapters.
2. Limit `@SuppressWarnings("removal")` to deprecated JEI overrides only; preserve future compiler warnings elsewhere.
3. Respect the amount in `JemiRecipeSlotBuilder.add(Fluid, long)`; before this patch it was discarded.
4. Scope `unchecked` suppression to the generic position return cast in `JemiPlaceable`.
5. Preserve all ProjectFlint 0.31.52-b recipe/GUI changes from pf.15.

## Not validated

No JEI/Gradle compile or Minecraft runtime test was performed here. Suppression does **not** solve the eventual API migration when JEI actually removes these old interface methods. Other Gradle notices may still appear; suppression is not a runtime optimization.
