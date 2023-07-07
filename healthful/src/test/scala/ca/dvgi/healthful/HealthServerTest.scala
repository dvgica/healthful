package ca.dvgi.healthful

import sttp.client3._
import sttp.model.StatusCode

class HealthServerTest extends munit.FunSuite {
  var server: HealthServer = _
  var backend: SttpBackend[Identity, Any] = _

  val serviceName = "foo"

  override def beforeAll(): Unit = {
    server = HealthServer(serviceName)
    backend = HttpClientSyncBackend()
  }

  override def afterAll(): Unit = {
    server.close()
    backend.close()
  }

  val serverHost = "http://localhost:9990"
  val livenessUri = uri"$serverHost/healthz"
  val readyUri = uri"$serverHost/ready"

  test("returns 200 on the liveness route for a GET") {
    val response = basicRequest
      .get(livenessUri)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok)
    assertEquals(response.body.getOrElse(fail("Unexpected body")), s"$serviceName is live")
  }

  test("returns 200 on the liveness route for a POST") {
    val response = basicRequest
      .post(livenessUri)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok)
    assertEquals(response.body.getOrElse(fail("Unexpected body")), s"$serviceName is live")
  }

  test("returns 503 on the ready route if not ready") {
    server.markUnready()

    val response = basicRequest
      .get(readyUri)
      .send(backend)

    assertEquals(response.code, StatusCode.ServiceUnavailable)
    assertEquals(
      response.body.left.getOrElse(fail("Unexpected body")),
      s"$serviceName is not ready"
    )
  }

  test("returns 200 on the ready route if ready") {
    server.markReady()

    val response = basicRequest
      .get(readyUri)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok)
    assertEquals(
      response.body.getOrElse(fail("Unexpected body")),
      s"$serviceName is ready"
    )
  }

  test("returns 200 on the ready route if ready for a POST") {
    server.markReady()

    val response = basicRequest
      .post(readyUri)
      .send(backend)

    assertEquals(response.code, StatusCode.Ok)
    assertEquals(
      response.body.getOrElse(fail("Unexpected body")),
      s"$serviceName is ready"
    )
  }

  test("returns 404 on other routes") {
    val response = basicRequest
      .get(uri"$serverHost/non-existent")
      .send(backend)

    assertEquals(response.code, StatusCode.NotFound)
  }
}
