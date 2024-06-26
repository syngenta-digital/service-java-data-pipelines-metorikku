package com.yotpo.metorikku.output.writers.kafka

import com.yotpo.metorikku.configuration.job.Streaming
import com.yotpo.metorikku.configuration.job.output.Kafka
import com.yotpo.metorikku.exceptions.MetorikkuException
import com.yotpo.metorikku.output.Writer
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.sql.DataFrame

class KafkaOutputWriter(props: Map[String, String], config: Option[Kafka]) extends Writer {

  case class KafkaOutputProperties(
      topic: String,
      keyColumn: Option[String],
      valueColumn: String,
      outputMode: String,
      triggerType: Option[String],
      triggerDuration: String,
      extraOptions: Option[Map[String, String]]
  )

  val log: Logger = LogManager.getLogger(this.getClass)

  val topic: String = props.get("topic") match {
    case Some(column) => column
    case None         => throw MetorikkuException("topic is mandatory of KafkaOutputWriter")
  }

  val valueColumn: String = props.get("valueColumn") match {
    case Some(column) => column
    case None         => throw MetorikkuException("valueColumn is mandatory of KafkaOutputWriter")
  }

  val kafkaOptions = KafkaOutputProperties(
    topic,
    props.get("keyColumn"),
    valueColumn,
    props.getOrElse("outputMode", "append"),
    props.get("triggerType"),
    props.getOrElse("triggerDuration", "10 seconds"),
    props.get("extraOptions").asInstanceOf[Option[Map[String, String]]]
  )

  override def write(dataFrame: DataFrame): Unit = {
    config match {
      case Some(kafkaConfig) =>
        val df: DataFrame = selectedColumnsDataframe(dataFrame)
        log.info(s"Writing Dataframe to Kafka Topic ${kafkaOptions.topic}")
        df.write.format("kafka").options(getKafkaOptions(kafkaConfig)).save()

      case None =>
    }
  }

  private def getKafkaOptions[T](kafkaConfig: Kafka): Map[String, String] = {
    Map[String, String](
      "kafka.bootstrap.servers" -> kafkaConfig.servers.mkString(","),
      "topic"                   -> kafkaOptions.topic
    ) ++ kafkaOptions.extraOptions.getOrElse(Map[String, String]())
  }

  private def selectedColumnsDataframe(dataFrame: DataFrame) = {
    val selectExpression = kafkaOptions.keyColumn match {
      case None =>
        dataFrame.selectExpr(s"${kafkaOptions.valueColumn} as value")
      case Some(column) =>
        dataFrame.selectExpr(
          s"CAST($column AS STRING) AS key",
          s"${kafkaOptions.valueColumn} as value"
        )
    }
    selectExpression
  }

  override def writeStream(dataFrame: DataFrame, streamingConfig: Option[Streaming]): Unit = {
    config match {
      case Some(kafkaConfig) =>
        log.info(s"Writing Dataframe to Kafka Topic ${kafkaOptions.topic}")
        val df: DataFrame     = selectedColumnsDataframe(dataFrame)
        val kafkaOutputStream = df.writeStream.format("kafka")

        kafkaOutputStream.options(getKafkaOptions(kafkaConfig))

        kafkaConfig.compressionType match {
          case Some(compressionType) =>
            kafkaOutputStream.option("kafka.compression.type", compressionType)
          case None =>
        }

        val deprecatedStreamingConfig = Option(
          Streaming(
            triggerMode = kafkaOptions.triggerType,
            triggerDuration = Option(kafkaOptions.triggerDuration),
            outputMode = Option(kafkaOptions.outputMode),
            checkpointLocation = kafkaConfig.checkpointLocation,
            batchMode = None,
            extraOptions = None
          )
        )

        streamingConfig.orElse(deprecatedStreamingConfig) match {
          case Some(config) => config.applyOptions(kafkaOutputStream)
          case None         =>
        }

        val query = kafkaOutputStream.start()
        query.awaitTermination()
      case None =>
    }
  }

}
