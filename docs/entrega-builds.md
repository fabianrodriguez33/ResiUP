# Builds de entrega: firma, versión y verificación

Guía del equipo para generar los entregables E3 (Android) y E4 (iOS). Ninguna contraseña se escribe aquí ni en el informe.

## 1. Versión

| Dato | Android | iOS |
|---|---|---|
| Identificador | `pe.upeu.resi.resiup` | el de `iosApp/Configuration/Config.xcconfig` |
| versionName | 1.0.0 | 1.0.0 (`MARKETING_VERSION`) |
| versionCode / build | 1 | 1 (`CURRENT_PROJECT_VERSION`) |
| Minificación | R8 activado (`isMinifyEnabled = true`) | — |

## 2. Crear el almacén de claves (una sola vez)

Se crea fuera del repositorio. La carpeta `claves/`, los `.jks` y `keystore.properties` están en `.gitignore`.

```
mkdir ..\claves
keytool -genkeypair -v -keystore ..\claves\resiup.jks -storetype JKS -keyalg RSA -keysize 2048 -validity 10000 -alias resiup
```

`keytool` pide la contraseña del almacén, la de la clave y los datos del titular. Guardar una copia del `.jks` fuera de la PC: si se pierde, no se puede publicar una actualización.

Luego crear `keystore.properties` en la raíz de ResiUP:

```
storeFile=../claves/resiup.jks
storePassword=<la del almacén>
keyAlias=resiup
keyPassword=<la de la clave>
```

Sin ese archivo la versión de entrega compila, pero queda sin firmar (`androidApp-release-unsigned.apk`).

## 3. Generar

```
./gradlew :androidApp:bundleRelease     # androidApp/build/outputs/bundle/release/androidApp-release.aab
./gradlew :androidApp:assembleRelease   # androidApp/build/outputs/apk/release/androidApp-release.apk
```

Tarda unos 10 minutos por R8. Renombrar para la entrega: `Equipo<N>_ProyectoFinal_Android.aab`.

## 4. Verificar la firma

```
apksigner verify --print-certs androidApp\build\outputs\apk\release\androidApp-release.apk
keytool -list -v -keystore ..\claves\resiup.jks -alias resiup
```

La huella SHA-256 de ambos comandos debe coincidir. Esa huella, el alias y la fecha de caducidad van en la ficha F-05 del informe.

## 5. Dirección del servicio en la versión de entrega

La versión de entrega solo acepta HTTPS (`resi.apiUrlRelease` en `local.properties`). El tráfico sin cifrar está permitido únicamente en depuración. Para la sustentación hay que decidir cómo llega el teléfono al servicio (ver `PENDIENTES.md`).

## 6. Reglas de R8

`androidApp/proguard-rules.pro` conserva lo que se crea por reflexión: SQLCipher, la base de WorkManager, los registradores de ML Kit, los inicializadores y los `Worker`. Sin ellas la app firmada se cierra al abrir (comprobado el 2026-11-16).
