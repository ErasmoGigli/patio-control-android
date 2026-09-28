# ADR 0004 — CSV como contrato, em vez de substituir a planilha

**Status:** aceito
**Data:** 2026-09

## Contexto

A operação já tem uma planilha de permanência com histórico de anos,
fórmulas de indicador, painel com filtros e comparativos. A tentação óbvia
ao construir um app é absorver tudo isso: dashboards no celular, histórico
no aparelho, a planilha aposentada.

## Decisão

O app **alimenta** a planilha e não a substitui. O contrato entre os dois é
o CSV, descrito em [CONTRATO-CSV.md](../CONTRATO-CSV.md) e travado por
testes que verificam a contagem de colunas.

## Motivos

1. **A planilha faz melhor a parte dela.** Filtro por período, comparativo
   por transportadora, tabela dinâmica — é o que ela sabe fazer, e ninguém
   precisa aprender nada novo para usá-la.
2. **Duas verdades é pior que uma.** Se o app tivesse o seu próprio painel,
   a primeira divergência entre os dois números destruiria a confiança nos
   dois.
3. **Adoção.** A gestão continua recebendo o indicador no formato que já
   lê. O que muda é só a origem do dado: digitado a partir de prancheta,
   agora colado do pacote.
4. **Risco isolado.** Se o app sair de operação por qualquer motivo, a
   planilha continua funcionando com preenchimento manual. Não há
   dependência de mão única.

## Consequências

- Existe um passo manual: extrair o `.zip` e colar três blocos. É
  deliberado, e leva menos de um minuto.
- Qualquer mudança de coluna precisa acontecer nos dois lados na mesma
  entrega. Os testes de contrato existem exatamente para que essa mudança
  não passe despercebida.
- O app mostra no aparelho apenas os indicadores da carreta que está na
  mão, que é o que ajuda a decidir no pátio. Análise é na planilha.
