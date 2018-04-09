COMP90049 Project 1

Requires JRE/JDK 8 installed

Compilation instructions :
1. Create a fat jar by running "./gradlew fatJar" inside the project folder
2. This will create a jar file in the ./build/libs directory

Run instructions :
1. Put the jar file in a folder with misspell.txt, correct.txt, and dictionary.txt
2. Run the jar using "java -jar [jarName] [optional outputFileName]"
3. If no output file name is supplied, the results will be output to SpellCorrect.txt
   in the same folder as the jar