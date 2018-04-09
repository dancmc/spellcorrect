package io.dancmc.spellcorrect

import java.util.concurrent.atomic.AtomicLong

/**
 * Damerau-Levenshtein algorithm, based on
 * Wagner, Robert A., and Roy Lowrance (1975)
 * An extension of the string-to-string correction problem. Journal of the ACM (JACM), 22(2), 177-183.
 */
class DamerauLevenshtein {

    var table = Array(100) { IntArray(100) { 0 } }
    val da = Array(128) { 0 }

    companion object {

        fun analyseBatch(misspellList: List<String>, correctList: List<String>, dictList: List<String>, dictSet: HashSet<String>): String {

            // initialise variables
            val overallSuggestionsList = ArrayList<ArrayList<String>>()
            val atomicTime = AtomicLong()


            // iterate through misspelled list and generate suggestions (every word should have at least 1 suggestion)
            parallelise(misspellList.size, overallSuggestionsList) { i ->
                val startTime = System.currentTimeMillis()

                val dl = DamerauLevenshtein()
                val subSuggestionsList = ArrayList<ArrayList<String>>()

                for (j in (i - 1) * 100..Math.min((i * 100) - 1, misspellList.size - 1)) {
                    var bestScore = Int.MAX_VALUE
                    val bestSuggestionsForWord = ArrayList<String>()
                    val misspelledWord = misspellList[j]

                    if (dictSet.contains(misspelledWord)) {
                        bestSuggestionsForWord.add(misspelledWord)
                    } else {
                        dictList.forEach {
                            val score = dl.analyseSingle(misspelledWord, it)
                            if (score < bestScore) {
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


            return analyseFindings("Damerau-Levenshtein", misspellList, correctList, overallSuggestionsList, atomicTime)

        }

    }

    fun analyseSingle(string1: String, string2: String): Int {


        da.fill(0)

        val lf = string1.length
        val lt = string2.length

        val max = Math.max(lf, lt)
        if(max>table.size){
            table = Array(max+2) { IntArray(max+2) { 0 } }
        }

        val maxDist = lf + lt

        table[0][0] = maxDist
        for (i in 0 until lf + 1) {
            table[i + 1][0] = maxDist
            table[i + 1][1] = i
        }
        for (j in 0 until lt + 1) {
            table[0][j + 1] = maxDist
            table[1][j + 1] = j
        }

        // when i/j/k/l used as table indices, must +2
        // when i/j/k/l used elsewhere besides as string index, must +1
        for (i in 1..lf) {
            var db = 0
            for (j in 1..lt) {
                val k = da[string2[j-1].toInt()]
                val l = db
                var cost: Int
                if (string1[i-1] == string2[j-1]) {
                    cost = 0
                    db = j
                } else {
                    cost = 1
                }

                table[i + 1][j + 1] =
                        Math.min(table[i][j] + cost,
                                Math.min(table[i+1][j] + 1,
                                        Math.min(table[i][j+1]+1,
                                                table[k][l] + (i-k-1) + 1 + (j-l-1))))
            }
            da[string1[i-1].toInt()] = i
        }

        return table[lf+1][lt+1]
    }


}