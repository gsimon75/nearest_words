# x86, x86_64, mips, mips64, armeabi, armeabi-v7a, arm64-v8a
PLATFORM=x86
ICU4J_MAJOR=60
ICU4J_MINOR=2
SQLITE_YEAR=2017
SQLITE_VERSION=3210000

ICU_JAR=icu4j-$(ICU4J_MAJOR)_$(ICU4J_MINOR).jar
SQLITE_AAR=sqlite-android-$(SQLITE_VERSION).aar
SQLITE_JAR=sqlite.jar

WST_CLASSES=WordSetTest.class WordSet.class WordSetNode.class WordSetInternalNode.class WordSetLeafNode.class Expression.class LetterSet.class Dump.class Helper.class
WST_LIBS=$(ICU_JAR) $(SQLITE_JAR) libsqliteX.so

all:		LDist.class $(WST_CLASSES) $(WST_LIBS) wordset.db

.PHONY:		clean run run1 run2 run3

CLASSPATH=$(ICU_JAR):$(SQLITE_JAR):.


clean:
		rm -f *.class

run:		$(WST_CLASSES) $(WST_LIBS)
		java -cp $(CLASSPATH) $(subst .class,,$<) test.words

run4:		$(WST_CLASSES) $(WST_LIBS)
		java -cp $(CLASSPATH) $(subst .class,,$<) a100.words

run1:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$<) kitten sitting

run2:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$<) távfutó távfűtő

run3:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$<) غرفة غُرْفَةٌ

%.class:	%.java
		javac -cp $(CLASSPATH) -g $^

$(ICU_JAR):
		curl -OL http://download.icu-project.org/files/icu4j/$(ICU4J_MAJOR).$(ICU4J_MINOR)/$@

$(SQLITE_AAR):
		curl -OL http://sqlite.org/$(SQLITE_YEAR)/$@

$(SQLITE_JAR):	$(SQLITE_AAR)
		unzip -p -o $< classes.jar >$@

wordset.db:	00_create_db.sql
		true | sqlite3 $@ -init $<
		

libsqliteX.so:	$(SQLITE_AAR)
		unzip -DD -j -o $< jni/$(PLATFORM)/$@

a.words:	german.words
		grep -i '^[aä]' $^ >$@

b.words:	german.words
		grep -i '^b' $^ >$@

a100.words:	a.words
		head -100 $^ >$@

a10.words:	a.words
		head -10 $^ >$@
