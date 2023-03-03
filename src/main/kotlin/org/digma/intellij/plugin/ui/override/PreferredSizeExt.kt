package org.digma.intellij.plugin.ui.override

import org.digma.intellij.plugin.ui.common.CopyableLabelHtml
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.plaf.basic.BasicHTML
import javax.swing.text.View


fun MultiLineHtmlLabel.calculatePreferredSize() {

    if (BasicHTML.isHTMLString(text)){
        val view = getClientProperty(BasicHTML.propertyKey)
        if (view != null){
            val width = getParentWidthNonZero(this)
            if (width > 0) {
                recalculateSize(this, width - 100)
            }
        }
    }
}

fun MultiLIneActionLink.calculatePreferredSize() {

    if (BasicHTML.isHTMLString(text)){
        val view = getClientProperty(BasicHTML.propertyKey)
        if (view != null){
            val width = getParentWidthNonZero(this)
            if (width > 0) {
                recalculateSize(this, width - 100)
            }
        }
    }
}


fun CopyableLabelHtml.calculatePreferredSize() {

    if (BasicHTML.isHTMLString(text)){
        val view = getClientProperty(BasicHTML.propertyKey)
        if (view != null){
            val width = getParentWidthNonZero(this)
            if (width > 0) {
                recalculateSize(this, width - 100)
            }
        }
    }
}






fun recalculateSize(component: JComponent, width: Int) {
//        val v = component.getClientProperty(BasicHTML.propertyKey) as View
//        val preferredSpan = v.getPreferredSpan(View.Y_AXIS)
//        val height = component.height
//        val margin = height - preferredSpan
    val actualSize = getActualSize(component, width)
    val actualHeight = (actualSize.height).toInt()
    val d = Dimension(actualSize.width, actualHeight)
    component.preferredSize = d
    component.size = d
}

private fun getActualSize(component: JComponent, width: Int): Dimension {
    val view = component.getClientProperty(BasicHTML.propertyKey) as View
    view.setSize(width.toFloat(), 0f)
    val w = view.getPreferredSpan(View.X_AXIS)
    val h = view.getPreferredSpan(View.Y_AXIS)
    return Dimension(Math.ceil(w.toDouble()).toInt(), Math.ceil(h.toDouble()).toInt())
}








private fun getParentWidthNonZero(comp: JComponent):Int{

    var c = comp.parent
    while (c != null && c.width <= 0){
        c = c.parent
    }

    if (c != null && c.width > 0){
        var w = c.width
        if (c.insets != null){
            w = w - c.insets.left - c.insets.right
        }
        if (c is JScrollPane){
            w -= c.verticalScrollBar.width
        }
        return w
    }

    return comp.width
}