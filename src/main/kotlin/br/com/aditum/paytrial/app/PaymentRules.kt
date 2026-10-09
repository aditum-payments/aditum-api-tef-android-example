package br.com.aditum.paytrial.app

import br.com.aditum.data.v2.enums.Acquirer
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.data.v2.enums.PaymentType
import br.com.aditum.data.v2.model.MerchantData
import br.com.aditum.paytrial.designsystem.Money

/**
 * Forma de pagamento oferecida, vinda do `MerchantData.paymentScheme` do estabelecimento: tipo, adquirente, esquema e
 * as restrições (mín./máx. de valor, parcelas e valor de parcela) por tipo de parcelamento.
 */
data class PaymentOption(
    val key: String, val label: String, val paymentType: PaymentType, val acquirer: Acquirer, val scheme: String,
    val rules: List<InstallmentRule>,
) {
    val isQr: Boolean get() = paymentType == PaymentType.Pix || paymentType == PaymentType.Wallet
    /** "Escolher no TEF": sem forma (`Undefined`) — o cartão decide, e o serviço pergunta o parcelamento depois. */
    val isAsk: Boolean get() = paymentType == PaymentType.Undefined
    /** Na ordem do serviço: à vista, loja, emissor, pré-datado e Parcelado Cliente por último. */
    val installmentTypes: List<InstallmentType> get() = rules.map { it.type }.distinct().sortedBy { ORDER.indexOf(it) }.ifEmpty { listOf(InstallmentType.None) }
    fun rule(t: InstallmentType): InstallmentRule? = rules.firstOrNull { it.type == t } ?: rules.firstOrNull()
}

private val ORDER = listOf(InstallmentType.None, InstallmentType.Merchant, InstallmentType.Issuer, InstallmentType.DueDate, InstallmentType.Client)

data class InstallmentRule(
    val type: InstallmentType, val minTransaction: Long, val maxTransaction: Long, val minInstallments: Int, val maxInstallments: Int,
    val minInstallmentAmount: Long, val maxInstallmentAmount: Long,
)

/** Regras de montagem do pedido: o que o estabelecimento permite, validado antes de chamar `pay` (o original não validava). */
object PaymentRules {

    private val CLIENT_TOTAL = Regex("""total R\$\s*([\d.]+,\d{2})""")

    /**
     * Total com juros do Parcelado Cliente escolhido no menu "Parcelado Cliente - <bandeira>" do serviço (opção
     * "5x de R$ 52,00 (total R$ 260,00)"): esse total passa a ser o valor da venda. Null em qualquer outro menu.
     */
    fun clientInstallmentTotal(title: String, option: String): Long? {
        if (!title.trim().startsWith("PARCELADO CLIENTE", ignoreCase = true)) return null
        return CLIENT_TOTAL.find(option)?.groupValues?.get(1)?.filter(Char::isDigit)?.toLongOrNull()
    }

    private val RANGE = Regex("""(\d+)\s*a\s*(\d+)""")

    /**
     * Pedido do número de parcelas (`startGetClearData` com o título "PARCELAS" e a faixa no texto, "Número de parcelas
     * (2 a 6)"): a faixa, para o seletor −/+ em vez do campo de texto. O contrato não tem índice próprio para parcelas.
     */
    fun installmentsRange(title: String?, description: String?): IntRange? {
        if (!title.orEmpty().trim().equals("PARCELAS", ignoreCase = true)) return null
        val m = RANGE.find(description.orEmpty()) ?: return 1..99
        val a = m.groupValues[1].toInt(); val b = m.groupValues[2].toInt()
        return if (b >= a) a..b else null
    }

    /** "Escolher no TEF" — a venda sem forma, como na tela do aditum-client: o cartão decide a forma. */
    val ASK = PaymentOption("ask", "Escolher no TEF", PaymentType.Undefined, Acquirer.Undefined, "", emptyList())

    fun options(m: MerchantData?): List<PaymentOption> {
        val schemes = m?.paymentScheme.orEmpty()
        if (schemes.isEmpty()) return FALLBACK + ASK
        // linha sem forma no contrato (a de "Pagamento de Faturas": dinheiro + conta, que o App2App não expressa) fica de fora
        return schemes.filter { it.paymentType != null && it.paymentType != PaymentType.Undefined }.mapIndexed { i, s ->
            val type = s.paymentType ?: PaymentType.Undefined
            val rules = s.paymentTypeRestriction.orEmpty().map { r ->
                InstallmentRule(
                    type = r.installmentType?.takeIf { it != InstallmentType.Undefined } ?: InstallmentType.None,
                    minTransaction = (r.minTransactionAmount ?: 0).toLong(), maxTransaction = r.maxTransactionAmount ?: 0L,
                    minInstallments = r.minInstallmentNumber ?: 0, maxInstallments = r.maxInstallmentNumber ?: 0,
                    minInstallmentAmount = (r.minInstallmentAmount ?: 0).toLong(), maxInstallmentAmount = r.maxInstallmentAmount ?: 0L,
                )
            }
            PaymentOption("${i}_${type.name}_${s.acquirer?.name}", s.name?.takeIf { it.isNotBlank() } ?: Labels.paymentType(type), type, s.acquirer ?: Acquirer.Undefined, s.scheme.orEmpty(), rules)
        } + ASK
    }

    /** Pré-autorização, incremento e captura: só crédito (o serviço recusa o resto) — e "Escolher no TEF" não cabe. */
    fun optionsFor(all: List<PaymentOption>, op: PayOperationType): List<PaymentOption> =
        if (op == PayOperationType.Authorization) all else all.filter { it.paymentType == PaymentType.Credit }

    /**
     * Parcelamentos da forma na operação: na pré-autorização sem o emissor e, quando o estabelecimento não parcela
     * pré-autorização, só à vista; captura e incremento seguem os da pré-autorização original (à vista aqui).
     */
    fun installmentTypesFor(o: PaymentOption, op: PayOperationType, m: MerchantData?): List<InstallmentType> = when (op) {
        PayOperationType.Authorization -> o.installmentTypes
        PayOperationType.PreAuthorization -> if (m?.preAuthorizationInstallmentEnabled == true) o.installmentTypes - InstallmentType.Issuer else listOf(InstallmentType.None)
        else -> listOf(InstallmentType.None)
    }.ifEmpty { listOf(InstallmentType.None) }

    /** `installmentNumber` do pedido: à vista e pré-datado 1; Parcelado Cliente 0 (bandeira e parcelas o serviço pergunta antes do cartão). */
    fun installmentNumber(t: InstallmentType, chosen: Int): Int = when (t) { InstallmentType.Merchant, InstallmentType.Issuer -> chosen; InstallmentType.Client -> 0; InstallmentType.Undefined -> 0; else -> 1 }

    /** Sem esquemas na carga: as formas básicas, e o serviço escolhe o adquirente. */
    private val FALLBACK = listOf(PaymentType.Credit, PaymentType.Debit, PaymentType.Voucher, PaymentType.Pix).map {
        PaymentOption("f_${it.name}", Labels.paymentType(it), it, Acquirer.Undefined, "", emptyList())
    }

    /** Operações que o estabelecimento permite. */
    fun operations(m: MerchantData?): List<PayOperationType> = buildList {
        add(PayOperationType.Authorization)
        if (m?.preAuthorizationEnabled == true) { add(PayOperationType.PreAuthorization); add(PayOperationType.PreAuthorizationCapture) }
        if (m?.incrementalPreAuthorizationEnabled == true) add(PayOperationType.IncrementalPreAuthorization)
    }

    fun needsOriginalNsu(op: PayOperationType) = op == PayOperationType.PreAuthorizationCapture || op == PayOperationType.IncrementalPreAuthorization

    /** Faixa de parcelas para o tipo escolhido: (mín, máx); à vista é (1, 1). */
    fun installmentRange(o: PaymentOption, t: InstallmentType): IntRange {
        if (t != InstallmentType.Merchant && t != InstallmentType.Issuer) return 1..1
        val r = o.rule(t) ?: return 2..12
        val min = r.minInstallments.coerceAtLeast(2); val max = r.maxInstallments.takeIf { it >= min } ?: 12
        return min..max
    }

    /** Primeira regra que o pedido quebra, em texto para o operador; null = pode enviar. */
    fun validate(amount: Long, o: PaymentOption?, t: InstallmentType, installments: Int, op: PayOperationType, originalNsu: String): String? {
        if (amount <= 0) return "Informe o valor"
        if (o == null) return "Escolha a forma de pagamento"
        if (needsOriginalNsu(op) && originalNsu.isBlank()) return "Informe o NSU da pré-autorização"
        if (o.isAsk) return null
        val r = o.rule(t) ?: return null
        if (r.minTransaction > 0 && amount < r.minTransaction) return "Valor mínimo: ${Money.brl(r.minTransaction)}"
        if (r.maxTransaction > 0 && r.maxTransaction < Long.MAX_VALUE / 2 && amount > r.maxTransaction) return "Valor máximo: ${Money.brl(r.maxTransaction)}"
        if (t == InstallmentType.Merchant || t == InstallmentType.Issuer) {
            val range = installmentRange(o, t)
            if (installments !in range) return "Parcelas: de ${range.first} a ${range.last}"
            val each = amount / installments
            if (r.minInstallmentAmount > 0 && each < r.minInstallmentAmount) return "Parcela mínima: ${Money.brl(r.minInstallmentAmount)}"
        }
        return null
    }
}
