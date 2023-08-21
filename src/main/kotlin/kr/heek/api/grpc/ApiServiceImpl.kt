package kr.heek.api.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kr.heek.api.*
import kr.heek.api.ApiGrpcKt.ApiCoroutineImplBase
import kr.heek.api.resolver.ResolveService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ApiServiceImpl
@Autowired constructor(
    private val resolverService: ResolveService,
) : ApiCoroutineImplBase() {

    override suspend fun getLibraries(request: GetLibrariesRequest): GetLibrariesResponse {
        return getLibrariesResponse {
            libraries.addAll(
                resolverService.findLibraries(request.keyword).map { (_, library) -> library }
            )
        }
    }

    override fun search(request: SearchRequest): Flow<SearchResponse> {
        if (request.libraryIdsCount > MAX_LIBRARY_IDS) {
            throw StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Library ids must be fewer than 10."))
        }
        return resolverService.search(request.term, request.libraryIdsList).catch {
            println(it)
        }
    }

    companion object {
        const val MAX_LIBRARY_IDS = 10;
    }
}
