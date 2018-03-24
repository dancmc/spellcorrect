package io.dancmc.spellcorrect
import java.util.concurrent.atomic.AtomicLong

// Algorithm adapted from Peter Norvig (2007-2016) http://norvig.com/spell-correct.html

class Neighbourhood {

    companion object {


        // exactly 1 edit away
        fun edits1(s: String): HashSet<String> {

            val resultSet = HashSet<String>()

            val alphabet = "abcdefghijklmnopqrstuvwxyz"
            val splits = ArrayList<Pair<String, String>>()
            for (i in 0..s.lastIndex) {
                splits.add(Pair(s.substring(0, i), s.substring(i)))
            }
            splits.add(Pair(s, ""))


            splits.forEach {
                // deletes
                if (it.second.length > 1) {
                    resultSet.add(it.first + it.second.substring(1))
                } else if (it.second.length == 1) {
                    resultSet.add(it.first)
                }

                // transposes
                if (it.second.length > 2) {
                    resultSet.add(it.first + it.second[1] + it.second[0] + it.second.substring(2))
                } else if (it.second.length == 2) {
                    resultSet.add(it.first + it.second[1] + it.second[0])
                }

                // replaces
                alphabet.forEach { letter ->
                    if (it.second.length > 1) {
                        resultSet.add(it.first + letter + it.second.substring(1))
                    } else if (it.second.length == 1) {
                        resultSet.add(it.first + letter)
                    }
                }

                // inserts
                alphabet.forEach { letter ->
                    resultSet.add(it.first + letter + it.second)
                }

            }

            resultSet.remove("")
            return resultSet
        }

        // exactly 2 edits away
        fun edits2(edits1: Set<String>): HashSet<String> {
            val edits2 = HashSet<String>()
            edits1.forEach {
                edits2.addAll(edits1(it))
            }
            return edits2
        }


        // up to 2 edits away
        fun edits1And2(s: String): HashSet<String> {

            val edits1 = edits1(s)

            val edits2 = HashSet<String>()
            edits1.forEach {
                edits2.addAll(edits1(it))
            }

            edits1.addAll(edits2)

            return edits2
        }

        private fun known(wordList: HashSet<String>, dictionary: HashSet<String>): ArrayList<String> {
            return wordList.filter { dictionary.contains(it) } as ArrayList<String>
        }

        fun analyseSingle(s: String, dictSet: HashSet<String>, edits:Edits): ArrayList<String> {

            val editFunction = when(edits){
                Edits.SINGLE-> ::edits1
                Edits.DOUBLE -> ::edits1And2
            }

            if (dictSet.contains(s)) {
                val resultSet = ArrayList<String>()
                resultSet.add(s)
                return resultSet
            }

            val suggestionSet = editFunction(s)

            return known(suggestionSet, dictSet)
        }


        fun analyseBatch(misspellList: List<String>, correctList: List<String>, dictSet: HashSet<String>,edits:Edits): String {

            // initialise variables
            val overallSuggestionsList = ArrayList<ArrayList<String>>()
            val atomicTime = AtomicLong()


            parallelise(misspellList.size, overallSuggestionsList){i->
                val startTime = System.currentTimeMillis()

                val subSuggestionsList = ArrayList<ArrayList<String>>()

                for (j in (i - 1) * 100..Math.min((i * 100) - 1, misspellList.size - 1)) {
                    subSuggestionsList.add(analyseSingle(misspellList[j], dictSet, edits))
                }

                atomicTime.addAndGet(System.currentTimeMillis() - startTime)

                return@parallelise subSuggestionsList
            }

            return analyseFindings("Neighbourhood (${edits.desc})", misspellList, correctList, overallSuggestionsList, atomicTime)
        }
    }

    enum class Edits(val desc:String){
        SINGLE("1 edit"), DOUBLE("2 edits")
    }

}