package com.franklinchen

import org.rogach.scallop._

class LookupConf(args: Seq[String]) extends ScallopConf(args) {
  val strictTrust = opt[Boolean]("strictTrust",
    descr = "use trust store")
  val username = trailArg[String](descr = "username for Sketch Engine")
  val password = trailArg[String](descr = "password for Sketch Engine")
  val corpus = trailArg[String](descr = "corpus, e.g., ententen12_1")
  val docnum = trailArg[Int](descr = "document number after 'doc#' prefix, e.g. 1048268 in doc#1048268")
}
