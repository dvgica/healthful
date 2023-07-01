package ca.dvgi.healthful

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

object HealthServer {
  def apply(serviceName: String, config: Config = ConfigFactory.load()): HealthServer = {
    config.checkValid(ConfigFactory.defaultReference(), configRoot)

    val rootConfig = config.getConfig(configRoot)

    new HealthServer(
      serviceName,
      rootConfig.getInt("port"),
      rootConfig.getString("liveness-path"),
      rootConfig.getString("readiness-path")
    )
  }
  private val configRoot = "healthful"
}

class HealthServer(serviceName: String, port: Int, livenessPath: String, readinessPath: String)
    extends AutoCloseable {
  private val log = LoggerFactory.getLogger(getClass)

  log.info(
    s"HealthServer starting on port $port with endpoints $livenessPath and $readinessPath..."
  )

  private val ready = new AtomicBoolean(false)

  private class LivenessHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val response = s"$serviceName is live"
      exchange.sendResponseHeaders(200, response.size.toLong)
      val outputStream = exchange.getResponseBody()
      outputStream.write(response.getBytes())
      outputStream.close()
    }
  }

  private class ReadinessHandler extends HttpHandler {
    override def handle(exchange: HttpExchange): Unit = {
      val (status, response) = if (ready.get) {
        (200, s"$serviceName is ready")
      } else {
        (503, s"$serviceName is not ready")
      }

      exchange.sendResponseHeaders(status, response.size.toLong)
      val outputStream = exchange.getResponseBody()
      outputStream.write(response.getBytes())
      outputStream.close()
    }
  }

  private val server = HttpServer.create(new InetSocketAddress(port), 0)
  server.createContext(livenessPath, new LivenessHandler)
  server.createContext(readinessPath, new ReadinessHandler)
  server.start()

  log.info(s"HealthServer started on port: ${port}")

  def markReady(): Unit = {
    ready.set(true)
    log.info("HealthServer marked as ready")
  }

  def markUnready(): Unit = {
    ready.set(false)
    log.info("HealthServer marked as unready")
  }

  def close(): Unit = {
    log.info("HealthServer shutting down...")
    server.stop(1)
    log.info("HealthServer shutdown complete")
  }
}
