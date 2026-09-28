# ADR 0001 — De aplicativo híbrido para nativo

**Status:** aceito
**Data:** 2026-09

## Contexto

A primeira versão deste controle foi um aplicativo **híbrido**: uma casca
nativa em Kotlin com um `WebView` de cerca de 200 linhas, e a aplicação
inteira em HTML, CSS e JavaScript puro num único arquivo, embutido no APK.
Persistência em `localStorage` e `IndexedDB`, compartilhamento por uma ponte
`@JavascriptInterface`, e um codificador de QR escrito à mão porque o
navegador não traz um.

Aquela escolha foi certa para o que ela precisava responder: **a operação
adota o registro digital em vez da prancheta?** Um piloto funcional ficou de
pé em dias, sem cadeia de dependências e sem etapa de build, e foi para o
pátio.

O que ele não é: o padrão com que apps Android são feitos. O caminho
principal hoje é nativo em Kotlin com Jetpack Compose — mais de 95% dos mil
maiores apps contêm Kotlin, e o Compose já é usado em produção por mais de
dois terços deles. O híbrido artesanal é o degrau de baixo de uma escada em
que também estão Flutter, React Native e o Capacitor.

## Decisão

Reconstruir o mesmo domínio como aplicativo **nativo em Kotlin com Jetpack
Compose**, seguindo o guia oficial de arquitetura, e manter o piloto híbrido
em operação até que o nativo o substitua.

## Motivos

1. **Manutenibilidade por terceiros.** O piloto só é manutenível por quem o
   escreveu. Qualquer desenvolvedor Android abre um projeto Compose com
   ViewModel e Room e entende em minutos.
2. **Integridade do dado.** `localStorage` é um balde de texto. Room traz
   schema, chave estrangeira, consulta tipada e, principalmente, **migração
   versionada** — mudar a estrutura sem apagar o histórico do conferente.
3. **Acesso ao hardware sem gambiarra.** A foto da avaria, que no híbrido
   exigiu três rodadas entre `ACTION_IMAGE_CAPTURE`, `FileProvider` e
   `getUserMedia`, aqui é um `ActivityResultContracts.TakePicture`.
4. **Testabilidade.** Regra de negócio em Kotlin puro roda na JVM em
   segundos, sem navegador headless.
5. **Caminho aberto.** A partir daqui, Kotlin Multiplatform é uma evolução
   possível para iOS; a casca WebView artesanal era um beco.

## Consequências

- Curva de aprendizado real em Compose, Room, Coroutines e Gradle.
- Mais arquivos e mais cerimônia para o mesmo comportamento visível.
- O contrato do CSV é mantido **idêntico**, de modo que os dois aplicativos
  alimentam a mesma planilha e a migração pode ser gradual, aparelho por
  aparelho.

## Alternativas consideradas

**Capacitor.** Empacotaria o HTML existente com plugins nativos prontos e
traria iOS quase de graça. Descartado como destino final por não resolver os
pontos 1, 2 e 4 — continuaria sendo um WebView com estado em armazenamento
de navegador. Permanece como plano B se o prazo apertar.

**Flutter ou React Native.** Ganho real só com iOS no horizonte, ao custo de
uma segunda linguagem e de uma cadeia de dependências que ninguém aqui vai
manter em dois anos.

**Manter o híbrido.** Sustentável enquanto for um aparelho e um pátio.
Deixa de ser no momento em que a operação depender do registro.
