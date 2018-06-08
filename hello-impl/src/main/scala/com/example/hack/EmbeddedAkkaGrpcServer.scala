package com.example.hack

import java.io._
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{ KeyFactory, KeyStore, SecureRandom }
import java.util.Base64

import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.{ Http, Http2, HttpsConnectionContext }
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import example.myapp.helloworld.grpc.GreeterServiceHandler
import javax.net.ssl.{ KeyManagerFactory, SSLContext }

import scala.concurrent.{ ExecutionContext, Future }

/**
  * This is a hack! Do not do this in your production code!!!
  */
class EmbeddedAkkaGrpcServer(remotePort:Int) {

  // important to enable HTTP/2 in ActorSystem's config
  val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())
  implicit val sys: ActorSystem = ActorSystem("HelloWorld", conf)
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = sys.dispatcher

  val service: HttpRequest => Future[HttpResponse] = GreeterServiceHandler(new GreeterServiceImpl(mat))
    .orElse { case _ => Future.successful(HttpResponse(StatusCodes.NotFound)) }

  private val eventualBinding: Future[Http.ServerBinding] = Http().bindAndHandleAsync(
    service,
    interface = "127.0.0.1",
    port = remotePort,
    connectionContext = serverHttpContext()
  )
  eventualBinding.foreach { binding =>
    println(s"gRPC server bound to: ${binding.localAddress}")
  }

  def shutdown =
    eventualBinding.flatMap {
      bind =>
        bind.unbind()
    }

  private def serverHttpContext(): HttpsConnectionContext = {
    // FIXME how would end users do this? TestUtils.loadCert? issue #89
    val keyEncoded = read(getClass.getResourceAsStream("/certs/server1.key"))
      .replace("-----BEGIN PRIVATE KEY-----\n", "")
      .replace("-----END PRIVATE KEY-----\n", "")
      .replace("\n", "")

    val decodedKey = Base64.getDecoder.decode(keyEncoded)

    val spec = new PKCS8EncodedKeySpec(decodedKey)

    val kf = KeyFactory.getInstance("RSA")
    val privateKey = kf.generatePrivate(spec)

    val fact = CertificateFactory.getInstance("X.509")
    //    val is = new FileInputStream(TestUtils.loadCert("server1.pem"))
    val cer = fact.generateCertificate(getClass.getResourceAsStream("/certs/server1.pem"))

    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry("private", privateKey, Array.empty, Array(cer))

    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)

    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

    new HttpsConnectionContext(context)
  }

  private def read(in: InputStream): String = {
    val bytes: Array[Byte] = {
      val baos = new ByteArrayOutputStream(math.max(64, in.available()))
      val buffer = Array.ofDim[Byte](32 * 1024)

      var bytesRead = in.read(buffer)
      while (bytesRead >= 0) {
        baos.write(buffer, 0, bytesRead)
        bytesRead = in.read(buffer)
      }
      baos.toByteArray
    }
    new String(bytes, "UTF-8")
  }

}