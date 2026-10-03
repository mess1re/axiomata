# Updating Axiomata

Replace the jar on both sides with the matching new version. Back up the world
before updating. Built-in data comes from the new jar; custom datapacks are not
rewritten.

The server config keeps valid construction settings. The loader adds missing
settings and updates comments. Existing values do not change when defaults do.

Both configs have `configVersion = 1`, the first versioned layout. Unversioned
server configs receive the field without resetting their settings. This version
is independent of the mod version and only needs to change when the config
layout or a setting's meaning changes. Such a change will need a migration and
a backup before the loader corrects the file, not just a new version number.

## Update notices

The loader checks separate `updates/*.json` files for Forge and NeoForge. The
release workflow advances them after uploads succeed; rerunning an old release
cannot replace a newer version number. These files do not contain changelogs.

The client shows one clickable message per launch when a newer version is
available. Failed checks, current versions and development builds ahead of a
release do not generate messages. No jars are downloaded or installed.

Set `updates.showNotice = false` in `config/axiomata-client.toml` to hide the chat
message. The loader's global version-check setting controls its network request
and standard Mods-screen indicator.
