package examples.slickstream

import org.postgresql.ds.PGSimpleDataSource
import slick.driver
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import slick.lifted.TableQuery

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by sajith on 1/25/18.
  */

case class Counter(id: Int, name: String)


class Counters(tag: Tag) extends Table[Counter](tag, "counter") {
  def id = column[Int]("id", O.PrimaryKey)

  def name = column[String]("name")


  def * = (id, name) <> (Counter.tupled, Counter.unapply _)
}

object DatabaseWrapper {
  val ds: PGSimpleDataSource = {
    val ds = new org.postgresql.ds.PGSimpleDataSource
    ds.setUser("postgres")
    ds.setPassword("postgres")
    ds.setDatabaseName("scala_test")
    val internalDb = Database.forDataSource(ds)
    ds
  }

  def db: driver.PostgresDriver.backend.DatabaseDef = Database.forDataSource(ds)

  def session: PostgresDriver.backend.Session = db.createSession()
}

object SlickExample {

  object CounterComp {
    val counter: TableQuery[Counters] = TableQuery[Counters]
  }

  def bootstrap(): Unit = {
    val db = DatabaseWrapper.db

    val setup = DBIO.seq(
      CounterComp.counter.schema.drop,
      CounterComp.counter.schema.create
    )

    Await.result(db.run(setup), 10 seconds)

    val ops = for (i <- 0 to 1000000) yield {
      DBIO.seq(
        CounterComp.counter += Counter(i, Random.nextString(10))
      )
    }
    ops.foreach(io => Await.result(db.run(io), 10 seconds))
  }

  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val db = DatabaseWrapper.db

    //this back pressures the DB connector,
    // so the next fetch query hits the db after the list fetched previously is processed.
    db.stream(CounterComp.counter.result.transactionally.withStatementParameters(fetchSize = 100)).foreach(i=>{
      Thread.sleep(100)
      println(i)
    })

    Thread.sleep(100000)
  }
}
