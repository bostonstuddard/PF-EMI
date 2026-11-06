# PF-EMI pf.19 — Source audit and manual testing

Pinned ProjectFlint commit: `8d8bd1533bf3a1d571ea062a0024fb2c74831aae` (v0.31.61-b). This supersedes the requested v0.31.60-b release.

## Source-matched updates

- v0.31.58: Forge manual reignition, 50-minute Fat-Burning Torch charges/no relight below Y0; Basin independent odds adjusted; Wooden Cup and Milk Cup recipes (3/5 in personal; 4/6 in Hardwood), fertilizer progression.
- v0.31.59: Basin capacity is four separate input items; FIFO processing and LIFO retrieval. Same per-stomp hunger cost; 2–3 stomps. Item rendering via actual item models.
- v0.31.60: Pure Mica Insulated Glass transmits light, PMI Sheet added.
- v0.31.61: Compiling `TransparentBlock` glass registration and Hardwood-only PMI Sheet recipe. The old `personal_pmi_sheets.json` still exists in GitHub but the upstream build cleanup deletes it. Do not advertise it in PF-EMI.

## EMI UI fixes

- Custom crafting `getInputs()` retains shaped grid holes and reused tools so the EMI filler sees every required slot.
- Recipe tree support is on for actual craft grids, Basin and process recipes; spawn eggs remain informational.
- InventoryRecipeHandler supports only ProjectFlint personal (2×2) grids in player inventory.
- The custom Hardwood 3×3 table screen uses an explicit category gate on its coerced recipe handler. Other mod crafting screens do not gain unintended Hardwood recipes.
- Ingredient transfers also cover Clay Grinder (1 input), Wooden Grinder (2), Sanding Station (1), Brick Furnace/Brick Blast Furnace (input slot 1). Results, fuel, and station durability remain controlled by the machine.
- Recipe transfer is a best-effort EMI inventory interaction and **does not bypass ProjectFlint's 60-tick server craft delay**.
- Native ProjectFlint custom-meat smelting/smoking/blasting/campfire pages are hidden in EMI, not removed from Minecraft. Preserve their source registrations pending an explicit ProjectFlint gameplay decision.
- Corrected a Cooked Turtle Meat-only alternate mob slot; no universal “Drops shown without Looting” sentence.
- Copper Slag 12.5%, Clay/Gravel/Sand Rare Earth 4%, Slag Rare Earth 1.8% reflect `BasinMaterialProcessingSystem`. Independent rolls remain independent.

## Mandatory runtime checks (not completed here)

- [ ] Compile `:fabric:build` with Gradle 9.2.1 and Java 21.
- [ ] In player's inventory, open personal Wooden Cups and use (+) to place ingredients in correct 2×2 slots.
- [ ] In Hardwood Table, fill shaped 3×3 PMI Sheets, Flint Axe and tool recipes; verify the right input slot mapping.
- [ ] Test reused Bone Knife and Flint File after actual crafts, including durability and full inventory.
- [ ] Use EMI crafting tree for 3-deep wood → Hardy Alloy → PMI Sheet progression and inspect reusable tools.
- [ ] Check cooked Turtle Meat in native Smelting, Smoking, Blasting and Campfire tabs; it should be hidden in EMI but upstream gameplay still supports the JSON recipes.
- [ ] Review Frog/Turtle spawn-egg loot pages; no Cooked Turtle Meat in Frog and no special secondary Turtle slot.
- [ ] Compare 13 Basin input recipes with source odds. Verify 4-slot FIFO and per-stomp hunger in game.
- [ ] Singleplayer, LAN and dedicated server recipe synchronization/fill tests; version mismatch behavior.
- [ ] Small and large GUI scale, compatibility with JEI and a world without ProjectFlint.

No compile- or game-runtime success is claimed without these checks.
