# Gerar Combinações & Monitoramento de Memória (Kotlin)

Este projeto monitora o consumo de memória da JVM ao longo do tempo e, no meio da execução, gera todas as combinações de 15 números (entre 1 e 25) que obrigatoriamente incluem um conjunto de números informado. Ao final, um gráfico é exibido com a evolução do uso de memória.

## Visão Geral
- Monitora memória em intervalos de 0,5s por ~10s.
- Na metade do período, executa a geração de combinações com números obrigatórios.
- Plota um gráfico utilizando a biblioteca XChart.

Pontos principais no código:
- `main`: orquestra o monitoramento, a geração de combinações e a plotagem do gráfico. Veja [app/src/main/kotlin/org/example/App.kt](app/src/main/kotlin/org/example/App.kt#L15-L40).
- `memoriaUsada()`: mede a memória utilizada pela JVM em MB. Veja [implementação](app/src/main/kotlin/org/example/App.kt#L42-L46).
- `gerarCombinacoesComObrigatorios(...)`: gera jogos de 15 números com obrigatórios. Veja [implementação](app/src/main/kotlin/org/example/App.kt#L48-L57).
- `combinacoes(lista, k)`: gera todas as combinações de tamanho `k` usando recursão. Veja [implementação](app/src/main/kotlin/org/example/App.kt#L59-L70).
- `plotarGrafico(...)`: monta e exibe o gráfico de memória. Veja [implementação](app/src/main/kotlin/org/example/App.kt#L72-L83).

## Como Funciona
1. O `main` inicia listas de tempo (`tempos`) e consumo de memória (`consumos`).
2. Define os números obrigatórios: veja [linha contendo `obrigatorios`](app/src/main/kotlin/org/example/App.kt#L21).
3. Executa um laço de 21 iterações (0..20), coletando memória a cada 0,5s.
4. Ao alcançar `i == 10`, chama `gerarCombinacoesComObrigatorios(obrigatorios)` e imprime o total gerado.
5. No fim, chama `plotarGrafico(...)` para exibir o gráfico com as séries coletadas.

## Algoritmo de Combinações
- `gerarCombinacoesComObrigatorios(...)` valida:
  - Todos os números devem estar em 1..25.
  - No máximo 15 números obrigatórios.
- Calcula quantos números faltam para completar 15 e gera todas as combinações possíveis a partir do conjunto restante.
- A função `combinacoes(lista, k)` é recursiva e retorna todas as listas de tamanho `k`:
  - Caso base: `k == 0` → lista vazia; lista vazia → sem combinações.
  - Passo recursivo: soma das soluções com/sem o primeiro elemento.

### Complexidade
O número de combinações cresce segundo $C(n, k) = \frac{n!}{k!(n-k)!}$. Por exemplo, com 2 obrigatórios:
- Restam $n = 23$ números e faltam $k = 13$.
- Isso rende $C(23, 13) \approx 497.420$ combinações.

Geração de centenas de milhares de combinações pode consumir muita memória e tempo. Este é justamente o efeito demonstrado no gráfico de consumo.

## Executando
Pré-requisitos: Java (JDK) e internet para baixar dependências do Gradle.

Windows:
```powershell
./gradlew.bat run
```

macOS/Linux:
```bash
./gradlew run
```

Testes:
```powershell
./gradlew.bat test
```

## Configuração
- Alterar números obrigatórios: edite [app/src/main/kotlin/org/example/App.kt](app/src/main/kotlin/org/example/App.kt#L21).
- Restrições:
  - Todos os números devem estar em `1..25`.
  - Tamanho máximo é sempre 15 (jogos de 15 números).
- Dicas para evitar explosão combinatória:
  - Aumente a quantidade de obrigatórios (reduz `k`).
  - Faça testes com conjuntos menores para validar lógica.

## Dependências
- Kotlin + Gradle.
- XChart (`org.knowm.xchart`) para gráficos (já configurado no build).

## Saída
- Console: mensagens de progresso e total de jogos gerados.
- Janela gráfica: linha do uso de memória em MB ao longo do tempo.

## Estrutura do Projeto
- Código principal: [app/src/main/kotlin/org/example/App.kt](app/src/main/kotlin/org/example/App.kt)
- Testes: [app/src/test/kotlin/org/example/AppTest.kt](app/src/test/kotlin/org/example/AppTest.kt)

## Observações
- A classe `App` possui apenas um exemplo de `greeting` e não é usada pelo fluxo principal.
- Para cenários reais, considere geração preguiçosa (lazy) ou filtros mais restritivos para reduzir memória.
