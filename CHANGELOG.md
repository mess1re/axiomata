# Changelog

## 0.1.0-beta.8

### Added

- Shared artillery APIs: projectile flight and aiming, multipart collisions, penetration, block damage and debris.
- Projectile and block-material profiles, datapack reloads and profile synchronization.
- Artillery particles and smoke textures, with scalable muzzle plumes for large guns.
- Manual and automated loading stages with item consumption, tool wear and cancellation.
- The `axiomata:quality_cost_exempt` item tag excludes fixed-count components from blueprint quality costs.

### Changed

- Tagged construction materials show their tag name instead of the name of the currently displayed item. Untranslated tags show their ID.
- Paper and ink remain in the drawing table when its screen is closed.

### Fixed

- Unfinished drawings reappear when the table is reopened. Replacing or spoiling paper clears the previous drawing.

## 0.1.0-beta.7

### Notes

- The blueprint definition and construction APIs changed; mods built against 0.1.0-beta.6 need to be rebuilt.
- Builds placed in the world need `result.entity`, and that entity must support construction. The entity is no longer guessed from the item, and blueprints no longer place blocks.
- Construction markup is now one file, `data/<namespace>/construction/<model>.json`, which the server sends to clients. Files in `assets/<namespace>/construction` are no longer read; export the markup again.

### Added

- Blueprint format 3: each stage lists its own materials, by item ID or item tag (`#minecraft:planks`); the build menu shows a tag as "any of …" and cycles through its items. A stage that is undone gives back the items paid for it; `returns` sets what a tag gives back once the stage stands. Files in the old format with lettered ingredients still load.
- Builds can start from a starter item instead of a drawn blueprint, and builds with a minimum stage can be finished early by sneak-attacking with the construction hammer.
- Mistakes in blueprint files are logged with the pack, the field and the reason; a broken override falls back to the file beneath it.
- Warnings at server start and reload for blueprints that players cannot obtain or that use fields with no effect.

### Changed

- Datapack blueprints override the ones inside mod jars on every loader, including NeoForge.

### Fixed

- A stage's materials are taken together, so an item material and a tag material can no longer be paid with the same stack.
- A hammer blow counts only when it lands on the highlighted section, including the gaps between its parts.

## 0.1.0-beta.6

### Fixed

- Fixed a crash on startup with Forge 1.20.1 when OptiFine is installed, or when Forge is older than 47.3.

## 0.1.0-beta.5

### Added

- A clickable update notice when entering a world, shown once per game launch. Disable it with `updates.showNotice` in the client config.
- Client and server configs now include `configVersion = 1`. Missing settings are added while valid existing values are kept; deleting the configs is not needed.

### Changed

- Construction hammer strikes now use wooden impact sounds. The section-completion sound is unchanged.

## 0.1.0-beta.4

### Added

- `BlueprintUsedEvent.placed()` returns the construction site a player placed in the world, so integrations can tell who started building it.

## 0.1.0-beta.3

### Fixed

- Fixed the Drawing Table screen not opening on NeoForge 1.21.1.

## 0.1.0-beta.2

### Fixed

- Fixed a client crash on NeoForge 1.21.1 caused by an incompatible renderer mixin signature.

## 0.1.0-beta.1

### Added

- Initial public beta of the blueprint construction and model-based collision systems.
- Forge 1.20.1 and NeoForge 1.21.1 builds.
