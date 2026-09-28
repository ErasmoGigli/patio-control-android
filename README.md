# Pátio — controle de permanência e ativos de giro

Aplicativo Android **offline** para registrar a permanência de carretas num
pátio de distribuição de bebidas, o retorno de ativos de giro (vasilhame,
paletes, barris) e as avarias de carga — e para alimentar, sem digitação
manual, a planilha de indicadores que a operação já usa.

Nativo em Kotlin com Jetpack Compose, banco local em Room, **sem servidor,
sem conta de usuário e sem tráfego de rede**.

---

## O problema

Numa distribuidora, o tempo que uma carreta passa dentro do pátio é custo
direto: diária de veículo, fila na doca, equipe parada. O controle é feito
em prancheta e depois transcrito para uma planilha — o que significa
horário anotado de memória, coluna trocada e indicador que chega tarde
demais para virar decisão.

Três informações costumam se perder juntas:

1. **Os horários da carreta.** Nove marcos, da chegada à saída, que
   sozinhos explicam onde o tempo foi embora — fila, descarga, espera pelo
   ativo de giro, liberação.
2. **O ativo de giro que voltou.** Vasilhame e palete são patrimônio; uma
   carreta que sai vazia quando poderia ter levado retorno é custo que não
   aparece em lugar nenhum.
3. **A avaria da carga.** Um palete tombado dentro da carreta atrasa a
   descarga e o carregamento — mas, sem registro, esse atraso vira "demora"
   sem causa.

## O que o app faz

- **Nove horários por carreta**, oito deles marcados com um toque, na ordem
  em que acontecem no pátio. Um botão grande por vez, não uma lista de nove
  campos: com luva e sol na tela, a sequência precisa ser óbvia.
- **Catorze indicadores de tempo** calculados na hora — permanência,
  descarga, esperas, ciclo, tempo produtivo — inclusive para a carreta que
  ainda está no pátio.
- **Doze situações** derivadas dos horários, nunca digitadas.
- **Ativos de giro por tipo**, uma linha por tipo, nunca misturando
  unidades: trinta paletes de vasilhame e dez barris não somam quarenta de
  coisa nenhuma.
- **Avarias com foto**, quantidade opcional — nem sempre dá para medir na
  hora, e obrigar um número faria o conferente inventar um.
- **Validação no encerramento**: o que impede a linha de chegar incompleta
  na planilha trava; o que é apenas incomum avisa.
- **Exportação num único `.zip`** com os três CSV e as fotos, no formato
  que o Excel em português espera.
- **Passagem de turno entre aparelhos** por QR code ou arquivo, sem rede.

## Arquitetura

Três camadas, seguindo o
[guia oficial de arquitetura do Android](https://developer.android.com/topic/architecture):

```
ui/       Compose + ViewModel     estado desce, evento sobe
domain/   Kotlin puro             regras de negócio, sem Android
data/     Room + DataStore        fonte única da verdade
```

O ponto que mais importa: **a camada de domínio não conhece Android.**
Cálculo de indicador, classificação de situação, verificação de pendências
e geração de CSV são funções puras de Kotlin, testáveis na JVM em segundos.
É isso que permite escrever um teste para a carreta que entra às 23:40 e sai
às 00:15 sem subir emulador.

| Decisão | Por quê |
|---|---|
| Room, não arquivo JSON | Schema relacional, consultas tipadas, migração versionada |
| Injeção manual, não Hilt | Grafo pequeno; ver [ADR 0003](docs/adr/0003-injecao-manual.md) |
| Rotas em string, não tipadas | Evita uma dependência só para três argumentos |
| CSV como contrato | A planilha já existe e funciona; o app alimenta, não substitui |
| Sem rede, sem servidor | Nada a provisionar, nada a manter, nada a vazar |

Documentação completa em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) e nas
decisões registradas em [`docs/adr/`](docs/adr).

## Testes

```bash
./gradlew testDebugUnitTest        # regras de negócio, na JVM
./gradlew connectedDebugAndroidTest # banco e interface, no aparelho
```

Os testes de JVM cobrem a aritmética de horário com virada de meia-noite,
os catorze indicadores, as doze situações, as regras de pendência, o
contrato do CSV (inclusive a contagem de colunas, que é o que trava uma
mudança acidental de layout) e a ida e volta do formato de passagem de
turno. Os instrumentados cobrem o banco e a navegação.

## Como compilar

```bash
git clone <este repositório>
cd patio-control-android
./gradlew assembleDebug
```

Requer JDK 17 e o Android SDK 35. Abrir a pasta no Android Studio também
funciona: o projeto sincroniza e compila sem nenhum passo extra.

Para gerar a versão assinada, crie um keystore, configure-o em
`~/.gradle/gradle.properties` e rode `./gradlew assembleRelease`. **Guarde
o keystore e a senha**: sem eles, uma atualização futura não instala por
cima da versão atual.

## Privacidade e segurança

O app não faz nenhuma requisição de rede. Não há servidor, conta de usuário,
analytics ou telemetria. Os dados ficam no armazenamento privado do
aplicativo e só saem do aparelho quando o operador aciona a exportação e
escolhe para onde enviar. A permissão de câmera não é declarada: a foto é
tirada pela câmera do sistema, que devolve a imagem por um `FileProvider`.

## Licença

MIT — ver [LICENSE](LICENSE).

---

Este repositório é a segunda geração do projeto. A primeira foi um
aplicativo híbrido (casca Kotlin com WebView e a aplicação em HTML e
JavaScript), construído para validar rapidamente se a operação adotaria o
registro digital. Ele cumpriu esse papel; este é o mesmo domínio,
reconstruído no padrão que o Android usa hoje. O raciocínio da troca está
em [ADR 0001](docs/adr/0001-de-hibrido-para-nativo.md).
