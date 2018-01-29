# x86, x86_64, mips, mips64, armeabi, armeabi-v7a, arm64-v8a
PLATFORM=x86
ICU4J_MAJOR=60
ICU4J_MINOR=2
SQLITE_VERSION=3.21.0

ICU_JAR=icu4j-$(ICU4J_MAJOR)_$(ICU4J_MINOR).jar
#SQLITE_JAR=sqlite-jdbc-$(SQLITE_VERSION).jar

#/usr/local/lib/libsqlitejdbc.so
SQLITE_JAR=/usr/local/share/java/classes/sqlitejdbc-native.jar


WST_CLASSES=WordSetTest.class WordSet.class WordSetNode.class WordSetInternalNode.class WordSetLeafNode.class Expression.class LetterSet.class Dump.class Helper.class WordSetDB.class
WST_LIBS=$(ICU_JAR) $(SQLITE_JAR)


all:		LDist.class $(WST_CLASSES) wordset.db

$(WST_CLASSES):	$(WST_LIBS)

.PHONY:		clean run run1 run2 run3

CLASSPATH=$(ICU_JAR):$(SQLITE_JAR):.

clean:
		rm -f *.class

run:		$(WST_CLASSES)
		java -cp $(CLASSPATH) $(subst .class,,$<)

run4:		$(WST_CLASSES)
		java -cp $(CLASSPATH) $(subst .class,,$<) a100.words

run1:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$<) kitten sitting

run2:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$<) távfutó távfűtő

run3:		LDist.class
		java -cp $(CLASSPATH) $(subst .class,,$<) غرفة غُرْفَةٌ

%.class:	%.java
		javac -cp $(CLASSPATH) -encoding UTF-8 -g $<

$(ICU_JAR):
		curl -OL http://download.icu-project.org/files/icu4j/$(ICU4J_MAJOR).$(ICU4J_MINOR)/$@

$(SQLITE_JAR):
		curl -OL https://bitbucket.org/xerial/sqlite-jdbc/downloads/$@

wordset.db:	00_create_db.sql
		true | sqlite3 $@ -init $<
		

#libsqliteX.so:	$(SQLITE_AAR)
#		unzip -DD -j -o $< jni/$(PLATFORM)/$@

a.words:	german.words
		grep -i '^[aä]' $^ >$@

b.words:	german.words
		grep -i '^b' $^ >$@

a100.words:	a.words
		head -100 $^ >$@

a10.words:	a.words
		head -10 $^ >$@
