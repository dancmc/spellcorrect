package io.dancmc.spellcorrect

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

fun analyseFindings(methodName:String, misspellList: List<String>, correctList: List<String>,  overallSuggestionsList:List<Collection<String>>,atomicTime:AtomicLong):String{

    val misspellListSize = misspellList.size.toDouble()
    var recallAbsolute = 0
    var totalIndividualPrecision = 0.0
    val suggestionCountArray = DoubleArray(misspellList.size)
    val standardDev = StandardDeviation()

    overallSuggestionsList.forEachIndexed { index, suggestions ->

        if (suggestions.contains(correctList[index])) {
            recallAbsolute++
            totalIndividualPrecision += 1 / suggestions.size.toDouble()
        }
        suggestionCountArray[index] = suggestions.size.toDouble()
    }


    Arrays.sort(suggestionCountArray)
    val totalSuggestions = suggestionCountArray.sum()


    return "$methodName\n" +
            "Average Time per Word : ${(atomicTime.get() / misspellListSize).roundToInt()}ms\n" +
            "Min Predictions per Word : ${suggestionCountArray.min()!!.toInt()}\n"+
            "Max Predictions per Word : ${suggestionCountArray.max()!!.toInt()}\n"+
            "Average Predictions per Word : ${Analysis.twoDecimalFormat.format(totalSuggestions/misspellListSize)}\n"+
            "Median Predictions per Word : ${Analysis.twoDecimalFormat.format(suggestionCountArray[suggestionCountArray.lastIndex/2])}\n"+
            "Prediction Variance : ${Analysis.twoDecimalFormat.format(standardDev.evaluate(suggestionCountArray))}\n"+
            "Recall : $recallAbsolute/$misspellListSize (${Analysis.percentFormat.format(recallAbsolute / misspellListSize)})\n" +
            "Aggregate Precision : $recallAbsolute/$totalSuggestions (${Analysis.percentFormat.format(recallAbsolute / totalSuggestions)})\n" +
            "Average Individual Precision : ${Analysis.percentFormat.format(totalIndividualPrecision / recallAbsolute)}\n"
}

/**
 * Utility method to process data in parallel and then recombine
 * Blocks till all threads complete
 */

fun parallelise(batchSize:Int, overallSuggestionsList:ArrayList<ArrayList<String>>,function:(Int)->ArrayList<ArrayList<String>>){
    runBlocking {
        val deferredArray = ArrayList<Deferred<ArrayList<ArrayList<String>>>>()

        for(i in 1..(batchSize+100)/100){
            deferredArray.add(async(CommonPool) {
                return@async function(i)
            })
        }

        deferredArray.forEach { overallSuggestionsList.addAll(it.await()) }
    }
}