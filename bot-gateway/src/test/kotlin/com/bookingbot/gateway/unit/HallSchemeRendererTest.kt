package com.bookingbot.gateway.unit

import com.bookingbot.gateway.hall.HallSchemeRenderer
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HallSchemeRendererTest {
    private fun template(path: String): BufferedImage {
        val img = BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
        ImageIO.write(img, "png", File(path))
        return img
    }

    @Test
    fun `render keeps size`() = runTest {
        val file = File.createTempFile("tpl", ".png")
        val tpl = template(file.absolutePath)
        val cfg = ConfigFactory.parseMap(mapOf("bot.hallSchemePath" to file.absolutePath))
        val renderer = HallSchemeRenderer(cfg)
        val outBytes = renderer.render(emptyList())
        val out = ImageIO.read(outBytes.inputStream())
        assertEquals(tpl.width, out.width)
        assertEquals(tpl.height, out.height)
    }

    @Test
    fun `green pixel when table free`() = runTest {
        val file = File.createTempFile("tpl", ".png")
        template(file.absolutePath)
        val cfg = ConfigFactory.parseMap(mapOf("bot.hallSchemePath" to file.absolutePath))
        val renderer = HallSchemeRenderer(cfg)
        val out = ImageIO.read(renderer.render(listOf(1)).inputStream())
        val color = Color(out.getRGB(80, 80))
        assertTrue(color.green > 150)
    }
}
