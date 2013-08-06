package com.franklinchen

import javax.net.ssl.SSLContext
import java.security.KeyStore
import org.apache.http.conn.ssl.SSLSocketFactory

// https://github.com/dispatch/dispatch/blob/master/http/src/main/scala/https.scala
object TrustingSSLSocketFactory {
  def newContext() = {
    val truststore = KeyStore.getInstance(KeyStore.getDefaultType())
    truststore.load(null, null)

    val context = SSLContext.getInstance(SSLSocketFactory.TLS)
    context.init(null, Array(AllX509TrustManager), null)
    context
  }

  def newFactory() = {
    new SSLSocketFactory(newContext(),
      SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
  }
}
