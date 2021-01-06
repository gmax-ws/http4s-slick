import com.typesafe.config.Config
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.http4s.server.middleware.CORSConfig
import slick.jdbc.H2Profile.api._

import java.io.InputStream
import java.security.{KeyStore, Security}
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import scala.jdk.CollectionConverters._
import scala.util.Using

package object gmax {

  private def sslContext(ksStream: InputStream, keystorePassword: String, keyManagerPass: String): SSLContext = {
    val ks = KeyStore.getInstance("JKS")
    ks.load(ksStream, keystorePassword.toCharArray)

    val kmf = KeyManagerFactory.getInstance(
      Option(Security.getProperty("ssl.KeyManagerFactory.algorithm"))
        .getOrElse(KeyManagerFactory.getDefaultAlgorithm))

    kmf.init(ks, keyManagerPass.toCharArray)

    val context = SSLContext.getInstance("TLS")
    context.init(kmf.getKeyManagers, null, null)
    context
  }

  def ssl(cfg: Config): SSLContext =
    Using(this.getClass.getResourceAsStream("/" + cfg.getString("keystore"))) { ksStream =>
      sslContext(ksStream,
        cfg.getString("keystorePassword"),
        cfg.getString("keyManagerPass"))
    }.getOrElse(null)

  def cors(cfg: Config): CORSConfig = CORSConfig(
    anyOrigin = cfg.getBoolean("anyOrigin"),
    anyMethod = cfg.getBoolean("anyMethod"),
    allowedMethods = Option(cfg.getStringList("allowedMethods").asScala.toSet),
    allowCredentials = cfg.getBoolean("allowCredentials"),
    maxAge = cfg.getDuration("maxAge").getSeconds) //1.day.toSeconds)

  def ds(cfg: Config): HikariDataSource = {
    val config = new HikariConfig()
    config.setDriverClassName(cfg.getString("driver"))
    config.setJdbcUrl(cfg.getString("url"))
    config.setUsername(cfg.getString("username"))
    config.setPassword(cfg.getString("password"))
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    new HikariDataSource(config)
  }

  def db(cfg: Config): Database = {
    val maxConnections = cfg.getInt("maxConnections")
    Database.forDataSource(ds(cfg), Option(maxConnections))
  }
}
