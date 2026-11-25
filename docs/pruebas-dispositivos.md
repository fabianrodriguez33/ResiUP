# Pruebas en dispositivos reales

Tres dispositivos Android distintos (modelo y versión de Android diferentes; conviene que uno sea antiguo o de gama baja). El recorrido de ocho pasos se ejecuta completo en cada uno, instalando el APK firmado entregado por el líder, con el entorno de desarrollo desconectado. No se anota ningún resultado que no se haya ejecutado: lo no probado queda como «no probado» con su motivo.

## Matriz

En la columna Recorrido se anota cada paso como OK, FALLA (con el id del defecto) o NO PROBADO (con motivo).

| Dispositivo | Sistema | Recorrido | Defectos | Probado por |
|---|---|---|---|---|
| Samsung Galaxy A12 | Android 13 | 1 OK · 2 OK · 3 OK · 4 OK · 5 OK · 6 OK · 7 OK · 8 OK | D-03, D-08, D-09, D-10, D-11, D-12, D-15, D-16, D-19 | Julio Fabián Rodríguez, 24/11/2026 |
| Samsung Galaxy A56 5G (SM-A566E) | Android 16 | 1 OK · 2 OK · 3 OK · 4 OK · 5 OK · 6 OK · 7 OK · 8 OK | D-08, D-09, D-10, D-11, D-13, D-14, D-17, D-18, D-19 | Jean-Pierre Miranda, 24/11/2026 |
| Redmi 9C | Android 10 | 1 OK · 2 OK · 3 OK · 4 OK · 5 OK · 6 OK · 7 OK · 8 OK | D-08, D-10, D-11 | Pedro Ricardo Alva Pardave, 24/11/2026 |
| iPhone | iOS | Bloqueado: sin Mac (ver [docs/ios-bloqueo.md](ios-bloqueo.md)) | | |

Los tres equipos instalaron el APK firmado. Roles usados en las pruebas por rol: A12, residente (permiso propio y con aprobación de los padres); A56, pastor; Redmi 9C, garita y apoderado.

Paso 4: la aplicación no ofrece al residente acciones de otro rol; la respuesta 403 se cubre con las pruebas automáticas del servicio.

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
| D-03 | Samsung A12 | Residente: crear un permiso y luego intentar crear otro | Se abre un formulario nuevo / no deja crear otro permiso | Media | Corregido en el código; falta reprobar con el APK final |
| D-08 | Todos (A12, A56 y Redmi 9C) | Ingresar un número de celular | Solo acepta 9 dígitos que empiecen con 9 / no lo valida | Baja | Corregido en el código; falta reprobar con el APK final |
| D-09 | A56 (pastor) y A12 (residente) | Pastor: eliminar un culto y revisar la lista en ambos roles | Desaparece al instante / sigue visible hasta cambiar de día y volver; el residente lo sigue viendo | Media | Corregido en el código; falta reprobar con el APK final |
| D-10 | Todos (A12, A56 y Redmi 9C) | Perfil: verificar un correo @upeu.edu.pe o @gmail.com | Llega el correo y pueden verificar el usuario o el administrador / no llega el correo | Media | Pendiente: correo SMTP ya configurado en Render; falta probar la llegada del código y el botón de verificación del administrador |
| D-11 | Todos (A12, A56 y Redmi 9C) | Verificar el correo y luego cambiar datos en el perfil | Pasa a la pantalla de edición / se queda en el perfil y el aviso aparece al volver a «Editar datos» | Baja | Corregido en el código; falta reprobar con el APK final |
| D-12 | Samsung A12 | Residente: cancelar su reserva e intentar reservar de nuevo | Permite reservar otra vez / no deja entrar | Alta | Corregido en el código; falta reprobar con el APK final |
| D-13 | Samsung A56 | Residente reserva; pastor abre Reservas antes y después de cerrarlas | El pastor la ve de inmediato y con detalle / solo aparece al cerrar y sin detalle | Alta | Corregido en el código; falta reprobar con el APK final |
| D-14 | Samsung A56 | Pastor: exportar cultos y reservas | Genera el archivo / no exporta | Media | Corregido en el código; falta reprobar con el APK final |
| D-15 | Samsung A12 | Residente: mirar el panel de permisos tras un cambio de estado y deslizar para actualizar | El estado cambia al instante o al deslizar / no cambia | Media | Corregido en el código; falta reprobar con el APK final |
| D-16 | Samsung A12 | Residente: revisar Cultos después de registrar su ingreso | Ve su asistencia registrada / no aparece | Media | Corregido en el código; falta reprobar con el APK final |
| D-17 | Samsung A56 | Pastor: eliminar la asistencia de un residente | Permite eliminarla / no se puede | Media | Corregido en el código; falta reprobar con el APK final |
| D-18 | Samsung A56 | Pastor: eliminar una reserva | Permite eliminarla / no se puede | Media | Corregido en el código; falta reprobar con el APK final |
| D-19 | A56 (pastor) y A12 (residente) | Pastor evalúa un cuarto; sus residentes consultan la nota | La ven al instante / solo se ve cuando están evaluados todos los cuartos | Media | Corregido en el código; falta reprobar con el APK final |

## Sugerencias de mejora

No son defectos; salieron durante las pruebas.

- Buscar por nombre o código en cultos, permisos e historial (pastor, monitor y garita).
- Al tocar una notificación, ir a la sección correspondiente.
- Limpieza: registrar la evaluación al final, cuando todos los del cuarto tengan nota, y señalar dónde falta.
- Usar en la solicitud de permisos el mismo calendario del resto de la aplicación.
- Apoderado: quitar «Mi perfil» de los accesos.
- Mostrar los permisos de los últimos dos días y un historial con todo el ciclo.
