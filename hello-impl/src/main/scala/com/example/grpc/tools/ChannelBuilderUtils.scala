package com.example.grpc.tools

import java.io.{ BufferedInputStream, BufferedOutputStream, File, FileOutputStream }

import io.grpc.netty.shaded.io.grpc.netty.{ GrpcSslContexts, NegotiationType, NettyChannelBuilder }
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext

private class PlaceHolder

object ChannelBuilderUtils {

  def build(host: String, port: Int) = {

    val sslContext: SslContext =
      try
        GrpcSslContexts.forClient.trustManager(loadCert("ca.pem")).build
      catch {
        case ex: Exception => throw new RuntimeException(ex)
      }

    val builder =
      NettyChannelBuilder
        .forAddress(host, port)
        .flowControlWindow(65 * 1024)
        .negotiationType(NegotiationType.TLS)
        .sslContext(sslContext)

    builder.overrideAuthority("foo.test.google.fr")

    builder.build
  }

  def loadCert(name: String): File = {
    val in = new BufferedInputStream(classOf[PlaceHolder].getResourceAsStream("/certs/" + name))
    val tmpFile: File = File.createTempFile(name, "")
    tmpFile.deleteOnExit()
    val os = new BufferedOutputStream(new FileOutputStream(tmpFile))
    try {
      var b = 0
      do {
        b = in.read
        if (b != -1)
          os.write(b)
        os.flush()
      } while (b != -1)
    } finally {
      in.close()
      os.close()
    }
    tmpFile
  }
}
