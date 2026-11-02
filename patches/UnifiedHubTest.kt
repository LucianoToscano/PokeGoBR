package br.com.pokegobr

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class UnifiedHubTest {
    @Test fun rawCoordinatesValid() {
        val point = GeoCoordinateParser.parse("-5.188, -37.344")
        assertNotNull(point)
        assertEquals(-5.188, point!!.latitude, 0.000001)
        assertEquals(-37.344, point.longitude, 0.000001)
    }
    @Test fun supportsGeoUriAndMapUrls() {
        assertEquals(37.421, GeoCoordinateParser.parse("geo:37.421,-122.084")!!.latitude, .0001)
        assertEquals(-37.344, GeoCoordinateParser.parse(
            "https://www.google.com/maps?q=-5.188%2C-37.344")!!.longitude, .0001)
        assertNotNull(GeoCoordinateParser.parse(
            "https://maps.google.com/maps/@-5.188,-37.344,14z"))
    }
    @Test fun rejectsInvalidOrNonCoordinateData() {
        assertNull(GeoCoordinateParser.parse("91,0"))
        assertNull(GeoCoordinateParser.parse("-5.0,181"))
        assertNull(GeoCoordinateParser.parse("PC 1499 · PS 150"))
        assertNull(GeoCoordinateParser.parse("https://maps.app.goo.gl/randomShortLink"))
    }
    @Test fun importsOnlyAllowedFields() {
        val json = """{"account":{"email":"private@example.org"},"pokemons":[
          {"name":"Haunter","cp":1499,"individualAttack":1,
           "individualDefense":15,"individualStamina":14,"trainerCode":"1234 5678 9012"},
          {"name":"Mewtwo","cp":4752,"individualAttack":15,
           "individualDefense":15,"individualStamina":15,"latitude":-5.0}]}"""
        val minimized = HubCollectionImporter.parseAndMinimize(json)
        assertEquals(2, minimized.length())
        assertEquals("1/15/14", minimized.getJSONObject(0).getString("iv"))
        assertEquals("15/15/15", minimized.getJSONObject(1).getString("iv"))
        val output = minimized.toString()
        assertFalse(output.contains("private@example.org"))
        assertFalse(output.contains("trainerCode"))
        assertFalse(output.contains("latitude"))
    }
    @Test fun incompleteIvNeverBecomesPerfect() {
        val json = """{"pokemons":[{"name":"Gengar","cp":1513,
          "individualAttack":15,"individualDefense":15}]}"""
        val data = HubCollectionImporter.parseAndMinimize(json)
        assertEquals("—", data.getJSONObject(0).getString("iv"))
    }
    @Test fun cannotImportIncorrectFormatOrEmptyNames() {
        try {
            HubCollectionImporter.parseAndMinimize("""{"items": []}""")
            fail("Expected invalid schema")
        } catch (_: IllegalArgumentException) { }
        try {
            HubCollectionImporter.parseAndMinimize("""{"pokemons":[{}]}""")
            fail("Expected unsupported records")
        } catch (_: IllegalArgumentException) { }
    }
    @Test fun taxonomyParsesSpeciesIndex() {
        val source = """{"results":[
           {"name":"bulbasaur","url":"https://pokeapi.co/api/v2/pokemon-species/1/"},
           {"name":"mewtwo","url":"https://pokeapi.co/api/v2/pokemon-species/150/"}
        ]}"""
        val species = HubData.parseSpecies(source)
        assertEquals(2, species.size)
        assertEquals(150, species[1].number)
        assertEquals("mewtwo", species[1].name)
    }
    @Test fun rejectsEmptySpeciesCatalog() {
        try {
            HubData.parseSpecies("""{"results":[]}""")
            fail("Expected empty-index protection")
        } catch (_: IllegalStateException) { }
    }
    @Test fun pokemonSearchIgnoresAccents() {
        assertEquals("pokemon", normalizeDexQuery("POKÉMON"))
    }
}
