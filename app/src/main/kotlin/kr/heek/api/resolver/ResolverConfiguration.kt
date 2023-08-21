package kr.heek.api.resolver

import com.google.auth.oauth2.GoogleCredentials
import io.grpc.CompositeChannelCredentials
import io.grpc.InsecureChannelCredentials
import io.grpc.TlsChannelCredentials
import io.grpc.auth.MoreCallCredentials
import io.grpc.netty.NettyChannelBuilder
import kr.heek.resolver.ResolverGrpcKt.ResolverCoroutineStub
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ResolverProperties::class)
class ResolverConfiguration
@Autowired constructor(
    private val props: ResolverProperties,
) {
    @Bean
    fun resolvers(): Map<String, ResolverCoroutineStub> = props.resolvers.mapValues { (_, config) ->
        val credentials = when (config.auth) {
            ResolverProperties.Auth.INSECURE ->
                InsecureChannelCredentials.create()

            ResolverProperties.Auth.GOOGLE ->
                CompositeChannelCredentials.create(
                    TlsChannelCredentials.create(),
                    MoreCallCredentials.from(GoogleCredentials.getApplicationDefault())
                )
        }
        ResolverCoroutineStub(
            NettyChannelBuilder
                .forTarget(config.target, credentials)
                .build()
        )
    }
}
