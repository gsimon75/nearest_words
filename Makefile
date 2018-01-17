TARGET=LDist.class WordSetTest.class WordSet.class WordSetNode.class WordSetInternalNode.class WordSetLeafNode.class Expression.class LetterSet.class Dump.class Helper.class
ICU4J_MAJOR=60
ICU4J_MINOR=2

ICU_JAR=icu4j-$(ICU4J_MAJOR)_$(ICU4J_MINOR).jar

all:		$(TARGET) $(ICU_JAR)

.PHONY:		clean run run1 run2 run3

CLASSPATH=$(ICU_JAR):.

clean:
		rm -f *.class

run:		WordSetTest.class WordSet.class WordSetNode.class WordSetInternalNode.class WordSetLeafNode.class Expression.class LetterSet.class Dump.class Helper.class
		java -cp $(CLASSPATH) $(subst .class,,$<) test.words

run4:		WordSetTest.class WordSet.class WordSetNode.class WordSetInternalNode.class WordSetLeafNode.class Expression.class LetterSet.class Dump.class Helper.class
		java -cp $(CLASSPATH) $(subst .class,,$<) a100.words

run1:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$^) kitten sitting

run2:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$^) távfutó távfűtő

run3:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$^) غرفة غُرْفَةٌ

%.class:	%.java
		javac -cp $(CLASSPATH) -g $^

$(ICU_JAR):
		curl -OL http://download.icu-project.org/files/icu4j/$(ICU4J_MAJOR).$(ICU4J_MINOR)/$@

a.words:	german.words
		grep -i '^[aä]' $^ >$@

b.words:	german.words
		grep -i '^b' $^ >$@

a100.words:	a.words
		head -100 $^ >$@

a10.words:	a.words
		head -10 $^ >$@
