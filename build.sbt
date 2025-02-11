organization := "org.syngenta"
name         := "service-java-data-pipelines-metorikku"

homepage := Some(
  url(
    "https://github.com/syngenta-digital/service-java-data-pipelines-metorikku"
  )
)

scmInfo := Some(
  ScmInfo(
    url(
      "https://github.com/syngenta-digital/service-java-data-pipelines-metorikku"
    ),
    "scm:git:git@github.com:syngenta-digital/service-java-data-pipelines-metorikku.git"
  )
)

scalaVersion := Option(System.getenv("SCALA_VERSION")).getOrElse("2.12.18")

val sparkVersion: Def.Initialize[String] = Def.setting {
  Option(System.getenv("SPARK_VERSION")).getOrElse("3.5.2")
}

val sparkShortVersion: Def.Initialize[String] = Def.setting {
  sparkVersion.value.split('.').take(2).mkString(".")
}

val jacksonVersion: Def.Initialize[String] = Def.setting {
  Option(System.getenv("JACKSON_VERSION")).getOrElse("2.15.2")
}

val sparkTestVersion: Def.Initialize[String] = Def.setting {
  "3.5.2_2.0.1"
}

// sbt-scalafix
semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision
scalacOptions += "-Ywarn-unused-import"

Test / testOptions := Seq(
  Tests.Argument(
    "-l",
    "com.yotpo.metorikku.tags.UnsupportedInCurrentVersion"
  )
)

lazy val excludeAvro              = ExclusionRule(organization = "org.apache.avro", name = "avro")
lazy val excludeSpark             = ExclusionRule(organization = "org.apache.spark")
lazy val excludeLog4j             = ExclusionRule(organization = "org.apache.logging.log4j")
lazy val excludeScalanlp          = ExclusionRule(organization = "org.scalanlp")
lazy val excludeJacksonCore       = ExclusionRule(organization = "com.fasterxml.jackson.core")
lazy val excludeJacksonDataFormat = ExclusionRule(organization = "com.fasterxml.jackson.dataformat")
lazy val excludeJacksonDataType   = ExclusionRule(organization = "com.fasterxml.jackson.datatype")
lazy val excludeJacksonModule     = ExclusionRule(organization = "com.fasterxml.jackson.module")
lazy val excludeAWS               = ExclusionRule(organization = "com.amazonaws")

libraryDependencies ++= Seq(
  "org.scala-lang"           % "scala-library"         % scalaVersion.value,
  
  "com.google.guava"         % "guava"                 % "25.0-jre",
  "com.google.code.gson"     % "gson"                  % "2.2.4",
  "com.typesafe.play"        %% "play-json"            % "2.10.6",
  "com.github.scopt"         %% "scopt"                % "4.1.0",
  "za.co.absa"               %% "abris"                % "3.2.2"  % "provided" excludeAll (excludeAvro),
  "org.apache.logging.log4j" % "log4j-api"             % "2.24.3" % "provided",
  "org.apache.logging.log4j" % "log4j-core"            % "2.24.3" % "provided",
  "org.apache.logging.log4j" % "log4j-slf4j-impl"      % "2.24.3" % "provided",
  "com.hubspot.jinjava"      % "jinjava"               % "2.7.0",
  "com.jayway.jsonpath"      % "json-path"             % "2.9.0",

  "org.apache.parquet"       % "parquet-avro"          % "1.15.0" % "provided",
  "com.amazon.deequ"         % "deequ"                 % ("2.0.9-spark-" + sparkShortVersion.value),
  "org.apache.avro"          % "avro"                  % "1.12.0" % "provided",

  "com.amazonaws"          % "aws-java-sdk-s3"                                    % "1.12.780",
  "com.amazonaws"          % "aws-java-sdk-dynamodb"                              % "1.12.780",
  "software.amazon.awssdk" % "dynamodb"                                           % "2.30.15",
  "software.amazon.awssdk" % "glue"                                               % "2.30.15",
  "software.amazon.awssdk" % "s3"                                                 % "2.30.15",
  "software.amazon.awssdk" % "sts"                                                % "2.30.15",

  
  "com.databricks"              %% "spark-xml"                                          % "0.18.0",
  "com.datastax.spark"          %% "spark-cassandra-connector"                          % "3.5.1",
  "com.outr"                    %% "hasher"                                             % "1.2.2",
  "com.redislabs"               %% "spark-redis"                                        % "3.1.0",
  "com.segment.analytics.java"  % "analytics"                                           % "2.1.1" % "provided",
  "com.syncron.amazonaws"       % "simba-athena-jdbc-driver"                            % "2.1.5" from s"https://downloads.athena.us-east-1.amazonaws.com/drivers/JDBC/SimbaAthenaJDBC-2.1.5.1000/AthenaJDBC42-2.1.5.1000.jar",
  "io.delta"                    %% "delta-spark"                                        % "3.3.0",
  "io.github.spark-redshift-community" %% "spark-redshift"                              % "6.3.0-spark_3.5" excludeAll (excludeAWS),
  "io.trino"                    % "trino-jdbc"                                          % "470",
  "io.vertx"                    % "vertx-json-schema"                                   % "4.5.12",
  "mysql"                       % "mysql-connector-java"                                % "8.0.33",
  "net.snowflake"               % "snowflake-jdbc"                                      % "3.22.0",
  "org.apache.hudi"             %% "hudi-spark-bundle"                                  % "0.10.0" % "provided",
  "org.apache.iceberg"          %% ("iceberg-spark-runtime-" + sparkShortVersion.value) % "1.7.1",
  "org.apache.kafka"            %% "kafka"                                              % "3.9.0",
  "org.apache.sedona"           %% ("sedona-spark-" + sparkShortVersion.value)          % "1.6.1",
  "org.datasyslab"              % "geotools-wrapper"                                    % "1.6.1-28.2",
  "org.influxdb"                % "influxdb-java"                                       % "2.24",
  "org.mongodb.spark"           %% "mongo-spark-connector"                              % "10.4.1",
  "org.postgresql"              % "postgresql"                                          % "42.7.5",
).map(
  _.excludeAll(
    excludeSpark,
    excludeJacksonCore,
    excludeJacksonDataFormat,
    excludeJacksonDataType,
    excludeJacksonModule,
    excludeScalanlp,
  )
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core"           % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-sql"            % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-mllib"          % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-hive"           % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-streaming"      % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-avro"           % sparkVersion.value % "provided",
  "org.apache.spark" %% "spark-hadoop-cloud" % sparkVersion.value % "provided" excludeAll (excludeAWS),
  "com.holdenkarau"  %% "spark-testing-base" % sparkTestVersion.value % "test",
)

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core"       % "jackson-annotations"     % jacksonVersion.value,
  "com.fasterxml.jackson.core"       % "jackson-core"            % jacksonVersion.value,
  "com.fasterxml.jackson.core"       % "jackson-databind"        % jacksonVersion.value,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion.value,
  "com.fasterxml.jackson.datatype"   % "jackson-datatype-jsr310" % jacksonVersion.value,
  "com.fasterxml.jackson.module"     %% "jackson-module-scala"   % jacksonVersion.value,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion.value,
  "com.fasterxml.jackson.datatype"   % "jackson-datatype-jdk8"   % jacksonVersion.value,
)

dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.core"       % "jackson-annotations"     % jacksonVersion.value,
  "com.fasterxml.jackson.core"       % "jackson-core"            % jacksonVersion.value,
  "com.fasterxml.jackson.core"       % "jackson-databind"        % jacksonVersion.value,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion.value,
  "com.fasterxml.jackson.datatype"   % "jackson-datatype-jsr310" % jacksonVersion.value,
  "com.fasterxml.jackson.module"     %% "jackson-module-scala"   % jacksonVersion.value,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion.value,
  "com.fasterxml.jackson.datatype"   % "jackson-datatype-jdk8"   % jacksonVersion.value,
)

assembly / assemblyShadeRules ++= Seq(
  ShadeRule
    .rename("com.google.**" -> "shaded.com.google.@1")
    .inAll
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  "confluent" at "https://packages.confluent.io/maven/"
)

fork := true

Test / javaOptions ++= Seq(
  "-Dspark.master=local[*]",
  "-Dspark.sql.session.timeZone=UTC",
  "-Duser.timezone=UTC"
)

// Assembly settings
Project.inConfig(Test)(baseAssemblySettings)

assembly / assemblyMergeStrategy := {
  case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
  case PathList("LICENSE", xs @ _*)               => MergeStrategy.discard
  case PathList("META-INF", "services", xs @ _*) =>
    MergeStrategy.filterDistinctLines
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("log4j.properties")  => MergeStrategy.first
  case PathList("hive-site.xml")     => MergeStrategy.discard
  case _                             => MergeStrategy.first
}

assembly / assemblyJarName := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"
assembly / assemblyOption := (assembly / assemblyOption).value
  .copy(cacheOutput = false)
assembly / assemblyOption := (assembly / assemblyOption).value
  .copy(cacheUnzip = false)
assembly / logLevel := Level.Error

// Publish settings
publishMavenStyle := true

// publishTo := Some("fury" at "https://maven.fury.io/syngenta-digital/")
// credentials += Credentials(
//   "fury",
//   "maven.fury.io",
//   sys.env.getOrElse("FURY_AUTH", ""),
//   "NOPASS"
// )

publishTo := Some(
  "GitHub Package Registry" at "https://maven.pkg.github.com/syngenta-digital/service-java-data-pipelines-metorikku"
)
credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  sys.env.getOrElse("GITHUB_EMAIL", ""),
  sys.env.getOrElse("GITHUB_TOKEN", "")
)

ThisBuild / versionScheme := Some("early-semver")

Compile / assembly / artifact := {
  val art = (Compile / assembly / artifact).value
  art.withClassifier(Some("assembly"))
}

addArtifact(Compile / assembly / artifact, assembly)

// Fix for SBT run to include the provided at runtime
Compile / run := Defaults
  .runTask(
    Compile / fullClasspath,
    Compile / run / mainClass,
    Compile / run / runner
  )
  .evaluated

releaseCommitMessage := s"Setting version to ${(ThisBuild / version).value} [skip ci]"

commands += Command.command("bump-patch") { state =>
  val extracted = Project extract state
  val customState = extracted.appendWithoutSession(
    Seq(
      releaseVersion := { ver =>
        sbtrelease
          .Version(ver)
          .map(_.bump(sbtrelease.Version.Bump.Bugfix).string)
          .getOrElse(sbtrelease.versionFormatError(ver))
      }
    ),
    state
  )
  Command.process("release with-defaults", customState)
}

commands += Command.command("bump-minor") { state =>
  println("Bumping minor version!")
  val extracted = Project extract state
  val customState = extracted.appendWithoutSession(
    Seq(
      releaseVersion := { ver =>
        sbtrelease
          .Version(ver)
          .map(_.bump(sbtrelease.Version.Bump.Minor).string)
          .getOrElse(sbtrelease.versionFormatError(ver))
      }
    ),
    state
  )
  Command.process("release with-defaults", customState)
}

commands += Command.command("bump-major") { state =>
  println("Bumping major version!")
  val extracted = Project extract state
  val customState = extracted.appendWithoutSession(
    Seq(
      releaseVersion := { ver =>
        sbtrelease
          .Version(ver)
          .map(_.bump(sbtrelease.Version.Bump.Major).string)
          .getOrElse(sbtrelease.versionFormatError(ver))
      }
    ),
    state
  )
  Command.process("release with-defaults", customState)
}

import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  pushChanges
)
