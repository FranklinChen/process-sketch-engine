package com.franklinchen

object LookupEnv {
  val username = sys.env("SKETCH_ENGINE_USERNAME")
  val password = sys.env("SKETCH_ENGINE_PASSWORD")
  val strictTrust = false
}
