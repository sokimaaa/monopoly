package com.sokima.monopoly
package application.port

import domain.{Position, Property, PropertyId}

trait PropertyRepository {
  def findById(propertyId: PropertyId): Option[Property]

  def findByPosition(position: Position): Option[Property]

  def update(property: Property): Either[String, Property]

  def findAll(): List[Property]
}
