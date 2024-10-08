package org.shrimp

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

object QuillDemo extends ZIOAppDefault {

  val program = for {
    repo <- ZIO.service[JobRepository]
    _ <- repo.create(Job(-1, "software enginner", "rockjvm.com", "rock jvm"))
    _ <- repo.create(Job(-1, "instructor", "rockjvm.com", "rock jvm"))
  } yield ()

  override def run = program.provide(
    JobRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase), // quill instance
    Quill.DataSource.fromPrefix("mydbconf") // read config section in application and spin up a datasource
  )
}

trait JobRepository {
  def create(job: Job): Task[Job]
  def update(id: Long, op: Job => Job): Task[Job]
  def delete(id: Long): Task[Job]
  def getById(id: Long): Task[Option[Job]]
  def get: Task[List[Job]]
}

class JobRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends JobRepository {
  import quill.*

  inline given schema: SchemaMeta[Job] = schemaMeta[Job]("jobs")
  inline given insMeta: InsertMeta[Job] = insertMeta[Job](_.id)
  inline given upMeta: UpdateMeta[Job] = updateMeta[Job](_.id)

  // lift: lift value from scala to Quill quotation DSL
  override def create(job: Job): Task[Job] = run {
    query[Job]
      .insertValue(lift(job))
      .returning(j => j)
  }

  override def update(id: Long, op: Job => Job): Task[Job] = for {
    current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing key $id"))
    updated <- run {
      query[Job]
        .filter(_.id == lift(id))
        .updateValue(lift(op(current)))
        .returning(j => j)
    }
  } yield updated

  override def delete(id: Long): Task[Job] = run {
    query[Job]
      .filter(_.id == lift(id))
      .delete
      .returning(j => j)
  }

  override def getById(id: Long): Task[Option[Job]] = run {
    query[Job]
      .filter(_.id == lift(id))
  }.map(_.headOption)

  override def get: Task[List[Job]] = run(query[Job])
}

object JobRepositoryLive {
  val layer = ZLayer(
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => JobRepositoryLive(quill))
  )
}
