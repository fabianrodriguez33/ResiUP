# ResiUP (aplicación móvil)

Aplicación de la Residencia Universitaria Fernando Stahl (UPeU), desarrollada con **Kotlin Multiplatform** y **Compose Multiplatform**.
Arquitectura: Clean Architecture + MVVM · Ktor Client · Koin · Kotlinx Serialization · StateFlow · SQLDelight (copia local).

Plataformas:
- **Android**: verificado (compilación, pruebas unitarias y lint).
- **iOS**: **sin verificar**. El código Kotlin de iOS compila desde Windows, pero el enlace del framework se omite porque exige macOS con Xcode, y el equipo no tiene un Mac disponible. Por eso no se ha probado en simulador ni en dispositivo.

## Requisitos

- **JDK 17 o superior** (probado con JDK 21). Gradle 9.5.1 lo descarga el wrapper; el proyecto compila a bytecode Java 11.
- **Android SDK** con los niveles del proyecto (compileSdk 37, minSdk 24). Instalarlo desde Android Studio, o definir la ruta en `local.properties` con `sdk.dir=...` o con la variable `ANDROID_HOME`.
- **API ResiAPI** corriendo en local (ver su README), o una URL de API accesible desde el teléfono o el emulador.
- Para iOS (no verificado): macOS con Xcode. Ver la nota de plataformas arriba.

## Cómo arrancar

1. Levantar ResiAPI (ver su README) y su base de datos.
2. Configurar la URL de la API en `local.properties` (no versionado):
   ```properties
   # Emulador (por defecto, si no se define nada): http://10.0.2.2:8080/
   # Opción más fiable en emulador o teléfono por USB:  adb reverse tcp:8080 tcp:8080
   resi.apiUrl=http://localhost:8080/
   # Teléfono por Wi‑Fi: IP de la PC en la red local y puerto 8080 abierto en el firewall
   # resi.apiUrl=http://192.168.1.50:8080/
   # Producción (HTTPS obligatorio):
   # resi.apiUrlRelease=https://api.ejemplo.pe/
   ```
3. Compilar e instalar la versión debug en un emulador o teléfono conectado:
   ```bash
   ./gradlew :androidApp:installDebug
   ```

Si el sistema no conserva el permiso de ejecución de `gradlew`, usar `sh ./gradlew ...` en su lugar.

HTTP en texto plano sólo está permitido en el build **debug**; release exige HTTPS.
En debug el login muestra una *vista previa de roles* (sólo navegación, sin sesión) y un catálogo de componentes.

## Pruebas

Pruebas unitarias de `shared` (lógica, repositorios con cliente simulado y copia local) y lint de Android:
```bash
./gradlew :shared:testAndroidHostTest :androidApp:lintDebug
```

Pruebas de interfaz (Compose), que requieren un emulador o teléfono conectado:
```bash
./gradlew :shared:connectedAndroidDeviceTest
```
Las pruebas de interfaz usan su propio conjunto de fuentes (`androidDeviceTest`) porque los nombres de prueba con espacios de `commonTest` no se pueden compilar a DEX en dispositivos.

Cobertura (Kover), para el informe que lee SonarQube:
```bash
./gradlew :shared:koverXmlReport
```
El informe queda en `shared/build/reports/kover/report.xml`.

## Servidor de producción

La API desplegada está en Render (plan gratuito), con el blueprint `render.yaml` de ResiAPI. En el plan gratuito la API se duerme tras unos 15 minutos sin uso y tarda cerca de 1 minuto en despertar, así que la primera petición puede demorar. Las fotos subidas se pierden al reiniciar.
