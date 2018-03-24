package io.dancmc.spellcorrect

import org.apache.commons.codec.language.Soundex
import java.util.concurrent.atomic.AtomicLong

class Soundex {

    companion object {

        fun analyseBatch(misspellList: List<String>, correctList: List<String>, dictList: List<String>,dictSet: HashSet<String>):String{

            // initialise variables
            val overallSuggestionsList = ArrayList<ArrayList<String>>()
            val atomicTime = AtomicLong()


            parallelise(misspellList.size, overallSuggestionsList) { i ->
                val startTime = System.currentTimeMillis()

                val subSuggestionsList = ArrayList<ArrayList<String>>()
                val soundex = Soundex()

                for (j in (i - 1) * 100..Math.min((i * 100) - 1, misspellList.size - 1)) {
                    var bestScore = Int.MIN_VALUE
                    val bestSuggestionsForWord = ArrayList<String>()
                    val misspelledWord = misspellList[j]

                    if(dictSet.contains(misspelledWord)){
                        bestSuggestionsForWord.add(misspelledWord)
                    } else {
                        dictList.forEach {
                            val score = soundex.difference(misspelledWord, it)
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

            return analyseFindings("Soundex", misspellList, correctList, overallSuggestionsList, atomicTime)
        }
    }
}