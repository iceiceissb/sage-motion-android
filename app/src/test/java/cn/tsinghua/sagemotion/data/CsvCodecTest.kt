package cn.tsinghua.sagemotion.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvCodecTest {
    @Test
    fun roundTripPreservesCommasQuotesAndChineseText() {
        val values = listOf("P001", "路线,识别", "用户说\"换路线\"", "中文详情", "")
        assertEquals(values, CsvCodec.decodeRow(CsvCodec.encodeRow(values)))
    }
}
