package pe.upeu.resi.resiup.presentation.navigation

import pe.upeu.resi.resiup.domain.model.Rol
import pe.upeu.resi.resiup.presentation.components.ItemNavegacion
import pe.upeu.resi.resiup.presentation.components.ResiIconos

object Destinos {
    const val Inicio = "inicio"
    const val Permisos = "permisos"
    const val Asistencias = "asistencias"
    const val Limpieza = "limpieza"
    const val Perfil = "perfil"
    const val Escanear = "escanear"
    const val Solicitudes = "solicitudes"
    const val Mas = "mas"
    const val Historial = "historial"
    const val Nuevo = "nuevo"
}

/** Cada rol ve únicamente las opciones que puede usar. */
fun Rol.destinos(): List<ItemNavegacion> = when (this) {
    Rol.Residente -> listOf(
        ItemNavegacion(Destinos.Inicio, "Inicio", ResiIconos.Inicio),
        ItemNavegacion(Destinos.Permisos, "Permisos", ResiIconos.Permisos),
        ItemNavegacion(Destinos.Asistencias, "Asistencias", ResiIconos.Asistencias),
        ItemNavegacion(Destinos.Limpieza, "Limpieza", ResiIconos.Limpieza),
        ItemNavegacion(Destinos.Perfil, "Perfil", ResiIconos.Perfil),
    )
    Rol.Preceptor -> listOf(
        ItemNavegacion(Destinos.Inicio, "Inicio", ResiIconos.Inicio),
        ItemNavegacion(Destinos.Escanear, "Escanear", ResiIconos.Escanear),
        ItemNavegacion(Destinos.Solicitudes, "Solicitudes", ResiIconos.Solicitudes),
        ItemNavegacion(Destinos.Limpieza, "Limpieza", ResiIconos.Limpieza),
        ItemNavegacion(Destinos.Mas, "Más", ResiIconos.Mas),
    )
    Rol.Garita -> listOf(
        ItemNavegacion(Destinos.Escanear, "Escanear", ResiIconos.Escanear),
        ItemNavegacion(Destinos.Historial, "Historial", ResiIconos.Historial),
        ItemNavegacion(Destinos.Mas, "Cuenta", ResiIconos.Perfil),
    )
    // Padres de familia: aceptan los permisos de sus hijos y envían los de pernoctar fuera.
    Rol.Apoderado -> listOf(
        ItemNavegacion(Destinos.Inicio, "Inicio", ResiIconos.Inicio),
        ItemNavegacion(Destinos.Solicitudes, "Permisos", ResiIconos.Solicitudes),
        ItemNavegacion(Destinos.Nuevo, "Enviar", ResiIconos.Permisos),
        ItemNavegacion(Destinos.Mas, "Cuenta", ResiIconos.Perfil),
    )
    // El administrador gestiona principalmente desde el panel web (pendiente de aprobación).
    Rol.Administrador -> listOf(
        ItemNavegacion(Destinos.Inicio, "Inicio", ResiIconos.Inicio),
        ItemNavegacion(Destinos.Mas, "Más", ResiIconos.Mas),
    )
}
