package org.example

import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.XYChart
import org.knowm.xchart.style.lines.SeriesLines
import org.knowm.xchart.style.markers.SeriesMarkers
import java.lang.management.ManagementFactory
import javax.management.NotificationEmitter
import javax.management.NotificationListener
import javax.management.openmbean.CompositeData
import com.sun.management.GarbageCollectionNotificationInfo

class AppSequenceTurb {

    private val memoryMxBean = ManagementFactory.getMemoryMXBean()
    private val gcEvents = mutableListOf<GcEvent>()
    private var startNs: Long = 0L

    val greeting: String
        get() = "Iniciando Gerador Combinacoes TURB (IntArray + GC)..."

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val stepIntervalArg = System.getProperty("stepInterval")?.toIntOrNull()
                ?: args.firstOrNull()?.toIntOrNull()
                ?: 10_000
            val obrigRaw = System.getProperty("obrigatorios") ?: args.drop(1).firstOrNull()
            AppSequenceTurb().run(stepIntervalArg, obrigRaw)
        }
    }

    fun run(stepInterval: Int, obrigRaw: String?) {
        println(greeting)
        registerGcListener()

        val tempos = mutableListOf<Double>()
        val consumos = mutableListOf<Long>()
        val obrigatorios = parseObrigatoriosToIntArray(obrigRaw)

        startNs = System.nanoTime()

        fun registrarStep(step: Int) {
            if (step % stepInterval == 0) {
                val elapsedS = (System.nanoTime() - startNs) / 1e9
                tempos.add(elapsedS)
                consumos.add(memoriaHeapMB())
                println("Step $step: ${consumos.last()} MB")
            }
        }

        println("Gerando combinacoes com obrigatorios ${obrigatorios.joinToString()}...")
        val total = gerarCombinacoesComObrigatoriosInt(obrigatorios, ::registrarStep, onCombination = null)
        println("Gerados $total jogos que contem os numeros ${obrigatorios.joinToString()}.")

        if (gcEvents.isNotEmpty()) {
            println("\nResumo GC (${gcEvents.size} eventos):")
            val totalGcMs = gcEvents.sumOf { it.durationMs }
            println("Tempo total em GC: ${totalGcMs} ms")
            val last = gcEvents.last()
            println("Ultimo GC: ${last.action} / ${last.cause}, duracao ${last.durationMs} ms, heap ${last.usedBeforeMB}MB -> ${last.usedAfterMB}MB")
        }

        plotarGrafico(tempos, consumos)
    }

    private fun memoriaHeapMB(): Long = memoryMxBean.heapMemoryUsage.used / (1024 * 1024)

    private fun parseObrigatoriosToIntArray(raw: String?): IntArray {
        val source = raw ?: System.getProperty("obrigatorios")
        if (source.isNullOrBlank()) return intArrayOf(1, 2)
        val nums = source.split(',', ' ', ';')
            .filter { it.isNotBlank() }
            .map { it.trim().toInt() }
        require(nums.all { it in 1..25 }) { "Numeros fora do intervalo 1..25" }
        val unique = nums.toSet()
        require(unique.size <= 15) { "Maximo de 15 numeros obrigatorios" }
        return unique.sorted().toIntArray()
    }

    fun gerarCombinacoesComObrigatoriosInt(
        obrigatorios: IntArray,
        onStep: (Int) -> Unit,
        onCombination: ((IntArray) -> Unit)?
    ): Long {
        require(obrigatorios.all { it in 1..25 }) { "Numeros fora do intervalo" }
        require(obrigatorios.distinct().size == obrigatorios.size) { "Obrigatorios repetidos" }
        require(obrigatorios.size <= 15) { "Maximo de 15 números" }

        val universo = IntArray(25) { it + 1 }
        val isObrig = BooleanArray(26)
        for (v in obrigatorios) isObrig[v] = true

        val restantes = IntArray(25 - obrigatorios.size)
        var ri = 0
        for (v in universo) if (!isObrig[v]) { restantes[ri++] = v }

        val kRest = 15 - obrigatorios.size
        val combo = IntArray(15)
        for (i in obrigatorios.indices) combo[i] = obrigatorios[i]

        var count = 0L

        fun backtrack(startIdx: Int, depthRest: Int, writePos: Int) {
            if (depthRest == kRest) {
                count++
                onStep(count.toInt())
                onCombination?.invoke(combo)
                return
            }
            val remainingSlots = kRest - depthRest
            val maxStart = restantes.size - remainingSlots
            var i = startIdx
            while (i <= maxStart) {
                combo[writePos] = restantes[i]
                backtrack(i + 1, depthRest + 1, writePos + 1)
                i++
            }
        }

        if (kRest >= 0) backtrack(0, 0, obrigatorios.size)
        return count
    }

    private fun registerGcListener() {
        val gcs = ManagementFactory.getGarbageCollectorMXBeans()
        for (gc in gcs) {
            if (gc is NotificationEmitter) {
                val listener = NotificationListener { notification, _ ->
                    if (notification.type == GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION) {
                        val cd = notification.userData as CompositeData
                        val info = GarbageCollectionNotificationInfo.from(cd)
                        val before = info.gcInfo.memoryUsageBeforeGc.values.sumOf { it.used }
                        val after = info.gcInfo.memoryUsageAfterGc.values.sumOf { it.used }
                        val event = GcEvent(
                            timeSeconds = System.nanoTime() / 1e9,
                            action = info.gcAction,
                            cause = info.gcCause,
                            durationMs = info.gcInfo.duration,
                            usedBeforeMB = (before / (1024 * 1024)).toLong(),
                            usedAfterMB = (after / (1024 * 1024)).toLong()
                        )
                        gcEvents.add(event)
                    }
                }
                gc.addNotificationListener(listener, null, null)
            }
        }
    }

    fun plotarGrafico(tempos: List<Double>, consumos: List<Long>) {
        val chart: XYChart = XYChartBuilder()
            .width(800).height(600)
            .title("Consumo de Memoria ao longo do tempo")
            .xAxisTitle("Tempo (s)")
            .yAxisTitle("Memoria (MB)")
            .build()

        chart.addSeries("Uso de Memoria", tempos, consumos)

        if (gcEvents.isNotEmpty()) {
            val startSeconds = startNs / 1e9
            val gcTimes = gcEvents.map { it.timeSeconds - startSeconds }
            val gcMem = gcEvents.map { it.usedAfterMB.toDouble() }
            val gcSeries = chart.addSeries("GC", gcTimes, gcMem)
            gcSeries.marker = SeriesMarkers.DIAMOND
            gcSeries.lineStyle = SeriesLines.NONE
        }

        SwingWrapper(chart).displayChart()
    }
}

data class GcEvent(
    val timeSeconds: Double,
    val action: String,
    val cause: String,
    val durationMs: Long,
    val usedBeforeMB: Long,
    val usedAfterMB: Long
)

