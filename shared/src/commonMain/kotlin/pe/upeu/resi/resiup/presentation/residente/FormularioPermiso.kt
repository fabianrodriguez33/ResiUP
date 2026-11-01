package pe.upeu.resi.resiup.presentation.residente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import pe.upeu.resi.resiup.domain.error.ErrorApi
import pe.upeu.resi.resiup.domain.model.CORTO_MAX_MINUTOS
import pe.upeu.resi.resiup.domain.model.NuevaSolicitud
import pe.upeu.resi.resiup.domain.model.SolicitudPermiso
import pe.upeu.resi.resiup.domain.model.TipoPermiso
import pe.upeu.resi.resiup.domain.repository.ConsultasResidenteRepository
import pe.upeu.resi.resiup.domain.repository.PermisoRepository
import pe.upeu.resi.resiup.presentation.components.CampoFechaHora
import pe.upeu.resi.resiup.presentation.components.CampoHora
import pe.upeu.resi.resiup.presentation.components.ResiCampoTexto
import pe.upeu.resi.resiup.presentation.components.Segmentos
import pe.upeu.resi.resiup.presentation.theme.Espacio
import pe.upeu.resi.resiup.presentation.util.ZonaResidencia
import pe.upeu.resi.resiup.presentation.util.ahora
import pe.upeu.resi.resiup.presentation.util.aErrorApi
import pe.upeu.resi.resiup.presentation.util.conHoraLima
import pe.upeu.resi.resiup.presentation.util.formatoLima

/**
 * Estado del formulario de permiso. Los campos siguen las hojas físicas de la residencia:
 * - Corto: lugar, motivo, fecha y hora de salida, hora de ingreso (el mismo día, máximo 2 horas).
 * - Largo: lugar, motivo, fecha y hora de salida y de ingreso.
 * - Pernoctar fuera: además, responsable de la casa, su celular y con quién viaja.
 */
data class SolicitudFormState(
    val tipo: TipoPermiso = TipoPermiso.Corto,
    val motivo: String = "",
    val destino: String = "",
    val salida: Instant? = null,
    val retorno: Instant? = null,
    val residenteId: Long? = null,
    val responsableNombre: String = "",
    val responsableCelular: String = "",
    val acompanante: String = "",
    val errores: Map<String, String> = emptyMap(),
    val enviando: Boolean = false,
    val error: ErrorApi? = null,
    val creada: SolicitudPermiso? = null,
)

/** Lógica común del formulario; el residente y el padre/madre solo cambian a quién se envía y qué tipos puede elegir. */
abstract class PermisoFormViewModel(
    val tiposPermitidos: List<TipoPermiso>,
    private val exigeHijo: Boolean,
    private val enviarSolicitud: suspend (NuevaSolicitud) -> Result<SolicitudPermiso>,
    private val alCrear: () -> Unit = {},
) : ViewModel() {
    protected val _estado = MutableStateFlow(SolicitudFormState(tipo = tiposPermitidos.first()))
    val estado: StateFlow<SolicitudFormState> = _estado.asStateFlow()

    fun tipo(v: TipoPermiso) = _estado.update {
        // Un corto vuelve el mismo día: si el ingreso quedó en otra fecha, se vuelve a pedir.
        val retorno = if (v == TipoPermiso.Corto && it.salida != null && it.retorno != null && !mismoDia(it.salida, it.retorno)) null else it.retorno
        it.copy(tipo = v, retorno = retorno, errores = emptyMap(), error = null)
    }
    fun hijo(id: Long) = _estado.update { it.copy(residenteId = id, errores = it.errores - "residenteId", error = null) }
    fun motivo(v: String) = _estado.update { it.copy(motivo = v.take(300), errores = it.errores - "motivo", error = null) }
    fun destino(v: String) = _estado.update { it.copy(destino = v.take(200), errores = it.errores - "destino", error = null) }
    fun responsableNombre(v: String) = _estado.update { it.copy(responsableNombre = v.take(150), errores = it.errores - "responsableNombre", error = null) }
    fun responsableCelular(v: String) = _estado.update { it.copy(responsableCelular = v.take(30), errores = it.errores - "responsableCelular", error = null) }
    fun acompanante(v: String) = _estado.update { it.copy(acompanante = v.take(150), error = null) }

    fun salida(v: Instant) = _estado.update {
        // En un corto, al cambiar el día de salida el ingreso sigue a la misma hora de ese día.
        val retorno = if (it.tipo == TipoPermiso.Corto && it.retorno != null) {
            val h = it.retorno.toLocalDateTime(ZonaResidencia)
            v.conHoraLima(h.hour, h.minute)
        } else it.retorno
        it.copy(salida = v, retorno = retorno, errores = it.errores - "salida" - "retorno", error = null)
    }

    fun retorno(v: Instant) = _estado.update { it.copy(retorno = v, errores = it.errores - "retorno", error = null) }

    /** Valida lo básico en el cliente; las reglas reales (anticipación, superposición, límites) las aplica el servidor. */
    fun enviar() {
        val s = _estado.value
        if (s.enviando) return
        val errores = validar(s)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        _estado.update { it.copy(enviando = true, error = null, errores = emptyMap()) }
        val pernocta = s.tipo == TipoPermiso.Pernocte
        viewModelScope.launch {
            enviarSolicitud(
                NuevaSolicitud(
                    s.tipo, s.motivo.trim(), s.destino.trim(), s.salida!!, s.retorno!!, if (exigeHijo) s.residenteId else null,
                    if (pernocta) s.responsableNombre.trim() else null, if (pernocta) s.responsableCelular.trim() else null,
                    if (pernocta) s.acompanante.trim().ifBlank { "Nadie" } else null,
                ),
            )
                .onSuccess { c -> alCrear(); _estado.update { it.copy(enviando = false, creada = c) } }
                .onFailure { e ->
                    val err = e.aErrorApi()
                    _estado.update {
                        if (err is ErrorApi.Validacion) it.copy(enviando = false, errores = err.porCampo)
                        else it.copy(enviando = false, error = err)
                    }
                }
        }
    }

    /** Deja el formulario en blanco para enviar otro permiso. */
    fun reiniciar() = _estado.update { SolicitudFormState(tipo = tiposPermitidos.first(), residenteId = it.residenteId) }

    private fun validar(s: SolicitudFormState): Map<String, String> = buildMap {
        if (exigeHijo && s.residenteId == null) put("residenteId", "Elige para quién es el permiso")
        if (s.destino.isBlank()) put("destino", "Indica el lugar")
        if (s.motivo.isBlank()) put("motivo", "Indica el motivo")
        if (s.tipo == TipoPermiso.Pernocte) {
            if (s.responsableNombre.isBlank()) put("responsableNombre", "Indica quién recibe en esa casa")
            if (s.responsableCelular.count { it.isDigit() } < 6) put("responsableCelular", "Indica un celular válido")
        }
        if (s.salida == null) put("salida", "Elige fecha y hora de salida")
        else if (s.salida < ahora().minus(Duration.parse("5m"))) put("salida", "La salida no puede estar en el pasado")
        if (s.retorno == null) put("retorno", if (s.tipo == TipoPermiso.Corto) "Elige la hora de ingreso" else "Elige fecha y hora de ingreso")
        else if (s.salida != null && s.retorno <= s.salida) put("retorno", "El ingreso debe ser después de la salida")
        else if (s.salida != null && s.tipo == TipoPermiso.Corto && (s.retorno - s.salida).inWholeMinutes > CORTO_MAX_MINUTOS) {
            put("retorno", "Un permiso corto dura máximo 2 horas. Si necesitas más tiempo, elige «Largo».")
        } else if (s.salida != null && s.tipo == TipoPermiso.Pernocte && mismoDia(s.salida, s.retorno)) {
            put("retorno", "Para pernoctar fuera el ingreso debe ser otro día.")
        }
    }

    private fun mismoDia(a: Instant, b: Instant) = a.toLocalDateTime(ZonaResidencia).date == b.toLocalDateTime(ZonaResidencia).date
}

/** Formulario del residente: solo corto y largo (el pernoctar lo envía su padre/madre). */
class SolicitudFormViewModel(
    repo: PermisoRepository,
    consultas: ConsultasResidenteRepository? = null,
    alCrear: () -> Unit = {},
) : PermisoFormViewModel(listOf(TipoPermiso.Corto, TipoPermiso.Largo), false, { repo.crear(it) }, alCrear) {
    private val _privilegio = MutableStateFlow<Boolean?>(null)

    /** Verdadero si el residente tiene el privilegio de enviar sus permisos sin pasar antes por su padre/madre (null = aún no se sabe). */
    val puedeSolicitarSolo: StateFlow<Boolean?> = _privilegio.asStateFlow()

    init {
        consultas?.let { c -> viewModelScope.launch { c.perfil().collect { _privilegio.value = it?.puedeSolicitarSolo } } }
    }
}

/** Campos del formulario según el tipo de permiso; los comparten el residente y el padre/madre. */
@Composable
fun CamposPermiso(s: SolicitudFormState, vm: PermisoFormViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Espacio.m)) {
        if (vm.tiposPermitidos.size > 1) {
            Text("Tipo de permiso", style = MaterialTheme.typography.labelMedium)
            Segmentos(vm.tiposPermitidos, s.tipo, { it.etiqueta }, vm::tipo)
            Text(
                when (s.tipo) {
                    TipoPermiso.Corto -> "Corto: sales y vuelves el mismo día, hasta 2 horas."
                    TipoPermiso.Largo -> "Largo: más de 2 horas."
                    TipoPermiso.Pernocte -> "Pernoctar fuera: duerme en otra casa. Se indica quién lo recibe."
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ResiCampoTexto(s.destino, vm::destino, "Lugar", error = s.errores["destino"], habilitado = !s.enviando)
        if (s.tipo == TipoPermiso.Pernocte) {
            ResiCampoTexto(s.responsableNombre, vm::responsableNombre, "Responsable de la casa a donde va", error = s.errores["responsableNombre"], habilitado = !s.enviando)
            ResiCampoTexto(
                s.responsableCelular, vm::responsableCelular, "Celular del responsable", error = s.errores["responsableCelular"],
                tipoTeclado = KeyboardType.Phone, habilitado = !s.enviando,
            )
        }
        ResiCampoTexto(s.motivo, vm::motivo, "Motivo", error = s.errores["motivo"], habilitado = !s.enviando)
        CampoFechaHora(if (s.tipo == TipoPermiso.Corto) "Salida (fecha y hora)" else "Salida", s.salida, s.errores["salida"], vm::salida)
        if (s.tipo == TipoPermiso.Corto) {
            CampoHora("Hora de ingreso", s.retorno, s.salida, s.errores["retorno"], vm::retorno)
        } else {
            CampoFechaHora("Ingreso", s.retorno, s.errores["retorno"], vm::retorno)
        }
        if (s.tipo == TipoPermiso.Pernocte) {
            ResiCampoTexto(s.acompanante, vm::acompanante, "Acompañado de", ayuda = "Si va solo, déjalo vacío: se registra «Nadie».", habilitado = !s.enviando)
        }
        if (s.tipo != TipoPermiso.Corto && s.retorno != null && s.salida != null) {
            Text("Del ${s.salida.formatoLima()} al ${s.retorno.formatoLima()}", Modifier.padding(start = Espacio.xs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
