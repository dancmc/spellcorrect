package io.dancmc.spellcorrect

import java.util.Arrays
import java.util.concurrent.atomic.AtomicLong


/**
 * NGram algorithm, based on KT lectures
 */
class NGram(val ngramLength : Int) {

    val string1NgramHashArray = LongArray(100){0}
    val string2NgramHashArray = LongArray(100){0}

    init {
        if(ngramLength>7 || ngramLength<2){
            println("Ngram lengths should be between 2 and 7 inclusive!")
        }
    }

    companion object {

        private val alphabet = "abcdefghijklmnopqrstuvwxyz_"

        // converts native int representation of character to unique integer from 1-27 and stores in table
        private val alphabetTable  = {
            val table = IntArray(200){0}
            alphabet.forEachIndexed { index, c ->
                table[c.toInt()] = index+1
            }
            table
        }()

        private val hashPowers = LongArray(7){Math.pow(27.0, it.toDouble()).toLong()}

        fun hashNgram(ngram:String):Long{
            var hashValue = 0L
            ngram.forEachIndexed { index, c ->
                val characterHash = alphabetTable[c.toInt()] * hashPowers[ngram.length-1-index]
                if(characterHash!=0L){
                    hashValue+=characterHash
                } else {
                    return 0L
                }
            }
            return hashValue
        }


        fun generateNgram(s:String, ngramSize:Int, array:LongArray){
            val padded = "${"_".repeat(ngramSize-1)}$s${"_".repeat(ngramSize-1)}"

            // length of padded string is s.length + 2 * (ngramSize-1)
            for (i in 0 until s.length+ngramSize-1){
                array[i] = hashNgram(padded.substring(i, i + ngramSize))
            }

        }



        fun analyseBatch(ngramSize: Int, misspellList: List<String>, correctList: List<String>, dictList: List<String>, dictSet: HashSet<String>): String {

            // initialise variables
            val overallSuggestionsList = ArrayList<ArrayList<String>>()
            val atomicTime = AtomicLong()


            parallelise(misspellList.size, overallSuggestionsList) { i ->
                val startTime = System.currentTimeMillis()
                val subSuggestionsList = ArrayList<ArrayList<String>>()

                val ngram = NGram(ngramSize)

                for (j in (i - 1) * 100..Math.min((i * 100) - 1, misspellList.size - 1)) {
                    var bestScore = Int.MAX_VALUE
                    val bestSuggestionsForWord = ArrayList<String>()
                    val misspelledWord = misspellList[j]

                    if (dictSet.contains(misspelledWord)) {
                        bestSuggestionsForWord.add(misspelledWord)
                    } else {
                        dictList.forEach {
                            val score = ngram.analyseSingle(misspelledWord, it)
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

            return analyseFindings("NGram ($ngramSize)", misspellList, correctList, overallSuggestionsList, atomicTime)
        }

        fun analyseBatchOptimised(ngramLength:Int, misspellList: List<String>, correctList: List<String>, dictList: List<String>, dictSet: HashSet<String>): String {

            // initialise variables
            val overallSuggestionsList = ArrayList<ArrayList<String>>()
            val atomicTime = AtomicLong()

            // create arraylist of dictionary ngram hash arrays
            val dictWordArrayList = ArrayList<LongArray>()
            dictList.forEach {
                val dictWordArray = LongArray(it.length+ngramLength-1)
                generateNgram(it, ngramLength, dictWordArray)
                Arrays.sort(dictWordArray)
                dictWordArrayList.add(dictWordArray)
            }

            parallelise(misspellList.size, overallSuggestionsList) { i ->
                val startTime = System.currentTimeMillis()
                val subSuggestionsList = ArrayList<ArrayList<String>>()


                for (j in (i - 1) * 100..Math.min((i * 100) - 1, misspellList.size - 1)) {
                    var bestScore = Int.MAX_VALUE
                    val bestSuggestionsForWord = ArrayList<String>()
                    val misspelledWord = misspellList[j]
                    val misspelledWordArray = LongArray(misspelledWord.length+ngramLength-1)
                    generateNgram(misspelledWord, ngramLength, misspelledWordArray)
                    Arrays.sort(misspelledWordArray)

                    if (dictSet.contains(misspelledWord)) {
                        bestSuggestionsForWord.add(misspelledWord)
                    } else {
                        dictWordArrayList.forEachIndexed { index, dictWordArray ->
                            val score = NGram.analyseSingleOptimised(misspelledWordArray, dictWordArray)
                            if (score < bestScore) {
                                bestScore = score
                                bestSuggestionsForWord.clear()
                                bestSuggestionsForWord.add(dictList[index])
                            } else if (score == bestScore) {
                                bestSuggestionsForWord.add(dictList[index])
                            }
                        }
                    }

                    subSuggestionsList.add(bestSuggestionsForWord)
                }

                atomicTime.addAndGet(System.currentTimeMillis() - startTime)

                return@parallelise subSuggestionsList
            }

            return analyseFindings("NGram Optimised ($ngramLength)", misspellList, correctList, overallSuggestionsList, atomicTime)
        }

        // expects sorted arrays of ngram hashes, with correct lengths
        private fun analyseSingleOptimised(ngramHashArray1 :LongArray, ngramHashArray2:LongArray):Int{

            var intersections = 0
            var i = 0
            var j = 0

            while(i<ngramHashArray1.size && j < ngramHashArray2.size){
                when{
                    ngramHashArray1[i]<ngramHashArray2[j] || ngramHashArray1[i]==0L-> i++
                    ngramHashArray1[i]>ngramHashArray2[j] || ngramHashArray2[j]==0L-> j++
                    else ->{
                        intersections++
                        i++
                        j++
                    }
                }
            }

            return ngramHashArray1.size + ngramHashArray2.size  - 2 * intersections
        }
    }



    fun analyseSingle(string1: String, string2: String): Int {

        // number of n-grams from padded string is x+n-1 where x is original string length
        val string1NgramCount = string1.length+ngramLength - 1
        val string2NgramCount = string2.length+ngramLength - 1

        generateNgram(string1, ngramLength, string1NgramHashArray)
        generateNgram(string2, ngramLength, string2NgramHashArray)


        Arrays.sort(string1NgramHashArray,0,string1NgramCount)
        Arrays.sort(string2NgramHashArray,0,string2NgramCount)

        var intersections = 0
        var i = 0
        var j = 0

        while(i<string1NgramCount && j < string2NgramCount){
            when{
                string1NgramHashArray[i]<string2NgramHashArray[j] || string1NgramHashArray[i]==0L-> i++
                string1NgramHashArray[i]>string2NgramHashArray[j] || string2NgramHashArray[j]==0L-> j++
                else ->{
                    intersections++
                    i++
                    j++
                }
            }
        }

        return string1NgramCount + string2NgramCount  - 2 * intersections

    }



}