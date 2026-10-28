# PF-EMI 1.21.3 Port Notice

PF-EMI keeps the runtime mod id `emi` for compatibility while using the visible name and archive name `PF-EMI` / `pf-emi`.

This source target is the Minecraft **1.21.3** forward port of the completed PF-EMI **1.21.2** work, ultimately based on EMI **1.1.23**. All PF-specific recipe synchronization and 1.21.2 rendering/API fixes are carried forward as the starting point.

Minecraft 1.21.2 moved authoritative recipe objects to the server, so PF-EMI continues to synchronize full recipe entries to the client for the EMI recipe index on 1.21.3.

The optional JEMI compatibility bridge remains disabled for this target because JEI did not publish a compatible 1.21.2/1.21.3 line.
