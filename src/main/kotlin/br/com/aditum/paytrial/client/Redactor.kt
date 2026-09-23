package br.com.aditum.paytrial.client

import org.json.JSONArray
import org.json.JSONObject

/**
 * Censura de dados sensíveis. Vale em TODO build, debug incluído (decisão do Kelvin, 23/09/2026), e roda antes de
 * qualquer coisa ser guardada no rastreio: nada sensível chega à tela, ao console ou à exportação. Não existe chave
 * que desligue. O comprovante impresso ou enviado ao cliente é o do autorizador, sem mudança.
 */
object Redactor {
    private const val DOT = "•"

    /** Número do cartão: só os 4 últimos. */
    fun pan(v: String?): String? = v?.let { val d = it.filter(Char::isDigit); if (d.length >= 4) DOT.repeat(4) + d.takeLast(4) else DOT.repeat(4) }

    /** Nome do portador: iniciais ("JOAO DA SILVA" → "J. D. S."). */
    fun name(v: String?): String? = v?.trim()?.split(Regex("[\\s/]+"))?.filter { it.isNotBlank() }?.joinToString(" ") { it.first().uppercaseChar() + "." }

    /** Token do parceiro: só os 4 últimos. */
    fun token(v: String?): String? = v?.let { if (it.length <= 4) DOT.repeat(4) else DOT.repeat(4) + it.takeLast(4) }

    /** Código de ativação: só os 3 últimos. */
    fun activation(v: String?): String? = v?.let { val d = it.filter(Char::isLetterOrDigit); DOT.repeat(6) + d.takeLast(3) }

    /** CPF: miolo visível ("•••.456.789-••"). */
    fun cpf(v: String?): String? = v?.filter(Char::isDigit)?.let { if (it.length == 11) "$DOT$DOT$DOT.${it.substring(3, 6)}.${it.substring(6, 9)}-$DOT$DOT" else DOT.repeat(it.length.coerceAtLeast(3)) }

    fun email(v: String?): String? = v?.let { val at = it.indexOf('@'); if (at <= 0) DOT.repeat(3) else it.first() + DOT.repeat(3) + it.substring(at) }

    fun phone(v: String?): String? = v?.filter(Char::isDigit)?.let { d -> if (d.length >= 10) "(${d.take(2)}) ${DOT.repeat(5)}-${d.takeLast(4)}" else DOT.repeat(4) + d.takeLast(4) }

    /** Destino do comprovante, conforme o meio (e-mail ou SMS). */
    fun sendTo(v: String?, method: String?): String? = if (method.equals("SMS", true) || (v != null && '@' !in v)) phone(v) else email(v)

    /** Linha de comprovante: sequências de 12+ dígitos (PAN, mesmo com espaços) viram os 4 últimos. */
    fun receiptLine(line: String): String = PAN_IN_TEXT.replace(line) { m -> val d = m.value.filter(Char::isDigit); DOT.repeat(4) + d.takeLast(4) }
    private val PAN_IN_TEXT = Regex("(?<!\\d)(?:\\d[ -]?){12,19}(?!\\d)")

    /** Dado digitado pelo operador (CVV, 4 últimos, documentos): nunca o valor, só o tamanho. */
    fun typed(v: String?): String = "\"${DOT.repeat((v?.length ?: 0).coerceIn(1, 8))}\" (${v?.length ?: 0})"

    /** Aplica as regras acima, pelo nome do campo, em qualquer JSON do contrato (recursivo). */
    fun json(value: Any?): Any? = when (value) {
        is JSONObject -> {
            val method = value.optString("method", "")
            for (k in value.keys().asSequence().toList()) {
                val v = value.opt(k)
                when (k) {
                    "cardNumber" -> value.put(k, pan(v as? String) ?: JSONObject.NULL)
                    "cardholderName" -> value.put(k, name(v as? String) ?: JSONObject.NULL)
                    "applicationToken" -> value.put(k, token(v as? String) ?: JSONObject.NULL)
                    "activationCode" -> value.put(k, activation(v as? String) ?: JSONObject.NULL)
                    "sendTo" -> value.put(k, sendTo(v as? String, method) ?: JSONObject.NULL)
                    "merchantReceipt", "cardholderReceipt" -> if (v is JSONArray) value.put(k, JSONArray((0 until v.length()).map { receiptLine(v.optString(it)) }))
                    else -> json(v)
                }
            }
            value
        }
        is JSONArray -> { for (i in 0 until value.length()) json(value.opt(i)); value }
        else -> value
    }
}
