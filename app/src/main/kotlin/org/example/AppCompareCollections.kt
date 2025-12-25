import kotlin.system.measureTimeMillis

fun main() {
    val tamanho = 1_000_000
    val alvo = tamanho - 1 // número que queremos buscar

    // Criando estruturas
    val lista = (1..tamanho).toList()
    val conjunto = (1..tamanho).toSet()
    val array = IntArray(tamanho) { it + 1 }

    // Teste com List<Int>
    val tempoLista = measureTimeMillis {
        println("List contem $alvo? ${lista.contains(alvo)}")
    }

    // Teste com Set<Int>
    val tempoSet = measureTimeMillis {
        println("Set contem $alvo? ${conjunto.contains(alvo)}")
    }

    // Teste com IntArray
    val tempoArray = measureTimeMillis {
        println("IntArray contem $alvo? ${array.contains(alvo)}")
    }

    println("\nResultados:")
    println("List<Int>   -> $tempoLista ms")
    println("Set<Int>    -> $tempoSet ms")
    println("IntArray    -> $tempoArray ms")
}

