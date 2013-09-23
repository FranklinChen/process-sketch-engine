package com.franklinchen

import javax.net.ssl.SSLContext
import java.security.KeyStore
import org.apache.http.conn.ssl.SSLConnectionSocketFactory

// https://github.com/dispatch/dispatch/blob/master/http/src/main/scala/https.scala
object TrustingSSLSocketFactory {
  def newContext() = {
    val truststore = KeyStore.getInstance(KeyStore.getDefaultType())
    truststore.load(null, null)

    val context = SSLContext.getInstance(SSLConnectionSocketFactory.TLS)
    context.init(null, Array(AllX509TrustManager), null)
    context
  }

  def newFactory() = {
    new SSLConnectionSocketFactory(newContext(),
      SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
  }
}
