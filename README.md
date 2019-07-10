# Nearest word search

We have an extensive dictionary and a probably misspelled user input, and our task is to find out the word the
user probably intended to enter. Even better if we could list the top-N closest words.


## Definition of 'close'

The classic [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) is used as first approach, 
but if needed, any other metric can be used.

`LDist.java` is a more-or-less straightforward implementation of the full-matrix solution, with some extra quirks
to deal with diacritic marks.


## The nearest-neighbors problem

In the classical nearest-neighbors problem we have a set of some entities and we want to list them ordered by their
distance from some arbitrary center point.

First we build an [R * tree](https://en.wikipedia.org/wiki/R*_tree) of them (whose elements are sets of entities),
and then create a priority queue and push the root of the tree to it.

As it is a *priority* queue, pushing an item needs a priority value as well, so from now on, by 'pushing a set to the queue'
we mean doing it with a priority value of **the smallest distance between the set boundary and the center point**.

If the center point is within the set boundary, then that distance is zero, meaning the highest priority.

So now we have a queue with one element: the root of the tree, that is, the set of all entities

The algorithm will consist of repeating this step as long as needed (or all entities are listed):

* Pop the most prioritised (lowest prio value) element from the queue
* If it's a leaf node of the tree (it contains only one entity): Emit that entity to the output
* If it's not a leaf node: Push all its child nodes (with their respective prio value) to the queue

That's all!

This algorithm guarantees that the tree nodes that are closer to the center point will be popped sooner,
so the leaf nodes will be popped in the order of increasing distance, exactly how we need them.

The real beauty is that we needn't produce all the result just to find the top-N, so we must pay
only for what we actually generate.

Sure, building the R * -tree is time-consuming enough, but it's required only once, and then the
individual searches will be quick.


## Finding the N closest words

Thus, our entities will be the words, so the tree elements must be some sort of expressions that represent
sets of words, in such a way that for each set we can easily calculate a lower boundary of the distances
between the set elements and an arbitrary 'center of search' word.

As first approach a simple method is used where for each letter position a set of letters is collected
that appear on that position in any elements of the word set. (The Expressions are sequences of LetterSets.)

For example, the representating word set for { 'apple', 'pear', 'plum' } is '[ap][pel][pau][lrm][e?]'
(the question mark shows that there the empty string is accepted as well).

The distance of a given string and the 'boundary' of such an Expression is surprisingly easy to calculate,
using a slightly modified version of the same Levenshtein algorithm.


## Building the tree

This step is important for two reasons:

* This requires the most of computing, so this is where the efficiency has the most impact
* The quality of the resulting tree (less overlapping nodes are the better) has an impact on the performance of the searches

Despite of the importance, in this PoC a simplistic algorithm is used:

* Add each word to the tree node that's nearest to it
* If the 'area' of a tree node grows too big, split it in two in a way that the sum of the resulting two areas is minimal

As "Premature optimisation is the root of all evil", I focused on the correctness of the algorithm rather than on the
performance, so it is slow (as expected), but the resulting tree performs surprisingly well in the searches.


## Usage and the files

The project is written in Java, orchestrated by make, and it pre-requires `sqlitejdbc` to be installed.

As a test, I used a vocabulary of German words (`german.words`), from which an SQLite database (`wordset.db`) is built.

This 'training' process can be done by `make train`, but be prepared that it'll take literally hours.

Searching this database can be done by `make search`, when you'll get an interactive prompt where you can enter a (misspelled) word, and then the result will be the list of the top-N nearest valid words along their distances from the input.

Sometimes you'll see fractional distances, that's because of the extensions to the pure Levenshtein algorithm: a case mismatch costs 0.5, and a mismatch of a diacritic costs 0.1 (the constants are in `Helper.java`).


### The sources

* `Helper.java`: The Levenshtein distance algorithm with all its belongings
* `LetterSet.java`: A set of letters (used in Expressions), along with its handling methods (weight, distance, add, addAll)
* `Expression.java`: A representing expression for word sets, with its methods (distance, area, add)
* `Vocabulary.java`: A base class that represents a vocabulary
* `VocabularyTrainer.java`: The algorithm of building the vocabulary
* `VocabularySearch.java`: The algorithm to do lookups from the vocabulary


### The database

An empty db can be created as `sqlite3 wordset.db < 00_create_db.sql`

The db structure is aimed to a more complex task:

* Maintaining vocabularies of different languages
* Connecting these vocabularies by introducing Concepts and linking them to actual words
* Also storing additional information (tags) about words

It's sort of a very simple version of FrameNet (with some similarities to WordNet), but with in-built multilingual aspects right from the start.

And of course, it's still a big ToDo on my list :D


