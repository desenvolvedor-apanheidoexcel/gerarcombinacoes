package org.example

import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYChart

class AppSequence {
    val greeting: String
        get() = "Iniciando Gerador Combinacoes Sequence..."

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val obrigatorios = parseObrigatoriosFromArgs(args)
            val (start, qtd) = parseIntervalFromArgs(args)
            AppSequence().run(obrigatorios, start, qtd)
        }
    }

    fun run(obrigatorios: Set<Int>, start: Int?, qtd: Int?) {
       
        println("Iniciando monitoramento de memoria (por steps)...")

        val tempos = mutableListOf<Double>()
        val consumos = mutableListOf<Long>()

        val inicioNs = System.nanoTime()
        val stepInterval = 10_000

        fun registrarStep(step: Int) {
            if (step % stepInterval == 0) {
                val elapsedS = (System.nanoTime() - inicioNs) / 1e9
                tempos.add(elapsedS)
                consumos.add(memoriaUsadaMB())
                println("Step $step: ${consumos.last()} MB")
            }
        }

        println("Gerando combinacoes com obrigatorios $obrigatorios...")
        val seq = gerarCombinacoesComObrigatoriosSeq(obrigatorios, ::registrarStep)
        if (start != null && qtd != null && start >= 1 && qtd > 0) {
            val end = start + qtd - 1
            var pos = 0L
            var printed = 0L
            for (jogo in seq) {
                pos++
                if (pos in start.toLong()..end.toLong()) {
                    println("Jogo #$pos: ${jogo.sorted()} ")
                    printed++
                    if (printed >= qtd) break
                }
            }
            println("Impressos $printed jogos do intervalo [$start..$end].")
        } else {
            var total = 0L
            for (jogo in seq) {
                total++
            }
            println("Gerados $total jogos que contem os numeros $obrigatorios.")
        }

        // Sempre imprimir o total de combinações geradas ao final
        run {
            val n = 25 - obrigatorios.size
            val k = 15 - obrigatorios.size
            val totalFinal = binomial(n, k)
            println("Total de jogos gerados: $totalFinal.")
        }

        if (tempos.isEmpty()) {
            val elapsedS = (System.nanoTime() - inicioNs) / 1e9
            tempos.add(elapsedS)
            consumos.add(memoriaUsadaMB())
        }
        plotarGrafico(tempos, consumos)
    }

    

    fun memoriaUsadaMB(): Long {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return usedBytes / (1024 * 1024)
    }

    fun gerarCombinacoesComObrigatoriosSeq(
        obrigatorios: Set<Int>,
        onStep: (Int) -> Unit
    ): Sequence<Set<Int>> {
        require(obrigatorios.all { it in 1..25 }) { "Numeros fora do intervalo" }
        require(obrigatorios.size <= 15) { "Maximo de 15 numeros" }

        val restante = (1..25).toSet() - obrigatorios
        val faltam = 15 - obrigatorios.size

        return combinacoesSeq(restante.toList(), faltam, onStep)
            .map { obrigatorios + it.toSet() }
    }

    fun <T> combinacoesSeq(
        lista: List<T>,
        k: Int,
        onStep: (Int) -> Unit
    ): Sequence<List<T>> = sequence {
        var count = 0
        val n = lista.size

        suspend fun SequenceScope<List<T>>.backtrack(start: Int, path: MutableList<T>) {
            if (path.size == k) {
                yield(path.toList())
                count++
                onStep(count)
                return
            }
            val maxStart = n - (k - path.size)
            for (i in start..maxStart) {
                path.add(lista[i])
                backtrack(i + 1, path)
                path.removeAt(path.lastIndex)
            }
        }

        if (k in 0..n) backtrack(0, mutableListOf())
    }

    fun plotarGrafico(tempos: List<Double>, consumos: List<Long>) {
        val chart = XYChartBuilder()
            .width(800).height(600)
            .title("Consumo de Memoria ao longo do tempo")
            .xAxisTitle("Tempo (s)")
            .yAxisTitle("Memoria (MB)")
            .build()

        chart.addSeries("Uso de Memoria", tempos, consumos)
        SwingWrapper(chart).displayChart()
    }


}

fun parseObrigatoriosFromArgs(args: Array<String>): Set<Int> {
    val raw = System.getProperty("obrigatorios") ?: args.firstOrNull()
    if (raw.isNullOrBlank()) return setOf(1, 2)
    val nums = raw.split(',', ' ', ';')
        .filter { it.isNotBlank() }
        .map { it.trim().toInt() }
    require(nums.all { it in 1..25 }) { "Numeros fora do intervalo 1..25" }
    val set = nums.toSet()
    require(set.size <= 15) { "Maximo de 15 numeros obrigatorios" }
    return set
}

fun parseIntervalFromArgs(args: Array<String>): Pair<Int?, Int?> {
    val startProp = System.getProperty("start")
    val qtdProp = System.getProperty("qtd")
    val start = startProp?.toIntOrNull()
    val qtd = qtdProp?.toIntOrNull()
    if (start != null && qtd != null) return start to qtd

    // Fallback: try to find two integers in args (supports --args="1500 1000")
    val nums = args.mapNotNull { it.toIntOrNull() }
    return if (nums.size >= 2) nums[0] to nums[1] else null to null
}

fun binomial(n: Int, kInput: Int): Long {
    require(n >= 0 && kInput >= 0 && kInput <= n) { "Parâmetros inválidos para binomial" }
    var k = kInput
    if (k > n - k) k = n - k
    var res = 1L
    for (i in 1..k) {
        res = res * (n - k + i) / i
    }
    return res
}