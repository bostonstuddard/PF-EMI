# PF-EMI

Project Flint's customized EMI build.

## Current build target

This tree targets **Minecraft 1.21.9** on **Fabric, Forge, and NeoForge**.

Versions used by this pass:

- Minecraft `1.21.9`
- Yarn `1.21.9+build.1`
- Fabric Loader `0.17.3`
- Fabric API `0.134.1+1.21.9`
- Forge `59.0.5`
- NeoForge `21.9.16-beta`
- Java `21`

## Building on Windows

Run:

```text
build.bat
```

The script locates a Java 21 JDK, builds all three maintained loaders, and copies the finished jars to:

```text
release/PF-EMI-1.21.9.jar
release/PF-EMI-1.21.9-FORGE.jar
release/PF-EMI-1.21.9-NEOFORGE.jar
```

## Building on Linux/macOS

Run:

```text
./build.sh
```

The same three-loader release set is written to `release/`.

## 1.21.9 port notes

Minecraft 1.21.9 changed GUI keyboard and mouse handling to use input record objects. PF-EMI's screens, widgets, input mixins, and Fabric runtime refmap have been migrated to the new descriptors while retaining PF-EMI's existing internal input behavior.

See `PORT_1.21.9_FABRIC_NOTES.md` for the pass-specific details.
