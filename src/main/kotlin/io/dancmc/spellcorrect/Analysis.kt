package io.dancmc.spellcorrect

import java.io.*
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class Analysis {

    companion object {

        val percentFormat = DecimalFormat("#0.00%")
        val msFormat = DecimalFormat("#0")
        val twoDecimalFormat = DecimalFormat("#0.##")


        @JvmStatic
        fun main(args: Array<String>) {

            // validate output file name
            val outputFileName = when (args.size) {
                0 -> "./SpellCorrect.txt"
                1 -> args[0]
                else -> {
                    println("Too many arguments (${args.size}), requires either filename or nothing")
                    if(args[0].startsWith("'")){
                        println("Try using \" instead of '")
                    }
                    return
                }
            }
            val outputFile = File(outputFileName)

            if (!outputFile.exists()) {
                val mkFile = outputFile.createNewFile()
                if (!mkFile) {
                    println("Unable to create file : $outputFileName")
                    return
                }
            }

            println("Validated directory ${outputFile.canonicalPath}")
            println("Reading in datasets...")

            // read all data sets into memory
            val misspellListMut = ArrayList<String>()
            val correctListMut = ArrayList<String>()
            val dictListMut = ArrayList<String>()


            try {

                BufferedReader(FileReader(File("./dictionary.txt"))).use { dictReader ->
                    var dictLine = dictReader.readLine()
                    while (dictLine != null) {
                        dictListMut.add(dictLine)
                        dictLine = dictReader.readLine()
                    }

                }

                BufferedReader(FileReader(File("./misspell.txt"))).use { misspellReader ->
                    var misspellLine = misspellReader.readLine()
                    while (misspellLine != null) {

                        misspellListMut.add(misspellLine)
                        misspellLine = misspellReader.readLine()
                    }
                }

                BufferedReader(FileReader(File("./correct.txt"))).use { correctReader ->
                    var correctLine = correctReader.readLine()
                    while (correctLine != null) {
                        correctListMut.add(correctLine)
                        correctLine = correctReader.readLine()
                    }
                }

            } catch (e: Exception) {
                println("Error : ${e.message}")
                println("Make sure misspell.txt, correct.txt, and dictionary.txt are in the same directory")
                return
            }

            println("Finished reading datasets")


            val misspellList = Collections.unmodifiableList(misspellListMut)
            val correctList = Collections.unmodifiableList(correctListMut)
            val dictList = Collections.unmodifiableList(dictListMut)
            val dictSet = HashSet<String>(dictListMut)


            // This section is concerned with analysis of the datasets
            var one = 0
            var two = 0
            var three = 0
            var four = 0
            var five = 0
            var six = 0

            misspellList.forEachIndexed { index, misspell ->
                val correct = correctList[index]

                if (misspell == correct) {
                    if (dictSet.contains(misspell) && dictSet.contains(correct)) {
                        one++
                    } else {
                        two++
                    }
                } else {
                    val mInDict = dictSet.contains(misspell)
                    val cInDict = dictSet.contains(correct)

                    when {
                        mInDict && !cInDict -> three++
                        !mInDict && cInDict -> four++
                        mInDict && cInDict -> five++
                        !mInDict && !cInDict -> six++
                    }

                }

            }
            val dataSetAnalysis = "Misspelled == Correct\n" +
                    "1. Both in dict : $one\n" +
                    "2. Both not in dict : $two\n\n" +
                    "Misspelled != Correct\n" +
                    "3. Only misspelled in dict : $three\n" +
                    "4. Only correct in dict : $four\n" +
                    "5. Both in dict : $five\n" +
                    "6. Both not in dict : $six\n\n" +
                    "Total : ${one + two + three + four + five + six}\n\n"


            // This section is concerned with analysis of the correction methods

            // For each method,
            // Run through list of misspelled words and produce list of suggestions for each
            // Misspelled words in dictionary should return only themselves

            // Produce data :
            // 1. Time taken to produce spelling suggestions for entire list of misspelled words
            // 2. Recall : percent of words with correct suggestion somewhere
            // 3. Aggregate Precision : percent of total suggestions which are correct
            // 4. Individual Precision : average percent of suggestions for each word which are correct

            println("Starting analysis...")

            try {
                FileWriter(outputFile, true).use { writer ->
                    writer.appendln(dataSetAnalysis)
                     writer.appendln(NeedlemanWunsch.analyseBatch(misspellList, correctList, dictList, dictSet))
                    println("Finished Needleman-Wunsch")
                    writer.appendln(Levenshtein.analyseBatch(misspellList, correctList, dictList, dictSet))
                    println("Finished Levenshtein")
                    writer.appendln(DamerauLevenshtein.analyseBatch(misspellList, correctList, dictList, dictSet))
                    println("Finished Damerau-Levenshtein")
                    writer.appendln(SmithWaterman.analyseBatch(misspellList, correctList, dictList, dictSet))
                    println("Finished Smith-Waterman")
                    writer.appendln(NGram.analyseBatchOptimised(2, misspellList, correctList, dictList, dictSet))
                    println("Finished NGram 2")
                    writer.appendln(NGram.analyseBatchOptimised(3, misspellList, correctList, dictList, dictSet))
                    println("Finished NGram 3")
                    writer.appendln(Neighbourhood.analyseBatch(misspellList, correctList, dictSet, Neighbourhood.Edits.SINGLE))
                    println("Finished Nearest Neighbour 1")
                    writer.appendln(Neighbourhood.analyseBatch(misspellList, correctList, dictSet, Neighbourhood.Edits.DOUBLE))
                    println("Finished Nearest Neighbour 2")
//                    writer.appendln(Combination.analyseBatch(misspellList, correctList, dictSet))
                }
                println("Results written to ${outputFile.canonicalPath}")
            } catch (e: IOException) {
                println("Error : ${e.message}")
            }

        }
    }

}