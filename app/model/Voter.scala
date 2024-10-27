package model

import play.api.libs.json.{Json, OFormat}

case class Address (
  street: String,
  city: String,
  state: String,
  country: String,
  postcode: String
)

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}

case class Voter(
  voterId: String,
  voterName: String,
  dateOfBirth: String,
  gender: String,
  nationality: String,
  registrationNumber: String,
  address: Address,
  email: String,
  phoneNumber: String,
  cellNumber: String,
  picture: String,
  registeredAge: Int
)

object Voter {
  implicit val format: OFormat[Voter] = {
    implicit val addressFormat = Address.format
    Json.format[Voter]
  }
}
