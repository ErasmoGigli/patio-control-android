# Contrato do CSV com a planilha

A exportação sai num `.zip` com esta estrutura:

```
patio-<APARELHO>-<AAAAMMDD-HHMM>.zip
├── registros-<carimbo>.csv    → aba REGISTROS,  coluna A
├── ativos-<carimbo>.csv       → aba AG_DETALHE, coluna A
├── avarias-<carimbo>.csv      → aba AVARIAS,    coluna A
├── fotos/
│   └── AVARIA_<carreta>_<n>_<sufixo>.jpg
└── leia-me.txt
```

## Formato

| Item | Valor | Motivo |
|---|---|---|
| Separador | `;` | Padrão do Excel em português |
| Codificação | UTF-8 **com BOM** | Sem o BOM, o Excel lê acento como lixo |
| Quebra de linha | `CRLF` | — |
| Data | `dd/MM/aaaa` | — |
| Hora | `HH:mm` | 24h, sem segundos |
| Decimal | vírgula | `12,5`, não `12.5` |
| Aspas | só quando necessário | Aspa interna é dobrada, conforme o RFC |

Campo não preenchido sai **vazio**, nunca como zero. A distinção importa:
descarga de zero minuto e descarga não medida são fatos diferentes.

## registros.csv — 29 colunas (A..AC)

| # | Coluna | Conteúdo |
|---|---|---|
| A | 1 | Identificador da carreta |
| B | 2 | Data |
| C | 3 | Turno |
| D | 4 | Transportadora |
| E | 5 | Placa da carreta |
| F | 6 | Placa do cavalo |
| G | 7 | Motorista |
| H | 8 | Tipo de veículo |
| I | 9 | Origem |
| J | 10 | Destino |
| K | 11 | Carga / mapa / documento |
| L | 12 | Conferente responsável |
| M | 13 | Produto recebido |
| N | 14 | Nota fiscal |
| O | 15 | Quantidade |
| P | 16 | Unidade |
| Q | 17 | Observação da carga |
| R | 18 | Horário programado |
| S | 19 | Chegada |
| T | 20 | Entrada na unidade |
| U | 21 | Início da descarga |
| V | 22 | Fim da descarga |
| W | 23 | Início do carregamento de AG |
| X | 24 | Fim do carregamento de AG |
| Y | 25 | Liberação |
| Z | 26 | Saída da unidade |
| AA | 27 | Carregada com AG (`Sim` / `Não` / vazio) |
| AB | 28 | Justificativa da saída sem AG |
| AC | 29 | Observação do carregamento |

As colunas digitáveis da planilha são contíguas de A a AC justamente para
que a colagem seja uma só, sem pular coluna calculada.

## ativos.csv — 7 colunas (A..G)

| # | Conteúdo |
|---|---|
| A | Identificador da carreta |
| B | Tipo de ativo |
| C | Quantidade |
| D | Unidade |
| E | Documento do AG |
| F | Destino do AG |
| G | Observação |

**Uma linha por tipo.** Dois tipos nunca compartilham linha, e unidades
diferentes nunca são somadas.

## avarias.csv — 7 colunas (A..G)

| # | Conteúdo |
|---|---|
| A | Identificador da carreta |
| B | Item avariado |
| C | Quantidade (pode ser vazia) |
| D | Unidade |
| E | Origem aparente |
| F | Observação |
| G | **Nome do arquivo de foto** |

A coluna G leva o nome do arquivo, nunca a imagem. O arquivo correspondente
está em `fotos/` dentro do mesmo pacote — é por isso que o pacote deve ser
guardado inteiro para manter o vínculo.

## Alterar este contrato

Mudar a ordem ou a quantidade de colunas quebra a colagem na planilha de
forma silenciosa: os dados entram desalinhados e os indicadores passam a
calcular sobre a coluna errada. Por isso:

1. Os testes em `CsvExportadorTest` travam a contagem de colunas de cada
   arquivo. Uma mudança acidental falha o build.
2. Qualquer alteração aqui exige alteração correspondente na planilha, na
   mesma entrega.
3. Se a mudança for inevitável, prefira **acrescentar** coluna ao final a
   reordenar as existentes.
