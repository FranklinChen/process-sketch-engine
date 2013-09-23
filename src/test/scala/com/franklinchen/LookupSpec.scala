package com.franklinchen

import org.specs2._
import org.specs2.specification._

class LookupSpec extends Specification { def is = s2"""
  Turn corpus and doc number into URL in Sketch Engine

    findToknum succeeds on good doc number $e1
    lookupUrl succeeds on good doc number $e2
    lookupUrl fails on bad doc number     $e3
  """
  import LookupEnv._

  val lookup = new Lookup(username, password, strictTrust)



  def e1 = {
    lookup.findToknum("ententen12_1", 1048268).toEither must beRight(
      747708013
    )
  }

  def e2 = {
    lookup.findUrl("ententen12_1", 1048268).toEither must beRight(
      "http://libcom.org/book/export/html/1426"
    )
  }

  def e3 = {
    lookup.findUrl("ententen12_1", 999999999).toEither must beLeft
  }
}
