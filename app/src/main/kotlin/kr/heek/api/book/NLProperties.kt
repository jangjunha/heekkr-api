package kr.heek.api.book

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nl")
class NLProperties (
    val api_key: String,
)
