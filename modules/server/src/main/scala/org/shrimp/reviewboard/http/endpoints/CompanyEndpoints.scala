package org.shrimp.reviewboard.http.endpoints

import org.shrimp.reviewboard.domain.data.Company
import org.shrimp.reviewboard.http.requests.CreateCompanyRequest
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody

trait CompanyEndpoints {
  val createEndpoint = endpoint
    .tag("companies")
    .name("create")
    .description("create a listing for a company")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])
  
  val getAllEndpoint = endpoint
    .tag("companies")
    .name("getAll")
    .description("get all company listings")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])
  
  val getByIdEndpoint = endpoint
    .tag("companies")
    .name("getById")
    .description("get company by its id")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])
}
