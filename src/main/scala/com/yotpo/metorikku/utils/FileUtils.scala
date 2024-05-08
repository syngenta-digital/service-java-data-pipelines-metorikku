package com.yotpo.metorikku.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.hubspot.jinjava.Jinjava
import com.yotpo.metorikku.input.readers.file.FileType
import org.apache.commons.io.FilenameUtils
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.stream.Collectors
import scala.collection.JavaConverters._
import com.yotpo.metorikku.configuration.ConfigurationType
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.{Draft, JsonSchemaOptions, Validator}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.OutputFormat
import org.apache.hadoop.conf.Configuration
import org.apache.log4j.LogManager
import org.apache.log4j.Logger

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.util.zip.GZIPInputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import org.apache.commons.codec.binary.Base64
import java.util.zip.GZIPOutputStream

case class HadoopPath(path: Path, fs: FileSystem) {
  def open: FSDataInputStream = {
    fs.open(path)
  }

  def getName: String = {
    path.getName
  }
}

object FileUtils {
  private val log: Logger = LogManager.getLogger(this.getClass)

  private var parentPath: Option[String] = None

  private val LOCAL_FILE_REGEX = "\\./(.+)".r
  private val S3_FILE_REGEX    = "s3://([^/]+)/(.+)".r

  def setParentPath(path: String): Unit = {
    parentPath = getFolder(path)
  }

  def getListOfLocalFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else if (d.isFile) {
      List(d)
    } else {
      throw new FileNotFoundException(s"No Files to Run ${dir}")
    }
  }

  def getObjectMapperByExtension(extension: String): Option[ObjectMapper] = {
    extension match {
      case "json" =>
        Option(
          new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        )
      case "yaml" | "yml" | _ =>
        Option(
          new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        )
    }
  }

  def getObjectMapperByFileName(fileName: String): Option[ObjectMapper] = {
    val extension = FilenameUtils.getExtension(fileName)
    getObjectMapperByExtension(extension)
  }

  def readConfigurationFile(path: String): String = {
    val envAndSystemProperties = getEnvProperties()
    val prefix = getFilesPathPrefix(Option.apply(envAndSystemProperties)).getOrElse("")

    val jinjava = new Jinjava()

    val fileContents = readFileWithHadoop(prefix + path)
    jinjava.render(fileContents, envAndSystemProperties.asJava)
  }

  def getEnvProperties(): Map[String, String] = {
    val envAndSystemProperties = System.getProperties().asScala ++= System.getenv().asScala
    envAndSystemProperties.toMap
  }

  def getFilesPathPrefix(envProperties: Option[Map[String, String]]): Option[String] = {
    envProperties.getOrElse(getEnvProperties()).get("CONFIG_FILES_PATH_PREFIX")
  }

  def getName(path: String): Option[String] = {
    path.split("/").lastOption
  }

  def getFolder(path: String): Option[String] = {
    Option(path.split("/").dropRight(1).mkString("/"))
  }

  def getHadoopPath(path: String): HadoopPath = {
    val hadoopConf = new Configuration()

    val file = new Path(path)

    val fs = file.getFileSystem(hadoopConf)
    HadoopPath(file, fs)
  }

  def readFileWithHadoop(path: String): String = {
    val finalPath = (path, parentPath) match {
      case (LOCAL_FILE_REGEX(path), Some(parentPath)) => {
        f"${parentPath}/${path}"
      }
      case _ => path
    }

    log.info(f"Reading file with Hadoop: ${path} -> ${finalPath}")

    val in = finalPath match {
      case S3_FILE_REGEX(bucketName, key) => {
        val s3Client = S3Client.builder().build()

        val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build()

        s3Client.getObject(getObjectRequest)
      }
      case _ => getHadoopPath(finalPath).open
    }

    val reader = new BufferedReader(new InputStreamReader(in))

    reader.lines.collect(Collectors.joining("\n"))
  }

  def isLocalDirectory(path: String): Boolean = {
    new File(path).isDirectory
  }

  def isLocalFile(path: String): Boolean = {
    new File(path).isFile
  }

  def getFileFormat(path: String): String = {
    FileType.getFileType(path) match {
      case FileType.json | FileType.jsonl => "json"
      case FileType.csv                   => "csv"
      case _                              => "parquet"
    }
  }

  private def convertToJson(
      input: String,
      inputObjectMapper: ObjectMapper = getObjectMapperByExtension("yaml").get
  ): String = {
    inputObjectMapper.registerModule(DefaultScalaModule)

    val jsonMapper = getObjectMapperByExtension("json").get
    jsonMapper.registerModule(DefaultScalaModule)

    val inputObject = inputObjectMapper.readValue(input, classOf[Map[String, AnyRef]])

    jsonMapper.writeValueAsString(inputObject)
  }

  def validateConfigFile(
      configFile: String,
      configType: ConfigurationType.ConfigurationType,
      configFileMapper: ObjectMapper
  ): Unit = {
    val schemaYaml = configType.getSchema()
    val schemaJson = convertToJson(schemaYaml)

    val configFileJson = convertToJson(configFile, configFileMapper)

    val schema = JsonSchema.of(new JsonObject(schemaJson))

    val validationResult = Validator
      .create(
        schema,
        new JsonSchemaOptions()
          .setDraft(Draft.DRAFT202012)
          .setBaseUri(
            s"https://github.com/syngenta-digital/service-java-data-pipelines-metorikku/schemas/${configType.toString()}"
          )
          .setOutputFormat(OutputFormat.Basic)
      )
      .validate(new JsonObject(configFileJson))

    validationResult.checkValidity()
  }

  def decompressFromGzip(input: String): String = {
    val compressedData = Base64.decodeBase64(input)
    val inputStream    = new GZIPInputStream(new ByteArrayInputStream(compressedData))
    val outputStream   = new ByteArrayOutputStream()
    val buffer         = new Array[Byte](1024) // scalastyle:ignore magic.number
    var len            = inputStream.read(buffer)
    while (len > 0) {
      outputStream.write(buffer, 0, len)
      len = inputStream.read(buffer)
    }
    inputStream.close()
    outputStream.close()
    outputStream.toString("UTF-8")
  }

  def compressToGzip(input: String): String = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val gzipOutputStream      = new GZIPOutputStream(byteArrayOutputStream)
    gzipOutputStream.write(input.getBytes("UTF-8"))
    gzipOutputStream.close()
    new String(Base64.encodeBase64(byteArrayOutputStream.toByteArray))
  }
}
