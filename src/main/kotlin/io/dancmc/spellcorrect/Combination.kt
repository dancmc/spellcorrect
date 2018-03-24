package io.dancmc.spellcorrect

import java.util.concurrent.atomic.AtomicLong

class Combination {


    companion object {

        fun analyseBatch(misspellList: List<String>, correctList: List<String>, dictSet: HashSet<String>):String {
            // initialise variables
            val overallSuggestionsList = ArrayList<ArrayList<String>>()
            val atomicTime = AtomicLong()


            parallelise(misspellList.size, overallSuggestionsList) { i ->
                val startTime = System.currentTimeMillis()

                val subSuggestionsList = ArrayList<ArrayList<String>>()

                val ngram = NGram(2)
//                val nw = NeedlemanWunsch()
//                val lv = Levenshtein()

                for (j in (i - 1) * 100..Math.min((i * 100) - 1, misspellList.size - 1)) {
                    val misspelledWord = misspellList[j]
                    var bestScore = Int.MAX_VALUE

                    val nnSuggestionList = Neighbourhood.analyseSingle(misspellList[j], dictSet,Neighbourhood.Edits.DOUBLE)

                    val ngramFilteredList = ArrayList<String>()

                    nnSuggestionList.forEach {
                        val score = ngram.analyseSingle(misspelledWord, it)
                        if (score < bestScore) {
                            bestScore = score
                            ngramFilteredList.clear()
                            ngramFilteredList.add(it)
                        } else if (score == bestScore) {
                            ngramFilteredList.add(it)
                        }
                    }

                    subSuggestionsList.add(ngramFilteredList)
                }

                atomicTime.addAndGet(System.currentTimeMillis() - startTime)

                return@parallelise subSuggestionsList
            }

            return analyseFindings("NN + NGram(2)", misspellList, correctList, overallSuggestionsList, atomicTime)
        }


    }

}