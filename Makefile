# x86, x86_64, mips, mips64, armeabi, armeabi-v7a, arm64-v8a
PLATFORM=x86
ICU4J_MAJOR=60
ICU4J_MINOR=2
SQLITE_VERSION=3.21.0

ICU_JAR=icu4j-$(ICU4J_MAJOR)_$(ICU4J_MINOR).jar
#SQLITE_JAR=sqlite-jdbc-$(SQLITE_VERSION).jar

#/usr/local/lib/libsqlitejdbc.so
SQLITE_JAR=/usr/local/share/java/classes/sqlitejdbc-native.jar

WST_CLASSES=Expression.class Helper.class LetterSet.class WordSet.class WordSetSearch.class WordSetTrainer.class

WST_LIBS=$(ICU_JAR) $(SQLITE_JAR)


all:		LDist.class $(WST_CLASSES) wordset.db

$(WST_CLASSES):	$(WST_LIBS)

.PHONY:		clean search train

CLASSPATH=$(ICU_JAR):$(SQLITE_JAR):.

clean:
		rm -f *.class

search:		$(WST_CLASSES)
		java -cp $(CLASSPATH) WordSetSearch


train:		$(WST_CLASSES)
		java -cp $(CLASSPATH) WordSetTrainer german.words


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
