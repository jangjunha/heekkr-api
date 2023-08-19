package kr.heek.api.resolver

import io.grpc.ManagedChannelBuilder
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
    fun resolvers(): Map<String, ResolverCoroutineStub> = props.targets.mapValues { (_, target) ->
        ResolverCoroutineStub(
            ManagedChannelBuilder.forTarget(target).usePlaintext().build()
        )
    }
}
