package org.digma.intellij.plugin.ui.common

import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.min

class CircledIcon(icon: Icon, margin: Int = 15) : JPanel() {

    private var iconPanel: JPanel = object : JPanel()
    {
        init {
            layout = BorderLayout()
            isOpaque = false
            alignmentX = Component.CENTER_ALIGNMENT
            alignmentY = Component.CENTER_ALIGNMENT
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val graphics = g as Graphics2D
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            graphics.color = parent.background
            val border = super.getBorder()?.getBorderInsets(this)?: JBUI.emptyInsets()
            val radius = min(width-border.left-border.right, height-border.top-border.bottom)
            graphics.fillOval(border.left, border.top, radius, radius)
        }
    }

    init {
        val iconLabel = JLabel(icon)
        iconLabel.isOpaque = false
        iconLabel.horizontalAlignment = SwingConstants.CENTER
        iconLabel.verticalAlignment = SwingConstants.CENTER
        iconLabel.border = JBUI.Borders.empty(margin)

        iconPanel.add(iconLabel)

        this.add(iconPanel)
        this.isOpaque = false
    }

}