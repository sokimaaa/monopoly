package com.sokima.monopoly
package domain

sealed trait PropertyType

object PropertyType {

  case object Street extends PropertyType
  
  case object Railroad extends PropertyType

  case object Utility extends PropertyType
}
