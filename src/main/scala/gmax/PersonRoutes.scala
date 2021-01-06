package gmax

import cats.effect.IO
import gmax.KVJson._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

sealed trait HealthRoutes extends Http4sDsl[IO] {
  def liveRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "ready" => Ok("OK")
    case GET -> Root / "live" => Ok("OK")
  }
}

object PersonRoutes extends HealthRoutes {

  def routes(personRepo: PersonRepo): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "persons" =>
      personRepo.getPersons flatMap {
        case Right(persons) => Ok(persons)
        case Left(message) => BadRequest(kvJson("message", message))
      }

    case req@POST -> Root / "person" =>
      req.decode[Person] { person =>
        personRepo.addPerson(person) flatMap {
          case Right(_) => Created(person)
          case Left(message) => BadRequest(kvJson("message", message))
        }
      }

    case GET -> Root / "person" / IntVar(id) =>
      personRepo.getPerson(id) flatMap {
        case Left(message) => NotFound(kvJson("message", message))
        case Right(person) => Ok(person)
      }

    case req@PUT -> Root / "person" =>
      req.decode[Person] { person =>
        personRepo.updatePerson(person) flatMap {
          case Left(message) => NotFound(kvJson("message", message))
          case Right(_) => Ok(person)
        }
      }

    case DELETE -> Root / "person" / IntVar(id) =>
      personRepo.deletePerson(id) flatMap {
        case Left(message) => NotFound(kvJson("message", message))
        case Right(p) => Ok(kvJson("result", p))
      }
  }
}