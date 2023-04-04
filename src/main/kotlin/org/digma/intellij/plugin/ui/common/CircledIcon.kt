package org.digma.intellij.plugin.ui.common

import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.min

class CircledIcon(icon: Icon) : JPanel() {

    init {
        val iconLabel = JLabel(icon)
        iconLabel.isOpaque = false
        iconLabel.horizontalAlignment = SwingConstants.CENTER
        iconLabel.verticalAlignment = SwingConstants.CENTER

        val iconPanel = object : JPanel(){
            var parent: JComponent? = null

            override fun getPreferredSize(): Dimension {
                return Dimension(80,80)
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val graphics = g as Graphics2D
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                graphics.color = parent?.getBackground() ?: super.getBackground()
                val border = super.getBorder()?.getBorderInsets(this)?: JBUI.emptyInsets()
                val radius = min(width-border.left-border.right, height-border.top-border.bottom)
                graphics.fillOval(border.left, border.top, radius, radius)
            }
        }
        iconPanel.parent = this
        iconPanel.layout = BorderLayout()
        iconPanel.isOpaque = false
        iconPanel.alignmentX = Component.CENTER_ALIGNMENT
        iconPanel.alignmentY = Component.CENTER_ALIGNMENT
        iconPanel.add(iconLabel)

        this.add(iconPanel)
        this.isOpaque = false
    }

    override fun setBackground(bg: Color?) {
        super.setBackground(bg)
    }
}