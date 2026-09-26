# Contributing

## What belongs in Axiomata

Axiomata contains reusable blueprint, construction and model-collision behavior. Content and
rules specific to one integrating mod should use the public API instead. If the API is missing an
extension point, add the smallest general-purpose hook needed by the integration.

## Shared code and compatibility

- `src` is shared by Forge 1.20.1 and NeoForge 1.21.1. Stonecutter branches should contain only the
  parts that differ between Minecraft or loader APIs.
- `me.mss1r.axiomata.blueprint.api` and `me.mss1r.axiomata.collision` are consumed by other mods.
  Changes to their contracts must account for those consumers.
- The Blockbench properties `axiomata_sections` and `axiomata_section` are consumed by model
  exporters and are part of the model format. Changes to them need a migration path.
