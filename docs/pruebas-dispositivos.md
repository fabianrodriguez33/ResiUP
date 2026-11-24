# Pruebas en dispositivos reales

Tres dispositivos Android distintos (modelo y versión de Android diferentes; conviene que uno sea antiguo o de gama baja). El recorrido de ocho pasos se ejecuta completo en cada uno, instalando el APK firmado entregado por el líder, con el entorno de desarrollo desconectado. No se anota ningún resultado que no se haya ejecutado: lo no probado queda como «no probado» con su motivo.

## Matriz

En la columna Recorrido se anota cada paso como OK, FALLA (con el id del defecto) o NO PROBADO (con motivo).

| Dispositivo | Sistema | Recorrido | Defectos | Probado por |
|---|---|---|---|---|
| Samsung Galaxy A56 5G (SM-A566E) | Android _pendiente_ | 1 OK · 2 OK · 3 NO PROBADO (la cuenta de residente de prueba está inhabilitada para crear permisos hasta el 28/11/2026 por faltas a cultos; «Crear permiso» sale deshabilitado) · 4 _ · 5 _ · 6 _ · 7 _ · 8 _ | | Jean-Pierre Miranda, 22/11/2026 |
| _pendiente_ | Android _pendiente_ | 1 _ · 2 _ · 3 _ · 4 _ · 5 _ · 6 _ · 7 _ · 8 _ | | |
| _pendiente_ | Android _pendiente_ | 1 _ · 2 _ · 3 _ · 4 _ · 5 _ · 6 _ · 7 _ · 8 _ | | |
| iPhone | iOS | Bloqueado: sin Mac (ver [docs/ios-bloqueo.md](ios-bloqueo.md)) | | |

## Recorrido (ocho pasos)

| N.º | Paso | Cómo se hace en ResiUP |
|---|---|---|
| 1 | Instalar desde el archivo entregado, sin cable ni entorno de desarrollo | Copiar el `.apk` firmado al teléfono, permitir orígenes desconocidos e instalar. Sin cable ni Android Studio. |
| 2 | Login correcto e incorrecto | Probar una clave equivocada y leer el mensaje; luego entrar con una cuenta de prueba válida. |
| 3 | CRUD completo con una operación rechazada por validación | Residente: solicitar un permiso, verlo en la lista, abrir el detalle y cancelarlo. Provocar un rechazo (retorno anterior a la salida). |
| 4 | Operación sin privilegio | Con el residente, intentar una acción del pastor: el servidor responde 403 y la sesión sigue abierta. |
| 5 | Modo avión: abrir, leer y crear un registro | Activar modo avión, abrir la app, ver inicio y asistencias y solicitar un permiso: queda «Pendiente de envío». |
| 6 | Recuperar la conexión y comprobar que sincroniza sin duplicar | Quitar el modo avión: el permiso se envía solo y aparece una única vez. |
| 7 | Permiso del sistema: concederlo, denegarlo y denegarlo definitivamente | Cuenta de garita: abrir el escáner y recorrer los tres estados del permiso de cámara. |
| 8 | Cerrar sesión y reabrir | Cerrar sesión, cerrar la app y abrirla: pide iniciar sesión y no muestra datos del usuario anterior. |

## Registro de defectos

Cada defecto lleva dispositivo, pasos para reproducir y estado. Severidad: alta si impide el recorrido, media si lo estorba, baja si es cosmética. Estado: corregido, pendiente o descartado (con motivo).

| Id. | Dispositivo | Pasos para reproducir | Esperado / obtenido | Sev. | Estado |
|---|---|---|---|---|---|
| D-01 | Emulador Pixel 8 (no cuenta como dispositivo real) | Residente → Perfil → desplazar hasta el final | Se espera ver «Cerrar sesión» completo / queda cortado detrás de la barra flotante | Baja | Pendiente |
