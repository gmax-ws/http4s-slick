package gmax

import cats.effect.{ContextShift, IO}
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.typesafe.config.Config
import javax.sql.DataSource
import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

case class Person(id: Int, name: String, age: Int)

sealed trait PersonModel {

  class Persons(tag: Tag) extends Table[(Int, String, Int)](tag, "PERSONS") {
    def id = column[Int]("ID", O.PrimaryKey)

    def name = column[String]("NAME")

    def age = column[Int]("AGE")

    def * = (id, name, age)

    // def * = (id, name, age) <> ((Person.apply _).tupled, Person.unapply)
  }

  val persons = TableQuery[Persons]

  def initialize(db: Database): Unit = {
    val setup = DBIO.seq(
      // Create table, including primary and foreign keys
      persons.schema.create,
      // Insert some persons
      persons += (58, "John", 62),
      persons += (63, "Hellen", 57),
      persons += (89, "Tedy", 31)
    )
    Await.result(db.run(setup), 10.seconds)
  }
}

sealed trait PersonApi {
  def getPerson(id: Int): IO[Either[String, Person]]

  def getPersons: IO[Either[String, List[Person]]]

  def addPerson(person: Person): IO[Either[String, Int]]

  def deletePerson(id: Int): IO[Either[String, Int]]

  def updatePerson(person: Person): IO[Either[String, Int]]
}

class PersonRepo(ds: DataSource, maxConnections: Int) extends PersonApi with PersonModel {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  val db = Database.forDataSource(ds, maxConnections = Option(maxConnections))
  initialize(db)

  def getPersons: IO[Either[String, List[Person]]] = IO.fromFuture(IO.delay(
    (for {
      person <- db.run(persons.result)
    } yield Right(person.map(tuple => Person tupled tuple).toList)).recover {
      case e: Exception => Left(e.getMessage)
    }
  ))

  def getPerson(id: Int): IO[Either[String, Person]] = IO.fromFuture(IO.delay(
    (for {
      persons <- db.run(persons.filter(_.id === id).result.headOption)
    } yield persons.map(tuple => Person tupled tuple).toRight(s"Person id=$id not found.")).recover {
      case e: Exception => Left(e.getMessage)
    }
  ))

  def addPerson(person: Person): IO[Either[String, Int]] = IO.fromFuture(IO.delay(
    (for {
      result <- db.run(persons += Person.unapply(person).get)
    } yield Right(result)).recover {
      case e: Exception => Left(e.getMessage)
    }
  ))

  def deletePerson(id: Int): IO[Either[String, Int]] = IO.fromFuture(IO.delay(
    (for {
      result <- db.run(persons.filter(_.id === id).delete)
    } yield if (result == 0) Left(s"Cannot delete, person id=$id not found.") else Right(result)).recover {
      case e: Exception => Left(e.getMessage)
    }
  ))

  def updatePerson(person: Person): IO[Either[String, Int]] = IO.fromFuture(IO.delay(
    (for {
      result <- db.run(persons.filter(_.id === person.id).update(Person.unapply(person).get))
    } yield if (result == 0) Left(s"Cannot update, person id=${person.id} not found.") else Right(result)).recover {
      case e: Exception => Left(e.getMessage)
    }
  ))
}

object PersonRepo {

  def apply(cfg: Config): PersonRepo = {

    val ds = new ComboPooledDataSource
    ds.setDriverClass(cfg.getString("driver"))
    ds.setJdbcUrl(cfg.getString("url"))
    ds.setUser(cfg.getString("username"))
    ds.setPassword(cfg.getString("password"))

    new PersonRepo(ds, cfg.getInt("maxConnections"))
  }
}