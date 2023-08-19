package kr.heek.api.grpc

import io.grpc.Server
import io.grpc.ServerBuilder
import kr.heek.api.resolver.ResolveService
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import java.io.IOException

class GrpcServer(
    private val port: Int,
    resolveService: ResolveService,
) : SmartLifecycle {
    private val logger = LoggerFactory.getLogger(GrpcServer::class.java)
    private val server: Server? = ServerBuilder.forPort(port)
        .addService(ApiServiceImpl(resolveService))
        .build()

    override fun start() {
        try {
            server!!.start()
        } catch (e: IOException) {
            logger.error("gRPC server raises error", e)
            throw RuntimeException(e)
        }
        logger.info("gRPC server started, listening on $port")

        val awaitThread = Thread {
            try {
                server.awaitTermination()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        awaitThread.name = "grpc-server-container"
        awaitThread.isDaemon = false
        awaitThread.start()
    }

    override fun stop() {
        if (server != null) {
            logger.info("*** shutting down gRPC server since JVM is shutting down")
            server.shutdown()
            server.awaitTermination()
            logger.info("*** gRPC server shut down")
        }
    }

    override fun isRunning(): Boolean {
        return server!!.isTerminated
    }
}
