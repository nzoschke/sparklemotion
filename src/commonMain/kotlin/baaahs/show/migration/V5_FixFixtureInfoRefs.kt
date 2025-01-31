package baaahs.show.migration

import baaahs.show.DataMigrator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Suppress("ClassName")
object V5_FixFixtureInfoRefs : DataMigrator.Migration(5) {
    private val dataSourceTypeMap = mapOf(
        "baaahs.Core.FixtureInfo" to "baaahs.Core:FixtureInfo"
    )

    override fun migrate(from: JsonObject): JsonObject {
        return from.toMutableMap().apply {
            mapObjsInDict("dataSources") { _, dataSource ->
                val type = dataSource["type"]?.jsonPrimitive?.contentOrNull
                if (type != null) {
                    dataSourceTypeMap[type]?.let { dataSource["type"] = JsonPrimitive(it) }
                }
            }
        }.toJsonObj()
    }
}