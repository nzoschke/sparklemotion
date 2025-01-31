package baaahs.plugin.core.datasource

import baaahs.ShowPlayer
import baaahs.gl.GlContext
import baaahs.gl.data.EngineFeed
import baaahs.gl.data.Feed
import baaahs.gl.data.ProgramFeed
import baaahs.gl.glsl.GlslProgram
import baaahs.gl.glsl.GlslType
import baaahs.gl.patch.ContentType
import baaahs.gl.shader.InputPort
import baaahs.plugin.classSerializer
import baaahs.plugin.core.CorePlugin
import baaahs.show.DataSource
import baaahs.show.DataSourceBuilder
import baaahs.util.RefCounted
import baaahs.util.RefCounter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Deprecated("Obsolete, going away soon.")
@Serializable
@SerialName("baaahs.Core:PixelCoordsTexture")
data class PixelCoordsTextureDataSource(@Transient val `_`: Boolean = true) : DataSource {
    companion object : DataSourceBuilder<PixelCoordsTextureDataSource> {
        override val title: String get() = "Pixel Coordinates"
        override val description: String get() = "Internal use only."
        override val resourceName: String get() = "PixelCoords"
        override val contentType: ContentType get() = ContentType.PixelCoordinatesTexture
        override val serializerRegistrar get() = classSerializer(serializer())
        override val internalOnly: Boolean = true

        override fun looksValid(inputPort: InputPort, suggestedContentTypes: Set<ContentType>): Boolean = false
        override fun build(inputPort: InputPort): PixelCoordsTextureDataSource =
            PixelCoordsTextureDataSource()
    }

    override val pluginPackage: String get() = CorePlugin.id
    override val title: String get() = "Pixel Coordinates Texture"
    override fun getType(): GlslType = GlslType.Sampler2D
    override val contentType: ContentType
        get() = ContentType.PixelCoordinatesTexture
    override fun suggestId(): String = "pixelCoordsTexture"

    override fun createFeed(showPlayer: ShowPlayer, id: String): Feed =
        object : Feed, RefCounted by RefCounter() {
            override fun bind(gl: GlContext): EngineFeed = object : EngineFeed {
                override fun bind(glslProgram: GlslProgram): ProgramFeed = object : ProgramFeed {
                    override val isValid: Boolean get() = false
                }
            }
        }
}