package com.franklinchen

import org.rogach.scallop._

object NumberConverterConf {
  case class SpreadsheetKey(key: String)

  val spreadsheetKeyConverter = new ValueConverter[SpreadsheetKey] {
    val httpKeyRegex = """^http.*key=([0-9a-zA-Z]+)(?:&.+)?$""".r
    val keyRegex = """^([0-9a-zA-Z]+)$""".r

    override def parse(s: List[(String, List[String])]) = s match {
      case (_, httpKeyRegex(key) :: Nil) :: Nil =>
        Right(Some(SpreadsheetKey(key)))
      case (_, keyRegex(key) :: Nil) :: Nil =>
        Right(Some(SpreadsheetKey(key)))
      case Nil => Right(None)
      case _ =>
        Left("bad spreadsheet key")
    }

    override val tag = scala.reflect.runtime.universe.typeTag[SpreadsheetKey]
    override val argType = ArgType.SINGLE
  }
}

class NumberConverterConf(args: Seq[String]) extends ScallopConf(args) {
  import NumberConverterConf._

  val googleUser = opt[String](
    descr = "username for Google Spreadsheet access",
    required = true
  )
  val googlePass = opt[String](
    descr = "password for Google Spreadsheet access; if not supplied, enter at console",
    default = None
  )
  val sketchEngineUser = opt[String](
    default = Some("macw"),
    descr = "username for Sketch Engine"
  )
  val sketchEnginePass = opt[String](
    descr = "password for Sketch Engine; if not supplied, enter at console",
    default = None
  )
  val corpus = opt[String](
    descr = "corpus, e.g., ententen12_1 (note: do not use ententen12)",
    default = Some("ententen12_1")
  )
  val key = opt[SpreadsheetKey](
    descr = "spreadsheet key or URL containing it, e.g., https://docs.google.com/spreadsheet/ccc?key=0Al0YjdE9Ckf6dFBscnQ3TUdCRDdTT05BUnFJRG1EWWc&usp=docslist_api or 0Al0YjdE9Ckf6dFBscnQ3TUdCRDdTT05BUnFJRG1EWWc",
    required = true
  )(spreadsheetKeyConverter)
}
