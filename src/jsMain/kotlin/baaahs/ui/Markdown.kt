package baaahs.ui

import external.markdownit.MarkdownIt
import kotlinext.js.jsObject
import org.w3c.dom.HTMLElement
import react.Props
import react.RBuilder
import react.RHandler
import react.dom.span

private val MarkdownView = xComponent<MarkdownProps>("Markdown", isPure = true) { props ->
    val mdRef = ref<HTMLElement>()
    val mdHtml = memo(props.children) {
        MarkdownIt(jsObject {
            html = true
            linkify = true
            typographer = true
        }).render(props.children)
    }

    onMount(props.children) {
        mdRef.current!!.innerHTML = mdHtml
    }

    span {
        ref = mdRef
    }
}

external interface MarkdownProps : Props {
    var children : dynamic
}

fun RBuilder.markdown(handler: RHandler<MarkdownProps>) =
    child(MarkdownView, handler = handler)