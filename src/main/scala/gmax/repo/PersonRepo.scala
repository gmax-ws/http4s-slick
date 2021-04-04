package gmax.repo

import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

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
      persons += (89, "Teddy", 31)
    )
    Await.result(db.run(setup), 10.seconds)
  }
}

trait PersonApi[F[_]] {
  def getPerson(id: Int): F[Either[String, Person]]

  def getPersons: F[Either[String, List[Person]]]

  def addPerson(person: Person): F[Either[String, Int]]

  def deletePerson(id: Int): F[Either[String, Int]]

  def updatePerson(person: Person): F[Either[String, Int]]
}

class PersonRepo(db: Database)(implicit ec: ExecutionContextExecutor) extends PersonApi[Future] with PersonModel {

  initialize(db)

  def getPersons: Future[Either[String, List[Person]]] =
    (for {
      person <- db.run(persons.result)
    } yield Right(person.map(tuple => Person tupled tuple).toList)).recover {
      case e: Exception => Left(e.getMessage)
    }

  def getPerson(id: Int): Future[Either[String, Person]] =
    (for {
      persons <- db.run(persons.filter(_.id === id).result.headOption)
    } yield persons.map(tuple => Person tupled tuple).toRight(s"Person id=$id not found.")).recover {
      case e: Exception => Left(e.getMessage)
    }

  def addPerson(person: Person): Future[Either[String, Int]] =
    (for {
      result <- db.run(persons += Person.unapply(person).get)
    } yield Right(result)).recover {
      case e: Exception => Left(e.getMessage)
    }

  def deletePerson(id: Int): Future[Either[String, Int]] =
    (for {
      result <- db.run(persons.filter(_.id === id).delete)
    } yield if (result == 0) Left(s"Cannot delete, person id=$id not found.") else Right(result)).recover {
      case e: Exception => Left(e.getMessage)
    }

  def updatePerson(person: Person): Future[Either[String, Int]] =
    (for {
      result <- db.run(persons.filter(_.id === person.id).update(Person.unapply(person).get))
    } yield if (result == 0) Left(s"Cannot update, person id=${person.id} not found.") else Right(result)).recover {
      case e: Exception => Left(e.getMessage)
    }
}

object PersonRepo {

  def apply(db: Database)(implicit ec: ExecutionContextExecutor): PersonRepo = {
    new PersonRepo(db)
  }
}