# Koremods - Modlauncher

Modlauncher frontend for [Koremods](https://gitlab.com/gofancy/koremods/koremods), a bytecode modification framework running KTS/JSR-223.


Currently supported Minecraft versions: 1.16.x

### Usage

Minimum requirements:
- Gradle 6 - required to properly consume dependency gradle metadata

Declare the dependency in your build.gradle, replacing `<version>` with the desired version
```groovy
dependencies {
    implementation group: 'wtf.gofancy.koremods', name: 'koremods-modlauncher', version: '<version>'
}
```
