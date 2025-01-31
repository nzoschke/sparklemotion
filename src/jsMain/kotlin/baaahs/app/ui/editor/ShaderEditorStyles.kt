package baaahs.app.ui.editor

import baaahs.app.ui.ShaderPreviewStyles
import baaahs.ui.child
import baaahs.ui.important
import kotlinx.css.*
import kotlinx.css.properties.deg
import kotlinx.css.properties.rotate
import kotlinx.css.properties.transform
import mui.material.styles.Theme
import styled.StyleSheet

class ShaderEditorStyles(private val theme: Theme) : StyleSheet("app-ui-editor-ShaderEditor", isStatic = true) {
    val propsAndPreview by css {
        display = Display.flex
        gap = 1.em
        marginTop = 1.em
        paddingRight = 1.em
        maxHeight = previewHeight

        child(ShaderPreviewStyles, ShaderPreviewStyles::container) {
            grow(Grow.NONE)
        }
    }

    val propsTabsAndPanels by css {
        grow(Grow.GROW)
        display = Display.flex
        flexDirection = FlexDirection.row
        position = Position.relative
    }

    val tabsContainer by css {
        backgroundColor = Color(theme.palette.primary.dark.asDynamic())
    }

    val propsPanel by css {
        display = Display.flex
        flexDirection = FlexDirection.row
        marginLeft = 1.em
        marginRight = 1.em
        overflow = Overflow.scroll

        child("TABLE") {
            display = Display.block
        }
    }

    val adjustGadgetsSwitch by css {
        position = Position.absolute
        right = 1.em
        top = 0.px
        transform { rotate((-90).deg) }
        declarations["transformOrigin"] = "top right"
    }

    val shaderProperties by css {
        display = Display.grid
        gridTemplateColumns = GridTemplateColumns("50% 50%")
        gridTemplateAreas = GridTemplateAreas("\"name name\" \"priority stream\" \"returnType returnType\"")
        gap = 1.em
        alignContent = Align.start

    }

    val shaderName by css {
        declarations["gridArea"] = "name"
    }

    val shaderPriority by css {
        declarations["gridArea"] = "priority"
    }

    val stream by css {
        declarations["gridArea"] = "stream"
    }

    val shaderReturnType by css {
        declarations["gridArea"] = "returnType"
    }

    val tab by css {
        important(::backgroundColor, theme.palette.background.paper)
        important(::textAlign, TextAlign.right)
    }

    val previewContainer by css {
        position = Position.relative
        width = previewWidth
        height = previewHeight
    }

    val settingsMenuAffordance by css {
        position = Position.absolute
        padding(2.px)
        backgroundColor = Color.white.withAlpha(.5)
        width = 2.em
        height = 2.em
        right = .5.em
        bottom = .5.em
    }

    val showAdvancedMenuItem by css {
        paddingTop = 0.px
        paddingBottom = 0.px
        paddingLeft = 8.px

        svg {
            width = .75.em
            height = .75.em
        }

        span {
            fontSize = .95.em
        }
    }

    companion object {
        val previewWidth = 250.px
        val previewHeight = 250.px
    }
}