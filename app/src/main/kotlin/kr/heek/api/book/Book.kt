package kr.heek.api.book

import kr.heek.PublishDate
import kr.heek.book

data class Book(
    val isbn: String,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val setIsbn: String? = null,
    val publishDate: PublishDate? = null,
) {
    fun toAPI() = book {
        isbn = this@Book.isbn
        title = this@Book.title
        this@Book.author?.also { author = it }
        this@Book.publisher?.also { publisher = it }
        this@Book.publishDate?.also {
            publishDate = it
        }
    }
}
