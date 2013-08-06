package com.franklinchen

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

object AllX509TrustManager extends X509TrustManager {
  override def checkClientTrusted(xcs: Array[X509Certificate],
    string: String) = {
  }

  override def checkServerTrusted(xcs: Array[X509Certificate],
    string: String) = {
  }

  override def getAcceptedIssuers = null
}
