package kr.heek.api.grpc

import com.linecorp.armeria.common.HttpHeaderNames
import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.common.grpc.protocol.GrpcHeaderNames
import com.linecorp.armeria.server.cors.CorsService
import com.linecorp.armeria.server.cors.CorsServiceBuilder
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.logging.AccessLogWriter
import com.linecorp.armeria.server.logging.LoggingService
import com.linecorp.armeria.spring.ArmeriaServerConfigurator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class ServerConfiguration
@Autowired constructor(
    private val corsProperties: CorsProperties,
) {

    @Bean
    fun armeriaServerConfiguration(apiServiceImpl: ApiServiceImpl) = ArmeriaServerConfigurator {
        if (corsProperties.origins.isNotEmpty()) {
            it.decorator(
                CorsService
                    .builder(corsProperties.origins)
                    .allowRequestMethods(HttpMethod.POST)
                    .allowRequestHeaders(HttpHeaderNames.CONTENT_TYPE, HttpHeaderNames.of("X-GRPC-WEB"))
                    .exposeHeaders(
                        GrpcHeaderNames.GRPC_STATUS,
                        GrpcHeaderNames.GRPC_MESSAGE,
                        GrpcHeaderNames.ARMERIA_GRPC_THROWABLEPROTO_BIN
                    )
                    .newDecorator()
            )
        }
        it.decorator(LoggingService.newDecorator())
        it.accessLogWriter(AccessLogWriter.combined(), false)
        it.service(
            GrpcService.builder()
                .addService(apiServiceImpl)
                .build()
        )
    }
}
