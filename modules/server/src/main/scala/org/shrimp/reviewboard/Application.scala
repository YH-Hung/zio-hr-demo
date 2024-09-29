package org.shrimp.reviewboard

import org.shrimp.reviewboard.http.HttpApi
import org.shrimp.reviewboard.services.CompanyService
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    server <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(endpoints)
    )
  } yield ()
  
  override def run = serverProgram.provide(
    Server.default,
    CompanyService.dummyLayer
  )
}
