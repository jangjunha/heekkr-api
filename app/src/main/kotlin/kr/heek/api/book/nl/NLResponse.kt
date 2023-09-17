package kr.heek.api.book.nl

import kotlinx.serialization.Serializable

@Serializable
data class NLResponse(
    val docs: List<NLBook>,
    val PAGE_NO: String,
    val TOTAL_COUNT: String,
)
