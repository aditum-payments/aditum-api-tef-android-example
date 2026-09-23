package br.com.aditum.paytrial.client

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A censura vale em todo build: nenhum dado da tabela de dados sensíveis pode escapar para tela, console ou exportação. */
class RedactorTest {
    @Test fun `numero do cartao so com os 4 ultimos`() {
        assertEquals("••••4412", Redactor.pan("4111111111114412"))
        assertEquals("••••4412", Redactor.pan("411111******4412"))
    }

    @Test fun `nome do portador vira iniciais`() = assertEquals("J. D. S.", Redactor.name("JOAO DA SILVA"))

    @Test fun `credenciais censuradas`() {
        assertEquals("••••Lfq9", Redactor.token("mk_Lfq9"))
        assertEquals("••••••044", Redactor.activation("880776044"))
    }

    @Test fun `destino do comprovante`() {
        assertEquals("c•••@exemplo.com.br", Redactor.sendTo("cliente@exemplo.com.br", "Email"))
        assertEquals("(11) •••••-4321", Redactor.sendTo("11987654321", "SMS"))
    }

    @Test fun `dado digitado nunca aparece, so o tamanho`() {
        val out = Redactor.typed("123")
        assertFalse(out.contains("123")); assertTrue(out.endsWith("(3)"))
    }

    @Test fun `CPF com o miolo visivel`() = assertEquals("•••.456.789-••", Redactor.cpf("123.456.789-09"))

    @Test fun `PAN dentro de comprovante`() {
        assertEquals("CARTAO ••••4412", Redactor.receiptLine("CARTAO 4111 1111 1111 4412"))
        assertEquals("NSU de 9 dígitos não é cartão", "NSU 000000862", Redactor.receiptLine("NSU 000000862"))
    }

    @Test fun `JSON do contrato censurado por nome de campo, em qualquer profundidade`() {
        val json = JSONObject().put("isApproved", true).put("charge", JSONObject()
            .put("cardNumber", "4111111111114412").put("cardholderName", "JOAO SILVA").put("nsu", "000000862")
            .put("cardholderReceipt", JSONArray(listOf("VISA", "4111111111114412"))))
            .put("init", JSONObject().put("applicationToken", "mk_Lfq9yMzRoYaHjowfxLvoyi").put("activationCode", "880776044"))
        val text = Redactor.json(json).toString()
        listOf("4111111111114412", "JOAO", "SILVA", "mk_Lfq9yMzRoYaH", "880776").forEach { assertFalse("vazou $it", text.contains(it)) }
        assertTrue(text.contains("000000862"))
    }
}
