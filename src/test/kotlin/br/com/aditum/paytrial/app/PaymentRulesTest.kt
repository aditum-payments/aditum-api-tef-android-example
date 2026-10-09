package br.com.aditum.paytrial.app

import br.com.aditum.data.v2.enums.Acquirer
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.data.v2.enums.PaymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** O pedido é validado contra as restrições do estabelecimento antes de chamar `pay` (o original não validava). */
class PaymentRulesTest {
    private val credit = PaymentOption("c", "Crédito", PaymentType.Credit, Acquirer.Simulator, "", listOf(
        InstallmentRule(InstallmentType.None, 100, 1_000_000, 0, 0, 0, 0),
        InstallmentRule(InstallmentType.Merchant, 1000, 1_000_000, 2, 12, 500, 0),
    ))

    @Test fun `valor zero e forma ausente`() {
        assertEquals("Informe o valor", PaymentRules.validate(0, credit, InstallmentType.None, 1, PayOperationType.Authorization, ""))
        assertEquals("Escolha a forma de pagamento", PaymentRules.validate(100, null, InstallmentType.None, 1, PayOperationType.Authorization, ""))
    }

    @Test fun `minimo da transacao`() = assertEquals("Valor mínimo: R$ 1,00", PaymentRules.validate(50, credit, InstallmentType.None, 1, PayOperationType.Authorization, ""))

    @Test fun `parcela abaixo do minimo`() {
        assertEquals("Parcela mínima: R$ 5,00", PaymentRules.validate(2000, credit, InstallmentType.Merchant, 6, PayOperationType.Authorization, ""))
        assertNull(PaymentRules.validate(2400, credit, InstallmentType.Merchant, 3, PayOperationType.Authorization, ""))
    }

    @Test fun `faixa de parcelas`() {
        assertEquals(2..12, PaymentRules.installmentRange(credit, InstallmentType.Merchant))
        assertEquals(1..1, PaymentRules.installmentRange(credit, InstallmentType.None))
        assertEquals("Parcelas: de 2 a 12", PaymentRules.validate(100_000, credit, InstallmentType.Merchant, 13, PayOperationType.Authorization, ""))
    }

    @Test fun `captura exige o NSU da pre-autorizacao`() =
        assertEquals("Informe o NSU da pré-autorização", PaymentRules.validate(1000, credit, InstallmentType.None, 1, PayOperationType.PreAuthorizationCapture, ""))

    @Test fun `sem esquemas na carga, as formas basicas e Escolher no TEF`() = assertEquals(listOf(PaymentType.Credit, PaymentType.Debit, PaymentType.Voucher, PaymentType.Pix, PaymentType.Undefined), PaymentRules.options(null).map { it.paymentType })

    @Test fun `total do parcelado cliente sai da opcao escolhida so no menu PARCELADO CLIENTE`() {
        assertEquals(26_000L, PaymentRules.clientInstallmentTotal("Parcelado Cliente - Visa", "5x de R$ 52,00 (total R$ 260,00)"))
        assertEquals(123_456L, PaymentRules.clientInstallmentTotal("Parcelado Cliente", "12x de R$ 102,88 (total R$ 1.234,56)"))
        assertNull(PaymentRules.clientInstallmentTotal("PARCELAMENTO", "Parcelado Cliente"))
        assertNull(PaymentRules.clientInstallmentTotal("SELECIONE:", "Credito Nubank"))
    }

    @Test fun `pedido de parcelas vira faixa para o seletor`() {
        assertEquals(2..6, PaymentRules.installmentsRange("PARCELAS", "Número de parcelas (2 a 6)"))
        assertNull(PaymentRules.installmentsRange("Data da cobrança(DDMM)", "Data da cobrança(DDMM)"))
    }

    @Test fun `pre-autorizacao so oferece credito, sem emissor, e a venda tem Escolher no TEF`() {
        val debit = PaymentOption("d", "Débito", PaymentType.Debit, Acquirer.Simulator, "", emptyList())
        val all = listOf(credit, debit, PaymentRules.ASK)
        assertEquals(listOf(credit), PaymentRules.optionsFor(all, PayOperationType.PreAuthorization))
        assertEquals(all, PaymentRules.optionsFor(all, PayOperationType.Authorization))
        val withIssuer = credit.copy(rules = credit.rules + InstallmentRule(InstallmentType.Issuer, 1000, 1_000_000, 2, 12, 500, 0))
        assertEquals(listOf(InstallmentType.None), PaymentRules.installmentTypesFor(withIssuer, PayOperationType.PreAuthorization, null))
    }

    @Test fun `parcelado cliente vai sem parcelas e pre-datado com uma`() {
        assertEquals(0, PaymentRules.installmentNumber(InstallmentType.Client, 5)); assertEquals(1, PaymentRules.installmentNumber(InstallmentType.DueDate, 5)); assertEquals(5, PaymentRules.installmentNumber(InstallmentType.Merchant, 5))
        val c = credit.copy(rules = credit.rules + InstallmentRule(InstallmentType.Client, 100, 1_000_000, 2, 12, 0, 0))
        assertEquals(listOf(InstallmentType.None, InstallmentType.Merchant, InstallmentType.Client), c.installmentTypes)
    }

    @Test fun `documento do estabelecimento formatado, CNPJ alfanumerico inclusive`() {
        assertEquals("12.ABC.345/01DE-35", Labels.document("12abc34501de35"))
        assertEquals("12.ABC.345/01DE-35", Labels.document("12.ABC.345/01DE-35"))
        assertEquals("42.932.470/8410-00", Labels.document("42932470841000"))
        assertEquals("123.456.789-09", Labels.document("12345678909")); assertEquals("CPF", Labels.documentKind("12345678909"))
        assertEquals("CNPJ", Labels.documentKind("12ABC34501DE35"))
    }
}
