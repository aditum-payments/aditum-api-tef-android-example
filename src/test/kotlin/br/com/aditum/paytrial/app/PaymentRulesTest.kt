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

    @Test fun `sem esquemas na carga, as formas basicas`() = assertEquals(listOf(PaymentType.Credit, PaymentType.Debit, PaymentType.Voucher, PaymentType.Pix), PaymentRules.options(null).map { it.paymentType })
}
