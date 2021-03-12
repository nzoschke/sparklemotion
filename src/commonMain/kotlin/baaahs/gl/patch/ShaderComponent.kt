package baaahs.gl.patch

import baaahs.gl.glsl.GlslCode
import baaahs.gl.glsl.GlslExpr
import baaahs.gl.glsl.GlslType
import baaahs.show.live.LinkedShaderInstance

class ShaderComponent(
    val id: String,
    val index: Int,
    private val shaderInstance: LinkedShaderInstance,
    private val findUpstreamComponent: (ProgramNode) -> Component
) : Component {
    override val title: String get() = shaderInstance.shader.title
    private val prefix = "p$index"
    private val namespace = GlslCode.Namespace(prefix + "_" + id)
    private val portMap: Map<String, GlslExpr>
    private val resultInReturnValue: Boolean
    private val resultVar: String

    init {
        val tmpPortMap = hashMapOf<String, GlslExpr>()

        shaderInstance.incomingLinks.forEach { (toPortId, fromLink) ->
            val inputPort = shaderInstance.shader.findInputPort(toPortId)

            val upstreamComponent = findUpstreamComponent(fromLink)
            var expression = upstreamComponent.getExpression(prefix)
            val type = upstreamComponent.resultType
            if (inputPort.type != type) {
                expression = inputPort.contentType.adapt(expression, type)
            }
            tmpPortMap[toPortId] = expression
        }

        var usesReturnValue = false
        val outputPort = shaderInstance.shader.outputPort
        if (outputPort.isReturnValue()) {
            usesReturnValue = true
            resultVar = namespace.internalQualify("result")
        } else {
            resultVar = namespace.qualify(outputPort.id)
            tmpPortMap[outputPort.id] = GlslExpr(resultVar)
        }

        portMap = tmpPortMap
        resultInReturnValue = usesReturnValue
    }

    override val outputVar: String = resultVar
    override val resultType: GlslType
        get() = shaderInstance.shader.outputPort.dataType

    override val invokeFromMain: Boolean
        get() = shaderInstance.injectedPorts.isEmpty()

    private val resolvedPortMap get() =
        portMap + mapOf(shaderInstance.shader.outputPort.id to GlslExpr(outputVar))

    override fun appendStructs(buf: StringBuilder) {
        val openShader = shaderInstance.shader
        val portStructs = openShader.portStructs
        openShader.glslCode.structs.forEach { struct ->
            if (!portStructs.contains(struct.glslType)) {
                buf.append(struct.glslType.toGlsl(namespace, portStructs.map { it.name }.toSet()))
            }
        }
    }

    override fun appendDeclarations(buf: StringBuilder) {
        val openShader = shaderInstance.shader

        buf.append("// Shader: ", openShader.title, "; namespace: ", prefix, "\n")
        buf.append("// ", openShader.title, "\n")

        buf.append("\n")
        with(openShader.outputPort) {
            buf.append("${dataType.glslLiteral} $resultVar = ${contentType.initializer(dataType).s};\n")
        }

        shaderInstance.incomingLinks.forEach { (portId, link) ->
            val inputPort = openShader.findInputPort(portId)
            val destComponent = findUpstreamComponent(link)

            if (shaderInstance.injectedPorts.contains(portId)) {
                val contentType = inputPort.contentType
                val type = inputPort.contentType.glslType
                buf.append("${type.glslLiteral} ${prefix}_global_$portId = " +
                        "${contentType.initializer(type).s};\n"  )
            }

            if (inputPort.isAbstractFunction) {
                val fn = inputPort.glslArgSite as GlslCode.GlslFunction
                buf.append(fn.toGlsl(namespace, emptySet(), emptyMap()))

                buf.append(" {\n")

                destComponent.appendInvokeAndSet(buf, inputPort.injectedData)
                buf.append("    return ", destComponent.outputVar, ";\n")
                buf.append("}\n")
            }
        }

        buf.append(openShader.toGlsl(namespace, resolvedPortMap), "\n")
    }

    override fun appendInvokeAndSet(buf: StringBuilder, injectionParams: Map<String, ContentType>) {
        buf.append("    // Invoke ", title, "\n")

        injectionParams.forEach { (paramName, contentType) ->
            shaderInstance.shader.inputPorts.forEach { inputPort ->
                if (inputPort.contentType == contentType) {
                    buf.append("    ${prefix}_global_${inputPort.id} = $paramName;\n")
                }
            }
        }

        val invocationGlsl = shaderInstance.shader.invoker(namespace, resolvedPortMap).toGlsl(resultVar)
        buf.append("    ", invocationGlsl, ";\n")
        buf.append("\n")
    }

    override fun getExpression(prefix: String): GlslExpr {
        val outputPort = shaderInstance.shader.outputPort
        return if (outputPort.isReturnValue()) {
            resultVar
        } else {
            namespace.qualify(outputPort.id)
        }.let { GlslExpr(it) }
    }

    override fun toString(): String = "ShaderComponent(${prefix}_$id)"

}