package com.bookingbot.gateway

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.files.TelegramFile
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/** Coordinates of tables on the hall scheme image. */
private val TABLE_COORDINATES: Map<Int, Point> = mapOf(
    1 to Point(80, 80),
    2 to Point(160, 80),
    3 to Point(80, 160),
    4 to Point(160, 160)
)

/**
 * Sends a hall scheme image with highlighted free tables.
 *
 * @param chatId Telegram chat identifier.
 * @param freeTables list of free table IDs to highlight.
 * @param hallSchemePath path to the base hall scheme image.
 */
fun Bot.sendHallScheme(
    chatId: Long,
    freeTables: List<Int>,
    hallSchemePath: String = "resources/hall.png"
) {
    // TODO: adjust TABLE_COORDINATES to match actual hall scheme layout

    val base = ImageIO.read(File(hallSchemePath))
    val image = BufferedImage(base.width, base.height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
        g.drawImage(base, 0, 0, null)
        g.font = Font("Inter", Font.BOLD, 20)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        for (id in freeTables) {
            val p = TABLE_COORDINATES[id] ?: continue
            g.color = Color(0, 200, 0)
            g.fillOval(p.x - 20, p.y - 20, 40, 40)
            g.color = Color.WHITE
            val text = id.toString()
            val m = g.fontMetrics
            val x = p.x - m.stringWidth(text) / 2
            val y = p.y + m.ascent / 2 - 2
            g.drawString(text, x, y)
        }
    } finally {
        g.dispose()
    }

    val tempFile = Files.createTempFile("scheme_", ".png")
    try {
        ImageIO.write(image, "png", tempFile.toFile())
        sendPhoto(ChatId.fromId(chatId), TelegramFile.ByFile(tempFile.toFile()))
    } finally {
        Files.deleteIfExists(tempFile)
    }
}
