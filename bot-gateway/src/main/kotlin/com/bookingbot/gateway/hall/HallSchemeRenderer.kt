package com.bookingbot.gateway.hall

import com.github.benmanes.caffeine.cache.Caffeine
import com.typesafe.config.Config
import java.awt.Color
import java.awt.Font
import java.awt.Point
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Duration
import javax.imageio.ImageIO

/** Renders hall scheme highlighting free tables. */
class HallSchemeRenderer(private val config: Config) {

    private val template: BufferedImage = ImageIO.read(File(config.getString("bot.hallSchemePath")))

    private val coordinates: Map<Int, Point> = mapOf(
        1 to Point(80, 80),
        2 to Point(160, 80),
        3 to Point(80, 160),
        4 to Point(160, 160)
    )

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build<List<Int>, ByteArray>()

    /**
     * Returns PNG image bytes with [freeTables] highlighted.
     */
    fun render(freeTables: List<Int>): ByteArray {
        val key = freeTables.sorted()
        return cache.get(key) {
            val image = BufferedImage(template.width, template.height, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            try {
                g.drawImage(template, 0, 0, null)
                g.font = Font("Inter", Font.BOLD, 20)
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                for (id in freeTables) {
                    val p = coordinates[id] ?: continue
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
            val out = ByteArrayOutputStream()
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }
}
