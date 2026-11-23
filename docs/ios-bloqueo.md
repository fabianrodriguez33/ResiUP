# iOS: bloqueo y evidencia

Fecha: 2026-11-22. Responsable: Jean-Pierre Miranda.

## Situación

La aplicación no se probó ni se firmó para iOS. El equipo no tiene un Mac. Por eso la fila iOS de [pruebas-dispositivos.md](pruebas-dispositivos.md) queda como «Bloqueado» y no se anota ningún paso como ejecutado.

## Causa

Compilar, firmar e instalar una app iOS exige macOS con Xcode. Ningún integrante del equipo dispone de un Mac; el desarrollo se hace en Windows.

- `xcodebuild` y el simulador de iOS solo existen en macOS.
- Kotlin/Native no enlaza el framework `Shared` de iOS desde Windows: la tarea de enlace se omite.
- Instalar en un iPhone real requiere además un equipo de desarrollo de Apple (`TEAM_ID`) para firmar. En `iosApp/Configuration/Config.xcconfig` el valor está vacío (`TEAM_ID=`).

## Evidencia

Comando ejecutado en Windows, sobre `develop`:

```
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

| Tarea | Resultado |
|---|---|
| `:shared:compileKotlinIosSimulatorArm64` | Termina sin errores (solo advertencias): el código Kotlin de `iosMain` compila |
| `:shared:linkDebugFrameworkIosSimulatorArm64` | `SKIPPED`: Gradle no enlaza el framework fuera de macOS |
| Compilación Xcode de `iosApp` | No ejecutable sin Mac |

Qué existe en el repositorio y qué no se pudo comprobar:

- Existe el proyecto `iosApp/iosApp.xcodeproj` y la configuración `Config.xcconfig` (`MARKETING_VERSION=1.0.0`, `CURRENT_PROJECT_VERSION=1`).
- Existen las implementaciones iOS de lo específico de plataforma: `AlmacenSeguroIos`, `Platform.ios`, `PlatformModule.ios`, `Plataforma.ios` y `MainViewController`.
- El compilador avisa de un punto a revisar: `AlmacenSeguroIos.kt:55`, «This cast can never succeed». No se ejecutó, así que no se sabe si falla en un dispositivo.
- No se verificó el arranque de la app, el login, la cámara ni el almacén seguro en iOS.

## Qué falta para cerrarlo

1. Un Mac con Xcode (propio, prestado o de la universidad) y un iPhone, o al menos el simulador.
2. Completar `TEAM_ID` con un equipo de Apple (una cuenta gratuita sirve para instalar en un dispositivo propio; distribuir exige cuenta de pago).
3. Compilar con Xcode, instalar y recorrer los ocho pasos de [pruebas-dispositivos.md](pruebas-dispositivos.md).
4. Revisar el aviso de `AlmacenSeguroIos.kt:55` antes de la prueba.

## Alcance de esta evidencia

Solo demuestra que el código compartido compila para iOS y que el enlace no se puede hacer desde Windows. No demuestra que la app funcione en iOS.
