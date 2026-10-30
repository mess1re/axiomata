# Contributing

## What belongs in Axiomata

Axiomata contains reusable blueprint, construction and model-collision behavior. Content and
rules specific to one integrating mod should use the public API instead. If the API is missing an
extension point, add the smallest general-purpose hook needed by the integration.

## Shared code and compatibility

- `src` is the shared module, with Stonecutter selecting Minecraft 1.20.1 or 1.21.1. Version
  conditions describe Minecraft/API evolution, not a loader choice.
- `forge/src`, `neoforge/src` and `fabric/src` contain entry points, native event adapters, loader-specific
  mixins and platform hooks. Architectury transforms and packages common code into each loader jar.
- Common code must not depend on loader APIs. The two config specs are the same exception used
  by Recruits: they compile against Forge Config API Port and use native specs at runtime.
- Keep published artifact IDs (`1.20.1-forge`, `1.21.1-neoforge`, `1.20.1-fabric`,
  `1.21.1-fabric`) and public API packages stable.
- `me.mss1r.axiomata.blueprint.api` and `me.mss1r.axiomata.collision` are consumed by other mods.
  Changes to their contracts must account for those consumers.
- The Blockbench properties `axiomata_sections` and `axiomata_section` are consumed by model
  exporters and are part of the model format. Changes to them need a migration path.


## Verification

`./gradlew build` builds both common versions, all four production loader jars and the test runtime.
Pure unit tests run in the common modules; Forge's update-notice adapter has its own unit tests.
Tests that require bootstrapped Minecraft classes and applied mixins run in `:test-runtime:test`
against the assembled NeoForge jar. This includes the entity lifecycle and collision tests;
loader-specific tests live in `neoforge/src/test`. The test runtime uses ModDev only to launch
Minecraft for JUnit; production modules use Architectury Loom.

Run `./gradlew :fabric:1.20.1:runGameTest :fabric:1.21.1:runGameTest` for Fabric integration tests.
They launch Minecraft servers and check persistent entity data, lifecycle registration, carried
structures, projectile openings, geometry interaction reach and block-protection hooks. Reports
are in `fabric/versions/<version>/build/test-results/fabric-gametest.xml`. Their test mod is isolated
in `fabric/src/testmod` and is not included in production jars. These tests do not replace testing
with a specific claim mod or a consuming mod's client renderer.

Development clients and dedicated servers use `:fabric:<version>:runClient` and `runServer`.
For a local multiplayer smoke test, `-Pclient.server=127.0.0.1:<port>` connects on startup;
`-Pclient.username=<name>` supplies the development username.

`checkCommonBoundary` rejects loader imports, bytecode descriptors and native extension names in
common code, including Forge's `canReachRaw`. Native startup APIs and event
callbacks retain their original classes in loader modules and delegate to shared implementations.
JitPack still publishes `com.github.mess1re.axiomata:1.20.1-forge:<tag>` and
`com.github.mess1re.axiomata:1.21.1-neoforge:<tag>`, plus the two `<minecraft>-fabric` artifacts.

Fabric uses Forge Config API Port for the same configuration specs and file names as the native
loaders. Its placement protection uses embedded Common Protection API 1.x, not simulated item use.
Update notices use `updates/fabric.json`, populated only when a release is published.

The configuration cache is disabled because Architectury 3.5 retains Gradle Project references
in its transformation tasks. The build cache remains enabled.
