package baaahs.plugin

import baaahs.BeatSource
import baaahs.RefCounted
import baaahs.RefCounter
import baaahs.ShowPlayer
import baaahs.app.ui.editor.PortLinkOption
import baaahs.gl.GlContext
import baaahs.gl.data.EngineFeed
import baaahs.gl.data.ProgramFeed
import baaahs.gl.data.SingleUniformFeed
import baaahs.gl.glsl.GlslProgram
import baaahs.gl.glsl.GlslType
import baaahs.gl.patch.ContentType
import baaahs.gl.shader.InputPort
import baaahs.show.DataSource
import baaahs.show.DataSourceBuilder
import baaahs.show.mutable.MutableDataSourcePort
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

class BeatLinkPlugin(internal val beatSource: BeatSource, internal val clock: baaahs.Clock) : Plugin {
    override val packageName: String = id
    override val title: String = "Beat Link"

    override fun resolveDataSource(inputPort: InputPort): DataSource {
        return BeatLinkDataSource()
    }

    override fun suggestContentTypes(inputPort: InputPort): Collection<ContentType> {
        val glslType = inputPort.type
        val isStream = inputPort.glslVar?.isVarying ?: false
        return if (glslType == GlslType.Float && !isStream)
            listOf(beatDataContentType)
        else
            emptyList()
    }

    override fun resolveContentType(type: String): ContentType? {
        return when (type) {
            "beat-link" -> beatDataContentType
            else -> null
        }
    }

    override fun suggestDataSources(
        inputPort: InputPort,
        suggestedContentTypes: Set<ContentType>
    ): List<PortLinkOption> {
        if ((inputPort.contentType == beatDataContentType
                    || suggestedContentTypes.contains(beatDataContentType))
            || (inputPort.type == GlslType.Float && inputPort.glslVar?.isVarying != true)
        ) {
            return listOf(
                PortLinkOption(
                    MutableDataSourcePort(BeatLinkDataSource()),
                    wasPurposeBuilt = true,
                    isExactContentType = inputPort.contentType == beatDataContentType,
                    isPluginSuggestion = true
                )
            )
        } else {
            return emptyList()
        }
    }

    override fun findDataSource(resourceName: String, inputPort: InputPort): DataSource? {
        TODO("Not yet implemented")
    }


    @Serializable
    @SerialName("baaahs.BeatLink:BeatLink")
    data class BeatLinkDataSource(@Transient val `_`: Boolean = true) : DataSource {
        companion object : DataSourceBuilder<BeatLinkDataSource> {
            override val resourceName: String get() = "BeatLink"
            override fun build(inputPort: InputPort): BeatLinkDataSource =
                BeatLinkDataSource()
        }

        override val pluginPackage: String get() = id
        override val title: String get() = "BeatLink"
        override fun getType(): GlslType = GlslType.Float
        override fun getContentType(): ContentType = beatDataContentType

        override fun createFeed(showPlayer: ShowPlayer, plugin: Plugin, id: String): baaahs.gl.data.Feed {
            plugin as BeatLinkPlugin

            return object : baaahs.gl.data.Feed, RefCounted by RefCounter() {
                override fun bind(gl: GlContext): EngineFeed = object : EngineFeed {
                    override fun bind(glslProgram: GlslProgram): ProgramFeed =
                        SingleUniformFeed(glslProgram, this@BeatLinkDataSource, id) { uniform ->
                            uniform.set(plugin.beatSource.getBeatData().fractionTillNextBeat(plugin.clock))
                        }
                }

                override fun release() {
                    super.release()
                }
            }
        }
    }

    class DataFeed(
        private val id: String,
        private val beatSource: BeatSource,
        private val clock: baaahs.Clock
    )

    companion object {
        val id = "baaahs.BeatLink"
        val beatDataContentType = ContentType("Beat Link", GlslType.Float)
    }
}

