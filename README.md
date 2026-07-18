# Free Camera

Free Camera is a client-side Minecraft camera mod. **Free Camera 3.0.0 is the current release**, and its actively maintained Fabric source is located in [`/3.0`](3.0/).

The remaining loader/version folders are retained as legacy source archives. They document earlier 2.2.0 implementations and are not the active Free Camera 3.x codebase.

## Source layout

| Folder | Loader | Minecraft | Mod version | Status |
| --- | --- | --- | --- | --- |
| [`3.0/`](3.0/) | Fabric | 26.1.2 | 3.0.0 | Current, actively maintained release |
| [`fabric-1.20.1/`](fabric-1.20.1/) | Fabric | 1.20.1 | 2.2.0 | Legacy archive |
| [`fabric-1.21.1/`](fabric-1.21.1/) | Fabric | 1.21.1 | 2.2.0 | Legacy archive |
| [`fabric-1.21.4/`](fabric-1.21.4/) | Fabric | 1.21.4 | 2.2.0 | Legacy archive |
| [`forge-1.20.1/`](forge-1.20.1/) | Forge | 1.20.1 | 2.2.0 | Legacy archive |
| [`forge-1.21.1/`](forge-1.21.1/) | Forge | 1.21.1 | 2.2.0 | Legacy archive |
| [`forge-1.21.4/`](forge-1.21.4/) | Forge | 1.21.4 | 2.2.0 | Legacy archive |
| [`neoforge-1.21.1/`](neoforge-1.21.1/) | NeoForge | 1.21.1 | 2.2.0 | Legacy archive |
| [`neoforge-1.21.4/`](neoforge-1.21.4/) | NeoForge | 1.21.4 | 2.2.0 | Legacy archive |

Each folder is an independent Gradle project with its own wrapper, properties, metadata, and source tree. Do not combine Gradle commands across folders.

## Building the current release

Free Camera 3.0.0 targets Minecraft 26.1.2, Fabric Loader 0.18.5+, Fabric API 0.145.4+26.1.2, and Java 25.

On Windows PowerShell:

```powershell
cd 3.0
$env:JAVA_HOME = 'path-to-jdk-25'
.\gradlew.bat clean build sourcesJar
```

On Linux or macOS:

```bash
cd 3.0
JAVA_HOME=/path/to/jdk-25 ./gradlew clean build sourcesJar
```

Successful builds create:

- `3.0/build/libs/free-camera-3.0.0.jar`
- `3.0/build/libs/free-camera-3.0.0-sources.jar`

The build runs the complete automated safety and behavior verification suite. Minecraft does not need to be launched to build or verify the release.

## Current release documentation

See [`3.0/README.md`](3.0/README.md) for installation, controls, camera modes, recording, paths, configuration, compatibility, and safety details. Release notes are maintained in [`3.0/CHANGELOG.md`](3.0/CHANGELOG.md).

## Legacy archives

Legacy folders are preserved for historical maintenance and reference. Their loader versions, Java requirements, features, metadata, and behavior may differ from Free Camera 3.0. Use each folder's own Gradle configuration when working on an archived version.

## License

Free Camera is available under the MIT License. See [`LICENSE`](LICENSE).
