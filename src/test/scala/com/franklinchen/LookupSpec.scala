package com.franklinchen

import org.specs2._
import org.specs2.specification._

class LookupSpec extends Specification { def is = s2"""
  Turn corpus and doc number into URL in Sketch Engine

    lookupUrl succeeds on good doc number $e1
    lookupUrl fails on bad doc number     $e2
  """
  import LookupEnv._

  val lookup = new Lookup(username, password, strictTrust)

  def e1 = {
    lookup.findUrl("ententen12_1", 1048268) must beSome(
      "http://libcom.org/book/export/html/1426"
    )
  }

  def e2 = {
    lookup.findUrl("ententen12_1", 999999999) must beNone
  }
}
