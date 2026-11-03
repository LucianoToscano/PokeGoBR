package br.com.pokegobr

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class ScanHistoryStoreTest {
    private val scanner = JSONArray("""[
      {"historyId":"a0000000-0000-4000-8000-000000000001",
       "name":"Treecko","cp":"152","hp":"43","iv":"0/12/15",
       "confirmed":false,"createdAt":1793653046292},
      {"historyId":"a0000000-0000-4000-8000-000000000002",
       "name":"Dragonite","cp":"4262","hp":"187","iv":"15/15/15",
       "confirmed":true,"createdAt":1793653047000}
    ]""")
    private val imported = JSONArray("""[
      {"name":"Haunter","speciesId":93,"cp":"1499","iv":"1/15/14",
       "glRank":"1","ulRank":"23"},
      {"name":"Gastly","speciesId":92,"cp":"471","iv":"—"}
    ]""")

    private fun rows(): List<SavedScan> =
        ScanHistoryStore.parse(scanner, "scanner", "Scanner") +
        ScanHistoryStore.parse(imported, "pokegenie", "Poke Genie")

    @Test fun legacyAndImportedRecordsStaySeparate() {
        val data = rows()
        assertEquals(4, data.size)
        assertEquals(2, data.count { it.own })
        assertEquals(2, data.count { !it.own })
        assertEquals("0/12/15", data[0].iv!!.joinToString("/"))
        assertEquals(60, data[0].percent)
        assertEquals(1, data[0].stars)
        assertEquals(100, data[1].percent)
        assertEquals(4, data[1].stars)
        assertTrue(data[1].verified)
        assertEquals("1", data[2].gl)
        assertNull(data[3].percent)
    }

    @Test fun filtersBySourceStatusIvAndPvp() {
        val d = rows()
        assertEquals(2, ScanHistoryStore.select(d, "", ScanFilter.OWN,
            ScanSort.DATE, true).size)
        assertEquals(2, ScanHistoryStore.select(d, "", ScanFilter.IMPORTS,
            ScanSort.IV, true).size)
        assertEquals(1, ScanHistoryStore.select(d, "", ScanFilter.VERIFIED,
            ScanSort.IV, true).size)
        assertEquals(1, ScanHistoryStore.select(d, "", ScanFilter.ESTIMATES,
            ScanSort.IV, true).size)
        assertEquals(1, ScanHistoryStore.select(d, "", ScanFilter.PERFECT,
            ScanSort.IV, true).size)
        assertEquals(1, ScanHistoryStore.select(d, "", ScanFilter.PVP,
            ScanSort.GL, false).size)
    }

    @Test fun sortingAndSearchDoNotInventMissingValues() {
        val d = rows()
        assertEquals("Dragonite", ScanHistoryStore.select(d, "", ScanFilter.ALL,
            ScanSort.IV, true).first().name)
        assertEquals("Gastly", ScanHistoryStore.select(d, "", ScanFilter.ALL,
            ScanSort.IV, true).last().name)
        assertEquals("Treecko", ScanHistoryStore.select(d, "152", ScanFilter.ALL,
            ScanSort.DATE, true).first().name)
        assertEquals("Haunter", ScanHistoryStore.select(d, "#93", ScanFilter.ALL,
            ScanSort.DATE, true).first().name)
        assertEquals("Dragonite", ScanHistoryStore.select(d, "15/15/15",
            ScanFilter.ALL, ScanSort.DATE, true).first().name)
    }

    @Test fun csvIncludesSourceButNotAccountOrScreenshotBytes() {
        val csv = ScanHistoryStore.exportCsv(rows())
        assertTrue(csv.contains("Poke Genie"))
        assertTrue(csv.contains("Treecko"))
        assertTrue(csv.contains("0/12/15").not()) // IVs are individual columns
        assertTrue(csv.contains("\"0\",\"12\",\"15\""))
        assertFalse(csv.contains("trainerCode"))
        assertFalse(csv.contains("imagePath"))
    }

    @Test fun hostileCsvNameCannotBecomeSpreadsheetFormula() {
        val bad = JSONArray("""[{"name":"=HYPERLINK(external)","iv":"0/0/0"}]""")
        val rows = ScanHistoryStore.parse(bad, "scanner", "Scanner")
        val csv = ScanHistoryStore.exportCsv(rows)
        assertTrue(csv.contains("'=HYPERLINK"))
    }

    @Test fun invalidIvsMustRemainUnknown() {
        val invalid = JSONArray("""[{"name":"Froakie","iv":"0/16/15"}]""")
        val rows = ScanHistoryStore.parse(invalid, "scanner", "Scanner")
        assertNull(rows[0].iv)
        assertNull(rows[0].percent)
    }
}
