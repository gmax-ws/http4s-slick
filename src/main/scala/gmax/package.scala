import java.io.InputStream
import java.security.{KeyStore, Security}

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.typesafe.config.Config
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import org.http4s.server.middleware.CORSConfig
import slick.jdbc.H2Profile.api._

import scala.jdk.CollectionConverters._

package object gmax {

  private def sslContext(ksStream: InputStream, keystorePassword: String, keyManagerPass: String): SSLContext = {
    val ks = KeyStore.getInstance("JKS")
    ks.load(ksStream, keystorePassword.toCharArray)
    ksStream.close()

    val kmf = KeyManagerFactory.getInstance(
      Option(Security.getProperty("ssl.KeyManagerFactory.algorithm"))
        .getOrElse(KeyManagerFactory.getDefaultAlgorithm))

    kmf.init(ks, keyManagerPass.toCharArray)

    val context = SSLContext.getInstance("TLS")
    context.init(kmf.getKeyManagers, null, null)

    context
  }

  def ssl(cfg: Config): SSLContext = {
    val ksStream = this.getClass.getResourceAsStream("/" + cfg.getString("keystore"))
    sslContext(ksStream,
      cfg.getString("keystorePassword"),
      cfg.getString("keyManagerPass"))
  }

  def cors(cfg: Config): CORSConfig = CORSConfig(
    anyOrigin = cfg.getBoolean("anyOrigin"),
    anyMethod = cfg.getBoolean("anyMethod"),
    allowedMethods = Option(cfg.getStringList("allowedMethods").asScala.toSet),
    allowCredentials = cfg.getBoolean("allowCredentials"),
    maxAge = cfg.getDuration("maxAge").getSeconds) //1.day.toSeconds)

  def ds(cfg: Config): ComboPooledDataSource = {
    val ds = new ComboPooledDataSource
    ds.setDriverClass(cfg.getString("driver"))
    ds.setJdbcUrl(cfg.getString("url"))
    ds.setUser(cfg.getString("username"))
    ds.setPassword(cfg.getString("password"))
    ds
  }

  def db(cfg: Config): Database = {
    val maxConnections = cfg.getInt("maxConnections")
    Database.forDataSource(ds(cfg), Option(maxConnections))
  }
}
