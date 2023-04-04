package org.digma.intellij.plugin.ui.common

import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.list.RoundedPanel
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.min

class CircularPanel : JPanel() {

    init {
        isOpaque = false
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(80,80)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val graphics = g as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        graphics.color = super.getBackground()
        val border = super.getBorder()?.getBorderInsets(this)?: JBUI.emptyInsets()
        val radius = min(width-border.left-border.right, height-border.top-border.bottom)
        graphics.fillOval(border.left, border.top, radius, radius)
    }
}