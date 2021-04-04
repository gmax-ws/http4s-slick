package gmax.routes

import cats.effect.IO
import gmax.json.KVJson._
import gmax.repo.{Person, PersonApi}
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

sealed trait Routes extends Http4sDsl[IO]

object HealthRoutes extends Routes {
  def healthRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "ready" => Ok("ready")
    case GET -> Root / "live" => Ok("live")
  }
}

object PersonRoutes extends Routes {

  def personRoutes(personRepo: PersonApi[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {

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