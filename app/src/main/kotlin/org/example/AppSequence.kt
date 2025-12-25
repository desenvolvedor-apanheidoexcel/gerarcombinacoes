package org.example

import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYChart

class AppSequence {
    val greeting: String
        get() = "Iniciando Gerador Combinações Sequence..."

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            AppSequence().run()
        }
    }

    fun run() {
       
        println("Iniciando monitoramento de memória (por steps)...")

        val tempos = mutableListOf<Double>()
        val consumos = mutableListOf<Long>()
        val obrigatorios = setOf(1, 2)

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

        println("Gerando combinações com obrigatórios $obrigatorios...")
        var total = 0L
        for (jogo in gerarCombinacoesComObrigatoriosSeq(obrigatorios, ::registrarStep)) {
            total++
        }
        println("Gerados $total jogos que contém os números $obrigatorios.")

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
        require(obrigatorios.all { it in 1..25 }) { "Números fora do intervalo" }
        require(obrigatorios.size <= 15) { "Máximo de 15 números" }

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
            .title("Consumo de Memória ao longo do tempo")
            .xAxisTitle("Tempo (s)")
            .yAxisTitle("Memória (MB)")
            .build()

        chart.addSeries("Uso de Memória", tempos, consumos)
        SwingWrapper(chart).displayChart()
    }


}