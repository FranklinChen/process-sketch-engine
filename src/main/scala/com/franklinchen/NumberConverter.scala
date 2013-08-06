package com.franklinchen

import scala.collection.mutable.{Buffer, Seq => MutableSeq}
import scala.collection.JavaConversions._
import com.github.theon.uri.Uri.parseUri

import com.google.gdata.data._
import com.google.gdata.data.batch._
import com.google.gdata.data.spreadsheet._
import com.google.gdata.client.spreadsheet._

import java.net.URL

import com.typesafe.scalalogging.slf4j.Logging

object NumberConverter {
  val docnumRegex = """^\s*(?:doc#)?(\d+)\s*$""".r

  // TODO How do we decide which column? "link" is column 8
  val urlColumn = 8

  def matchesDocnum(value: String) = value match {
    case docnumRegex(_) => true
    case _ => false
  }

  import scala.tools.jline.console.ConsoleReader
  lazy val consoleReader = new ConsoleReader()

  def readPassword(str: String) = {
    consoleReader.readLine(s"$str password: ", '*')
  }

  def main(args: Array[String]) {
    val conf = new NumberConverterConf(args)

    val sketchEnginePass = conf.sketchEnginePass.get match {
      case Some(pass) => pass
      case None => readPassword("Sketch Engine")
    }

    val googlePass = conf.googlePass.get match {
      case Some(pass) => pass
      case None => readPassword("Google docs")
    }

    val converter = new NumberConverter(conf.googleUser(),
      googlePass,
      new Lookup(
        conf.sketchEngineUser(),
        sketchEnginePass
      )
    )

    val (unfoundDocnums, batchFailures) = converter.doConversion(
      conf.corpus(),
      conf.key().key
    )

    for (unfoundDocnum <- unfoundDocnums) {
      println(s"warning: could not find doc#$unfoundDocnum")
    }
    for (batchFailure <- batchFailures) {
      println(s"error: $batchFailure")
    }
  }
}

class NumberConverter(username: String,
  password: String,
  lookup: Lookup)
    extends Logging {
  import NumberConverter._

  // Log in
  val service = new SpreadsheetService("Document Numbers to Urls")
  service.setUserCredentials(username, password)

  val urlFactory = FeedURLFactory.getDefault()
  assert(urlFactory != null)

  /**
    @return List of failures
    */
  def doConversion(corpus: String, key: String): (Seq[Int], Seq[String]) = {
    // TODO Can we assume using only the first worksheet?
    // "od6" is the first worksheet
    val cellFeedUrl = urlFactory.getCellFeedUrl(key,
      "od6",
      "private",
      "full")
    if (cellFeedUrl == null) {
      sys.error(s"no cell feed URL obtained from $key")
    }

    val requestUrl = new URL(
      parseUri(cellFeedUrl.toString) ?
        ("min-col" -> urlColumn) &
        ("max-col" -> urlColumn)
    )

    val cellFeed = service.getFeed(requestUrl, classOf[CellFeed])
    if (cellFeed == null) {
      sys.error(s"no cell feed from $requestUrl")
    }

    // Use the optimized batch update.
    val cellEntries = cellFeed.getEntries()
    val (unfoundDocnums, batchEntries) = (
      (Seq[Int](), Buffer[CellEntry]()) /:
        cellEntries
    ) {
      (p: (Seq[Int], Buffer[CellEntry]), cellEntry: CellEntry) => {
        val (us, bs) = p
        val cell = cellEntry.getCell()
        assert(cell != null)
        val row = cell.getRow()
        val col = cell.getCol()

        cell.getValue() match {
          case docnumRegex(docnumString) =>
            logger.info(s"saw $docnumString")
            val docnum = docnumString.toInt
            lookup.findUrl(corpus, docnum) match {
              case None =>
                val message = s"$corpus, $docnum: failed to find, ignoring"
                logger.info(message)
                (us :+ docnum, bs)
              case Some(url) =>
                val idString = s"${cellFeedUrl}/R${row}C${col}"
                val batchEntry = new CellEntry(row, col, url)
                batchEntry.setId(idString)
                batchEntry.changeInputValueLocal(url)

                BatchUtils.setBatchId(batchEntry, idString)
                BatchUtils.setBatchOperationType(batchEntry,
                  BatchOperationType.UPDATE)
                (us, bs :+ batchEntry)
            }
          case _ =>
            // Do nothing
            (us, bs)
        }
      }
    }
    val batchRequest = new CellFeed()
    batchRequest.setEntries(batchEntries)

    // Submit the update
    val batchLink = cellFeed.getLink(ILink.Rel.FEED_BATCH, ILink.Type.ATOM);

    /* Work around unfixed bug from 2009 not yet fixed in 2013!!
     https://code.google.com/p/gdata-java-client/issues/detail?id=103
     */
    service.setHeader("If-Match", "*")
    val batchResponse = service.batch(new URL(batchLink.getHref()),
      batchRequest);
    service.setHeader("If-Match", null)

    // Check the results
    val batchResponseEntries = batchResponse.getEntries()
    val batchFailures = for {
      cellEntry <- batchResponseEntries
      if (!BatchUtils.isSuccess(cellEntry))
      batchId = BatchUtils.getBatchId(cellEntry)
      status = BatchUtils.getBatchStatus(cellEntry)
    } yield s"$batchId failed (${status.getReason()}) ${status.getContent()}"

    (unfoundDocnums, batchFailures)
  }
}
