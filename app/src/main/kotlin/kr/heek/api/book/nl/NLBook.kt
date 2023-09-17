package kr.heek.api.book.nl

import kotlinx.serialization.Serializable
import kr.heek.api.book.Book
import kr.heek.publishDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class NLBook(
    val PUBLISHER: String? = null,
    val AUTHOR: String? = null,
    val SERIES_TITLE: String? = null,
    val SET_ISBN: String? = null,
    val SERIES_NO: String? = null,
    val EA_ISBN: String,
    val VOL: String? = null,
    val TITLE: String,
    val REAL_PUBLISH_DATE: String? = null,
    val PUBLISH_PREDATE: String? = null,
) {
    fun toBook() = Book(
        title = listOfNotNull(TITLE, VOL).filter { it.isNotBlank() }.joinToString(" "),
        author = AUTHOR?.ifBlank { null },
        publisher = PUBLISHER?.ifBlank { null },
        isbn = EA_ISBN,
        setIsbn = SET_ISBN?.ifBlank { null },
        publishDate = listOfNotNull(REAL_PUBLISH_DATE, PUBLISH_PREDATE).filter { it.isNotBlank() }
            .map { LocalDate.parse(it, DATE_FORMAT) }
            .map { publishDate {
                year = it.year
                month = it.monthValue
                day = it.dayOfMonth
            } }
            .firstOrNull()
    )

    companion object {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")!!
    }
}
