package com.franklinchen

import scalaz._
import Scalaz._

import org.apache.http.client.fluent._
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.{HttpClientBuilder,LaxRedirectStrategy}

import org.json4s._
import org.json4s.native.JsonMethods.{parse,compact,pretty,render}
import org.json4s.JsonDSL._


import scala.collection.JavaConversions._
import java.io.File
import java.io.InputStream

import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author Franklin Chen, FranklinChen@cmu.edu
 * @note Uses newer Http Components 4.x
 */
object Lookup {
  type Corpus = String
  type Docnum = Int
  type Toknum = Int

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

  def findToknum(corpus: String, docnum: Docnum): String \/ Toknum = {
    val url = new URIBuilder(firstUrl).
      addParameter("corpname", corpus).
      addParameter("queryselector", "cqlrow").
      addParameter("cql", s"<doc#$docnum>").
      addParameter("format", "json").
      build()
    var jsonStream: InputStream = null
    try {
      jsonStream = executor.execute(
        Request.Get(url)
      ).returnContent().asStream()

      val json = parse(jsonStream)
      ((json \ "Lines")(0) \ "toknum").extract[Toknum].right
    } catch {
      case t: Throwable => (t.getMessage + ": toknum").left
    } finally {
      if (jsonStream != null) {
        jsonStream.close()
      }
    }
  }

  def findUrl(corpus: String, docnum: Docnum): String \/ String = {
    findToknum(corpus, docnum) match {
      case -\/(e) => e.left
      case \/-(toknum) =>
        val url = new URIBuilder(fullrefUrl).
          addParameter("corpname", corpus).
          addParameter("pos", toknum.toString).
          addParameter("format", "json").
          build()
        //logger.info(s"url: $url")
        var jsonStream: InputStream = null
        try {
          jsonStream = executor.execute(
            Request.Get(url)
          ).returnContent().asStream()

          val json = parse(jsonStream)
          //logger.info(s"rawFindUrl json: ${pretty(render(json))}")
          (json \ "doc_url").extract[String].right
        } catch {
          case t: Throwable => (t.getMessage + ": doc_url").left
        } finally {
          if (jsonStream != null) {
            jsonStream.close()
          }
        }
    }
  }
}
