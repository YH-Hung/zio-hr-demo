package org.shrimp.reviewboard.http

import org.shrimp.reviewboard.http.controllers.{BaseController, CompanyController, HealthController}

object HttpApi {
  
  private def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)
  
  private def makeControllers = for {
    health <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
  } yield List(health, companies)
  
  val endpointsZIO = makeControllers.map(gatherRoutes)
}
