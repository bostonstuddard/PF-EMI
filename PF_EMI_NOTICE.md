# PF-EMI 1.21.2 Port Notice

PF-EMI keeps the runtime mod id `emi` for compatibility while using the visible name and archive name `PF-EMI` / `pf-emi`.

This source target is a Minecraft **1.21.2** port based on the EMI **1.1.23** 1.21.1 code line, carrying forward the PF-EMI fork identity and the recipe-viewer behavior needed by ProjectFlint.

Minecraft 1.21.2 moved authoritative recipe objects to the server. PF-EMI therefore includes its own recipe-entry synchronization payload so the client can still build the complete EMI recipe index when PF-EMI is present on both sides.

The optional JEMI compatibility bridge is disabled for this target because JEI did not publish a 1.21.2/1.21.3 build.
