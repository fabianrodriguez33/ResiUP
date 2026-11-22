package pe.upeu.resi.resiup.domain.model

import kotlinx.serialization.Serializable

// Datos de administración (residentes, habitaciones, autorizaciones y semestres).
@Serializable data class HabAdmin(val id: Long, val numero: String, val capacidad: Int, val activa: Boolean = true, val ocupantes: Long = 0, val disponibles: Long = 0)
@Serializable data class HabResumenAdmin(val id: Long, val numero: String, val cama: String? = null)
@Serializable data class ResidenteAdmin(val id: Long, val codigo: String, val nombreCompleto: String, val telefono: String? = null, val activo: Boolean = true, val habitacion: HabResumenAdmin? = null)
@Serializable data class AutorizacionAdmin(val id: Long, val tipo: String, val inicio: String, val fin: String? = null, val referencia: String, val revocada: Boolean = false, val motivoRevocacion: String? = null, val vigente: Boolean = false)
@Serializable data class NuevoResidenteAdmin(val username: String, val password: String, val nombreCompleto: String, val codigo: String, val telefono: String, val email: String, val carrera: String? = null, val ciclo: Int? = null)
@Serializable data class AsignacionAdmin(val habitacionId: Long, val cama: String?)
@Serializable data class NuevaAutorizacionAdmin(val tipo: String, val referencia: String)
@Serializable data class RevocacionAdmin(val motivo: String)
@Serializable data class SemestreAdmin(val id: Long, val nombre: String, val inicio: String, val fin: String, val activo: Boolean = false)
@Serializable data class OcupanteAdmin(val residenteId: Long, val codigo: String, val nombreCompleto: String, val cama: String? = null)
@Serializable data class NuevoSemestreAdmin(val nombre: String, val inicio: String, val fin: String)

/** Cuenta del personal (pastor o monitor, garita, administración). El rol «PRECEPTOR» es el de pastor o monitor. */
@Serializable data class UsuarioAdmin(val id: Long, val username: String, val nombreCompleto: String, val rol: String, val activo: Boolean = true)
@Serializable data class NuevoUsuarioAdmin(val username: String, val password: String, val nombreCompleto: String, val rol: String)
