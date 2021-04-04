package gmax

import cats.data.Kleisli
import cats.effect._
import cats.tagless.implicits.toFunctorKOps
import cats.tagless.{Derive, FunctorK}
import cats.~>
import com.typesafe.config.ConfigFactory
import gmax.repo.{PersonApi, PersonRepo}
import gmax.routes.{HealthRoutes, PersonRoutes}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.server.middleware.{CORS, CORSConfig, GZip}
import org.http4s.{Http, Request, Response}

import scala.concurrent.Future

object Main extends IOApp {

  import scala.concurrent.ExecutionContext.global

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val functorK: FunctorK[PersonApi] = Derive.functorK[PersonApi]

  private val fk: Future ~> IO = new (Future ~> IO) {
    def apply[A](t: Future[A]): IO[A] = IO.fromFuture(IO.delay(t))
  }

  private val cfg = ConfigFactory.load("application.conf")
  private val personRepo: PersonApi[Future] = PersonRepo(db(cfg.getConfig("db")))(global)
  private val personRepoIO: PersonApi[IO] = personRepo.mapK(fk)

  private val routes: Kleisli[IO, Request[IO], Response[IO]] = Router[IO](
    "/api" -> PersonRoutes.personRoutes(personRepoIO),
    "/.well-known" -> HealthRoutes.healthRoutes
  ).orNotFound

  private val methodConfig: CORSConfig = cors(cfg.getConfig("cors"))
  private val corsRoutes: Http[IO, IO] = GZip(CORS(routes, methodConfig))

  override def run(args: List[String]): IO[ExitCode] = {

    if (cfg.getBoolean("app.ssl")) {
      BlazeServerBuilder[IO](global)
        .bindHttp(cfg.getInt("https.port"), cfg.getString("https.host"))
        .withHttpApp(corsRoutes)
        .withSslContext(ssl(cfg.getConfig("https.ssl")))
        .serve
        .compile
        .drain
        .as(ExitCode.Success)

    } else {
      BlazeServerBuilder[IO](global)
        .bindHttp(cfg.getInt("http.port"), cfg.getString("http.host"))
        .withHttpApp(corsRoutes)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }
}