<p align="center">
  <img src="src/main/resources/axiomata.png" alt="Axiomata" width="192">
</p>

# Axiomata

Axiomata provides the foundation for complex objects that players build and interact with directly
in the world, beyond the usual item, block and single-hitbox entity systems. It currently includes:

- A data-driven blueprint system with drawing quality and staged, in-world construction
- Model-based geometry used for collision, interaction, moving surfaces, climbing and pathfinding

## For mod developers

Blueprint integrations use `UnderConstruction`. Model collision integrations use
`CollidableStructure`. Their supporting APIs are under `me.mss1r.axiomata.blueprint.api` and
`me.mss1r.axiomata.collision`.

### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
    maven { url 'https://maven.architectury.dev/' }
}

dependencies {
    implementation "com.github.mess1re.axiomata:${minecraftVersion}-${loader}:${axiomataVersion}"
}
```

`minecraftVersion` and `loader` select the matching build; `axiomataVersion` is the release tag.
ForgeGradle projects should pass the coordinate through `fg.deobf(...)`.

## Building

Run `./gradlew build` (`gradlew.bat build` on Windows). Version-specific jars are written to
`versions/<target>/build/libs`.

## Blockbench tool

The construction-section plugin and its installation instructions are in
[`tools/blockbench`](tools/blockbench).

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

Source code, build scripts and non-artistic data are licensed under
[LGPL-3.0-only](LICENSE). Original artistic assets are covered by
[`LICENSE-ASSETS.md`](LICENSE-ASSETS.md).
