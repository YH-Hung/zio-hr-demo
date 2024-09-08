package org.shrimp.reviewboard.http.controllers

import org.shrimp.reviewboard.domain.data.Company

import collection.mutable
import org.shrimp.reviewboard.http.endpoints.CompanyEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.*

class CompanyController private extends BaseController with CompanyEndpoints {
  val db = mutable.Map[Long, Company](-1L -> Company(-1, "invalid", "no company", "nothing.com"))

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    ZIO.succeed {
      val newId = db.keys.max + 1
      val newCompany = req.toCompany(newId)
      db += (newId -> newCompany)
      newCompany
    }
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess(_ => ZIO.succeed(db.values.toList))
  
  val getById = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO.attempt(id.toLong)
      .map(db.get)
  }
  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO = ZIO.succeed(new CompanyController)
}
