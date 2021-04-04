package gmax.repo

import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape

import scala.concurrent._
import scala.concurrent.duration._

case class Person(id: Int, name: String, age: Int)

sealed trait PersonModel {

  class Persons(tag: Tag) extends Table[Person](tag, "PERSONS") {
    def id: Rep[Int] = column[Int]("ID", O.PrimaryKey)

    def name: Rep[String] = column[String]("NAME")

    def age: Rep[Int] = column[Int]("AGE")

    def * : ProvenShape[Person] = (id, name, age) <> (Person.tupled, Person.unapply)
  }

  val persons = TableQuery[Persons]

  def initialize(db: Database): Unit = {
    val setup = DBIO.seq(
      // Create table, including primary and foreign keys
      persons.schema.create,
      // Insert some persons
      persons += Person(58, "John", 62),
      persons += Person(63, "Hellen", 57),
      persons += Person(89, "Teddy", 31)
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
      personList <- db.run(persons.result)
    } yield Right(personList.toList)).recover {
      case e: Exception => Left(e.getMessage)
    }

  def getPerson(id: Int): Future[Either[String, Person]] =
    (for {
      person <- db.run(persons.filter(_.id === id).result.headOption)
    } yield person match {
      case Some(p) => Right(p)
      case None => Left(s"Person id=$id not found.")
    }).recover {
      case e: Exception => Left(e.getMessage)
    }

  def addPerson(person: Person): Future[Either[String, Int]] =
    (for {
      result <- db.run(persons += person)
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
      result <- db.run(persons.filter(_.id === person.id).update(person))
    } yield if (result == 0) Left(s"Cannot update, person id=${person.id} not found.") else Right(result)).recover {
      case e: Exception => Left(e.getMessage)
    }
}

object PersonRepo {

  def apply(db: Database)(implicit ec: ExecutionContextExecutor): PersonRepo = {
    new PersonRepo(db)
  }
}