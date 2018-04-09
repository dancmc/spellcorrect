package io.dancmc.spellcorrect

import java.util.concurrent.atomic.AtomicLong


/**
 * Needleman-Wunsch algorithm, based on KT lectures
 */
class NeedlemanWunsch {

    // initialise table which will be reused to save memory
    var table = Array(100) { IntArray(100) { 0 } }

    companion object {

        const val indelPenalty = -1

        fun equal(char1: Char, char2: Char): Int {
            return if (char1 == char2) 1 else -1
        }

        fun analyseBatch(misspellList: List<String>, correctList: List<String>, dictList: List<String>, dictSet:HashSet<String>) :String{

            // initialise variables
            val overallSuggestionsList = ArrayList<ArrayList<String>>()
            val atomicTime = AtomicLong()


            // iterate through misspelled list and generate suggestions (every word should have at least 1 suggestion)
            parallelise(misspellList.size, overallSuggestionsList) { i ->
                val startTime = System.currentTimeMillis()

                val nw = NeedlemanWunsch()
                val subSuggestionsList = ArrayList<ArrayList<String>>()

                for (j in (i - 1) * 100..Math.min((i * 100) - 1, misspellList.size - 1)) {
                    var bestScore = Int.MIN_VALUE
                    val bestSuggestionsForWord = ArrayList<String>()
                    val misspelledWord = misspellList[j]

                    if(dictSet.contains(misspelledWord)){
                        bestSuggestionsForWord.add(misspelledWord)
                    } else {
                        dictList.forEach {
                            val score = nw.analyseSingle(misspelledWord, it)
                            if (score > bestScore) {
                                bestScore = score
                                bestSuggestionsForWord.clear()
                                bestSuggestionsForWord.add(it)
                            } else if (score == bestScore) {
                                bestSuggestionsForWord.add(it)
                            }
                        }
                    }

                    subSuggestionsList.add(bestSuggestionsForWord)
                }

                atomicTime.addAndGet(System.currentTimeMillis() - startTime)

                return@parallelise subSuggestionsList
            }

            return analyseFindings("Needleman-Wunsch", misspellList, correctList, overallSuggestionsList, atomicTime)

        }


    }

    fun analyseSingle(string1: String, string2: String): Int {

        val lf = string1.length
        val lt = string2.length

        val max = Math.max(lf, lt)
        if(max>table.size){
            table = Array(max+1) { IntArray(max+1) { 0 } }
        }


        for (i in 0 until lf + 1) {
            table[0][i] = -i
        }

        for (i in 0 until lt) {

            val previousRow = table[i]
            val currentRow = table[i + 1]

            currentRow[0] = -i - 1

            for (j in 0 until lf) {
                currentRow[j + 1] =
                        Math.max(currentRow[j] + indelPenalty,
                                Math.max(previousRow[j + 1] + indelPenalty,
                                        previousRow[j] + equal(string1[j], string2[i])))
            }

        }

//        table.take(15).forEach {
//            println(it.joinToString { int-> StringUtils.leftPad(int.toString(), 2) })
//        }

        return table[lt][lf]
    }


}