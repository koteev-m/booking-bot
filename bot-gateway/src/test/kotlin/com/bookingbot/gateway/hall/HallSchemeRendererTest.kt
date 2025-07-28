package com.bookingbot.gateway.hall

import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertEquals

class HallSchemeRendererTest {
    private fun createTemplate(path: String): BufferedImage {
        val img = BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
        ImageIO.write(img, "png", File(path))
        return img
    }

    @Test
    fun `empty list keeps size`() = runTest {
        val file = File.createTempFile("template", ".png")
        val template = createTemplate(file.absolutePath)
        val cfg = ConfigFactory.parseMap(mapOf("bot.hallSchemePath" to file.absolutePath))
        val renderer = HallSchemeRenderer(cfg)
        val bytes = renderer.render(emptyList())
        val out = ImageIO.read(bytes.inputStream())
        assertEquals(template.width, out.width)
        assertEquals(template.height, out.height)
    }

    @Test
    fun `table pixel colored`() = runTest {
        val file = File.createTempFile("template", ".png")
        createTemplate(file.absolutePath)
        val cfg = ConfigFactory.parseMap(mapOf("bot.hallSchemePath" to file.absolutePath))
        val renderer = HallSchemeRenderer(cfg)
        val bytes = renderer.render(listOf(1))
        val out = ImageIO.read(bytes.inputStream())
        val color = Color(out.getRGB(80, 80))
        assertEquals(Color(0, 200, 0).rgb, color.rgb)
    }
}
