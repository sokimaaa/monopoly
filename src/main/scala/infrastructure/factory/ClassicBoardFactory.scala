package com.sokima.monopoly
package infrastructure.factory

import domain.*

class ClassicBoardFactory extends application.port.BoardFactory {

  def createBoard(): Board = {
    val properties = createClassicProperties()

    val squares = Vector(
      Square.Go(Position(0)),
      Square.PropertySquare(properties(0)),
      Square.CommunityChest(Position(2)),
      Square.PropertySquare(properties(1)),
      Square.Tax(Position(4), Money(200)),
      Square.PropertySquare(properties(2)),
      Square.PropertySquare(properties(3)),
      Square.Chance(Position(7)),
      Square.PropertySquare(properties(4)),
      Square.PropertySquare(properties(5)),
      Square.Jail(Position(10)),
      Square.PropertySquare(properties(6)),
      Square.PropertySquare(properties(7)),
      Square.PropertySquare(properties(8)),
      Square.PropertySquare(properties(9)),
      Square.PropertySquare(properties(10)),
      Square.PropertySquare(properties(11)),
      Square.CommunityChest(Position(17)),
      Square.PropertySquare(properties(12)),
      Square.PropertySquare(properties(13)),
      Square.FreeParking(Position(20)),
      Square.PropertySquare(properties(14)),
      Square.Chance(Position(22)),
      Square.PropertySquare(properties(15)),
      Square.PropertySquare(properties(16)),
      Square.PropertySquare(properties(17)),
      Square.PropertySquare(properties(18)),
      Square.PropertySquare(properties(19)),
      Square.PropertySquare(properties(20)),
      Square.PropertySquare(properties(21)),
      Square.GoToJail(Position(30)),
      Square.PropertySquare(properties(22)),
      Square.PropertySquare(properties(23)),
      Square.CommunityChest(Position(33)),
      Square.PropertySquare(properties(24)),
      Square.PropertySquare(properties(25)),
      Square.Chance(Position(36)),
      Square.PropertySquare(properties(26)),
      Square.Tax(Position(38), Money(100)),
      Square.PropertySquare(properties(27))
    )

    Board(squares)
  }

  private def createClassicProperties(): Vector[Property] = {
    Vector(
      Property(PropertyId("med_ave"), "Mediterranean Avenue", Position(1), Money(60), Money(2), PropertyType.Street),
      Property(PropertyId("baltic_ave"), "Baltic Avenue", Position(3), Money(60), Money(4), PropertyType.Street),
      Property(PropertyId("reading_rr"), "Reading Railroad", Position(5), Money(200), Money(25), PropertyType.Railroad),
      Property(PropertyId("oriental_ave"), "Oriental Avenue", Position(6), Money(100), Money(6), PropertyType.Street),
      Property(PropertyId("vermont_ave"), "Vermont Avenue", Position(8), Money(100), Money(6), PropertyType.Street),
      Property(PropertyId("conn_ave"), "Connecticut Avenue", Position(9), Money(120), Money(8), PropertyType.Street),
      Property(PropertyId("stcharles_pl"), "St. Charles Place", Position(11), Money(140), Money(10), PropertyType.Street),
      Property(PropertyId("electric_co"), "Electric Company", Position(12), Money(150), Money(15), PropertyType.Utility),
      Property(PropertyId("states_ave"), "States Avenue", Position(13), Money(140), Money(10), PropertyType.Street),
      Property(PropertyId("virginia_ave"), "Virginia Avenue", Position(14), Money(160), Money(12), PropertyType.Street),
      Property(PropertyId("penn_rr"), "Pennsylvania Railroad", Position(15), Money(200), Money(25), PropertyType.Railroad),
      Property(PropertyId("stjames_pl"), "St. James Place", Position(16), Money(180), Money(14), PropertyType.Street),
      Property(PropertyId("tennessee_ave"), "Tennessee Avenue", Position(18), Money(180), Money(14), PropertyType.Street),
      Property(PropertyId("newyork_ave"), "New York Avenue", Position(19), Money(200), Money(16), PropertyType.Street),
      Property(PropertyId("kentucky_ave"), "Kentucky Avenue", Position(21), Money(220), Money(18), PropertyType.Street),
      Property(PropertyId("indiana_ave"), "Indiana Avenue", Position(23), Money(220), Money(18), PropertyType.Street),
      Property(PropertyId("illinois_ave"), "Illinois Avenue", Position(24), Money(240), Money(20), PropertyType.Street),
      Property(PropertyId("bo_rr"), "B&O Railroad", Position(25), Money(200), Money(25), PropertyType.Railroad),
      Property(PropertyId("atlantic_ave"), "Atlantic Avenue", Position(26), Money(260), Money(22), PropertyType.Street),
      Property(PropertyId("ventnor_ave"), "Ventnor Avenue", Position(27), Money(260), Money(22), PropertyType.Street),
      Property(PropertyId("water_works"), "Water Works", Position(28), Money(150), Money(15), PropertyType.Utility),
      Property(PropertyId("marvin_gdns"), "Marvin Gardens", Position(29), Money(280), Money(24), PropertyType.Street),
      Property(PropertyId("pacific_ave"), "Pacific Avenue", Position(31), Money(300), Money(26), PropertyType.Street),
      Property(PropertyId("nc_ave"), "North Carolina Avenue", Position(32), Money(300), Money(26), PropertyType.Street),
      Property(PropertyId("penn_ave"), "Pennsylvania Avenue", Position(34), Money(320), Money(28), PropertyType.Street),
      Property(PropertyId("shortline_rr"), "Short Line", Position(35), Money(200), Money(25), PropertyType.Railroad),
      Property(PropertyId("park_pl"), "Park Place", Position(37), Money(350), Money(35), PropertyType.Street),
      Property(PropertyId("boardwalk"), "Boardwalk", Position(39), Money(400), Money(50), PropertyType.Street)
    )
  }

  def extractProperties(board: Board): Map[PropertyId, Property] = {
    board.squares.collect {
      case Square.PropertySquare(prop) => prop.id -> prop
    }.toMap
  }
}
