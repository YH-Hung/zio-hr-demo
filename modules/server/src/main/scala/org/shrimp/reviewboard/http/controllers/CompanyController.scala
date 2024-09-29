package org.shrimp.reviewboard.http.controllers

import org.shrimp.reviewboard.domain.data.Company

import collection.mutable
import org.shrimp.reviewboard.http.endpoints.CompanyEndpoints
import org.shrimp.reviewboard.services.CompanyService
import sttp.tapir.server.ServerEndpoint
import zio.*

class CompanyController private (services: CompanyService) extends BaseController with CompanyEndpoints {

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess(services.create)

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess(req => services.getAll)
  
  val getById = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO.attempt(id.toLong)
      .flatMap(services.getById)
      .catchSome {
        case _: NumberFormatException => services.getBySlug(id)
      }
  }
  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO = ZIO.serviceWith[CompanyService] {
    service =>
      new CompanyController(service)
  }
}
