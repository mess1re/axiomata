# Contributing

## What belongs in Axiomata

Axiomata contains reusable blueprint, construction and model-collision behavior. Content and
rules specific to one integrating mod should use the public API instead. If the API is missing an
extension point, add the smallest general-purpose hook needed by the integration.

## Shared code and compatibility

- `src` is the shared module, with Stonecutter selecting Minecraft 1.20.1 or 1.21.1. Version
  conditions describe Minecraft/API evolution, not a loader choice.
- `forge/src` and `neoforge/src` contain entry points, native event adapters, loader-specific
  mixins and platform hooks. Architectury transforms and packages common code into each loader jar.
- Common code must not depend on loader APIs. The two config specs are the same exception used
  by Recruits: they compile against Forge Config API Port and use native specs at runtime.
- Keep published artifact IDs (`1.20.1-forge`, `1.21.1-neoforge`) and public API packages stable.
- `me.mss1r.axiomata.blueprint.api` and `me.mss1r.axiomata.collision` are consumed by other mods.
  Changes to their contracts must account for those consumers.
- The Blockbench properties `axiomata_sections` and `axiomata_section` are consumed by model
  exporters and are part of the model format. Changes to them need a migration path.


## Verification

`./gradlew build` builds both common versions, both production loader jars and the test runtime.
Pure unit tests run in the common modules; Forge's update-notice adapter has its own unit tests.
Tests that require bootstrapped Minecraft classes and applied mixins run in `:test-runtime:test`
against the assembled NeoForge jar. This includes the entity lifecycle and collision tests;
loader-specific tests live in `neoforge/src/test`. The test runtime uses ModDev only to launch
Minecraft for JUnit; production modules use Architectury Loom.

`checkCommonBoundary` rejects loader API references in common code. Native startup APIs and event
callbacks retain their original classes in loader modules and delegate to shared implementations.
JitPack still publishes `com.github.mess1re.axiomata:1.20.1-forge:<tag>` and
`com.github.mess1re.axiomata:1.21.1-neoforge:<tag>`.

The configuration cache is disabled because Architectury 3.5 retains Gradle Project references
in its transformation tasks. The build cache remains enabled.
