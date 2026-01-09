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
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.sokybot.game.asset.IMediaAssetProvider;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;
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

    @Reference
    private IGamePersistenceFactory persistenceFactory;

    // Configuration method (OSGi DS)
    @Activate
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

        IGameDataLookup lookup = persistenceFactory.getLookup(this.gamePath);
        if (lookup != null) {
             Optional<NPCEntity> npc = lookup.findNPC(charId);
             if (npc.isPresent()) {
                 String path = npc.get().getIconPath();
                 // TODO: Use path to extract image from PK2. 
                 // For now, returning empty as per instructions.
                 return Optional.empty();
             }
        }
        
        return Optional.empty(); 
    }

    @Override
    public Optional<Image> findSkillIcon(int skillId) {
        // Placeholder for future implementation using persistence
         IGameDataLookup lookup = persistenceFactory.getLookup(this.gamePath);
         if (lookup != null) {
             // Optional<SkillEntity> skill = lookup.findSkill(skillId);
             // ...
         }
        return Optional.empty();
    }

    @Override
    public Optional<Image> findItemIcon(int itemId) {
         IGameDataLookup lookup = persistenceFactory.getLookup(this.gamePath);
         if (lookup != null) {
             Optional<ItemEntity> item = lookup.findItem(itemId);
             if (item.isPresent()) {
                 String path = item.get().getIconPath();
                 // TODO: extract
                 return Optional.empty();
             }
         }
        return Optional.empty();
    }

    private Image extractMiniMap(short sectorX, short sectorY) {
        if (gamePath == null) return null;
        
        // Assuming gamePath points to the root folder containing Media.pk2
        try (IPk2Driver driver = IPk2Driver.open(this.gamePath + "/Media.pk2")) {
            // ... existing logic ...
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
