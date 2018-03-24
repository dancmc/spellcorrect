package io.dancmc.spellcorrect

import org.apache.commons.text.similarity.JaroWinklerDistance
import java.util.concurrent.atomic.AtomicLong

class JaroWinkler {

    companion object {


        fun analyseBatch(misspellList: List<String>, correctList: List<String>, dictList: List<String>, dictSet:HashSet<String>) :String{

            // initialise variables
            val overallSuggestionsList = ArrayList<ArrayList<String>>()
            val atomicTime = AtomicLong()


            // iterate through misspelled list and generate suggestions (every word should have at least 1 suggestion)
            parallelise(misspellList.size, overallSuggestionsList) { i ->
                val startTime = System.currentTimeMillis()

                val jw = JaroWinklerDistance()
                val subSuggestionsList = ArrayList<ArrayList<String>>()

                for (j in (i - 1) * 100..Math.min((i * 100) - 1, misspellList.size - 1)) {
                    var bestScore = Double.MIN_VALUE
                    val bestSuggestionsForWord = ArrayList<String>()
                    val misspelledWord = misspellList[j]

                    if(dictSet.contains(misspelledWord)){
                        bestSuggestionsForWord.add(misspelledWord)
                    } else {
                        dictList.forEach {
                            val score = jw.apply(misspelledWord, it)
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

            return analyseFindings("Jaro-Winkler", misspellList, correctList, overallSuggestionsList, atomicTime)

        }


    }


}