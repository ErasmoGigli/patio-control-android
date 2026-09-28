# ADR 0003 — Injeção de dependência manual, sem Hilt

**Status:** aceito
**Data:** 2026-09

## Contexto

Hilt é o padrão recomendado para injeção de dependência em Android e
aparece em praticamente todo projeto de referência. A pergunta é se ele se
justifica **neste** projeto.

O grafo inteiro de dependências do app é: um banco, três DAO, um DataStore,
um armazenamento de fotos e um repositório. Um objeto de cada, vivendo pelo
tempo do processo. Não há variantes por build, escopos aninhados, nem
implementações alternativas para trocar em teste — os testes de regra de
negócio não tocam em nenhuma dessas classes, porque o domínio é puro.

## Decisão

Injeção manual, com um `AppContainer` criado na `Application` e alcançado
pelas factories de ViewModel através de
`ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY`. É o padrão que
a própria documentação do Android descreve para apps pequenos.

## Motivos

Hilt resolveria exatamente o mesmo grafo ao custo de um processador de
anotação a mais no build, código gerado para depurar quando algo não
resolve, e uma camada de conceitos (`@Module`, `@InstallIn`, escopos) que
quem chega no projeto precisa conhecer antes de entender o app.

Adotar uma ferramenta porque ela é o padrão, e não porque o problema a pede,
é o tipo de complexidade que parece profissionalismo e cobra manutenção.

## Consequências

- Cada nova dependência exige uma linha no `AppContainer`, manualmente.
- Se o grafo crescer a ponto de doer — múltiplos escopos, variantes, muitas
  implementações — a troca por Hilt é mecânica, porque **nada fora do
  `AppContainer` sabe como as dependências são construídas**. Esse
  isolamento é o que torna a decisão reversível, e é o que a justifica.
