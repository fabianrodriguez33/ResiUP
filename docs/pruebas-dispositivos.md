# Pruebas en dispositivos reales

Formato F-06 y F-07 del curso. Mínimo tres dispositivos distintos: dos Android y uno iOS, de modelos o versiones diferentes. El recorrido se ejecuta completo en cada uno, instalando el archivo firmado y con el entorno de desarrollo desconectado.

Una fila vacía se deja con su motivo escrito; no se borra.

## Matriz

| Dispositivo y versión del sistema | Plataforma | Build instalado | Pasos ejecutados | Resultado | Responsable y fecha |
|---|---|---|---|---|---|
| _pendiente_ | Android | 1.0.0 (1) | | | |
| _pendiente_ | Android | 1.0.0 (1) | | | |
| _pendiente_ | iOS | | | | |

## Recorrido (ocho pasos)

| N.º | Paso | Cómo se hace en ResiUP |
|---|---|---|
| 1 | Instalación desde el archivo firmado, sin el entorno de desarrollo conectado | Copiar el `.apk` al teléfono, permitir orígenes desconocidos e instalar. Sin cable ni Android Studio. |
| 2 | Inicio de sesión correcto e incorrecto | Entrar con un residente de prueba; antes, probar una clave equivocada y leer el mensaje. |
| 3 | CRUD completo, con un rechazo de validación | Residente: solicitar un permiso, verlo en la lista, abrir el detalle y cancelarlo. Provocar un rechazo (por ejemplo, retorno anterior a la salida). |
| 4 | Operación sin el privilegio necesario | Con el residente, intentar una acción del pastor; el servidor responde 403 y la sesión sigue abierta. |
| 5 | Modo avión: abrir, leer y crear un registro | Activar modo avión, abrir la app, ver inicio y asistencias, y solicitar un permiso: queda «Pendiente de envío». |
| 6 | Recuperar la conexión y sincronizar | Quitar el modo avión; el permiso se envía solo y aparece una única vez. |
| 7 | Permiso: conceder, denegar y denegar para siempre | Pastor o garita: abrir el escáner y recorrer los tres estados del permiso de cámara. |
| 8 | Cerrar sesión y reabrir la aplicación | Cerrar sesión, cerrar la app, abrirla: pide iniciar sesión y no muestra datos del usuario anterior. |

## Registro de defectos

Severidad: alta si impide el recorrido, media si lo estorba, baja si es cosmética. Estado: corregido, pendiente o descartado (con motivo).

| Id. | Dispositivo | Pasos para reproducir | Esperado / obtenido | Sev. | Estado |
|---|---|---|---|---|---|
| D-01 | Emulador Pixel 8 (no cuenta como dispositivo real) | Residente → Perfil → desplazar hasta el final | Se espera ver «Cerrar sesión» completo / queda cortado detrás de la barra flotante | Baja | Pendiente |
