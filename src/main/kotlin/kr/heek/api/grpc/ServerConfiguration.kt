package kr.heek.api.grpc

import kr.heek.api.resolver.ResolveService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ServerConfiguration {

    @Value("\${grpc.server.port}")
    private val grpcServerPort = 0

    @Bean
    @Autowired
    fun grpcServer(resolveService: ResolveService): GrpcServer {
        return GrpcServer(grpcServerPort, resolveService)
    }
}
