package com.franklinchen

import org.apache.http.client.fluent._
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.{HttpClientBuilder,LaxRedirectStrategy}

import org.json4s._
import org.json4s.native.JsonMethods.{parse,compact,render}
import org.json4s.JsonDSL._

import org.mapdb.DBMaker
import scala.collection.JavaConversions._
import java.io.File
import scala.collection.mutable.{Map => MutableMap}

import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author Franklin Chen, FranklinChen@cmu.edu
 * @note Uses newer Http Components 4.x
 */
object Lookup {
  type Corpus = String
  type Docnum = Int
  type Toknum = Int

  val dbPath = new File(sys.env("HOME"), ".sketch_engine_cache")

  val db = DBMaker.newFileDB(dbPath).
    closeOnJvmShutdown().
    make()

  val toknumCache: MutableMap[(Corpus, Docnum), Toknum] =
    db.getHashMap[(Corpus, Docnum), Toknum]("toknum")

  val urlCache: MutableMap[(Corpus, Toknum), String] =
    db.getHashMap[(Corpus, Toknum), String]("url")

  val host = "https://beta.sketchengine.co.uk"

  val loginUrl = s"$host/login/"
  val baseUrl = s"$host/bonito/run.cgi"
  val firstUrl = s"$baseUrl/first"
  val fullrefUrl = s"$baseUrl/fullref"

  implicit val formats = DefaultFormats

  def main(args: Array[String]): Unit = {
    val conf = new LookupConf(args)

    val lookup = new Lookup(conf.username(),
      conf.password(),
      conf.strictTrust())
    val url = lookup.findUrl(conf.corpus(),
      conf.docnum())
    println(url)
  }
}

class Lookup(username: String,
  password: String,
  strictTrust: Boolean = false) extends Logging {
  import Lookup._

  val executor = {
    // 4.3-beta2: https://hc.apache.org/news.html
    // https://hc.apache.org/httpcomponents-client-dev/httpclient/apidocs/org/apache/http/impl/client/HttpClientBuilder.html
    val client =
      if (strictTrust) {
        HttpClientBuilder.create().
          setRedirectStrategy(new LaxRedirectStrategy()).
          build()
      } else {
        HttpClientBuilder.create().
          setRedirectStrategy(new LaxRedirectStrategy()).
          setSSLSocketFactory(TrustingSSLSocketFactory.newFactory()).
          build()
      }

    val executor = Executor.newInstance(client)

    val loginInitialResult = executor.execute(
      Request.Get(loginUrl)
    )

    val loginResult = executor.execute(
      Request.Post(loginUrl).
        bodyForm(
          Form.form().add("submit", "ok").
            add("username", username).
            add("password", password).
            build()
        )
    ).returnContent().asString()
    //logger.info(loginResult)

    // TODO Change
    assert(loginResult contains "user:")

    executor
  }

  def findToknum(corpus: String, docnum: Docnum): Option[Toknum] =
    toknumCache.get((corpus, docnum)) match {
      case yes @ Some(_) => yes
      case None =>
        rawFindToknum(corpus, docnum) match {
          case result @ Some(computed) =>
            logger.info(s"storing into toknum cache for $corpus, $docnum")
            toknumCache((corpus, docnum)) = computed
            db.commit()
            result
          case None =>
            None
        }
    }

  def findUrl(corpus: String, toknum: Docnum): Option[String] =
    urlCache.get((corpus, toknum)) match {
      case yes @ Some(_) => yes
      case None =>
        rawFindUrl(corpus, toknum) match {
          case result @ Some(computed) =>
            logger.info(s"storing into url cache for $corpus, $toknum")
            urlCache((corpus, toknum)) = computed
            db.commit()
            result
          case None =>
            None
        }
    }
    

  def rawFindToknum(corpus: String, docnum: Docnum): Option[Toknum] = {
    val url = new URIBuilder(firstUrl).
      addParameter("corpname", corpus).
      addParameter("queryselector", "cqlrow").
      addParameter("cql", s"<doc#$docnum>").
      addParameter("format", "json").
      build()
    val jsonStream = executor.execute(
      Request.Get(url)
    ).returnContent().asStream()

    try {
      val json = parse(jsonStream)
      val toknum = ((json \ "Lines")(0) \ "toknum").extractOpt[Toknum]
      toknum
    } finally {
      jsonStream.close()
    }
  }

  def rawFindUrl(corpus: String, docnum: Docnum): Option[String] = {
    val toknum = findToknum(corpus, docnum)

    val url = new URIBuilder(fullrefUrl).
      addParameter("corpname", corpus).
      addParameter("pos", toknum.toString).
      addParameter("format", "json").
      build()
    val jsonStream = executor.execute(
      Request.Get(url)
    ).returnContent().asStream()

    try {
      val json = parse(jsonStream)
      (json \ "doc_url").extractOpt[String]
    } finally {
      jsonStream.close()
    }
  }
}
