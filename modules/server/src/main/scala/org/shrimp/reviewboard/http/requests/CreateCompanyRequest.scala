package org.shrimp.reviewboard.http.requests

import zio.json.{DeriveJsonCodec, JsonCodec}
import org.shrimp.reviewboard.domain.data.Company

final case class CreateCompanyRequest (name: String, url: String,
                                 location: Option[String], country: Option[String], industry: Option[String],
                                 image: Option[String], tags: Option[List[String]]) {
  def toCompany(id: Long) =
    Company(id, Company.makeSlug(name), name, url, location, country, industry, image, tags.getOrElse(List()))
}

object CreateCompanyRequest {
  given codec: JsonCodec[CreateCompanyRequest] = DeriveJsonCodec.gen[CreateCompanyRequest]
}