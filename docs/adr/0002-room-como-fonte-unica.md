# ADR 0002 — Room como fonte única da verdade

**Status:** aceito
**Data:** 2026-09

## Contexto

O app precisa guardar carretas, ativos de giro, avarias e fotos num aparelho
que fica o turno inteiro sem rede e pode ser fechado pelo sistema a qualquer
momento. As alternativas eram arquivo JSON, `SharedPreferences`/DataStore
para tudo, ou banco relacional.

## Decisão

**Room** (SQLite) para os dados operacionais, **DataStore** para
configuração e cadastros, e **arquivo** para as fotos.

## Motivos

Os dados são relacionais de verdade: uma carreta tem N ativos de giro e N
avarias, e as consultas reais da tela são filtros — o que está aberto, o
que foi encerrado e ainda não exportado, quantas vezes esta placa apareceu
hoje. Isso é `WHERE` e `INDEX`, não varredura de lista em memória.

Três ganhos concretos sobre JSON:

1. **Migração versionada.** `exportSchema = true` deixa o schema em
   `app/schemas`, versionado no repositório, e toda mudança de estrutura
   entra como migração escrita e revisável. Não existe
   `fallbackToDestructiveMigration`: apagar o banco numa atualização
   significaria perder turno de coleta.
2. **Consulta verificada em tempo de compilação.** Um erro de SQL falha o
   build, não o turno.
3. **Escrita atômica.** Nada de arquivo meio gravado quando o sistema
   encerra o app.

Configuração vai para DataStore porque é um punhado de valores sem relação
entre si e sem histórico — colocá-los no banco seria cerimônia sem retorno.

Foto vai para arquivo porque imagem em BLOB incha o banco e atrasa toda
consulta que faz `SELECT *`, sem nenhuma vantagem. O banco guarda o nome; o
`FotoStore` guarda o arquivo.

## Consequências

- Duas fontes de persistência para coordenar, e a limpeza da foto órfã
  passa a ser responsabilidade explícita do repositório (é o que faz
  `salvarAvaria` ao detectar troca de imagem).
- Uma dependência de processador de anotação (KSP) no build.
