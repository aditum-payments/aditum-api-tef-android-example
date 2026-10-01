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
./gradlew assembleDebug -Ppaytrial.device=P2
```

**Um APK por dispositivo, como o serviço.** O nome segue o do APK de serviço:
`build/dist/AditumPayTrial-A2A-<D|P>-<MODELO>-3.0.0+<hash>-<tipo>.apk`.

- `-Ppaytrial.device=<MODELO>` (ou `PAYTRIAL_DEVICE`): o alvo — `GPOS760`, `GPOS720`, `GPOS700X`, `GPOS700MINI`,
  `GPOS700`, `PAX`, `P2`, `AXIUM`, `L300`, `L3_2024`, `L400`, `X990` ou `TEFSDK` — ou um apelido do hardware
  (`P2MINI`, `A910S`, `A920`, `EX4000`, `SX7000`, `X990UX`, `L3`…), com os mesmos tokens do app. Ele escolhe a **chave de assinatura do fabricante** e entra no nome. Sem ele,
  o nome sai sem o modelo (`AditumPayTrial-A2A-D-3.0.0+<hash>-debug.apk`) e o release com a chave de debug. Dispositivo desconhecido falha o build com a lista.
- `-Ppaytrial.environment=development|production` (ou `ENVIRONMENT`): `D` ou `P` no nome. Produção exige o token de
  produção (abaixo); com o de desenvolvimento, o build falha.
- `-Ppaytrial.distribution=true|false` (ou `PAYTRIAL_DISTRIBUTION`): **modo de distribuição** — o app para o lojista, sem
  as ferramentas de integração (seção Desenvolvedor, Console, Requisição livre, inspetor, abas cruas; modo de integração e
  prévia sempre desligados). Sem o parâmetro, ligado em produção e desligado em desenvolvimento. Fora de produção o APK
  ganha `-dist` no fim (`…-debug-dist.apk`) para não se confundir com o de integração.

A tabela dispositivo → chave espelha a matriz de modelos do `aditum-tef` (`build-matrix/.../Devices.kt`); o
`scripts/sync-example-aar.sh --check` acusa quando as duas divergem. De dentro do `aditum-tef`, o mesmo build é
`./gradlew -p sdk/example assembleDebug -Ppaytrial.device=P2`, ou `scripts/build-all.sh --no-android --example`, que
monta um por dispositivo nos mesmos `--family`/`--model`/`--type` do Android.

**Token do integrador** (`InitRequest.applicationToken`): `-Ppaytrial.token=…`, variável `PAYTRIAL_TOKEN` ou
`paytrial.token` em `gradle.properties`. O valor versionado (`paytrial-dev-token`) só serve em desenvolvimento.

**Assinatura**, com as mesmas regras e os mesmos segredos do serviço (debug e release):
- sai com a **chave do fabricante** do dispositivo quando existe (Gertec GPOS, Positivo L300/L400); sem ela, com a
  da **Aditum** (PAX, Sunmi, Ingenico, Verifone, TefSDK, e sem `-Ppaytrial.device`); a de debug do Android só se nem a
  da Aditum estiver disponível, com aviso. O log do build diz qual chave assinou;
- o `.jks` vem de `-Ppaytrial.signing.dir` (padrão: `certs/` do `aditum-tef`, dois níveis acima) e as senhas, nesta
  ordem, de `-Ppaytrial.signing.<chave>.storePassword`, de `ADITUM_SIGNING_<CHAVE>_STORE` / `_KEY` / `_ALIAS` (ex.:
  `ADITUM_SIGNING_GERTEC_GPOS760_STORE`) ou do `signing.properties` do `aditum-tef` (`-Ppaytrial.signing.properties`
  para outro arquivo). De dentro do `aditum-tef` assina sem nenhum passo manual;
- no CI (`CI`/`TF_BUILD`, ou `-Ppaytrial.signing.strict=true`) o release sem a chave do dispositivo falha;
- `-Ppaytrial.keystore=/caminho.jks` com `PAYTRIAL_STORE_PASSWORD`, `PAYTRIAL_KEY_ALIAS` e `PAYTRIAL_KEY_PASSWORD` (um
  `.jks` só) vence a tabela.

Um Pay Trial já instalado com outra chave (os de antes de 25/09 saíam com a de debug do Android) não aceita o novo por
cima: desinstale antes (`adb uninstall br.com.aditum.paytrial`).

O dispositivo e o ambiente do build aparecem em **Mais › Sobre**.

## Instalar e testar

São dois APKs no terminal: o serviço da Aditum e este app. O app standalone da Aditum usa o mesmo pacote do
serviço, então só um dos dois cabe no aparelho.

**Instale o serviço primeiro.** A permissão `BIND_TEF` é criada pelo APK do serviço, e o Android não a concede a um
app que já estava instalado antes dela existir. Se o integrador foi instalado antes, reinstale-o por cima
(`adb install -r`); o banner da tela de conexão avisa quando é esse o caso. Vale para qualquer app integrador.

```bash
adb install -r AditumPayTrial-A2A-D-P2-3.0.0+<hash>-debug.apk
```

Na primeira tela informe o código de ativação do estabelecimento e toque **Iniciar**. Ali também se escolhe quem
desenha as telas durante a operação:

- **Telas da Aditum** (`useOnlySdk = false`): o serviço abre as próprias telas para cartão, senha e menus e as
  fecha antes de responder. O resultado é sempre mostrado pelo integrador.
- **Telas do integrador** (`useOnlySdk = true`): nenhuma tela da Aditum; mensagens, senha, menus e QR Code chegam
  por `IPaymentCallback` e o app desenha tudo.

O **TefSDK não tem as telas da Aditum**: o Pay Trial buildado com `-Ppaytrial.device=TEFSDK` só oferece as do
integrador, e o serviço TefSDK segue sempre com `useOnlySdk = true` (avisa no `init` quando o integrador pediu as da
Aditum; o Pay Trial sem dispositivo esconde a escolha ao receber o aviso).

O comprovante que o integrador recebe (`merchantReceipt`/`cardholderReceipt`) é sempre o **padrão**, em linhas de
texto; o personalizado em HTML é só do app standalone da Aditum.

As mensagens do pinpad (`InitRequest.pinpadMessages`) ficam na mesma tela e valem nos dois modos.

Se o banner disser *"versão standalone, sem o serviço App2App"*, o terminal tem o app standalone da Aditum no lugar
do serviço; *"Sem a permissão BIND_TEF"* é o caso da ordem de instalação acima.

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
   Terminal sem impressora (TefSDK, P2 Mini…) devolve `getPrinterSdk()` nulo. **Térmica externa USB** (Mais ›
   Impressora): o próprio app imprime, sem o serviço, em qualquer impressora USB de classe padrão que fale ESC/POS
   (Elgin/Bematech i9 testada; Epson, Tanca, Gertec G250…), pela USB host/OTG do aparelho: o mesmo bitmap na largura do
   papel (80 mm = 576 pontos, 58 mm = 384) vira imagem ESC/POS (`GS v 0`), com corte entre as vias
   (`app/UsbPrinter.kt`, codificador em `libs/aditum-escpos-<versão>.jar`). A impressora aparece pelo nome que ela
   informa e o Android pede a permissão de USB uma vez; com o pinpad também na USB, use um hub OTG com fonte.
8. **Opcionais do 3.0.0** (no fim da interface; serviço 2.x responde `null`): `getMerchantCustomization()` traz o logo
   e as cores do estabelecimento — o Pay Trial aplica as cores ao tema (`designsystem/Theme.kt`, `PtBrand`) e usa o
   logo na tela inicial, no comprovante na tela e no impresso (`app/ReceiptPrinter.kt`); sem logo, usa o da Aditum,
   como a standalone —; e
   `getTerminalStatus()` diz se há pinpad externo (TefSDK) e se ele está plugado. `listPinpads()` (USB + pareados), `scanPinpads()` (busca de
   30 s, cada aparelho na hora) e `selectPinpad()` (conecta) escolhem o pinpad externo do TefSDK (tela "Pinpad").

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
scripts/sync-example-aar.sh           # gera o .aar (catálogo legacy, minSdk 22) e o jar ESC/POS e copia para sdk/example/libs
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
