package kr.heek.api.book

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kr.heek.api.book.nl.NLResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class BookService
@Autowired constructor(
    private val nlProperties: NLProperties,
) {
    suspend fun resolve(isbn: String): Book? {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
        val response: NLResponse = client.get("https://www.nl.go.kr/seoji/SearchApi.do") {
            url {
                parameters.append("isbn", isbn)
                parameters.append("result_style", "json")
                parameters.append("page_no", "1")
                parameters.append("page_size", "1")
                parameters.append("cert_key", nlProperties.api_key)
            }
        }.body()
        return response.docs.firstOrNull()?.toBook()
    }
}
