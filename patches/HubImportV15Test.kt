package br.com.pokegobr

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class HubImportV15Test {
    @Test fun pokeGenieCsvRecognizedWithPvpRanks() {
        val csv = """Index,Nome,Formato,Pokemon,PC,Atq IV,Def IV,PS IV,Class # (G),Class # (U),Class # (L)
1,Haunter,Normal,93,1499,1,15,14,1,23,
2,Froakie,Evento,656,281,3,15,12,15,,25
"""
        val imported = HubImportV15.parse(csv.toByteArray(), mapOf(93 to "Haunter"))
        assertEquals("pokegenie", imported.key)
        assertEquals(2, imported.count)
        assertEquals(2, imported.recognizedIvs)
        assertEquals(2, imported.ranked)
        assertEquals("1/15/14", imported.records.getJSONObject(0).getString("iv"))
        assertEquals("1", imported.records.getJSONObject(0).getString("glRank"))
        assertEquals("656", imported.records.getJSONObject(1).getInt("speciesId").toString())
        assertEquals("25", imported.records.getJSONObject(1).getString("llRank"))
    }

    @Test fun calcyIvWithDecimalsAndRankRanges() {
        val csv = """Scan date,Nr,Name,CP,ØATT IV,ØDEF IV,ØHP IV,GL Rank (min),GL Rank (max),UL Rank (min),UL Rank (max),LL Rank (min),LL Rank (max)
4/15/26,134,Vaporeon,2492,15.0,15.0,15.0,,,1667,1667,,
4/15/26,92,Gastly,471,1.5,15.0,14.0,1,50,,,,
"""
        val batch = HubImportV15.parse(csv.toByteArray())
        assertEquals("calcy", batch.key)
        assertEquals(2, batch.count)
        assertEquals("15/15/15", batch.records.getJSONObject(0).getString("iv"))
        assertEquals("1667", batch.records.getJSONObject(0).getString("ulRank"))
        assertEquals("—", batch.records.getJSONObject(1).getString("iv"))
        assertEquals("1–50", batch.records.getJSONObject(1).getString("glRank"))
    }

    @Test fun supportsQuotedCommasAndNewlinesInCsv() {
        val csv = "A,B,C\n1,\"nickname, name\",\"first\nsecond\"\n"
        val rows = CsvTableV15.read(csv)
        assertEquals(2, rows.size)
        assertEquals("nickname, name", rows[1][1])
        assertEquals("first\nsecond", rows[1][2])
    }

    @Test fun plainPgsStatsSkipsEggsAndKeepsCorrectIdAndIv() {
        val raw = """{"account":{"name":"PRIVATE_Trainer","trainerCode":"SECRET"},
          "pokemons":[
            {"pokemonId":93,"cp":1499,"individualAttack":1,
             "individualDefense":15,"individualStamina":14,
             "ownerName":"SECRET_OWNER","deployedGymLatDegree":-5.188},
            {"pokemonId":0,"isEgg":true,"cp":0,"individualAttack":0,
             "individualDefense":0,"individualStamina":0},
            {"pokemonId":656,"cp":281,"individualAttack":3,
             "individualDefense":15,"individualStamina":12}
          ]}"""
        val batch = HubImportV15.parse(raw.toByteArray(), mapOf(93 to "Haunter", 656 to "Froakie"))
        assertEquals("pgsstats", batch.key)
        assertEquals(2, batch.count)
        assertEquals("Froakie", batch.records.getJSONObject(1).getString("name"))
        assertEquals("3/15/12", batch.records.getJSONObject(1).getString("iv"))
        val data = batch.records.toString()
        assertFalse(data.contains("PRIVATE_Trainer"))
        assertFalse(data.contains("SECRET_OWNER"))
        assertFalse(data.contains("deployedGymLatDegree"))
        assertFalse(data.contains("trainerCode"))
    }

    @Test fun zipWithSinglePgsStatsIsAccepted() {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use {
            it.putNextEntry(ZipEntry("PGSStats.json"))
            it.write("""{"pokemons":[{"pokemonId":150,"cp":4752,
              "individualAttack":15,"individualDefense":15,"individualStamina":15}]}""".toByteArray())
            it.closeEntry()
        }
        val batch = HubImportV15.parse(buffer.toByteArray(), mapOf(150 to "Mewtwo"))
        assertEquals(1, batch.count)
        assertEquals("Mewtwo", batch.records.getJSONObject(0).getString("name"))
        assertEquals("15/15/15", batch.records.getJSONObject(0).getString("iv"))
    }

    @Test fun unsupportedJavaStreamGivesHelpfulError() {
        val stream = byteArrayOf(0xAC.toByte(), 0xED.toByte(), 0, 5, 1)
        try {
            HubImportV15.parse(stream)
            fail("DAT should not be imported")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("serialização Java"))
        }
    }

    @Test fun rejectsUnrecognizedCsv() {
        try {
            HubImportV15.parse("Unknown,A\n1,2\n".toByteArray())
            fail("Unknown schema must fail")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("CSV"))
        }
    }

    @Test fun rejectZipWithoutPgsStats() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("unrelated.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
        }
        try {
            HubImportV15.parse(out.toByteArray())
            fail("Unknown ZIP must fail")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message!!.contains("PGSStats"))
        }
    }
}
