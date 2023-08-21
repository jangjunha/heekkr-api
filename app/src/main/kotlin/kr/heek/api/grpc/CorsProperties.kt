package kr.heek.api.grpc

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
class CorsProperties (
    val origins: List<String> = emptyList(),
)
