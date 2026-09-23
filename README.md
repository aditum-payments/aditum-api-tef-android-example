# Aditum Pay Trial

App de exemplo da integração **App2App** da Aditum (pacote `br.com.aditum.paytrial`). Faz o papel de um app
integrador de verdade — venda, cancelamento, pré-autorização, confirmação manual, comprovantes, relatório — e ao
mesmo tempo mostra, em qualquer tela, a requisição e a resposta cruas de cada chamada ao serviço, para depurar a
integração.

Roda de **Android 5.1 (API 22)** em diante.

## O que ele usa da Aditum

Só o que um integrador recebe:

| Peça | Onde |
|---|---|
| SDK de integração (AIDL + contratos) | `libs/AditumSdkIntegration-A2A-3.0.0.aar` |
| Serviço App2App | o APK da Aditum instalado no terminal (`br.com.aditum.smartpostef`, variante *service*) |

Não há dependência de código do projeto `aditum-tef`: tema, ícones e fontes são do próprio app (`designsystem/`).
A pasta compila sozinha, fora do repositório principal.

## Build

Requer JDK 17 (o Gradle baixa um se não houver) e o Android SDK (`local.properties` com `sdk.dir`, ou
`ANDROID_HOME`).

```bash
./gradlew assembleDebug
```

O APK sai em `build/dist/AditumPayTrial-3.0.0+<hash>-debug.apk`. De dentro do `aditum-tef`, o mesmo build é
`./gradlew -p sdk/example assembleDebug` ou `scripts/build-all.sh --no-android --example`.

**Token do integrador** (`InitRequest.applicationToken`): `-Ppaytrial.token=…`, variável `PAYTRIAL_TOKEN` ou
`paytrial.token` em `gradle.properties`. O valor versionado (`paytrial-dev-token`) só serve em desenvolvimento.

**Assinatura com a chave do fabricante** (terminais que só instalam APK assinado):
`-Ppaytrial.keystore=/caminho/arquivo.jks`, com as senhas em `PAYTRIAL_STORE_PASSWORD`, `PAYTRIAL_KEY_ALIAS` e
`PAYTRIAL_KEY_PASSWORD`. Sem isso, sai com a chave de debug.

## Instalar e testar

São dois APKs no terminal: o serviço da Aditum e este app. O app standalone da Aditum usa o mesmo pacote do
serviço, então só um dos dois cabe no aparelho.

```bash
adb install -r AditumPayTrial-3.0.0+<hash>-debug.apk
```

Na primeira tela informe o código de ativação do estabelecimento e toque **Iniciar**. Ali também se escolhe quem
desenha as telas durante a operação:

- **Telas da Aditum** (`useOnlySdk = false`): o serviço abre as próprias telas para cartão, senha e menus e as
  fecha antes de responder. O resultado é sempre mostrado pelo integrador.
- **Telas do integrador** (`useOnlySdk = true`): nenhuma tela da Aditum; mensagens, senha, menus e QR Code chegam
  por `IPaymentCallback` e o app desenha tudo.

As mensagens do pinpad (`InitRequest.pinpadMessages`) ficam na mesma tela e valem nos dois modos.

Se o banner disser *"versão standalone, sem o serviço App2App"*, o terminal tem o app standalone da Aditum no lugar
do serviço.

## Checklist de integração

O que este app faz e que todo integrador precisa fazer (ver `client/AditumClient.kt`):

1. **Manifesto:** a permissão `br.com.aditum.smartpostef.permission.BIND_TEF` e
   `<queries><package android:name="br.com.aditum.smartpostef" /></queries>`.
2. **Conexão:** `startForegroundService` (API 26+) e `bindService` na action `br.com.aditum.AditumSdkService` com
   `setPackage("br.com.aditum.smartpostef")`; `linkToDeath` para saber quando o serviço morre e religar.
3. **Callback único:** `registerPaymentCallback` logo depois de conectar, para receber notificações, senha, menus e
   QR Code de qualquer operação.
4. **`init` antes de tudo:** ativa o terminal na primeira vez e devolve o estabelecimento (`getMerchantData`), de
   onde saem as formas de pagamento, parcelamentos e valores mínimos que a tela de venda respeita.
5. **Confirmação:** a transação aprovada fica pendente até `confirmTransaction(nsu)`. Enquanto não confirmada,
   desfazer é `cancel` com o NSU dela; `pendingTransactions` lista o que falta confirmar.
6. **Abortar:** `abortOperation` interrompe a operação em andamento (o voltar do Android faz isso durante a venda).
7. **Impressão:** `getDeviceSdk().print(bitmap, callback)` com o comprovante já desenhado (`app/ReceiptPrinter.kt`).

## Dados crus e censura

O chip escuro no canto inferior da tela (a última chamada, com o tempo de resposta) e o **Console** (Mais ›
Desenvolvedor) mostram cada chamada com a requisição, a resposta e os callbacks recebidos, em JSON. O rastreio fica
só em memória, e o console pode ser exportado para anexar a um chamado.

**Dados sensíveis são censurados em todos os builds, debug inclusive** (`client/Redactor.kt`): cartão só com os
4 últimos dígitos, nome do portador só com as iniciais, token e código de ativação truncados, destino do
comprovante mascarado, dados digitados só com o tamanho, e números de cartão e CPF nas linhas do comprovante. A
censura acontece antes de o dado entrar no rastreio, então vale para tela, console e exportação.

## Atualizar o SDK

Quando o contrato muda no `aditum-tef`, o `.aar` daqui precisa ser gerado de novo:

```bash
scripts/sync-example-aar.sh           # gera (catálogo legacy, minSdk 22) e copia para sdk/example/libs
scripts/sync-example-aar.sh --check   # só confere; o build-all.sh --example falha se estiver defasado
```

## Estrutura

```
client/        conexão com o serviço, rastreio das chamadas, JSON do contrato e censura
app/           estado das telas (MainViewModel), regras de pagamento, preferências, comprovante em bitmap
ui/            telas: conexão/início, venda, resultado/comprovante, pós-venda, Mais/desenvolvedor
designsystem/  tema (Material 3, #2F2256, Nunito Sans), componentes e ícones Tabler (MIT)
tools/icons.py regenera designsystem/PtIcons.kt a partir do pacote npm dos Tabler Icons
```
