# Gerador de Combinações Numéricas & Monitoramento de Memória (Kotlin)

Este projeto é um protótipo para validar e demonstrar diferentes implementações de um gerador de combinações numéricas (jogos de 15 números entre 1 e 25 que incluem um conjunto de obrigatórios) e como cada abordagem impacta o consumo de memória da JVM. Ao final de cada execução, um gráfico é exibido com a evolução do uso de memória e, opcionalmente, marcadores de eventos de GC.

## Objetivo
- Comparar três variações de implementação e seus efeitos na memória:
  - `App`: medição externa e geração “em bloco”.
  - `AppSequence`: medição por steps com `Sequence`, ainda usando `List/Set` por combinação.
  - `AppSequenceTurb`: geração “streaming” com `IntArray` reaproveitado, métrica via `MemoryMXBean` e overlay de eventos de GC.

## Variações e Diferenças

### 1) `App` — Medição periódica externa + geração em bloco
- Arquivo: [app/src/main/kotlin/org/example/App.kt](app/src/main/kotlin/org/example/App.kt)
- Como funciona:
  - Mede memória a cada 0,5s usando `Runtime.totalMemory - freeMemory` em um loop de ~10s.
  - No meio do período, chama a geração de combinações e só mede o consumo novamente no próximo tick, após o processamento terminar.
  - Gera todas as combinações em memória, retornando `List<Set<Int>>`.
- Efeito na memória:
  - Alto consumo e picos (“serrote”): cada combinação materializa novas listas/sets, causando muita pressão no GC.
  - Métrica menos estável por usar `totalMemory - freeMemory` (sensível à expansão da heap).
- Má prática evidenciada:
  - “Gerar tudo e guardar”: acumular milhões de combinações em `List<Set<Int>>` é caro em memória.
  - Medir memória fora do ponto de maior churn (processo de geração), perdendo o detalhe dos steps.

### 2) `AppSequence` — Medição por steps + `Sequence` (lazy), porém com `List/Set`
- Arquivo: [app/src/main/kotlin/org/example/AppSequence.kt](app/src/main/kotlin/org/example/AppSequence.kt)
- Como funciona:
  - Implementa um gerador `Sequence<List<T>>` com backtracking que produz cada combinação lazily.
  - Mede memória “por step” (a cada N combinações) dentro do fluxo, sem acumular resultados em uma lista gigante.
- Efeito na memória:
  - Melhora relevante sobre `App`: não acumula todas as combinações e mede durante o processo.
  - Ainda há churn porque cada combinação cria novas `List/Set` (autoboxing de `Int` e alocações curtas), gerando eventos de GC ocasionais.
- Boas práticas adotadas:
  - “Streaming” com `Sequence` para evitar acumular.
  - Medição dentro do processo (callbacks por step).

### 3) `AppSequenceTurb` — IntArray + Métrica robusta + GC overlay
- Arquivo: [app/src/main/kotlin/org/example/AppSequenceTurb.kt](app/src/main/kotlin/org/example/AppSequenceTurb.kt)
- Como funciona:
  - Backtracking escreve cada combinação diretamente em um `IntArray` prealocado (`combo`) e o reaproveita em todas as combinações.
  - Evita criar `List<Int>`/`Set<Int>` e evitar `toSet()` por combinação.
  - Mede memória via `MemoryMXBean.heapMemoryUsage.used` — leitura mais estável da heap usada.
  - Registra eventos de GC via JMX (`GarbageCollectionNotificationInfo`) e sobrepõe marcadores “GC” no gráfico.
  - `stepInterval` configurável por argumento (CLI) ou propriedade de projeto.
- Efeito na memória:
  - Curva quase plana, com pequenos degraus até ~10–11 MB, confirmando baixa alocação e pouca pressão de GC.
  - Marcadores “GC” aparecem raramente; quando presentes, correlacionam-se com pequenos ajustes da linha de memória.
- Boas práticas adotadas:
  - Reutilização de buffers (`IntArray`).
  - Medição no ponto crítico (por step, dentro da geração).
  - Observabilidade do GC embutida no gráfico e no console.

## O que foi má prática e como corrigimos
- Má prática:
  - Criar `List<Set<Int>>` com todas as combinações: explode memória e força GC frequente.
  - Medição só “do lado de fora” (timer), sem granularidade do processo.
  - Uso de estruturas com autoboxing (`List<Int>`, `Set<Int>`) por combinação.
- Correções (boas práticas):
  - “Streaming” real: produzir e consumir por passo (sem acumular tudo).
  - Usar `IntArray` e buffer reaproveitado para evitar alocações curtas.
  - Medir com `MemoryMXBean` e, opcionalmente, fixar heap (`-Xms = -Xmx`) em tarefas de diagnóstico.

## Como acessar as combinações geradas em cada variação
- `App`:
  - `gerarCombinacoesComObrigatorios(...)` retorna `List<Set<Int>>`.
  - Cuidado: manter tudo em memória é caro. Útil apenas para casos pequenos.
- `AppSequence`:
  - Retorna `Sequence<Set<Int>>`. Itere e processe a cada item, sem acumular.
  - Ex.: `for (jogo in gerarCombinacoesComObrigatoriosSeq(obrig, onStep)) { /* processa */ }`.
- `AppSequenceTurb`:
  - `gerarCombinacoesComObrigatoriosInt(obrig, onStep, onCombination)` NÃO acumula; chama `onCombination(IntArray)` por combinação quando fornecido.
  - Ex.: passar um lambda que copie/filtre/escreva para arquivo conforme necessário.

## Complexidade e contagens
- A contagem segue $C(n, k) = \frac{n!}{k!(n-k)!}$. Com 2 obrigatórios, sobram $n=23$ e faltam $k=13$, resultando em $C(23,13) = 1{,}144{,}066$ combinações.
- Aumentar o número de obrigatórios reduz `k` e o total de combinações.

## Dependências
- Kotlin Stdlib.
- XChart (`org.knowm.xchart`) para gráficos.
- JUnit 5 para testes.
- Guava (utilitários de apoio).

Veja o `build.gradle.kts` do módulo `app` para versões e toolchain (Java 21).

## Rodando no VS Code
- Abra o terminal integrado do VS Code na raiz do workspace.
- Comandos (Windows):
```bat
.\gradlew.bat clean
.\gradlew.bat build
```
- Execuções sem trocar `mainClass` (tasks dedicadas no módulo `app`):
```bat
.\gradlew.bat clean runApp   // roda App (top-level main: AppKt)
.\gradlew.bat clean runSeq   // roda AppSequence (streaming com Sequence)
.\gradlew.bat clean runTurb  // roda AppSequenceTurb (IntArray + GC overlay)
```
- Ajustar frequência de amostra (TURB):
```bat
.\gradlew.bat runTurb -PstepInterval=5000
```
- Alternativamente, via `application.mainClass`:
  - Em [app/build.gradle.kts](app/build.gradle.kts), defina `mainClass` para:
    - `org.example.AppKt` (App)
    - `org.example.AppSequence` (AppSequence)
    - `org.example.AppSequenceTurb` (TURB)
  - E rode:
```bat
.\gradlew.bat run
```

## Saída e Observabilidade
- Console: logs de progressão e total de combinações geradas.
- Gráfico (XChart): série “Uso de Memória” ao longo do tempo.
- TURB: série “GC” com marcadores (diamantes) indicando eventos de coleta. No console, resumo com ação, causa, duração e heap antes/depois do último GC.
 - Total final: ao término da execução, todos os 3 variantes imprimem o total de combinações geradas no formato “Total de jogos gerados: X”, onde X segue \(C(25 - |obrigatórios|,\; 15 - |obrigatórios|)\).
 - Reimpressão no TURB: quando `-Pstart`/`-Pqtd` são usados, os jogos selecionados também são reimpressos no final como um pequeno resumo.

## Estrutura do Projeto
- Variações:
  - [App](app/src/main/kotlin/org/example/App.kt)
  - [AppSequence](app/src/main/kotlin/org/example/AppSequence.kt)
  - [AppSequenceTurb](app/src/main/kotlin/org/example/AppSequenceTurb.kt)
- Build do módulo: [app/build.gradle.kts](app/build.gradle.kts)
- Testes: [app/src/test/kotlin/org/example/AppTest.kt](app/src/test/kotlin/org/example/AppTest.kt)

## Executando com argumentos (obrigatórios)
- Formatos aceitos: `1,2,3` ou `1 2 3` ou `1;2;3`.
- Via propriedade de projeto `-Pobrigatorios`:
```bat
.\gradlew.bat runApp -Pobrigatorios=1,2,3
.\gradlew.bat runSeq -Pobrigatorios=1 2 3
.\gradlew.bat runTurb -Pobrigatorios=1,2,3 -PstepInterval=5000
```
- Via `--args` (argumentos diretos):
  - `App` e `AppSequence` esperam apenas a lista de obrigatórios:
  ```bat
  .\gradlew.bat runApp --args="1,2,3"
  .\gradlew.bat runSeq --args="1 2 3"
  ```
  - `AppSequenceTurb` espera primeiro `stepInterval` e depois os obrigatórios (opcional):
  ```bat
  .\gradlew.bat runTurb --args="5000 1,2,3"
  .\gradlew.bat runTurb --args="10000"   // sem obrigatórios, usa padrão 1,2
  ```

## Intervalo de impressão (start/qtd)
- Todos os 3 variantes aceitam um intervalo opcional para imprimir apenas uma fatia das combinações geradas:
  - `-Pstart`: índice inicial (1-based)
  - `-Pqtd`: quantidade de combinações a imprimir
- Exemplos (Windows):
  ```bat
  .\gradlew.bat runApp -Pobrigatorios=1,2 -Pstart=1500 -Pqtd=1000
  .\gradlew.bat runSeq -Pobrigatorios="1 2" -Pstart=1500 -Pqtd=1000
  .\gradlew.bat runTurb -Pobrigatorios=1,2 -Pstart=1500 -Pqtd=1000 -PstepInterval=5000
  ```
- Observações:
  - Em `App` (em bloco), o intervalo é recortado após gerar a lista completa.
  - Em `AppSequence` e `TURB`, a geração é streaming; o intervalo controla apenas o que é impresso, sem acumular tudo.

## Dicas finais
- Para experimentar perfis de memória, fixe heap nas tasks (ex.: `-Xms512m -Xmx512m` em `runTurb`).
- Evite copiar o `IntArray` no `onCombination` se não for necessário; processe inline.
- Use conjuntos de obrigatórios maiores nos testes para reduzir o espaço combinatório.

---
Em resumo: `App` demonstra o problema de “gerar tudo e guardar”; `AppSequence` melhora com streaming e medição por step; `AppSequenceTurb` maximiza a eficiência com `IntArray`, métrica robusta e overlay de GC — resultando em uma curva de memória estável e baixa.