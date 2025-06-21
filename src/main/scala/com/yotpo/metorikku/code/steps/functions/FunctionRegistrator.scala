package com.yotpo.metorikku.code.steps

import java.sql.Timestamp

import com.yotpo.metorikku.code.steps.functions.UserDefinedFunctions
import org.apache.spark.sql.functions.udf

object FunctionRegistrator {
  def run(
      spark: org.apache.spark.sql.SparkSession
  ): Unit = {
    spark.udf.register(
      "EPOCH_MILLI_TO_TIMESTAMP",
      udf[Timestamp, Long](UserDefinedFunctions.epochMilliToTimestamp)
    )

    spark.udf.register(
      "GET_JSON_OBJECT",
      udf[String, String, String]((jsonTxt: String, path: String) =>
        UserDefinedFunctions.getJsonObject(jsonTxt, path)
      )
    )

    spark.udf.register(
      "GET_JSON_OBJECT_GZIP",
      udf[String, String, String, Boolean, Boolean](
        UserDefinedFunctions.getJsonObject
      )
    )

    spark.udf.register(
      "GET_JSON_OBJECTS",
      udf[List[String], String, List[String]]((jsonTxt: String, paths: List[String]) =>
        UserDefinedFunctions.getJsonObjects(jsonTxt, paths)
      )
    )

    spark.udf.register(
      "GET_JSON_OBJECTS_GZIP",
      udf[List[String], String, List[String], Boolean, Boolean](
        UserDefinedFunctions.getJsonObjects
      )
    )

    spark.udf.register(
      "GET_JSON_DOUBLE_LISTS",
      udf[List[List[Double]], String, List[String]]((jsonTxt: String, paths: List[String]) =>
        UserDefinedFunctions.getJsonDoubleLists(jsonTxt, paths)
      )
    )
  }
}
