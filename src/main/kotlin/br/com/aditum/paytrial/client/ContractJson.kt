package br.com.aditum.paytrial.client

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Qualquer objeto do contrato App2App (parcelables de `br.com.aditum.*`) vira JSON com os **nomes de campo do AIDL**,
 * por reflexão sobre as propriedades — o que foi e voltou pelo serviço, sem mapeamento à mão que envelheça quando o
 * contrato ganhar campo. Enums saem pelo nome; datas em ISO local. Sempre passa pelo [Redactor].
 */
object ContractJson {
    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    fun redacted(value: Any?): String = when (val j = Redactor.json(toJson(value))) {
        is JSONObject -> j.toString(2)
        is JSONArray -> j.toString(2)
        null, JSONObject.NULL -> "null"
        is String -> JSONObject.quote(j)
        else -> j.toString()
    }

    fun toJson(value: Any?): Any? = when (value) {
        null -> JSONObject.NULL
        is String, is Boolean, is Int, is Long, is Double, is Float, is Short, is Byte -> value
        is Char -> value.toString()
        is ByteArray -> "<${value.size} bytes>"   // logo do getMerchantCustomization: o tamanho basta no rastreio
        is Enum<*> -> value.name
        is Date -> synchronized(iso) { iso.format(value) }
        is Collection<*> -> JSONArray(value.map { toJson(it) })
        is Array<*> -> JSONArray(value.map { toJson(it) })
        is Map<*, *> -> JSONObject().apply { value.forEach { (k, v) -> put(k.toString(), toJson(v)) } }
        else -> objectToJson(value)
    }

    private fun objectToJson(o: Any): Any {
        if (!o.javaClass.name.startsWith("br.com.aditum")) return o.toString()
        val json = JSONObject()
        var c: Class<*>? = o.javaClass
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (Modifier.isStatic(f.modifiers) || f.isSynthetic || '$' in f.name) continue
                runCatching { f.isAccessible = true; json.put(f.name, toJson(f.get(o))) }
            }
            c = c.superclass
        }
        return json
    }
}
