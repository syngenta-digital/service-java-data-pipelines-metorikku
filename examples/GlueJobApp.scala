import scala.collection.JavaConverters._
import com.yotpo.metorikku.Metorikku

object GlueJobApp {
  def main(sysArgs: Array[String]) {
    val envVars: Map[String, String] = Map(
      "AWS_REGION" -> "eu-central-1", // Obligatory for Metorikku to run in Glue
      "SPARK_VERSION" -> "3.5", // Obligatory for Metorikku to run in Glue

      "ENV" -> "dev",

      "SOME_PARAMETER_FOR_TEMPLATING_1" -> "VALUE_1",
      "SOME_PARAMETER_FOR_TEMPLATING_2" -> "VALUE_2",
    )

    // Set environment variables for the templating the Meotrikku job and metrics configuration
    envVars.foreach { case (key, value) =>
      System.setProperty(key, value)
    }

    val args: Array[String] = Array("-c", "s3://some_bucket/some_path/some_job.yaml")

    Metorikku.main(args)
  }
}