-- # Database for the WordSet project

-- ## Languages
-- Languages are identified by their ISO lang code, and they have separate
-- r-trees, so the id of their root node is here as well.
DROP TABLE IF EXISTS lang_t;
CREATE TABLE lang_t (
	id              INTEGER PRIMARY KEY,
	isocode         TEXT, -- ISO 639-2/T three-letter code, 'in own language' i.e. 'deu' instead of 'ger'
	rootnode        INTEGER -- REFERENCES node_t
);

-- ## Classes
-- No need to store it in DB, won't ever look it up dynamically, and it'll never change.
--
-- enum word_classes_t {
--     NOUN         = 0, // eg. home, happiness, chocolate
--     VERB         = 1, // eg. walk, happen, be
--     ADJECTIVE    = 2, // eg. big, sweet, bright
--     ADVERB       = 3, // eg. very, quite, quickly, today
--     PRONOUN      = 4, // eg. your, we, there, who, myself
--     PREPOSITION  = 5, // eg. in, of, to, with
--     CONJUNCTION  = 6, // eg. and, but, so, therefore
--     INTERJECTION = 7, // eg. oh, alas, oops
--     ARTICLE      = 8, // eg. a, an, the
-- }

-- ## Concepts
-- Concepts are the meaning behind the words. A word may describe different concepts,
-- (eg. 'gas' may mean the state of substance, or the propellant of cars), and a
-- concept may be described by different words (eg. 'petrol' and 'gas' may mean the
-- same thing), so there is an N-to-N relationship between words and concepts.
-- Moreover, it sometimes depends on the language, like that languages of sea-faring
-- cultures have more concepts about parts of ships than of those in-land cultures.
DROP TABLE IF EXISTS concept_t;
CREATE TABLE concept_t (
	id              INTEGER PRIMARY KEY,
	class           INTEGER -- enum word_classes_t
);
DROP INDEX IF EXISTS concept_class_i;
CREATE INDEX concept_class_i ON concept_t (class);

-- ## Tags
-- Tags are arbitrary labels that represent thematic categories (eg. 'plants', 'food',
-- 'religion', 'moving'), cultural aspects (eg. 'slang', 'archaic', 'official'),
-- dialects (eg. 'us', 'brazil'), or anything else :)
DROP TABLE IF EXISTS tag_t;
CREATE TABLE tag_t (
	id              INTEGER PRIMARY KEY,
	name            TEXT, -- in English, fallback
	concept         INTEGER -- REFERENCES concept_t -- for translations
);

-- ## Words
-- More correctly, lemmata: canonical forms of word stems, without any conjugation
-- that is not required for the meaning (eg. 'get on' instead of 'getting on', but
-- not just 'get'). Also it is language-specific, because 'Bad' means a different
-- concept in English than in German :)
DROP TABLE IF EXISTS word_t;
CREATE TABLE word_t (
	id              INTEGER PRIMARY KEY,
	lang            INTEGER, -- REFERENCES lang_t
	value           TEXT
);
DROP INDEX IF EXISTS word_lang_i;
CREATE INDEX word_lang_i ON word_t (lang);
DROP INDEX IF EXISTS word_value_i;
CREATE INDEX word_value_i ON word_t (value);

-- ## Nodes
-- For looking up words based on similarity, we need an R-tree (for each
-- language) with a metric being a modified Levenshtein distance.
-- Each row of this table represents a node in such an R-tree, and has a
-- type (leaf or internal node), a boundary expression that covers all its
-- content ('abc|ef|apq|...', because it's easier to parse than '[abc][ef][apq]...'),
-- the number of the mandatory prefix (the rest is optional), and the id-s
-- of the child nodes/words as a blob of little-endian uint32_t-s.
-- (No search is needed by child nodes, so it spares us a connecting table,
-- and this is a very frequent query, so its performance *does* count.)
--
-- enum node_type_t {
--     LEAF         = 0, // contains id-s of words
--     INTERNAL     = 1, // contains id-s of other nodes
-- }
DROP TABLE IF EXISTS node_t;
CREATE TABLE node_t (
	id              INTEGER PRIMARY KEY,
	ntype           INTEGER, -- enum node_type_t
	boundary        TEXT,
	mandatory_pfx   INTEGER,
	children        BLOB
);

-- ## Connection table: Words-to-concepts
DROP TABLE IF EXISTS word_concept_t;
CREATE TABLE word_concept_t (
	word            INTEGER, -- REFERENCES word_t
	concept         INTEGER -- REFERENCES concept_t
);
DROP INDEX IF EXISTS w_c_word_i;
CREATE INDEX w_c_word_i ON word_concept_t (word);
DROP INDEX IF EXISTS w_c_concept_i;
CREATE INDEX w_c_concept_i ON word_concept_t (concept);

-- ## Connection table: Tags-to-concepts
DROP TABLE IF EXISTS tag_concept_t;
CREATE TABLE tag_concept_t (
	tag             INTEGER, -- REFERENCES tag_t
	concept         INTEGER -- REFERENCES concept_t
);
DROP INDEX IF EXISTS t_c_word_i;
CREATE INDEX t_c_word_i ON tag_concept_t (tag);
DROP INDEX IF EXISTS t_c_concept_i;
CREATE INDEX t_c_concept_i ON tag_concept_t (concept);

-- ## TODO: relation of concepts, like
-- noun-to-noun: generalisation-of, specialisation-of (eg. 'rose' is spec. of 'flower' is spec. of 'plant' is spec. of 'object')
-- adjective-to-noun: abstraction-of, state-of (eg. 'silence' is abstr. of 'silent', 'silentness' is state of 'silent')
-- adjective-to-adjective: opposite-of, higher-degree-of (eg. 'cold' is opp. of 'hot', 'hot' is higher deg. of 'warm')
-- verb-to-noun: result-of (eg. 'meal' is result of 'cook')
-- etc.
