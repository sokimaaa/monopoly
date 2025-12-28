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
    def street(
        id: String,
        name: String,
        position: Int,
        price: Int,
        rents: StreetRentTable,
        houseCost: Int,
        group: ColorGroup
    ): Property =
      Property(
        PropertyId(id),
        name,
        Position(position),
        Money(price),
        rents.base,
        PropertyType.Street,
        colorGroup = Some(group),
        rentTable = Some(rents),
        houseCost = Some(Money(houseCost))
      )

    def railroad(id: String, name: String, position: Int): Property =
      Property(
        PropertyId(id),
        name,
        Position(position),
        Money(200),
        Money(25),
        PropertyType.Railroad
      )

    def utility(id: String, name: String, position: Int): Property =
      Property(
        PropertyId(id),
        name,
        Position(position),
        Money(150),
        Money(15),
        PropertyType.Utility
      )

    Vector(
      street(
        "med_ave",
        "Mediterranean Avenue",
        1,
        60,
        StreetRentTable(Money(2), Money(10), Money(30), Money(90), Money(160), Money(250)),
        50,
        ColorGroup.Brown
      ),
      street(
        "baltic_ave",
        "Baltic Avenue",
        3,
        60,
        StreetRentTable(Money(4), Money(20), Money(60), Money(180), Money(320), Money(450)),
        50,
        ColorGroup.Brown
      ),
      railroad("reading_rr", "Reading Railroad", 5),
      street(
        "oriental_ave",
        "Oriental Avenue",
        6,
        100,
        StreetRentTable(Money(6), Money(30), Money(90), Money(270), Money(400), Money(550)),
        50,
        ColorGroup.LightBlue
      ),
      street(
        "vermont_ave",
        "Vermont Avenue",
        8,
        100,
        StreetRentTable(Money(6), Money(30), Money(90), Money(270), Money(400), Money(550)),
        50,
        ColorGroup.LightBlue
      ),
      street(
        "conn_ave",
        "Connecticut Avenue",
        9,
        120,
        StreetRentTable(Money(8), Money(40), Money(100), Money(300), Money(450), Money(600)),
        50,
        ColorGroup.LightBlue
      ),
      street(
        "stcharles_pl",
        "St. Charles Place",
        11,
        140,
        StreetRentTable(Money(10), Money(50), Money(150), Money(450), Money(625), Money(750)),
        100,
        ColorGroup.Pink
      ),
      utility("electric_co", "Electric Company", 12),
      street(
        "states_ave",
        "States Avenue",
        13,
        140,
        StreetRentTable(Money(10), Money(50), Money(150), Money(450), Money(625), Money(750)),
        100,
        ColorGroup.Pink
      ),
      street(
        "virginia_ave",
        "Virginia Avenue",
        14,
        160,
        StreetRentTable(Money(12), Money(60), Money(180), Money(500), Money(700), Money(900)),
        100,
        ColorGroup.Pink
      ),
      railroad("penn_rr", "Pennsylvania Railroad", 15),
      street(
        "stjames_pl",
        "St. James Place",
        16,
        180,
        StreetRentTable(Money(14), Money(70), Money(200), Money(550), Money(750), Money(950)),
        100,
        ColorGroup.Orange
      ),
      street(
        "tennessee_ave",
        "Tennessee Avenue",
        18,
        180,
        StreetRentTable(Money(14), Money(70), Money(200), Money(550), Money(750), Money(950)),
        100,
        ColorGroup.Orange
      ),
      street(
        "newyork_ave",
        "New York Avenue",
        19,
        200,
        StreetRentTable(Money(16), Money(80), Money(220), Money(600), Money(800), Money(1000)),
        100,
        ColorGroup.Orange
      ),
      street(
        "kentucky_ave",
        "Kentucky Avenue",
        21,
        220,
        StreetRentTable(Money(18), Money(90), Money(250), Money(700), Money(875), Money(1050)),
        150,
        ColorGroup.Red
      ),
      street(
        "indiana_ave",
        "Indiana Avenue",
        23,
        220,
        StreetRentTable(Money(18), Money(90), Money(250), Money(700), Money(875), Money(1050)),
        150,
        ColorGroup.Red
      ),
      street(
        "illinois_ave",
        "Illinois Avenue",
        24,
        240,
        StreetRentTable(Money(20), Money(100), Money(300), Money(750), Money(925), Money(1100)),
        150,
        ColorGroup.Red
      ),
      railroad("bo_rr", "B&O Railroad", 25),
      street(
        "atlantic_ave",
        "Atlantic Avenue",
        26,
        260,
        StreetRentTable(Money(22), Money(110), Money(330), Money(800), Money(975), Money(1150)),
        150,
        ColorGroup.Yellow
      ),
      street(
        "ventnor_ave",
        "Ventnor Avenue",
        27,
        260,
        StreetRentTable(Money(22), Money(110), Money(330), Money(800), Money(975), Money(1150)),
        150,
        ColorGroup.Yellow
      ),
      utility("water_works", "Water Works", 28),
      street(
        "marvin_gdns",
        "Marvin Gardens",
        29,
        280,
        StreetRentTable(Money(24), Money(120), Money(360), Money(850), Money(1025), Money(1200)),
        150,
        ColorGroup.Yellow
      ),
      street(
        "pacific_ave",
        "Pacific Avenue",
        31,
        300,
        StreetRentTable(Money(26), Money(130), Money(390), Money(900), Money(1100), Money(1275)),
        200,
        ColorGroup.Green
      ),
      street(
        "nc_ave",
        "North Carolina Avenue",
        32,
        300,
        StreetRentTable(Money(26), Money(130), Money(390), Money(900), Money(1100), Money(1275)),
        200,
        ColorGroup.Green
      ),
      street(
        "penn_ave",
        "Pennsylvania Avenue",
        34,
        320,
        StreetRentTable(Money(28), Money(150), Money(450), Money(1000), Money(1200), Money(1400)),
        200,
        ColorGroup.Green
      ),
      railroad("shortline_rr", "Short Line", 35),
      street(
        "park_pl",
        "Park Place",
        37,
        350,
        StreetRentTable(Money(35), Money(175), Money(500), Money(1100), Money(1300), Money(1500)),
        200,
        ColorGroup.DarkBlue
      ),
      street(
        "boardwalk",
        "Boardwalk",
        39,
        400,
        StreetRentTable(Money(50), Money(200), Money(600), Money(1400), Money(1700), Money(2000)),
        200,
        ColorGroup.DarkBlue
      )
    )
  }

  def extractProperties(board: Board): Map[PropertyId, Property] =
    board.squares.collect {
      case Square.PropertySquare(prop) => prop.id -> prop
    }.toMap
}
