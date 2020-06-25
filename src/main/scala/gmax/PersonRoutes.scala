package gmax

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object PersonRoutes extends Http4sDsl[IO] {

  def routes(personRepo: PersonRepo): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "persons" =>
      personRepo.getPersons flatMap {
        case Right(persons) => Ok(persons)
        case Left(message) => BadRequest(Map("message" -> message))
      }

    case req@POST -> Root / "person" =>
      req.decode[Person] { person =>
        personRepo.addPerson(person) flatMap {
          case Right(p) => Created(p)
          case Left(message) => BadRequest(Map("message" -> message))
        }
      }

    case GET -> Root / "person" / IntVar(id) =>
      personRepo.getPerson(id) flatMap {
        case Left(message) => NotFound(Map("message" -> message))
        case Right(person) => Ok(person)
      }

    case req@PUT -> Root / "person" =>
      req.decode[Person] { person =>
        personRepo.updatePerson(person) flatMap {
          case Left(message) => NotFound(Map("message" -> message))
          case Right(p) => Ok(p)
        }
      }

    case DELETE -> Root / "person" / IntVar(id) =>
      personRepo.deletePerson(id) flatMap {
        case Left(message) => NotFound(Map("message" -> message))
        case Right(p) => Ok(Map("result" -> p))
      }
  }
}