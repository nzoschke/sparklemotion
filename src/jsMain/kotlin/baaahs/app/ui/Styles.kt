package baaahs.app.ui

import baaahs.app.ui.document.FileUploadStyles
import baaahs.app.ui.editor.ShaderEditorStyles
import baaahs.app.ui.editor.ShaderHelpStyles
import baaahs.app.ui.editor.ThemedEditableStyles
import baaahs.app.ui.editor.layout.LayoutEditorStyles
import baaahs.app.ui.gadgets.color.ColorWheelStyles
import baaahs.app.ui.gadgets.slider.ThemedStyles
import baaahs.app.ui.layout.LayoutStyles
import baaahs.app.ui.model.ModelEditorStyles
import baaahs.mapper.ControllerEditorStyles
import baaahs.mapper.MapperStyles
import baaahs.ui.*
import baaahs.ui.components.UiComponentStyles
import baaahs.ui.diagnostics.DiagnosticsStyles
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.js.Object
import mui.material.styles.Theme
import mui.system.Breakpoint
import styled.StyleSheet
import styled.injectGlobal
import baaahs.app.ui.controls.Styles as ControlsStyles

class AllStyles(val theme: Theme) {
    val themeStyles = ThemeStyles(theme)
    val appUi by lazy { themeStyles }
    val editor by lazy { baaahs.ui.editor.Styles(theme) }
    val controls by lazy { baaahs.app.ui.controls.ThemeStyles(theme) }
    val gadgetsSlider by lazy { ThemedStyles(theme) }
    val editableManager by lazy { ThemedEditableStyles(theme) }
    val layout by lazy { LayoutStyles(theme) }
    val layoutEditor by lazy { LayoutEditorStyles(theme) }
    val controllerEditor by lazy { ControllerEditorStyles(theme) }
    val modelEditor by lazy { ModelEditorStyles(theme) }
    val mapper by lazy { MapperStyles(theme) }
    val shaderEditor by lazy { ShaderEditorStyles(theme) }
    val shaderHelp by lazy { ShaderHelpStyles(theme) }
    val uiComponents by lazy { UiComponentStyles(theme) }
    val fileUploadStyles by lazy { FileUploadStyles(theme) }
    val diagnosticsStyles by lazy { DiagnosticsStyles(theme) }

    fun injectGlobals() {
        injectGlobal(Styles.global)
        injectGlobal(appUi.global)
        injectGlobal(ControlsStyles.global)
        injectGlobal(ColorWheelStyles.global)
        injectGlobal(layout.global)
    }
}

fun linearRepeating(
    color1: Color,
    color2: Color,
    interval: LinearDimension = 10.px,
    angle: Angle = (-45).deg
): String {
    return """
        repeating-linear-gradient(
            $angle,
            $color1,
            $color1 $interval,
            $color2 $interval,
            $color2 ${interval.times(2)}
        );

    """.trimIndent()
}

class ThemeStyles(val theme: Theme) : StyleSheet("app-ui-theme", isStatic = true) {
    private val drawerWidth = 260.px

    val help by css {

    }

    val global = CssBuilder().apply {
        "header" {
            color = Color(theme.palette.primary.contrastText.asDynamic())
            backgroundColor = Color(theme.palette.primary.dark.asDynamic())
            fontSize = 0.875.rem
            fontWeight = FontWeight.w600
            lineHeight = LineHeight("48px")
            paddingLeft = 16.px
            paddingRight = 16.px
            position = Position.relative

            child(this@ThemeStyles, ::help) {
                fontSize = 1.rem
                position = Position.absolute
                top = .5.em
                right = 1.em

                child("a") {
                    color = Color(theme.palette.primary.contrastText.asDynamic())
                }
            }
        }

        ".app-ui-editModeOn .app-ui-theme-appToolbar" {
            backgroundImage = Image(linearRepeating(
                theme.palette.secondary.main.asColor().withAlpha(.35),
                theme.palette.secondary.main.asColor().withAlpha(.15)
            ))
        }

        ".app-ui-editModeOn .app-ui-theme-appContent" {
            backgroundImage = Image(linearRepeating(
                theme.palette.secondary.dark.asColor().withAlpha(.25),
                theme.palette.secondary.dark.asColor().withAlpha(.10)
            ))
        }

        ".app-ui-editModeOff .app-ui-theme-editButton" {
            visibility = Visibility.hidden
            userSelect = UserSelect.none
        }
    }

    private val drawerClosedShift = partial {
        transform { translateX(0.px) }
        transition(
            ::transform,
            timing = Timing(theme.transitions.easing.sharp),
            duration = Time(theme.transitions.duration.enteringScreen.toString())
        )
    }

    private val drawerOpenShift = partial {
        transform { translateX(drawerWidth) }
        transition(
            ::transform,
            timing = Timing(theme.transitions.easing.sharp),
            duration = Time(theme.transitions.duration.leavingScreen.toString())
        )
    }

    val appRoot by css {
        display = Display.grid
        gridTemplateRows = GridTemplateRows("4em minmax(0, 1fr)")

        position = Position.absolute
        width = 100.pct
        height = 100.pct
    }

    val appDrawerOpen by css {}
    val appDrawerClosed by css {}

    val appContent by css {
        display = Display.flex
        flexDirection = FlexDirection.column

        transition(::background, 300.ms)

        within(appDrawerOpen) { mixIn(drawerOpenShift) }
        within(appDrawerClosed) { mixIn(drawerClosedShift) }
    }

    val appToolbar by css {
        mixIn(theme.mixins.toolbar as Object)

        descendants(this@ThemeStyles, ::title) {
            flexGrow = 1.0
            whiteSpace = WhiteSpace.pre
            marginRight = .75.em

            theme.breakpoints.down(Breakpoint.sm)() {
                fontSize = .9.rem
            }
        }

        transition(::background, 300.ms)

        within(appDrawerOpen) { mixIn(drawerOpenShift) }
        within(appDrawerClosed) { mixIn(drawerClosedShift) }
    }

    val appToolbarTabs by css {
        grow(Grow.GROW)
    }

    val appToolbarTab by css {
        flex(1.0, 0.0, FlexBasis.zero)
        flexDirection = FlexDirection.row
    }

    val appToolbarTabSelected by css {
        important(::color, theme.palette.text.primary.asColor())
    }

    val appToolbarActions by css {
        display = Display.flex
    }

    val appToolbarEditModeActions by css {
        display = Display.flex
        flexDirection = FlexDirection.row
    }

    val title by css {
        display = Display.flex
        userSelect = UserSelect.none
    }

    val titleHeader by css {
        position = Position.absolute
        top = 0.em
        fontSize = .7.em
        opacity = .6
    }
    val titleFooter by css {
        position = Position.absolute
        bottom = 5.px
        fontSize = .6.em
        opacity = .6

        child("svg") {
            fontSize = 1.em
        }
    }

    val inactive by css {}

    val problemBadge by css {
        paddingLeft = 1.em
        filter = "drop-shadow(0px 0px 2px white)"
    }

    val editModeButton by css {
        padding = "5px 1em"
        marginLeft = 1.em
        marginRight = 1.em
        color = theme.palette.primary.contrastText.asColor()
        important(::backgroundColor, theme.palette.primary.main.asColor())
        borderColor = theme.palette.primary.contrastText.asColor()

        svg {
            fontSize = 18.px
            marginRight = 0.25.em
        }
    }

    val editModeButtonSelected by css {
        important(::color, theme.palette.error.contrastText.asColor())
        important(::backgroundColor, theme.palette.error.main.asColor())
        important(::borderColor, theme.palette.error.contrastText.asColor())
    }

    val editButton by css {
        paddingLeft = 1.em
    }

    val logotype by css {
        position = Position.absolute
        top = 0.5.em
        right = 0.5.em
        fontSize = 0.6.rem
        userSelect = UserSelect.none
    }

    val appToolbarProblemsIcon by css {
        transform.translateY(1.em)
        filter = "drop-shadow(0px 0px 2px white)"

        ".infoSeverity" { color = Color.darkGray }
        ".warnSeverity" { color = Color.orange }
        ".errorSeverity" { color = Color.red }
    }

    val showProblem by css {
        margin = "auto"

        +"infoSeverity" { color = Color.darkGray }
        +"warnSeverity" { color = Color.orange }
        +"errorSeverity" { color = Color.red }
    }

    val showProblemsDialogContent by css {
        display = Display.grid
        gap = 1.em
        gridTemplateColumns = GridTemplateColumns(GridAutoRows.auto, GridAutoRows.auto)

        h4 { margin = "unset" }
    }

    val appToolbarHelpIcon by css {
        transform.translateY(1.em)
    }

    val appDrawer by css {
        position = Position.absolute
        width = drawerWidth
        height = 100.pct
        flexShrink = 0.0
    }

    val appDrawerPaper by css {
//        position = Position.relative
        put("position", "relative !important")
        width = drawerWidth
    }

    val appDrawerHeader by css {
        display = Display.flex
        alignItems = Align.center
        padding = theme.spacing.asDynamic()(0, 1).toString()
        mixIn(theme.mixins.toolbar)
        justifyContent = JustifyContent.flexEnd
    }

    val appModeTab by css {
        minWidth = 0.px
    }

    val noShowLoadedPaper by css {
        height = 100.pct
        display = Display.flex
        flexDirection = FlexDirection.column
        alignItems = Align.center
        justifyContent = JustifyContent.center

        within(appDrawerOpen) {
            width = 100.pct - drawerWidth
        }
    }

    val showTabs by css {
        background = theme.palette.background.paper
    }
}

object Styles : StyleSheet("app-ui", isStatic = true) {
    val adminRoot by css {
        display = Display.flex

        top = 0.px
        left = 0.px
        bottom = 0.px
        right = 0.px
        position = Position.absolute
        display = Display.flex
        flexDirection = FlexDirection.column
        overflow = Overflow.hidden
    }

    val adminTabPanel by css {
        overflow = Overflow.hidden
        grow(Grow.GROW_SHRINK)
    }

    val showLayout by css {
        display = Display.grid
        height = 100.pct
        gap = 2.px
        padding(2.px)
    }

    val layoutPanelPaper by css {
        display = Display.flex
        flexDirection = FlexDirection.column
        overflow = Overflow.scroll

        header {
            lineHeight = 2.em.lh
        }
    }

    val layoutPanel by css {
        display = Display.flex
        overflow = Overflow.scroll
        flex(1.0)
    }

    val layoutControls by css {
        display = Display.inlineFlex
        grow(Grow.NONE)
        position = Position.relative
        height = 100.pct
        verticalAlign = VerticalAlign.top

        transition(::minWidth, duration = .5.s)
        transition(::minHeight, duration = .5.s)
    }

    val section0Controls by css {
    }

    val section1Controls by css {
    }

    val section2Controls by css {
    }

    val addToSectionButton by css {
        opacity = 0
        transition(StyledElement::visibility, duration = 0.25.s, timing = Timing.linear)
        important(StyledElement::position, Position.absolute)
        right = 0.px
        top = 0.px
        important(StyledElement::padding, 0.px)
        zIndex = 101
    }

    val controlSections = arrayListOf(
        section0Controls,
        section1Controls,
        section2Controls
    )

    val unplacedControlsPalette by css {
        display = Display.flex
        flexDirection = FlexDirection.column
        position = Position.fixed
        left = 3.em
        top = 60.vh
        width = 15.em
        height = 33.vh
        zIndex = 100
        opacity = 0
        transition(::opacity, duration = 1.s)
        pointerEvents = PointerEvents.none

        hover {
            descendants(this@Styles, ::dragHandle) {
                opacity = 1
            }
        }

        descendants(ControlsStyles, ControlsStyles::controlBox) {
            marginBottom = 0.25.em
        }
    }

    val unplacedControlsPaper by css {
        display = Display.flex
        flexDirection = FlexDirection.column
        grow(Grow.GROW)
        padding(1.em)
    }

    val unplacedControlsDroppable by css {
        position = Position.relative
        overflowY = Overflow.scroll
//        minHeight = 4.em
        width = 100.pct
        height = 100.pct
//        width = 15.em
//        height = 33.vh
    }

    val controlPanelHelpText by css {
        display = Display.none
    }

    val dragHandle by css {
        opacity = .2
        transition(::visibility, duration = 0.25.s, timing = Timing.linear)
        position = Position.absolute
        right = 2.px
        top = 0.5.em
        zIndex = 101
    }

    val editModeOn by css {
        descendants(this@Styles, ::layoutControls) {
            paddingLeft = 1.25.em
            minWidth = 3.em
            minHeight = 3.em
            border = "1px solid black"
            borderRadius = 3.px

            transition(duration = 0.5.s)
        }

        descendants(this@Styles, ::layoutPanel) {
            overflow = Overflow.scroll
        }

        descendants(this@Styles, ::section0Controls) {
            background = linearRepeating(Color.lightPink.withAlpha(.5), Color.lightPink.withAlpha(.25))
        }

        descendants(this@Styles, ::section1Controls) {
            background = linearRepeating(Color.lightGreen.withAlpha(.5), Color.lightGreen.withAlpha(.25))
        }

        descendants(this@Styles, ::section2Controls) {
            background = linearRepeating(Color.lightBlue.withAlpha(.5), Color.lightBlue.withAlpha(.25))
        }

        descendants(this@Styles, ::controlPanelHelpText) {
            display = Display.block
            position = Position.absolute
            bottom = 0.5.em
            left = 0.5.px
            declarations["writing-mode"] = "vertical-lr"
        }

        descendants(this@Styles, ::addToSectionButton) {
            opacity = 1
        }

        descendants(ControlsStyles, ControlsStyles::controlBox) {
            padding(3.px)
            border(
                width = 1.px,
                style = BorderStyle.solid,
                color = Color.black.withAlpha(.5),
                borderRadius = 3.px
            )
            margin = ".5em"
        }

        descendants(ControlsStyles, ControlsStyles::editButton) {
            display = Display.inherit
            opacity = .2
        }

        descendants(ControlsStyles, ControlsStyles::dragHandle) {
            display = Display.inherit
            opacity = .2
        }

        descendants(ControlsStyles, ControlsStyles::horizontalButtonList) {
            paddingRight = 1.em
        }

        descendants(ControlsStyles, ControlsStyles::verticalButtonList) {
            paddingBottom = 1.em
        }

        descendants(".app-ui-controls-buttonGroupCard") {
            descendants(ControlsStyles, ControlsStyles::controlButton) {
                transform { scale(.9) }
            }
        }
    }

    val editModeOff by css {
        descendants(this@Styles, ::layoutControls) {
            transition(duration = 0.5.s)
        }

        descendants(this@Styles, ::unplacedControlsPalette) {
            opacity = 0;
            display = Display.none;
        }
    }

    val serverNoticeBackdrop by css {
        important(::position, Position.relative)
        width = 100.pct
        height = 100.pct
        important(::zIndex, 2000)

        child("div") {
            maxWidth = 80.pct
        }

        descendants("code") {
            whiteSpace = WhiteSpace.preWrap
        }
    }

    val serverNoticeAlertMessage by css {
        maxWidth = 40.em
    }

    val serverNoticeMessage by css {
    }

    val serverNoticeStackTrace by css {
        fontSize = .8.em
        height = 10.em
        overflow = Overflow.scroll
    }

    val global = CssBuilder().apply {
        body {
            fontSize = 0.875.rem
            lineHeight = LineHeight("1.43")
        }

        button {
            fontFamily = "inherit"
        }

        ".${editModeOn.name}.${unplacedControlsPalette.name}" {
            opacity = 1
            pointerEvents = PointerEvents.auto
        }
    }
}
