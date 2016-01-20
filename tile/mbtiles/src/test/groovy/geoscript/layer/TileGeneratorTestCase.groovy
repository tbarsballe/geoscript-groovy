package geoscript.layer

import geoscript.geom.Bounds
import geoscript.style.Fill
import geoscript.style.Stroke
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

import static org.junit.Assert.*

/**
 * The TileGenerator Unit Test
 * @author Jared Erickson
 */
class TileGeneratorTestCase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    @Test
    void generateMbTiles() {
        Shapefile shp = new Shapefile(new File(getClass().getClassLoader().getResource("states.shp").toURI()))
        shp.style = new Fill("wheat") + new Stroke("navy", 0.1)
        File file = folder.newFile("states.mbtiles")
        MBTiles mbtiles = new MBTiles(
                file, "states", "A map of the united states"
        )
        ImageTileRenderer renderer = new ImageTileRenderer(mbtiles, shp)
        TileGenerator generator = new TileGenerator()
        generator.generate(mbtiles, renderer, 0, 2)
        assertNotNull mbtiles.get(0, 0, 0).data
        assertNotNull mbtiles.get(1, 1, 1).data
        assertNotNull mbtiles.get(2, 2, 2).data
        mbtiles.close()
    }

    private static class ColorRenderer implements TileRenderer {
        Color color
        ColorRenderer(Color color) {
            this.color = color
        }
        @Override
        byte[] render(Bounds b) {
            BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
            Graphics2D g2d = image.createGraphics()
            g2d.color = color
            g2d.fillRect(0, 0, 256, 256)
            g2d.dispose()
            ByteArrayOutputStream out = new ByteArrayOutputStream()
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }

    @Test
    void generateMissing() {
        File file = folder.newFile("states.mbtiles")
        MBTiles mbtiles = new MBTiles(
                file, "colors", "Color Blocks"
        )
        TileGenerator generator = new TileGenerator()
        // Generate all tiles with solid blue color
        generator.generate(mbtiles, new ColorRenderer(Color.BLUE), 0, 2)
        (0..2).each { int z ->
            mbtiles.tiles(z).each { ImageTile t ->
                assertNotNull t.data
                assertTrue t.base64String.startsWith("iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAYAAABccqhmAAACXElEQVR42u3UMQEAAAzCMPyb")
            }
        }
        // Delete zoom level 0
        mbtiles.delete(mbtiles.tiles(0))
        // Generate tiles but only the missing tiles (zoom level 0)
        generator.generate(mbtiles, new ColorRenderer(Color.RED), 0, 2, missingOnly: true)
        // Zoom level 0 tiles should now be present but RED
        mbtiles.tiles(0).each { ImageTile t ->
            assertNotNull t.data
            assertTrue t.base64String.startsWith("iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAYAAABccqhmAAACXUlEQVR42u3UMQEAAAiAMPqX1")
        }
        // Zoom level 1 and 2 tiles should still be BLUE
        (1..2).each { int z ->
            mbtiles.tiles(z).each { ImageTile t ->
                assertNotNull t.data
                assertTrue t.base64String.startsWith("iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAYAAABccqhmAAACXElEQVR42u3UMQEAAAzCMPyb")
            }
        }
        mbtiles.close()
    }
}