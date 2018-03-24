package io.dancmc.spellcorrect

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class Analysis {

    companion object {

        val percentFormat = DecimalFormat("#0.00%")
        val msFormat = DecimalFormat("#0")
        val twoDecimalFormat = DecimalFormat("#0.##")

        lateinit var first : List<Collection<String>>
        lateinit var second : List<Collection<String>>

        @JvmStatic
        fun main(args: Array<String>) {


            val misspellData = ArrayList<String>()
            val correctData = ArrayList<String>()
            val dictDataList = ArrayList<String>()

            try {
                BufferedReader(FileReader(File("/users/daniel/downloads/dictionary.txt"))).use { dictReader ->
                    var dictLine = dictReader.readLine()
                    while (dictLine != null) {
                        dictDataList.add(dictLine)
                        dictLine = dictReader.readLine()
                    }

                }

                BufferedReader(FileReader(File("/users/daniel/downloads/misspell.txt"))).use { misspellReader ->
                    var misspellLine = misspellReader.readLine()
                    while (misspellLine != null) {
                        misspellData.add(misspellLine)
                        misspellLine = misspellReader.readLine()
                    }
                }

                BufferedReader(FileReader(File("/users/daniel/downloads/correct.txt"))).use { correctReader ->
                    var correctLine = correctReader.readLine()
                    while (correctLine != null) {
                        correctData.add(correctLine)
                        correctLine = correctReader.readLine()
                    }
                }

            } catch (e: Exception) {
                println("Error : ${e.message}")
                return
            }


            val dictDataSet = HashSet<String>(dictDataList)

            Collections.unmodifiableList(misspellData)
            Collections.unmodifiableList(correctData)
            Collections.unmodifiableList(dictDataList)
            Collections.unmodifiableSet(dictDataSet)


            // For each method,
            // Run through list of misspelled words and produce list of suggestions for each
            // Words in dictionary should return only themselves

            // Produce data :
            // 1. Time taken to produce spelling suggestions for entire list of misspelled words
            // 2. Recall : percent of words with correct suggestion somewhere
            // 3. Aggregate Precision : percent of total suggestions which are correct
            // 4. Individual Precision : average percent of suggestions for each word which are correct


            val time = System.currentTimeMillis()

//            val lv = Levenshtein()
//            val dl = DamerauLevenshtein()
            val nw = NeedlemanWunsch()
            val sw = SmithWaterman()
            val n2 = NGram(2)
            val n3 = NGram(6)

//            dictDataList.forEachIndexed { index, s ->
//                lv.analyseSingle("hippocrate", s)
//            }
//            println(Neighbourhood.analyseBatch(misspellData, correctData, dictDataSet, Neighbourhood.Edits.SINGLE))
//            NeedlemanWunsch.analyseBatch(misspellData,correctData, dictDataList, dictDataSet)
//            println(NGram.analyseBatchOptimised(3, misspellData, correctData, dictDataList, dictDataSet))
//            SmithWaterman.analyseBatch(misspellData,correctData, dictDataList, dictDataSet)
//            println("\n")
//            Levenshtein.analyseBatch(misspellData,correctData, dictDataList, dictDataSet)
//            println("\n")
//            DamerauLevenshtein.analyseBatch(misspellData,correctData, dictDataList, dictDataSet)
//
//            first.forEachIndexed { index, collection ->
//
//                val correct = correctData[index]
//
//                if(collection.contains(correct) && !second[index].contains(correct)){
//                    println("${misspellData[index]}, $correct")
//                    println(collection.joinToString())
//                    println(second[index].joinToString())
//                    println()
//                }
//
//            }
//

//            println(JaroWinkler.analyseBatch(misspellData, correctData,  dictDataList, dictDataSet))
//                println(JaroWinklerDistance().apply("uiqn dd", "umbrwella"))
            var counter = 0
            misspellData.forEachIndexed {index, s->
                if(!dictDataSet.contains(s) && !dictDataSet.contains(correctData[index])){
                    counter ++
                }
            }
            println(counter)

            FileWriter(File("/Users/daniel/Downloads/SpellCorrect Analysis.txt"), true).use { writer ->
//                writer.appendln(NeedlemanWunsch.analyseBatch(misspellData, correctData, dictDataList, dictDataSet))
//                writer.appendln(Levenshtein.analyseBatch(misspellData, correctData, dictDataList, dictDataSet))
//                writer.appendln(DamerauLevenshtein.analyseBatch(misspellData, correctData, dictDataList, dictDataSet))
//                writer.appendln(SmithWaterman.analyseBatch(misspellData, correctData, dictDataList, dictDataSet))
//                writer.appendln(NGram.analyseBatchOptimised(2, misspellData, correctData, dictDataList, dictDataSet))
//                writer.appendln(NGram.analyseBatchOptimised(3, misspellData, correctData, dictDataList, dictDataSet))
//                writer.appendln(Neighbourhood.analyseBatch(misspellData, correctData, dictDataSet, Neighbourhood.Edits.SINGLE))
//                writer.appendln(Neighbourhood.analyseBatch(misspellData, correctData, dictDataSet, Neighbourhood.Edits.DOUBLE))
//                writer.appendln(Soundex.analyseBatch(misspellData, correctData, dictDataList, dictDataSet))
//                writer.appendln(Combination.analyseBatch(misspellData, correctData,  dictDataSet))
            }
//            println(System.currentTimeMillis() - time)
        }
    }

}