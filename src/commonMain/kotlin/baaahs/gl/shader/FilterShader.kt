package baaahs.gl.shader

import baaahs.app.ui.CommonIcons
import baaahs.gl.glsl.GlslType
import baaahs.gl.patch.ContentType
import baaahs.plugin.objectSerializer
import baaahs.show.ShaderChannel
import baaahs.show.ShaderType
import baaahs.ui.Icon
import kotlinx.serialization.SerialName

@SerialName("baaahs.Core:Filter")
object FilterShader : ShaderPrototype("baaahs.Core:Filter") {
    override val serializerRegistrar = objectSerializer(id, this)

    override val shaderType: ShaderType
        get() = ShaderType.Filter

    override val entryPointName: String get() = "mainFilter"

    override val implicitInputPorts: List<InputPort> = listOf(
        InputPort("gl_FragCoord", GlslType.Vec4, "Coordinates", ContentType.UvCoordinateStream),
        InputPort("gl_FragColor", GlslType.Vec4, "Input Color", ContentType.ColorStream)
    )

    override val wellKnownInputPorts = listOf(
        InputPort("intensity", GlslType.Float, "Intensity", ContentType.Float), // TODO: ContentType.ZeroToOne
        InputPort("time", GlslType.Float, "Time", ContentType.Time),
        InputPort("startTime", GlslType.Float, "Activated Time", ContentType.Time),
        InputPort("endTime", GlslType.Float, "Deactivated Time", ContentType.Time)
//                        varying vec2 surfacePosition; TODO
    )

    override val defaultInputPortsByType: Map<Pair<GlslType, Boolean>, InputPort> = listOf(
            InputPort("uv", GlslType.Vec2, "U/V Coordinates", ContentType.UvCoordinateStream),
            InputPort("color", GlslType.Vec4, "Upstream Color", ContentType.ColorStream)
    )
            .associateBy { it.type to (it.contentType?.isStream ?: false) }

    override val defaultUpstreams: Map<ContentType, ShaderChannel> =
        mapOf(ContentType.ColorStream to ShaderChannel.Main)
    override val title: String = "Filter"
    override val outputPort: OutputPort
        get() = OutputPort(ContentType.ColorStream)
    override val icon: Icon = CommonIcons.FilterShader

    override val template: String = """
        vec4 mainFilter(vec4 inColor) {
            return inColor;
        }
    """.trimIndent()
}