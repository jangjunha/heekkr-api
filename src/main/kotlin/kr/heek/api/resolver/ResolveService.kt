package kr.heek.api.resolver

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kr.heek.Library
import kr.heek.api.SearchResponse
import kr.heek.api.searchEntity
import kr.heek.api.searchResponse
import kr.heek.resolver.ResolverGrpcKt.ResolverCoroutineStub
import kr.heek.resolver.getLibrariesRequest
import kr.heek.resolver.searchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ResolveService
@Autowired constructor(
    private val resolvers: Map<String, ResolverCoroutineStub>,
) {

    suspend fun findLibraries(): List<Library> = coroutineScope {
        resolvers.values
            .map { async { it.getLibraries(getLibrariesRequest { }) } }
            .awaitAll()
            .flatMap { it.librariesList }
    }

    fun search(keyword: String, libraryIds: List<String>): Flow<SearchResponse> {
        val resolverIds = libraryIds.map { it.split(":").first() }.toSet()
        return resolvers
            .filter { (key, _) -> resolverIds.contains(key) }
            .map { (key, resolver) ->
                flow {
                    emit(
                        resolver.search(searchRequest {
                            this.term = keyword
                            this.libraryIds.addAll(
                                libraryIds
                                    .filter { id -> id.startsWith(key) }
                            )
                        })
                    )
                }
            }
            .asFlow()
            .flatMapMerge { it }
            .flatMapMerge { it }
            .map {
                searchResponse {
                    entities.addAll(it.entitiesList.map {
                        searchEntity {
                            book = it.book
                            holdingSummaries.addAll(it.holdingSummariesList)
                            score = 1.0
                        }
                    })
                }
            }
    }
}
