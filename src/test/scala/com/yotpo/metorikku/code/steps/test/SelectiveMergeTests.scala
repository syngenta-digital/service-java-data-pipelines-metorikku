package com.yotpo.metorikku.code.steps.test

import com.yotpo.metorikku.code.steps.SelectiveMerge
import com.yotpo.metorikku.code.steps.SelectiveMerge.merge
import com.yotpo.metorikku.exceptions.MetorikkuException
import org.apache.log4j.{Level, LogManager, Logger}
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer

//noinspection ScalaStyle
class SelectiveMergeTests extends AnyFunSuite with BeforeAndAfterEach {
  private val log: Logger                = LogManager.getLogger(this.getClass)
  private var sparkSession: SparkSession = _
  Logger.getLogger("org").setLevel(Level.WARN)

  override def beforeEach() {
    sparkSession = SparkSession
      .builder()
      .appName("udf tests")
      .master("local")
      .config("", "")
      .getOrCreate()
  }

  def assertSuccess(df1: DataFrame, df2: DataFrame, isEqual: Boolean): Unit = {
    val sortedSchemeArrBuff: ArrayBuffer[String] = ArrayBuffer[String]()
    df1.schema
      .sortBy({ f: StructField => f.name })
      .map({ f: StructField => sortedSchemeArrBuff += f.name })
    val sortedSchemeArr: Array[String] = sortedSchemeArrBuff.sortWith(_ < _).toArray

    val sortedMergedDf   = df1.orderBy("employee_name").select("employee_name", sortedSchemeArr: _*)
    val sortedExpectedDf = df2.orderBy("employee_name").select("employee_name", sortedSchemeArr: _*)
    val equals           = sortedMergedDf.except(sortedExpectedDf).isEmpty

    if (equals != isEqual) {
      if (!equals) {
        log.error("Actual and expected differ:")
        log.error("Actual:\n" + getDfAsStr(sortedMergedDf))
        log.error("Expected:\n" + getDfAsStr(sortedExpectedDf))
        assert(false)
      } else {
        log.error("Actual and expected are equal (but expected to differ)")
        assert(false)
      }
    }
  }

  def getDfAsStr(df: DataFrame): String = {
    val showString = classOf[org.apache.spark.sql.DataFrame]
      .getDeclaredMethod("showString", classOf[Int], classOf[Int], classOf[Boolean])
    showString.setAccessible(true)
    showString
      .invoke(df, 10.asInstanceOf[Object], 20.asInstanceOf[Object], false.asInstanceOf[Object])
      .asInstanceOf[String]
  }

  test("Selective merge") {
    val sparkSession = SparkSession.builder.appName("test").getOrCreate()
    val sqlContext   = sparkSession.sqlContext
    import sqlContext.implicits._

    val employeeData1 = Seq(
      ("James", 1, 11, 111, 1111),
      ("Maria", 2, 22, 222, 2222)
    )
    val df1 = employeeData1.toDF("employee_name", "salary", "age", "fake", "fake2")

    val employeeData2 = Seq(
      ("James", 1, 33, 333),
      ("Jen", 4, 44, 444),
      ("Jeff", 5, 55, 555)
    )
    val df2 = employeeData2.toDF("employee_name", "salary", "age", "bonus")

    val simpleDataExpectedAfterMerge = Seq(
      (
        "James",
        Integer.valueOf(1) /* Salary */,
        Integer.valueOf(33) /* age */,
        Integer.valueOf(111) /* fake */,
        Integer.valueOf(1111) /* fake2 */,
        Integer.valueOf(333) /* bonus */
      ),
      (
        "Maria",
        null.asInstanceOf[Integer] /* Salary */,
        null.asInstanceOf[Integer] /* age */,
        Integer.valueOf(222) /* fake */,
        Integer.valueOf(2222) /* fake2 */,
        null.asInstanceOf[Integer] /* bonus */
      ),
      (
        "Jen",
        Integer.valueOf(4) /* Salary */,
        Integer.valueOf(44) /* age */,
        null.asInstanceOf[Integer] /* fake */,
        null.asInstanceOf[Integer] /* fake2 */,
        Integer.valueOf(444) /* bonus */
      ),
      (
        "Jeff",
        Integer.valueOf(5) /* Salary */,
        Integer.valueOf(55) /* age */,
        null.asInstanceOf[Integer] /* fake */,
        null.asInstanceOf[Integer] /* fake2 */,
        Integer.valueOf(555) /* bonus */
      )
    )
    val expectedDf =
      simpleDataExpectedAfterMerge.toDF("employee_name", "salary", "age", "fake", "fake2", "bonus")

    val simpleDataNotExpectedAfterMerge = Seq(
      (
        "James",
        Integer.valueOf(10) /* Salary */,
        Integer.valueOf(33) /* age */,
        Integer.valueOf(111) /* fake */,
        Integer.valueOf(1111) /* fake2 */,
        Integer.valueOf(333) /* bonus */
      ),
      (
        "Maria",
        Integer.valueOf(20) /* Salary */,
        Integer.valueOf(22) /* age */,
        Integer.valueOf(222) /* fake */,
        Integer.valueOf(2222) /* fake2 */,
        null.asInstanceOf[Integer] /* bonus */
      ),
      (
        "Jen",
        Integer.valueOf(40) /* Salary */,
        Integer.valueOf(44) /* age */,
        null.asInstanceOf[Integer] /* fake */,
        null.asInstanceOf[Integer] /* fake2 */,
        Integer.valueOf(444) /* bonus */
      ),
      (
        "Jeff",
        Integer.valueOf(50) /* Salary */,
        Integer.valueOf(55) /* age */,
        null.asInstanceOf[Integer] /* fake */,
        null.asInstanceOf[Integer] /* fake2 */,
        Integer.valueOf(555) /* bonus */
      )
    )
    val notExpectedDf = simpleDataNotExpectedAfterMerge.toDF(
      "employee_name",
      "salary",
      "age",
      "fake",
      "fake2",
      "bonus"
    )

    val mergedDf = merge(df1, df2, Seq("employee_name"))

    assertSuccess(mergedDf, expectedDf, isEqual = true)
    assertSuccess(mergedDf, notExpectedDf, isEqual = false)
  }

  test("String and numbers mixed fields") {
    val sparkSession = SparkSession.builder.appName("test").getOrCreate()
    val sqlContext   = sparkSession.sqlContext
    import sqlContext.implicits._

    val employeeData1 = Seq(
      ("James", "Sharon", 11, 111, 1111),
      ("Maria", "Bob", 22, 222, 2222)
    )
    val df1 = employeeData1.toDF("employee_name", "last_name", "age", "fake", "fake2")

    val employeeData2 = Seq(
      ("James", 1, 33, 333),
      ("Jen", 4, 44, 444),
      ("Jeff", 5, 55, 555)
    )
    val df2 = employeeData2.toDF("employee_name", "salary", "age", "bonus")

    val simpleDataExpectedAfterMerge = Seq(
      (
        "James",
        "Sharon" /* Last Name */,
        Integer.valueOf(1) /* Salary */,
        Integer.valueOf(33) /* age */,
        Integer.valueOf(111) /* fake */,
        Integer.valueOf(1111) /* fake2 */,
        Integer.valueOf(333) /* bonus */
      ),
      (
        "Maria",
        "Bob" /* Last Name */,
        null.asInstanceOf[Integer] /* Salary */,
        null.asInstanceOf[Integer] /* age */,
        Integer.valueOf(222) /* fake */,
        Integer.valueOf(2222) /* fake2 */,
        null.asInstanceOf[Integer] /* bonus */
      ),
      (
        "Jen",
        null.asInstanceOf[String] /* Last Name */,
        Integer.valueOf(4) /* Salary */,
        Integer.valueOf(44) /* age */,
        null.asInstanceOf[Integer] /* fake */,
        null.asInstanceOf[Integer] /* fake2 */,
        Integer.valueOf(444) /* bonus */
      ),
      (
        "Jeff",
        null.asInstanceOf[String] /* Last Name */,
        Integer.valueOf(5) /* Salary */,
        Integer.valueOf(55) /* age */,
        null.asInstanceOf[Integer] /* fake */,
        null.asInstanceOf[Integer] /* fake2 */,
        Integer.valueOf(555) /* bonus */
      )
    )
    val expectedDf = simpleDataExpectedAfterMerge.toDF(
      "employee_name",
      "last_name",
      "salary",
      "age",
      "fake",
      "fake2",
      "bonus"
    )

    val mergedDf = merge(df1, df2, Seq("employee_name"))

    assertSuccess(mergedDf, expectedDf, isEqual = true)
  }

  test("df2 has more columns") {
    val sparkSession = SparkSession.builder.appName("test").getOrCreate()
    val sqlContext   = sparkSession.sqlContext
    import sqlContext.implicits._

    val employeeData1 = Seq(
      ("James", 1, 11),
      ("Maria", 2, 22),
      ("Albert", 3, 33)
    )
    val df1 = employeeData1.toDF("employee_name", "salary", "age")

    val employeeData2 = Seq(
      ("James", 10, 33, 333, 3333),
      ("Jen", 4, 44, 444, 4444)
    )
    val df2 = employeeData2.toDF("employee_name", "salary", "age", "bonus", "fake")

    val simpleDataExpectedAfterMerge = Seq(
      (
        "James",
        Integer.valueOf(10) /* Salary */,
        Integer.valueOf(33) /* age */,
        Integer.valueOf(333) /* Bonus */,
        Integer.valueOf(3333) /* fake */
      ),
      (
        "Maria",
        null.asInstanceOf[Integer] /* Salary */,
        null.asInstanceOf[Integer] /* age */,
        null.asInstanceOf[Integer] /* Bonus */,
        null.asInstanceOf[Integer] /* fake */
      ),
      (
        "Jen",
        Integer.valueOf(4) /* Salary */,
        Integer.valueOf(44) /* age */,
        Integer.valueOf(444) /* Bonus */,
        Integer.valueOf(4444) /* fake */
      ),
      (
        "Albert",
        null.asInstanceOf[Integer] /* Salary */,
        null.asInstanceOf[Integer] /* age */,
        null.asInstanceOf[Integer] /* Bonus */,
        null.asInstanceOf[Integer] /* fake */
      )
    )
    val expectedDf =
      simpleDataExpectedAfterMerge.toDF("employee_name", "salary", "age", "bonus", "fake")

    val mergedDf = merge(df1, df2, Seq("employee_name"))

    assertSuccess(mergedDf, expectedDf, isEqual = true)
  }

  test("Empty params metorikku exception") {
    val params: Option[Map[String, String]] = Option(Map("df1" -> "df1Name", "df2" -> "df2Name"))
    assertThrows[MetorikkuException] {
      SelectiveMerge.run(sparkSession, "MetricName", "DataFrameName", params)
    }
  }

  override def afterEach() {
    sparkSession.stop()
  }
}
