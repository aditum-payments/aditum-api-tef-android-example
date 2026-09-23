package br.com.aditum.paytrial.client

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** Uma entrada do rastreio: chamada ao serviço (com resposta ou erro) ou callback recebido. Tudo já censurado. */
data class TraceEntry(
    val id: Long, val kind: Kind, val method: String, val screen: String, val startedAt: Long,
    val request: String? = null, val response: String? = null, val error: String? = null, val durationMs: Long? = null,
) {
    enum class Kind { CALL, CALLBACK }
    val running: Boolean get() = kind == Kind.CALL && durationMs == null
    val failed: Boolean get() = error != null
}

/**
 * Rastreio da sessão: a fonte do chip do inspetor, das abas Requisição/Resposta e do Console. Guarda as últimas
 * [capacity] entradas, só em memória. A censura acontece antes de chegar aqui ([ContractJson.redacted]).
 */
class TraceLog(private val capacity: Int = 500) {
    private val seq = AtomicLong()
    private val _entries = MutableStateFlow<List<TraceEntry>>(emptyList())
    val entries: StateFlow<List<TraceEntry>> get() = _entries

    fun begin(method: String, screen: String, request: String?): Long {
        val e = TraceEntry(seq.incrementAndGet(), TraceEntry.Kind.CALL, method, screen, System.currentTimeMillis(), request = request)
        _entries.update { (it + e).takeLast(capacity) }; return e.id
    }
    fun end(id: Long, response: String?, error: String?) = _entries.update { list ->
        list.map { if (it.id == id) it.copy(response = response, error = error, durationMs = System.currentTimeMillis() - it.startedAt) else it }
    }
    fun callback(method: String, screen: String, payload: String?) {
        val e = TraceEntry(seq.incrementAndGet(), TraceEntry.Kind.CALLBACK, method, screen, System.currentTimeMillis(), response = payload, durationMs = 0)
        _entries.update { (it + e).takeLast(capacity) }
    }
    fun clear() { _entries.value = emptyList() }
    fun find(id: Long): TraceEntry? = _entries.value.firstOrNull { it.id == id }

    /** A sessão inteira em JSON, para anexar num chamado (já censurada). */
    fun export(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
        return JSONObject().put("app", "Aditum Pay Trial").put("exportedAt", fmt.format(Date())).put("entries", JSONArray(_entries.value.map { e ->
            JSONObject().put("id", e.id).put("kind", e.kind.name).put("method", e.method).put("screen", e.screen).put("at", fmt.format(Date(e.startedAt)))
                .put("durationMs", e.durationMs ?: JSONObject.NULL).put("request", e.request?.let(::parse) ?: JSONObject.NULL)
                .put("response", e.response?.let(::parse) ?: JSONObject.NULL).put("error", e.error ?: JSONObject.NULL)
        })).toString(2)
    }
    private fun parse(s: String): Any = runCatching { if (s.trimStart().startsWith("[")) JSONArray(s) else JSONObject(s) }.getOrDefault(s)
}
