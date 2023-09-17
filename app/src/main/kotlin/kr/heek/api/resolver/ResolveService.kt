package kr.heek.api.resolver

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kr.heek.Library
import kr.heek.api.SearchResponse
import kr.heek.api.book.Book
import kr.heek.api.book.BookService
import kr.heek.api.book.RequestContext
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
    private val bookService: BookService,
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

    private suspend fun _search(keyword: String, libraryIds: List<String>): Flow<SearchResponse> {
        val context = RequestContext(bookService::resolve)

        val analyzer = KoreanAnalyzer()
        val query = buildQuery(keyword, analyzer)

        val libraries = findLibraries()

        return resolvers
            .map { (key, resolver) ->
                val targetLibraryIds = libraries
                    .filter { (resolver, _) -> resolver == key }
                    .filter { (_, library) -> libraryIds.contains(library.id) }
                    .map { it.second.id }
                if (targetLibraryIds.isNotEmpty()) {
                    resolver.search(searchRequest {
                        this.term = keyword
                        this.libraryIds.addAll(targetLibraryIds)
                    })
                } else emptyFlow()
            }
            .asFlow()
            .flatMapMerge { it }
            .map { response ->
                val entities = response.entitiesList
                    .map {
                        it to Book(
                            title = it.book.title,
                            author = it.book.author,
                            publisher = it.book.publisher,
                            publishDate = it.book.publishDate,
                            isbn = it.book.isbn,
                        )
                    }
                    .map { (entity, book) ->
                        entity to (context.request(book.isbn) ?: book)
                    }
                    .map { (entity, book) ->
                        searchEntity {
                            this.book = book.toAPI()
                            this.holdingSummaries.addAll(entity.holdingSummariesList)
                            this.url = entity.url
                            this.score = calculateScore(buildDocument(book), query)
                        }
                    }
                searchResponse {
                    this.entities.addAll(entities)
                }
            }
    }

    fun search(keyword: String, libraryIds: List<String>): Flow<SearchResponse> = flow {
        emit(_search(keyword, libraryIds))
    }.flatMapMerge { it }


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

        private fun <T> buildDirectory(
            entities: List<T>,
            analyzer: Analyzer,
            transform: (T, Int) -> Document
        ): Directory {
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

        private fun buildDocument(book: Book): Document {
            val doc = Document()
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
