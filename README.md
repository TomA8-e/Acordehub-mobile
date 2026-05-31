# Acorde Hub

Aplicacion Android para musicos: registro/login con Firebase, perfil musical, artistas favoritos desde Spotify y publicacion basica de proyectos.

## Requisitos

- Android Studio
- JDK compatible con Android Gradle Plugin
- Proyecto Firebase configurado con `app/google-services.json` local
- Credenciales de Spotify Developer

## Configuracion local

El archivo `local.properties` no se sube al repositorio. Ademas del `sdk.dir`, debe incluir:

```properties
spotify.clientId=TU_CLIENT_ID
spotify.clientSecret=TU_CLIENT_SECRET
```

El archivo `app/google-services.json` tampoco se sube al repositorio. Descargalo desde Firebase Console y colocalo en `app/` antes de compilar en una maquina nueva.

## Compilar APK debug

```powershell
.\gradlew.bat assembleDebug
```

La APK queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```
