# Changelog

## 0.1.0-beta.5

### Added

- A clickable update notice when entering a world, shown once per game launch. It can be disabled in the client config.
- Config layout versions. Existing settings are preserved when missing fields are added.

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
