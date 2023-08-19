package kr.heek.api.grpc

import kotlinx.coroutines.flow.Flow
import kr.heek.api.*
import kr.heek.api.ApiGrpcKt.ApiCoroutineImplBase
import kr.heek.api.resolver.ResolveService
import org.springframework.beans.factory.annotation.Autowired

class ApiServiceImpl
@Autowired constructor(
    private val resolverService: ResolveService,
): ApiCoroutineImplBase() {

    override suspend fun getLibraries(request: GetLibrariesRequest): GetLibrariesResponse {
        return getLibrariesResponse { libraries.addAll(resolverService.findLibraries()) }
    }

    override fun search(request: SearchRequest): Flow<SearchResponse> =
        resolverService.search(request.term, request.libraryIdsList)
}
