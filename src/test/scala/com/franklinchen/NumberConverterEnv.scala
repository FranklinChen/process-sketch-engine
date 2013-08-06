package com.franklinchen

object NumberConverterEnv {
  val username = sys.env("GOOGLE_USERNAME")
  val password = sys.env("GOOGLE_PASSWORD")
}
