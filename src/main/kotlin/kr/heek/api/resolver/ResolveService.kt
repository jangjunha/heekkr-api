package kr.heek.api.resolver

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kr.heek.Book
import kr.heek.Library
import kr.heek.api.SearchResponse
import kr.heek.api.searchEntity
import kr.heek.api.searchResponse
import kr.heek.resolver.ResolverGrpcKt.ResolverCoroutineStub
import kr.heek.resolver.getLibrariesRequest
import kr.heek.resolver.searchRequest
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.ko.KoreanAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.lucene.store.Directory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ResolveService
@Autowired constructor(
    private val resolvers: Map<String, ResolverCoroutineStub>,
) {

    @CacheEvict("libraries", allEntries = true)
    @Scheduled(fixedRateString = (1000 * 60 * 60 * 6).toString())
    suspend fun findLibraries(): List<Pair<String, Library>> = coroutineScope {
        resolvers
            .map { (id, resolver) -> async { Pair(id, resolver.getLibraries(getLibrariesRequest { })) } }
            .awaitAll()
            .flatMap { (id, response) ->
                response.librariesList
                    .map { Pair(id, it) }
            }
    }

    suspend fun findLibraries(keyword: String?): List<Pair<String, Library>> =
        findLibraries()
            .run {
                if (!keyword.isNullOrBlank())
                    sortedBy { (_, library) ->
                        val query = QueryParser("name", KoreanAnalyzer()).parse(keyword)
                        val doc = Document()
                        doc.add(Field("name", library.name, TextField.TYPE_NOT_STORED))
                        -calculateScore(doc, query)
                    }
                else
                    this
            }

    fun search(keyword: String, libraryIds: List<String>): Flow<SearchResponse> {
        return resolvers
            .map { (key, resolver) ->
                flow {
                    val libraries = findLibraries()
                    val targetLibraryIds = libraries
                        .filter { (resolver, _) -> resolver == key }
                        .filter { (_, library) -> libraryIds.contains(library.id) }
                        .map { it.second.id }
                    if (targetLibraryIds.isNotEmpty()) {
                        emit(
                            resolver.search(searchRequest {
                                this.term = keyword
                                this.libraryIds.addAll(targetLibraryIds)
                            })
                        )
                    }
                }
            }
            .asFlow()
            .flatMapMerge { it }
            .flatMapMerge { it }
            .map { response ->
                val analyzer = KoreanAnalyzer()
                val query = buildQuery(keyword, analyzer)
                searchResponse {
                    entities.addAll(
                        response.entitiesList.map { entity ->
                            searchEntity {
                                book = entity.book
                                holdingSummaries.addAll(entity.holdingSummariesList)
                                url = entity.url
                                score = calculateScore(buildDocument(entity.book, 0), query)
                            }
                        }
                    )
                }
            }
//            .map { it ->
//                val analyzer = KoreanAnalyzer()
//                val directory = buildDirectory(
//                    it.entitiesList.map { it.book },
//                    analyzer,
//                ) { b, i -> buildDocument(b, i) }
//
//                val query = buildQuery(keyword, analyzer)
//                val highlighter = Highlighter(SimpleHTMLFormatter(), QueryScorer(query))
//
//                val indexReader = DirectoryReader.open(directory)
//                val indexSearcher = IndexSearcher(indexReader)
//                val docs = indexSearcher.search(
//                    query,
//                    indexReader.numDocs(),
//                ).scoreDocs
//                val storedFields = indexSearcher.storedFields()
//                searchResponse {
//                    this.entities.addAll(
//                        docs.map { scoreDoc ->
//                            val doc = storedFields.document(scoreDoc.doc)
//                            val index = doc.getField("_index").numericValue().toInt()
//                            val entity = it.entitiesList[index]
//                            searchEntity {
//                                this.book = book {
//                                    entity.book.isbn?.let {
//                                        isbn = it
//                                    }
//                                    entity.book.title?.let {
//                                        title = highlighter.getBestFragment(analyzer, "title", it) ?: it
//                                    }
//                                    entity.book.description?.let {
//                                        description = highlighter.getBestFragment(analyzer, "description", it) ?: it
//                                    }
//                                    entity.book.author?.let {
//                                        author = highlighter.getBestFragment(analyzer, "author", it) ?: it
//                                    }
//                                    entity.book.publisher?.let {
//                                        publisher = highlighter.getBestFragment(analyzer, "publisher", it) ?: it
//                                    }
//                                    entity.book.publishDate?.let {
//                                        publishDate = it
//                                    }
//                                }
//                                this.holdingSummaries.addAll(entity.holdingSummariesList)
//                                this.url = entity.url
//                                this.score = scoreDoc.score.toDouble()
//                            }
//                        })
//                    indexReader.close()
//                    directory.close()
//                }
//            }
    }

    companion object {
        fun buildQuery(keyword: String, analyzer: Analyzer): Query {
            return MultiFieldQueryParser(
                arrayOf("isbn", "title", "description", "author", "publisher"),
                analyzer,
                mapOf(
                    Pair("description", 0.2f),
                    Pair("author", 0.9f),
                    Pair("publisher", 0.2f),
                )
            ).parse(keyword)
        }

        fun calculateScore(document: Document, query: Query): Double {
            val analyzer = KoreanAnalyzer()
            val directory = buildDirectory(listOf(document), analyzer) { d, _ -> d }
            val indexReader = DirectoryReader.open(directory)
            val indexSearcher = IndexSearcher(indexReader)
            indexSearcher.similarity = ClassicSimilarity()
            val explanation = indexSearcher.explain(query, 0)
            return explanation.value.toDouble()
        }

        fun <T> buildDirectory(entities: List<T>, analyzer: Analyzer, transform: (T, Int) -> Document): Directory {
            val directory = ByteBuffersDirectory()
            val indexWriter = IndexWriter(
                directory,
                IndexWriterConfig(analyzer),
            )
            for ((index, book) in entities.withIndex()) {
                val doc = transform(book, index)
                indexWriter.addDocument(doc)
            }
            indexWriter.close()
            return directory
        }

        private fun buildDocument(book: Book, index: Int): Document {
            val doc = Document()
//            doc.add(StoredField("_index", index))
            doc.add(
                Field(
                    "isbn",
                    book.isbn,
                    StringField.TYPE_STORED,
                )
            )
            doc.add(
                Field(
                    "title",
                    book.title,
                    TextField.TYPE_STORED,
                )
            )
            doc.add(
                Field(
                    "description",
                    book.description,
                    TextField.TYPE_STORED,
                )
            )
            doc.add(
                Field(
                    "author",
                    book.author,
                    StringField.TYPE_STORED,
                )
            )
            doc.add(
                Field(
                    "publisher",
                    book.publisher,
                    StringField.TYPE_STORED,
                )
            )
            return doc
        }
    }
}
