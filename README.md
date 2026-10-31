# ResiUP (aplicación móvil)

Kotlin Multiplatform + Compose Multiplatform (Android verificado; iOS sin verificar por falta de Mac).
Arquitectura: Clean Architecture + MVVM · Ktor Client · Koin · Kotlinx Serialization · StateFlow.

## Ejecutar contra la API local

1. Levantar ResiAPI (ver su README) y PostgreSQL.
2. Elegir cómo llega la app a la API; en `local.properties` (no versionado):
   ```properties
   # Emulador (por defecto, si no se define nada): http://10.0.2.2:8080/
   # Opción más fiable en emulador o teléfono por USB:  adb reverse tcp:8080 tcp:8080
   resi.apiUrl=http://localhost:8080/
   # Teléfono por Wi‑Fi: IP de la PC en la red local y puerto 8080 abierto en el firewall
   # resi.apiUrl=http://192.168.1.50:8080/
   # Producción (HTTPS obligatorio):
   # resi.apiUrlRelease=https://api.ejemplo.pe/
   ```
3. Compilar e instalar: `./gradlew :androidApp:installDebug`

HTTP en texto plano sólo está permitido en el build **debug**; release exige HTTPS.
En debug el login muestra una *vista previa de roles* (sólo navegación, sin sesión) y un catálogo de componentes.

## Pruebas
```bash
./gradlew :shared:testAndroidHostTest :androidApp:lintDebug
```
Pruebas de interfaz (Compose, requieren un emulador o teléfono conectado): `./gradlew :shared:connectedAndroidDeviceTest`.
Las pruebas de interfaz usan su propio conjunto de fuentes (`androidDeviceTest`) porque los nombres de prueba con espacios de `commonTest` no se pueden compilar a DEX en dispositivos.
