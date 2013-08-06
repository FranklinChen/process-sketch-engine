package com.franklinchen

import org.specs2._

class NumberConverterSpec extends Specification { def is = s2"""
  Use old Google Spreadsheet Java API

    Match docnum

      with doc# $e1
      without doc# $e2

    Try conversion
    
      with only good lookups $e3
  """
  import NumberConverterEnv._

  val lookup = new Lookup(LookupEnv.username,
    LookupEnv.password,
    LookupEnv.strictTrust
  )

  val converter = new NumberConverter(username, password, lookup)

  val key = "0Al0YjdE9Ckf6dFBscnQ3TUdCRDdTT05BUnFJRG1EWWc"

  def e1 = NumberConverter.matchesDocnum(" doc#23423 ") must beTrue

  def e2 = NumberConverter.matchesDocnum(" 23423 ") must beTrue

  // TODO how best test mutating actual Google Spreadsheet?
  def e3 = {
    val corpus = "ententen12_1"

    // TODO make custom matcher
    val (unfoundDocnums, batchFailures) = converter.doConversion(corpus, key)
    unfoundDocnums must be empty ;
    batchFailures must be empty
  }

  // TODO test failure
}
