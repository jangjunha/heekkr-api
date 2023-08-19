package kr.heek.api.resolver

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "heekkr.resolver")
data class ResolverProperties(
    val targets: Map<String, String>,
)
