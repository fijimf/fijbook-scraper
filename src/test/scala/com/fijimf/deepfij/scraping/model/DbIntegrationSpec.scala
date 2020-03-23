package com.fijimf.deepfij.scraping.model

import java.sql.DriverManager
import java.util

import cats.effect.{ContextShift, IO, Resource}
import com.spotify.docker.client.DockerClient.ListContainersParam
import com.spotify.docker.client.messages.{ContainerConfig, ContainerCreation, HostConfig, PortBinding}
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import doobie.hikari.HikariTransactor
import doobie.util.{Colors, ExecutionContexts}
import org.flywaydb.core.Flyway
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

abstract class DbIntegrationSpec extends FunSpec with BeforeAndAfterAll with Matchers with doobie.scalatest.IOChecker {

  override val colors: Colors.Ansi.type = doobie.util.Colors.Ansi // just for docs
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  val containerName: String

  val port: String

  val user = "fijuser"
  val password = "password"
  val database = "deepfijdb"
  val driver = "org.postgresql.Driver"
  lazy val url: String = s"jdbc:postgresql://localhost:$port/$database"

  def dockerClient(): IO[DefaultDockerClient] = IO {
    DefaultDockerClient.fromEnv().build()
  }

  def createDockerContainer(docker: DockerClient): IO[String] = IO {
    docker.pull("postgres:12.2")

    docker
      .listContainers(ListContainersParam.allContainers(true))
      .asScala
      .toList
      .filter(_.names().contains(containerName))
      .foreach(c => {
        docker.killContainer(c.id())
        docker.removeContainer(c.id())
      })

    val hostConfig: HostConfig = {
      val hostPorts = new util.ArrayList[PortBinding]
      hostPorts.add(PortBinding.of("0.0.0.0", port))

      val portBindings = new util.HashMap[String, util.List[PortBinding]]
      portBindings.put("5432/tcp", hostPorts)
      HostConfig.builder.portBindings(portBindings).build
    }

    val containerConfig: ContainerConfig = ContainerConfig
      .builder
      .hostConfig(hostConfig)
      .image("postgres:latest")
      .exposedPorts(s"$port/tcp")
      .env(s"POSTGRES_USER=$user", s"POSTGRES_PASSWORD=$password", s"POSTGRES_DB=$database")
      .build
    val creation: ContainerCreation = docker.createContainer(containerConfig, containerName)
    val id: String = creation.id
    docker.startContainer(id)

    @tailrec
    def readyCheck(): Unit = {
      Try {
        Class.forName(driver)
        DriverManager.getConnection(url, user, password)
      } match {
        case Success(c) => c.close()
        case Failure(_) =>
          Thread.sleep(250)
          readyCheck()
      }
    }

    readyCheck()
    id
  }

  def cleanUpDockerContainer(docker: DockerClient, containerId: String): IO[Unit] = IO {
    docker.killContainer(containerId)
    docker.removeContainer(containerId)
  }

  val txResource: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      te <- ExecutionContexts.cachedThreadPool[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
        driver,
        url,
        user,
        password,
        ce,
        te
      )
    } yield xa

  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  lazy val (transactor, dbClose, containerId) = {
    for {
      d <- dockerClient()
      container <- createDockerContainer(d)
      (xa, close) <- txResource.allocated
    } yield {
      (xa, close, container)
    }
    }.unsafeRunSync()

  def initDB(xa: HikariTransactor[IO]): IO[Int] = {
    xa.configure { dataSource =>
      IO {
        Flyway
          .configure()
          .dataSource(dataSource)
          .locations("classpath:db-scraping/migration")
          .baselineOnMigrate(true)
          .table("flyway_schema_history_scraping")
          .load()
          .migrate()
      }
    }
  }

  override def beforeAll(): Unit = {
    initDB(transactor).unsafeRunSync()
  }

  override def afterAll(): Unit = {
    for {
      _ <- dbClose
      d <- dockerClient()
      _ <- cleanUpDockerContainer(d, containerId)
    } yield {

    }
    }.unsafeRunSync()
}
