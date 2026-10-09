package br.com.aditum.paytrial.app

import br.com.aditum.data.v2.enums.CardBrand
import br.com.aditum.data.v2.enums.ChargeStatus
import br.com.aditum.data.v2.enums.InstallmentType
import br.com.aditum.data.v2.enums.PayOperationType
import br.com.aditum.data.v2.enums.PaymentType
import br.com.aditum.data.v2.enums.PrintStatus
import br.com.aditum.data.v2.enums.TransactionStatus
import br.com.aditum.paytrial.designsystem.PtIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Textos em pt-BR para os enums do contrato. */
object Labels {
    fun paymentType(t: PaymentType?) = when (t) { PaymentType.Credit -> "Crédito"; PaymentType.Debit -> "Débito"; PaymentType.Voucher -> "Voucher"; PaymentType.Pix -> "Pix"; PaymentType.Wallet -> "Carteira"; else -> "Indefinido" }
    fun paymentIcon(t: PaymentType?) = when (t) { PaymentType.Credit -> PtIcons.Credit; PaymentType.Debit -> PtIcons.Debit; PaymentType.Voucher -> PtIcons.Voucher; PaymentType.Pix -> PtIcons.Pix; PaymentType.Wallet -> PtIcons.Wallet; else -> PtIcons.Sale }
    fun installmentType(t: InstallmentType?) = when (t) { InstallmentType.None -> "À vista"; InstallmentType.Merchant -> "Loja"; InstallmentType.Issuer -> "Emissor"; InstallmentType.DueDate -> "Pré-datado"; InstallmentType.Client -> "Parcelado Cliente"; else -> "À vista" }
    fun operation(o: PayOperationType?) = when (o) { PayOperationType.Authorization -> "Venda"; PayOperationType.PreAuthorization -> "Pré-autorização"; PayOperationType.PreAuthorizationCapture -> "Captura"; PayOperationType.IncrementalPreAuthorization -> "Incremento"; else -> "Venda" }
    fun chargeStatus(s: ChargeStatus?) = when (s) { ChargeStatus.Authorized -> "Autorizada"; ChargeStatus.PreAuthorized -> "Pré-autorizada"; ChargeStatus.Canceled -> "Cancelada"; ChargeStatus.Partial -> "Parcial"; ChargeStatus.NotAuthorized -> "Negada"; ChargeStatus.Pending -> "Aguardando confirmação"; ChargeStatus.PendingCancel -> "Cancelamento pendente"; ChargeStatus.Expired -> "Expirada"; else -> "Indefinido" }
    fun brand(b: CardBrand?) = if (b == null || b == CardBrand.Undefined) "" else b.name.uppercase()
    fun printStatus(s: PrintStatus?) = when (s) { PrintStatus.Ok -> "Impresso"; PrintStatus.OutOfPaper -> "Sem papel"; PrintStatus.Busy -> "Impressora ocupada"; PrintStatus.Overheat -> "Impressora superaquecida"; PrintStatus.MalFunctions -> "Falha na impressora"; PrintStatus.DataTooLarge -> "Comprovante grande demais"; else -> "Falha na impressão (${s?.name})" }

    /** O que o app mostra enquanto uma chamada ao serviço está em curso (a barra de processo). */
    fun callTitle(method: String) = when (method) {
        "init" -> "Inicializando o terminal…"; "updateEmvTables" -> "Atualizando as tabelas…"; "getMerchantData" -> "Buscando o estabelecimento…"
        "charge" -> "Consultando a transação…"; "pendingTransactions" -> "Consultando as pendências…"; "confirmTransaction" -> "Confirmando a venda…"
        "generateReport" -> "Gerando o relatório…"; "deactivate" -> "Desativando o terminal…"; "sendReceipt" -> "Enviando o comprovante…"
        "print" -> "Imprimindo…"; "getDeviceSdk" -> "Consultando a impressora…"; "pay" -> "Pagamento em andamento…"; "cancel" -> "Cancelamento em andamento…"
        else -> "Aguardando o serviço…"
    }

    /** Título legível do passo, a partir do status da notificação (a mensagem do serviço vem embaixo). */
    fun step(s: TransactionStatus?) = when (s) {
        TransactionStatus.StartingTransaction -> "Iniciando"; TransactionStatus.ChargingTables -> "Carregando tabelas"; TransactionStatus.ValidatingOnline -> "Validando"
        TransactionStatus.CheckCardEvent, TransactionStatus.WaitingCardEvent -> "Aguardando cartão"; TransactionStatus.WaitingTypedCard -> "Aguardando dados do cartão"
        TransactionStatus.GettingPin -> "Senha"; TransactionStatus.ProcessingOnline -> "Autorizando"; TransactionStatus.ProcessingReversion -> "Desfazendo"
        TransactionStatus.WaitingRemoveCard -> "Retire o cartão"; TransactionStatus.Canceled -> "Cancelada"; TransactionStatus.FinishingTransaction -> "Finalizando"
        TransactionStatus.Finished -> "Concluída"; TransactionStatus.ValidatingNsu -> "Validando NSU"; TransactionStatus.WaitingQrcodeReading -> "Aguardando leitura do QR Code"
        else -> "Processando"
    }

    /** "2026-09-23T09:46:10" → "23/09 09:46"; qualquer outro formato sai como veio. */
    fun dateTime(iso: String?): String = iso?.let { s -> Regex("(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2})").find(s)?.destructured?.let { (_, mo, d, h, mi) -> "$d/$mo $h:$mi" } ?: s } ?: ""
    fun time(iso: String?): String = iso?.let { s -> Regex("[T ](\\d{2}:\\d{2})").find(s)?.groupValues?.get(1) ?: "" } ?: ""
    fun dateTime(d: Date?): String = d?.let { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(it) } ?: ""

    /**
     * CPF ou CNPJ para exibir, como o `Masks.document` do app: o CNPJ alfanumérico (12 primeiras posições com letras,
     * Receita 2026) sai `12.ABC.345/01DE-35`; o numérico `12.345.678/0001-95`; o CPF `123.456.789-09`. Vem cru ou já
     * pontuado; outro tamanho volta como veio.
     */
    fun document(value: String): String {
        val c = value.filter(Char::isLetterOrDigit).uppercase()
        return when {
            c.length == 14 && c.takeLast(2).all(Char::isDigit) -> "${c.take(2)}.${c.substring(2, 5)}.${c.substring(5, 8)}/${c.substring(8, 12)}-${c.takeLast(2)}"
            c.length == 11 && c.all(Char::isDigit) -> "${c.take(3)}.${c.substring(3, 6)}.${c.substring(6, 9)}-${c.takeLast(2)}"
            else -> value
        }
    }

    /** "CNPJ" ou "CPF", pelo tamanho do documento (o MerchantData traz o do estabelecimento). */
    fun documentKind(value: String): String = if (value.count(Char::isLetterOrDigit) == 11) "CPF" else "CNPJ"
}
