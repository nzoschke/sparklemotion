package baaahs.show.mutable

import baaahs.show.Surfaces

class VisitationLog {
    val patchHolders = mutableSetOf<MutablePatchHolder>()
    val patches = mutableSetOf<MutablePatch>()
    val surfaces = mutableSetOf<Surfaces>()
    val shaderInstances = mutableSetOf<MutableShaderInstance>()
    val shaders = mutableSetOf<MutableShader>()
    val shaderChannels = mutableSetOf<MutableShaderChannel>()
    val controls = mutableSetOf<MutableControl>()
    val dataSources = mutableSetOf<MutableDataSourcePort>()
}