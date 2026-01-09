package org.sokybot.pk2extractor.datapk2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.region.SectorRefData;
import org.sokybot.pk2extractor.dto.region.SectorRefData.*;

/**
 * Extracts sector navmesh data from .nvm files in Data.pk2.
 * Pure extraction with streaming callbacks.
 */
public class NavMeshExtractor implements IExtractor<SectorRefData> {

    private static final String NAME = "Sector NavMesh Data";
    private static final String FILE_PATTERN = "(^nv_([0-9a-fA-F]{2})([0-9a-fA-F]{2})\\.nvm$)";

    @Override
    public Class<SectorRefData> getDtoClass() {
        return SectorRefData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<SectorRefData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) progressListener.onStart(NAME, -1);
            
            driver.find(FILE_PATTERN).forEach(jmx -> {
                try {
                    ByteBuffer buffer = ByteBuffer.wrap(Pk2ExtractorUtils.toByteArray(jmx));
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    
                    byte[] header = new byte[12];
                    buffer.get(header);
                    
                    String fileName = jmx.getName();
                    String strSectorYX = fileName.substring(3, fileName.lastIndexOf("."));
                    short sectorYX = Short.parseShort(strSectorYX, 16);
                    
                    SectorRefData dto = new SectorRefData();
                    dto.setSectorYX(sectorYX);
                    
                    // Extract nav objects
                    extractNavObjects(buffer, dto.getNavObjects());
                    
                    // Extract nav cells
                    extractNavCells(buffer, dto.getNavCells());
                    
                    // Extract nav borders
                    extractNavBorders(buffer, dto.getNavBorders(), dto.getNavCells(), sectorYX);
                    
                    // Extract nav cell links
                    extractNavCellLinks(buffer, dto.getNavCellLinks(), dto.getNavCells());
                    
                    // Skip grid data
                    buffer.position(buffer.position() + 0x12000);
                    
                    counter[0]++;
                    if (listener != null) listener.onExtracted(dto);
                    if (progressListener != null)
                        progressListener.onProgress(NAME, counter[0], -1, strSectorYX);
                        
                } catch (Exception e) {
                    // Skip malformed files
                }
            });
            
            long duration = System.currentTimeMillis() - startTime;
            if (listener != null) listener.onComplete(counter[0]);
            if (progressListener != null) progressListener.onComplete(NAME, counter[0], duration);
            
        } catch (Exception e) {
            if (listener != null) listener.onError(e);
            if (progressListener != null) progressListener.onError(NAME, e);
        }
    }
    
    private void extractNavObjects(ByteBuffer buffer, List<NavObjectRefData> navObjects) {
        short count = buffer.getShort();
        for (int i = 0; i < count; i++) {
            NavObjectRefData obj = NavObjectRefData.builder()
                .objectId(buffer.getInt())
                .posX(buffer.getFloat() / 10)
                .posZ(buffer.getFloat() / 10)
                .posY(192 - buffer.getFloat() / 10)
                .collisionFlag(buffer.getShort())
                .yaw(buffer.getFloat())
                .uniqueId(buffer.getShort())
                .scale(buffer.getShort())
                .eventZoneFlag(buffer.getShort())
                .regionId(buffer.getShort())
                .build();
            navObjects.add(obj);
            
            short mountCount = buffer.getShort();
            for (int ii = 0; ii < mountCount; ii++) {
                buffer.position(buffer.position() + 6);
            }
        }
    }
    
    private void extractNavCells(ByteBuffer buffer, List<NavCellRefData> navCells) {
        int cellCount = buffer.getInt();
        int cellExtraCount = buffer.getInt();
        
        for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
            float minX = buffer.getFloat() / 10;
            float minY = 192 - buffer.getFloat() / 10;
            float maxX = buffer.getFloat() / 10;
            float maxY = 192 - buffer.getFloat() / 10;
            
            List<Short> navObjectIndices = new ArrayList<>();
            byte entryCount = buffer.get();
            for (int i = 0; i < entryCount; i++) {
                navObjectIndices.add(buffer.getShort());
            }
            
            navCells.add(NavCellRefData.builder()
                .minX(minX).minY(minY)
                .maxX(maxX).maxY(maxY)
                .navObjectIndices(navObjectIndices)
                .navBorderIndices(new ArrayList<>())
                .navCellLinkIndices(new ArrayList<>())
                .build());
        }
    }
    
    private void extractNavBorders(ByteBuffer buffer, List<NavBorderRefData> navBorders, 
                                   List<NavCellRefData> cells, short sectorYX) {
        int regionLinkCount = buffer.getInt();
        for (short linkIndex = 0; linkIndex < regionLinkCount; linkIndex++) {
            float minX = buffer.getFloat() / 10;
            float minY = 192 - buffer.getFloat() / 10;
            float maxX = buffer.getFloat() / 10;
            float maxY = 192 - buffer.getFloat() / 10;
            
            byte lineFlag = buffer.get();
            byte lineSource = buffer.get();
            byte lineDestination = buffer.get();
            short cellSource = buffer.getShort();
            short cellDestination = buffer.getShort();
            short regionSource = buffer.getShort();
            short regionDestination = buffer.getShort();
            
            if (sectorYX == regionSource && cellSource < cells.size()) {
                cells.get(cellSource).getNavBorderIndices().add(linkIndex);
            } else if (cellDestination < cells.size()) {
                cells.get(cellDestination).getNavBorderIndices().add(linkIndex);
            }
            
            navBorders.add(NavBorderRefData.builder()
                .minX(minX).minY(minY)
                .maxX(maxX).maxY(maxY)
                .lineFlag(lineFlag)
                .lineSource(lineSource)
                .lineDestination(lineDestination)
                .cellSourceIndex(cellSource)
                .cellDestinationIndex(cellDestination)
                .regionSourceYX(regionSource)
                .regionDestinationYX(regionDestination)
                .build());
        }
    }
    
    private void extractNavCellLinks(ByteBuffer buffer, List<NavCellLinkRefData> navCellLinks,
                                     List<NavCellRefData> navCells) {
        int cellLinkCount = buffer.getInt();
        for (short linkIndex = 0; linkIndex < cellLinkCount; linkIndex++) {
            float minX = buffer.getInt() / 10f;
            float minY = 192 - buffer.getFloat() / 10;
            float maxX = buffer.getInt() / 10f;
            float maxY = 192 - buffer.getFloat() / 10;
            
            byte lineFlag = buffer.get();
            byte lineSource = buffer.get();
            byte lineDestination = buffer.get();
            short cellSource = buffer.getShort();
            short cellDestination = buffer.getShort();
            
            if (cellSource != (short) 0xFFFF && cellSource < navCells.size()) {
                navCells.get(cellSource).getNavCellLinkIndices().add(linkIndex);
            }
            if (cellDestination != (short) 0xFFFF && cellDestination < navCells.size()) {
                navCells.get(cellDestination).getNavCellLinkIndices().add(linkIndex);
            }
            
            navCellLinks.add(NavCellLinkRefData.builder()
                .minX(minX).minY(minY)
                .maxX(maxX).maxY(maxY)
                .lineFlag(lineFlag)
                .lineSource(lineSource)
                .lineDestination(lineDestination)
                .cellSourceIndex(cellSource)
                .cellDestinationIndex(cellDestination)
                .build());
        }
    }
}
