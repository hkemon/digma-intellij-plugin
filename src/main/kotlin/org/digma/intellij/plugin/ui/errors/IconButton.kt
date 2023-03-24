package org.digma.intellij.plugin.ui.errors

import org.digma.intellij.plugin.ui.common.Laf
import java.awt.Cursor
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JButton

internal class IconButton : JButton {

    private val defaultIcon: Icon
    private val selectedIcon: Icon?

    constructor(defaultIcon: Icon, selectedIcon: Icon? = null) : super(defaultIcon) {
        this.defaultIcon = defaultIcon
        this.selectedIcon = selectedIcon

        initButton()

        addActionListener {
            isSelected = !isSelected
        }
    }

    constructor(defaultIcon: Icon, selectedIcon: Icon?, action: () -> Unit) : super(defaultIcon) {
        this.defaultIcon = defaultIcon
        this.selectedIcon = selectedIcon

        initButton()

        addActionListener {
            isSelected = !isSelected
            action()
        }
    }

    private fun initButton() {
        isOpaque = false
        isContentAreaFilled = false
        isBorderPainted = false
        background = Laf.Colors.TRANSPARENT
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        val hoverBackground = Laf.Colors.LIST_ITEM_BACKGROUND

        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                background = hoverBackground
            }

            override fun mouseExited(e: MouseEvent?) {
                background = if (isSelected) {
                    hoverBackground
                } else {
                    Laf.Colors.TRANSPARENT
                }
            }

            override fun mousePressed(e: MouseEvent?) {
                if (isSelected && selectedIcon != null) {
                    icon = selectedIcon
                }
                background = Laf.Colors.TRANSPARENT
            }

            override fun mouseReleased(e: MouseEvent?) {
                background = if (isSelected) {
                    hoverBackground
                } else {
                    Laf.Colors.TRANSPARENT
                }
            }
        })
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)

        if (selectedIcon != null) {
            icon = if (isSelected) {
                selectedIcon
            } else {
                defaultIcon
            }
        }
    }

    override fun paintComponent(g: Graphics) {
        g.color = background
        g.fillRect(0, 0, width, height)
        super.paintComponent(g)
    }
}
