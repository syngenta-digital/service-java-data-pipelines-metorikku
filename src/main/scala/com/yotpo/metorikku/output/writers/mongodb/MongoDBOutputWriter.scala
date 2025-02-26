package com.yotpo.metorikku.output.writers.mongodb

import com.yotpo.metorikku.configuration.job.output.MongoDB
import com.yotpo.metorikku.output.Writer
import org.apache.log4j.LogManager
import org.apache.spark.sql.{DataFrame, SaveMode}

import org.bson.{BsonDocument, BsonArray}
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.connection.SslSettings
import com.mongodb.Block
import com.mongodb.ConnectionString
import scala.util.Try
import scala.util.Success
import scala.util.Failure

class MongoDBOutputWriter(
    props: Map[String, String],
    mongoDBConf: Option[MongoDB]
) extends Writer {

  case class MongoDBOutputProperties(
      saveMode: SaveMode,
      database: String,
      collection: String,
      ssl: Option[Boolean],
      sslDomainMatch: Option[Boolean]
  )

  @transient lazy val log = LogManager.getLogger(this.getClass)

  val mongoDBProps = MongoDBOutputProperties(
    SaveMode.valueOf(props.get("saveMode").getOrElse("Append")),
    props("database"),
    props("collection"),
    mongoDBConf
      .map(
        _.options
          .getOrElse(Map())
          .get("ssl")
          .asInstanceOf[Option[String]]
          .map(_.toBoolean)
          .asInstanceOf[Option[Boolean]]
      )
      .get,
    mongoDBConf
      .map(
        _.options
          .getOrElse(Map())
          .get("ssl.domain_match")
          .asInstanceOf[Option[String]]
          .map(_.toBoolean)
          .asInstanceOf[Option[Boolean]]
      )
      .get
  )

  private def executeCommand(
      mongoDBConf: MongoDB,
      mongoDBProps: MongoDBOutputProperties,
      command: String
  ): Unit = {
    try {
      val settings = MongoClientSettings
        .builder()
        .applyConnectionString(new ConnectionString(mongoDBConf.uri))
        .applyToSslSettings(new Block[SslSettings.Builder]() {
          def apply(builder: SslSettings.Builder) {
            builder
              .enabled(mongoDBProps.ssl.getOrElse(false))
              .invalidHostNameAllowed(
                !mongoDBProps.sslDomainMatch.getOrElse(false)
              )
          }
        })
        .build()

      val client = MongoClients.create(settings)

      val database = client.getDatabase(mongoDBProps.database)

      Try(BsonArray.parse(command))
        .map(d => {
          (0 until d.size()).map(i => d.get(i).asDocument()).toList
        })
        .getOrElse(List(BsonDocument.parse(command)))
        .foreach(x =>
          Try({
            log.info(
              s"Running command in DB[${mongoDBProps.database}]: ${x}"
            )
            database.runCommand(x)
          }) match {
            case Success(commandResult) =>
              log.info(
                s"Finish command in DB[${mongoDBProps.database}]: ${commandResult}"
              )
            case Failure(e) =>
              log.error(
                s"Failed to run command in DB[${mongoDBProps.database}]: ${e.getMessage}"
              )
          }
        )
    } catch {
      case e: Exception =>
        log.error(
          s"Failed to run command in DB[${mongoDBProps.database}]: ${e.getMessage}"
        )
    }
  }

  override def write(dataFrame: DataFrame): Unit = {
    mongoDBConf match {
      case Some(mongoDBConf) =>
        props.get("preCommand") match {
          case Some(command) => {
            log.info(s"Executing preCommand: ${command}")
            executeCommand(mongoDBConf, mongoDBProps, command)
            log.info(s"Executed preCommand: ${command}")
          }
          case _ =>
        }

        var options = collection.mutable.Map[String, String](
          "connection.uri" -> mongoDBConf.uri,
          "database"       -> mongoDBProps.database,
          "collection"     -> mongoDBProps.collection
        )

        mongoDBProps.ssl match {
          case Some(ssl) => options += "ssl" -> ssl.toString()
          case None      =>
        }

        mongoDBProps.sslDomainMatch match {
          case Some(sslDomainMatch) =>
            options += "ssl.domain_match" -> sslDomainMatch.toString()
          case None => options += "ssl.domain_match" -> "false"
        }

        options ++= props

        dataFrame.write.format("com.mongodb.spark.sql.connector.MongoTableProvider").mode(mongoDBProps.saveMode).options(options).save()

        props.get("postCommand") match {
          case Some(command) => {
            log.info(s"Executing postCommand: ${command}")
            executeCommand(mongoDBConf, mongoDBProps, command)
            log.info(s"Executed postCommand: ${command}")
          }
          case _ =>
        }

      case None =>
    }
  }
}
