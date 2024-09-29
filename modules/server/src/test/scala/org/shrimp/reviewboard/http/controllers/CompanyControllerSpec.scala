package org.shrimp.reviewboard.http.controllers

import org.shrimp.reviewboard.domain.data.Company
import org.shrimp.reviewboard.http.requests.CreateCompanyRequest
import org.shrimp.reviewboard.services.CompanyService
import org.shrimp.reviewboard.syntax.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val rtjvm = Company(1, "rock-the-jvm", "Rock the Jvm", "rockthejvm.com")
  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] = ZIO.succeed(rtjvm)

    override def getAll: Task[List[Company]] = ZIO.succeed(List(rtjvm))

    override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(
      if (id == 1) Some(rtjvm) else None
    )

    override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed(
      if (slug == rtjvm.slug) Some(rtjvm) else None
    )
  }

  private def backendStubZIO(endPointFun: CompanyController => ServerEndpoint[Any, Task]) = for {
    controller <- CompanyController.makeZIO
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(endPointFun(controller))
        .backend())
  } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyControllerSpec")(
      test("post company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest.post(uri"/companies")
            .body(CreateCompanyRequest("Rock the Jvm", "rockthejvm.com").toJson)
            .send(backendStub)
        } yield response.body

        assertZIO(program)(
          Assertion.assertion("inspect http response from getAll") { respBdy =>
            respBdy.toOption
              .flatMap(_.fromJson[Company].toOption)
              .contains(Company(1, "rock-the-jvm", "Rock the Jvm", "rockthejvm.com"))
          }
        )
      },

      test("get all") {
        val program = for {
          controller <- CompanyController.makeZIO
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest.get(uri"/companies")
            .send(backendStub)
        } yield response.body

        assertZIO(program)(
          Assertion.assertion("inspect http response from getAll") { respBdy =>
            respBdy.toOption
              .flatMap(_.fromJson[List[Company]].toOption)
              .contains(List(rtjvm))
          }
        )
      },
      test("get by id") {
        val program = for {
          controller <- CompanyController.makeZIO
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest.get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert(
          Assertion.assertion("inspect http response from getAll") { respBdy =>
            respBdy.toOption
              .flatMap(_.fromJson[Company].toOption)
              .contains(rtjvm)
          }
        )
      }
    ).provide(ZLayer.succeed(serviceStub))
}
