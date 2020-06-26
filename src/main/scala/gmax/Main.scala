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

object Main extends IOApp {

  private val cfg = ConfigFactory.load()
  private val personRepo: PersonRepo = PersonRepo(db(cfg.getConfig("db")))

  private val routes: Kleisli[IO, Request[IO], Response[IO]] = Router[IO](
    "/api" -> PersonRoutes.routes(personRepo)
  ).orNotFound

  private val methodConfig: CORSConfig = cors(cfg.getConfig("cors"))
  private val corsRoutes: Http[IO, IO] = GZip(CORS(routes, methodConfig))

  override def run(args: List[String]): IO[ExitCode] = {
    if (cfg.getBoolean("app.ssl")) {
      BlazeServerBuilder[IO]
        .bindHttp(cfg.getInt("https.port"), cfg.getString("https.host"))
        .withHttpApp(corsRoutes)
        .withSslContext(ssl(cfg.getConfig("https.ssl")))
        .serve
        .compile
        .drain
        .as(ExitCode.Success)

    } else {
      BlazeServerBuilder[IO]
        .bindHttp(cfg.getInt("http.port"), cfg.getString("http.host"))
        .withHttpApp(corsRoutes)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }
}