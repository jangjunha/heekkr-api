package kr.heek.api.resolver

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "heekkr")
data class ResolverProperties(
    val resolvers: Map<String, Resolver>,
) {
    data class Resolver (
        val target: String,
        val auth: Auth = Auth.INSECURE,
    )

    enum class Auth {
        INSECURE,
        GOOGLE,
    }
}
