package cn.tsinghua.sagemotion.data

internal object CsvCodec {
    fun encodeRow(values: List<String>): String =
        values.joinToString(",") { value -> "\"${value.replace("\"", "\"\"")}\"" }

    fun decodeRow(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        var quoted = false
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        values += current.toString()
        return values
    }
}
