# Player Locations (Fabric) for Aternos

Aternos can't expose extra ports, so the mod PUSHES player positions to a small
website you host elsewhere.

## 1. Host the website (free: Render, Railway, Glitch, Replit, Fly.io)
- Upload the `website/` folder, start command: `node server.js` (Node 18+)
- Set environment variable `SECRET` to a long random string.
- Note the URL, e.g. https://my-map.onrender.com

## 2. Build the mod (Java 25 + Gradle 9.5+)
    cd mod
    gradle wrapper --gradle-version 9.5.1
    ./gradlew build
Jar: `mod/build/libs/playerlocations-1.0.0.jar` (not the -sources jar).

Targets Minecraft 26.2 with Mojang names. Version numbers are in
`mod/gradle.properties`; if Gradle can't find the Fabric API version, pick the
latest 26.2 one from https://fabricmc.net/develop. If the build reports an
error in the Java file, Minecraft 26.x renamed something: send me the error.

## 3. Install on Aternos
- set the server software to Fabric, Minecraft 26.2.
- Mods tab: install "Fabric API", and upload `playerlocations-1.0.0.jar`.
- Start the server once; it creates `config/playerlocations.properties`
  (Files tab). Set:
    url=https://my-map.onrender.com/update
    secret=<same SECRET as the website>
- Restart the server and open your website.

Free hosts may sleep when idle; the first page load can take ~30s.
