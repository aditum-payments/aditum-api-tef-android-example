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
    val installmentTypes: List<InstallmentType> get() = rules.map { it.type }.distinct().ifEmpty { listOf(InstallmentType.None) }
    fun rule(t: InstallmentType): InstallmentRule? = rules.firstOrNull { it.type == t } ?: rules.firstOrNull()
}

data class InstallmentRule(
    val type: InstallmentType, val minTransaction: Long, val maxTransaction: Long, val minInstallments: Int, val maxInstallments: Int,
    val minInstallmentAmount: Long, val maxInstallmentAmount: Long,
)

/** Regras de montagem do pedido: o que o estabelecimento permite, validado antes de chamar `pay` (o original não validava). */
object PaymentRules {

    fun options(m: MerchantData?): List<PaymentOption> {
        val schemes = m?.paymentScheme.orEmpty()
        if (schemes.isEmpty()) return FALLBACK
        return schemes.mapIndexed { i, s ->
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
        }
    }

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
        if (t == InstallmentType.None || t == InstallmentType.Undefined) return 1..1
        val r = o.rule(t) ?: return 2..12
        val min = r.minInstallments.coerceAtLeast(2); val max = r.maxInstallments.takeIf { it >= min } ?: 12
        return min..max
    }

    /** Primeira regra que o pedido quebra, em texto para o operador; null = pode enviar. */
    fun validate(amount: Long, o: PaymentOption?, t: InstallmentType, installments: Int, op: PayOperationType, originalNsu: String): String? {
        if (amount <= 0) return "Informe o valor"
        if (o == null) return "Escolha a forma de pagamento"
        if (needsOriginalNsu(op) && originalNsu.isBlank()) return "Informe o NSU da pré-autorização"
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
