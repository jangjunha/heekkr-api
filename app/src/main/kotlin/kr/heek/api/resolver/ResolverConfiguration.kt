package kr.heek.api.resolver

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.IdTokenCredentials
import com.google.auth.oauth2.IdTokenProvider
import io.grpc.CompositeChannelCredentials
import io.grpc.InsecureChannelCredentials
import io.grpc.TlsChannelCredentials
import io.grpc.auth.MoreCallCredentials
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.channel.ChannelOption
import kr.heek.resolver.ResolverGrpcKt.ResolverCoroutineStub
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

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
                    MoreCallCredentials.from(
                        IdTokenCredentials
                            .newBuilder()
                            .setIdTokenProvider(GoogleCredentials.getApplicationDefault() as IdTokenProvider)
                            .setTargetAudience("https://${config.target}/")
                            .build()
                    )
                )
        }
        ResolverCoroutineStub(
            NettyChannelBuilder
                .forTarget(config.target, credentials)
                .withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, TimeUnit.SECONDS.toMillis(30).toInt())
                .build()
        )
    }
}
