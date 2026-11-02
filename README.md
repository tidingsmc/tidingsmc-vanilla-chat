# Tidings Vanilla Chat

A [Tidings](https://github.com/tidingsmc/tidingsmc) add-on that adds a **Chat** tab, to talk with the server from your phone.
Players can read the chat, send messages, and receive notifications when mentioned.

## Server setup

Put the jar in `mods/` next to Tidings and restart the server. Players don't need to install anything.

## Development

Requirements: a JDK 17+ (Gradle downloads Java 17 for Forge automatically).

The mod compiles against Tidings without bundling it. Gradle downloads Tidings from Modrinth Maven, at the version set by `tidings_version` in `gradle.properties`.

```sh
./gradlew build        # mod jar in build/libs/
./gradlew test         # unit tests
./gradlew runServer    # dev server with Tidings and this mod (accept the EULA in run/eula.txt,
                       # and set app-url in run/config/tidings-common.toml)
```

## License

AGPL-3.0-or-later. See [LICENSE](LICENSE).
