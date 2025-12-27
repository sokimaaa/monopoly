package com.sokima.monopoly
package infrastructure.adapter.persistence

import domain.{Position, Property, PropertyId}
import application.port.PropertyRepository

import scala.collection.mutable

class InMemoryPropertyRepository(initialProperties: Map[PropertyId, Property])
    extends PropertyRepository {

  private val storage = mutable.Map[PropertyId, Property](initialProperties.toSeq: _*)

  def findById(propertyId: PropertyId): Option[Property] = storage.get(propertyId)

  def findByPosition(position: Position): Option[Property] =
    storage.values.find(_.position == position)

  def update(property: Property): Either[String, Property] = {
    storage.update(property.id, property)
    Right(property)
  }

  def findAll(): List[Property] = storage.values.toList
}
