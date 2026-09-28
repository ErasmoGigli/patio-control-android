# Arquitetura

## Visão geral

```
┌─────────────────────────────────────────────────────────────┐
│ ui/                                                          │
│   Composables (telas)  ←  estado  ←  ViewModel  →  eventos   │
│   Navegação com NavHost, uma Activity só                     │
└───────────────────────────────┬─────────────────────────────┘
                                │ modelos de domínio
┌───────────────────────────────▼─────────────────────────────┐
│ domain/                                                      │
│   Modelos, cálculo de indicadores, classificação de          │
│   situação, pendências, exportação CSV, passagem de turno    │
│   Kotlin puro — nenhum import android.*                      │
└───────────────────────────────┬─────────────────────────────┘
                                │ modelos de domínio
┌───────────────────────────────▼─────────────────────────────┐
│ data/                                                        │
│   PatioRepository  ─  fonte única da verdade                 │
│     ├─ Room (carreta, ativo_giro, avaria)                    │
│     ├─ DataStore (configuração e cadastros)                  │
│     └─ FotoStore (arquivos de imagem)                        │
└─────────────────────────────────────────────────────────────┘
```

## Princípios aplicados

**Separação de responsabilidades.** Uma tela não sabe que existe SQLite; o
repositório não sabe que existe Compose; o domínio não sabe que existe
Android. A fronteira entre banco e domínio é explícita, em
`data/local/Mapeadores.kt`: o Room nunca vê a enum `Marco`, e o domínio
nunca vê `CarretaEntity`.

**Fonte única da verdade.** O banco é o dono do dado. Nenhuma tela guarda
uma cópia que possa divergir — o formulário mantém estado local apenas
enquanto está sendo digitado, e salva por inteiro.

**Fluxo unidirecional.** O `ViewModel` expõe um `StateFlow<Estado>`
montado por `combine` sobre os fluxos do repositório; a tela coleta com
`collectAsStateWithLifecycle` e devolve eventos por função. Não existe
caminho em que o dado muda e a tela não sabe.

**A UI é função do estado.** Inclusive o tempo: um relógio emite a cada 30
segundos e entra no `combine`, o que faz a permanência da carreta em
andamento subir sozinha sem nenhum Composable ler `LocalTime.now()`.

## Modelo de dados

Três tabelas, com chave estrangeira e exclusão em cascata:

```
carreta (id TEXT PK)
  ├── ativo_giro (carretaId FK)
  └── avaria     (carretaId FK)
```

O identificador da carreta é `AAAA-AAAAMMDD-NNN`: código do aparelho, data e
sequência local. Sem servidor para distribuir identificadores, é esse
prefixo que impede dois aparelhos de gerarem a mesma chave no mesmo dia.

Os nove horários são nove colunas, não um mapa serializado. Custa mais
linhas no mapeador e paga em consulta, índice e correspondência direta com
a planilha.

`exportSchema = true`: o JSON do schema fica versionado em `app/schemas`, o
que permite revisar uma migração em code review antes de ela chegar no
aparelho do conferente. Não há `fallbackToDestructiveMigration` — apagar o
banco numa atualização é perder turno de coleta.

## Regras de negócio

Concentradas em `domain/Calculos.kt`, todas puras:

- **`CalculadoraIndicadores`** — catorze indicadores. Toda diferença é
  calculada em módulo 24h, porque a operação atravessa a meia-noite todo
  dia. Chegada adiantada conta como zero de atraso, nunca como 23 horas.
- **`ClassificadorSituacao`** — doze situações, testadas do marco mais
  avançado para o mais antigo, de modo que inserir um marco no meio não
  reescreve a regra inteira.
- **`VerificadorPendencias`** — o que trava o encerramento.
- **`VerificadorAlertas`** — o que só avisa. A distinção é deliberada:
  travar o que é apenas incomum ensina o operador a procurar como burlar.

## Integração com a planilha

O contrato é o CSV, descrito em [CONTRATO-CSV.md](CONTRATO-CSV.md). Formato
ditado pelo Excel em português: separador `;`, decimal com vírgula, data
`dd/MM/aaaa`, hora `HH:mm`, UTF-8 com BOM.

A exportação sai num único `.zip` com os três CSV na raiz, as fotos em
`fotos/` e um `leia-me.txt`. Um arquivo só em vez de vários soltos: nada se
perde no envio e a foto continua ligada ao registro pelo nome.

Os testes em `CsvExportadorTest` travam a contagem de colunas. Se um deles
quebrar, a culpa não é do teste — alguém mudou o layout e a colagem vai
desalinhar.

## Passagem de turno

Sem servidor, a transferência entre aparelhos acontece por QR code na tela
de um celular lido pela câmera do outro, ou por arquivo. O formato é de
linhas com separador de um caractere, e não JSON, porque a capacidade do QR
é o que manda. Está em `domain/Passagem.kt`, com escape explícito para que
uma observação com barra vertical não quebre a leitura do outro lado.

Receber duas vezes o mesmo pacote não duplica carreta: um id que já existe
no aparelho é ignorado.

## O que ficou de fora, e por quê

- **Leitura do QR pela câmera.** A geração está pronta; a leitura exige
  CameraX mais análise de quadro, e o caminho por arquivo já entrega a mesma
  transferência. Entra quando houver dois aparelhos Android para testar de
  verdade.
- **Painel de indicadores no app.** A planilha já faz isso melhor, com
  histórico e filtro. Duplicar seria manter duas verdades.
- **Sincronização entre aparelhos.** Exigiria servidor, que é exatamente o
  que este projeto não tem.
