package br.com.aditum.paytrial.designsystem

/** Valores sempre em centavos (Long), nunca ponto flutuante. */
object Money {
    fun brl(cents: Long): String {
        val neg = cents < 0; val c = kotlin.math.abs(cents)
        val reais = (c / 100).toString().reversed().chunked(3).joinToString(".").reversed()
        return (if (neg) "-" else "") + "R$ " + reais + "," + (c % 100).toString().padStart(2, '0')
    }
    /** Teclado de valor: dígitos entram pela direita (R$ 0,01 → R$ 0,12 → R$ 1,23). */
    fun push(cents: Long, digits: String, max: Long = 99_999_999_99L): Long = digits.fold(cents) { acc, d -> val n = acc * 10 + (d - '0'); if (n > max) acc else n }
}
