# Binnie's Mods for NeoForge 1.21.1

This repository contains the NeoForge 1.21.1 port of Binnie's Mods.

Build with Java 21:

```powershell
.\gradlew.bat --no-daemon --console=plain clean build
```

The development launch tasks are `runClient`, `runServer`, `runGameTestServer`,
and `runData`. Forestry 3.0.0-alpha2 and its split modules are resolved from
Maven Local or ModMaven.
