package gmax

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.typesafe.config.ConfigFactory
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.{CORS, CORSConfig, GZip}
import org.http4s.{Http, Request, Response}

import scala.concurrent.duration._

object Main extends IOApp {

  private val cfg = ConfigFactory.load()
  private val personRepo: PersonRepo = PersonRepo(cfg.getConfig("db"))

  val methodConfig: CORSConfig = CORSConfig(
    anyOrigin = true,
    anyMethod = false,
    allowedMethods = Some(Set("GET", "POST", "PUT", "DELETE")),
    allowCredentials = true,
    maxAge = 1.day.toSeconds)

  val routes: Kleisli[IO, Request[IO], Response[IO]] = Router[IO](
    "/persons" -> PersonRoutes.routes(personRepo)
  ).orNotFound

  val corsRoutes: Http[IO, IO] = GZip(CORS(routes, methodConfig))

  override def run(args: List[String]): IO[ExitCode] = {

    BlazeServerBuilder[IO]
      .bindHttp(cfg.getInt("http.port"), cfg.getString("http.host"))
      .withHttpApp(corsRoutes)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}