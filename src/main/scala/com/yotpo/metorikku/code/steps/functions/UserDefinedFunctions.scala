package com.yotpo.metorikku.code.steps.functions

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import net.minidev.json.{JSONValue, JSONArray}
import scala.jdk.CollectionConverters._

import java.sql.Timestamp
import java.time.Instant
import scala.util.Try
import com.yotpo.metorikku.utils.FileUtils

object UserDefinedFunctions {

  def epochMilliToTimestamp(timestamp_epoch: Long): Timestamp = {
    val instant: Instant = Instant.ofEpochMilli(timestamp_epoch)
    Timestamp.from(instant)
  }

  private val jsonPathConfig = Configuration
    .defaultConfiguration()
    .addOptions(com.jayway.jsonpath.Option.ALWAYS_RETURN_LIST)

  def getJsonObject(
      jsonTxt: String,
      path: String,
      compressIn: Boolean = false,
      compressOut: Boolean = false
  ): String = {
    Option(getJsonObjects(jsonTxt, List(path), compressIn, compressOut).headOption)
      .map(x => {
        x.head
      }).orNull // scalastyle:ignore null
  }

  def getJsonObjects(
      jsonTxt: String,
      paths: List[String],
      compressIn: Boolean = false,
      compressOut: Boolean = false
  ): List[String] = {
    Try({
      val finalJsonTxt = compressIn match {
        case true => FileUtils.decompressFromGzip(jsonTxt)
        case _    => jsonTxt
      }

      jsonPathConfig.jsonProvider().parse(finalJsonTxt)
    }).map(document => {
      paths.map(path => {
        Try({
          (compressOut, JSONValue.toJSONString(JsonPath.read(document, path))) match {
            case (true, value) => {
              FileUtils.compressToGzip(value)
            }
            case (_, value) => value
          }
        }).getOrElse(null) // scalastyle:ignore null
      })
    }).getOrElse(null) // scalastyle:ignore null
  }

  def getJsonDoubleLists(
                      jsonTxt: String,
                      paths: List[String]
                    ): List[List[Double]] = {
    Try({
      jsonPathConfig.jsonProvider().parse(jsonTxt)
    }).map(document => {
      paths.map(path => {
        Try({
          val rawValue: JSONArray = JsonPath.read(document, path)
          rawValue.asScala.toList.asInstanceOf[List[Double]]
        }).getOrElse(null) // scalastyle:ignore null
      })
    }).get // scalastyle:ignore null
  }
}
