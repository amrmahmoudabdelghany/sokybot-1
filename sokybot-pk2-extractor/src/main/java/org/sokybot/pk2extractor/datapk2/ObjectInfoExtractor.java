package org.sokybot.pk2extractor.datapk2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2extractor.ExtractionListener;
import org.sokybot.pk2extractor.ExtractionProgressListener;
import org.sokybot.pk2extractor.IExtractor;
import org.sokybot.pk2extractor.Pk2ExtractorUtils;
import org.sokybot.pk2extractor.dto.ObjectNavMeshData;
import org.sokybot.pk2extractor.dto.ObjectNavMeshData.ObjectGroundTriData;
import org.sokybot.pk2extractor.dto.ObjectNavMeshData.ObjectLineRefData;
import org.sokybot.pk2extractor.dto.ObjectNavMeshData.PositionData;

/**
 * Extracts object navmesh data from binary .bsr/.bms files in Data.pk2.
 * Pure extraction with streaming callbacks.
 */
public class ObjectInfoExtractor implements IExtractor<ObjectNavMeshData> {

    private static final String NAME = "Object NavMesh Data";

    @Override
    public Class<ObjectNavMeshData> getDtoClass() {
        return ObjectNavMeshData.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void extract(IPk2Driver driver, 
                        ExtractionListener<ObjectNavMeshData> listener,
                        ExtractionProgressListener progressListener) {
        
        long startTime = System.currentTimeMillis();
        int[] counter = {0};
        
        try {
            if (progressListener != null) progressListener.onStart(NAME, -1);
            
            driver.findFirst("\\navmesh\\object.ifo").ifPresent(jmx -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(jmx.getInputStream()));
                reader.lines()
                    .filter(line -> line.length() > 5 && !line.equals("JMXVOBJI1000"))
                    .forEach(line -> {
                        try {
                            int id = Integer.parseInt(line.substring(0, 5));
                            String bsrPath = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
                            bsrPath = processPath(bsrPath);
                            
                            if (bsrPath != null && !bsrPath.isBlank() && bsrPath.endsWith(".bsr")) {
                                driver.findFirst(bsrPath)
                                    .map(this::extractBmsPath)
                                    .flatMap(driver::findFirst)
                                    .ifPresent(bmsFile -> {
                                        ObjectNavMeshData dto = extractObjectNavmesh(id, bmsFile);
                                        if (dto != null) {
                                            counter[0]++;
                                            if (listener != null) listener.onExtracted(dto);
                                            if (progressListener != null)
                                                progressListener.onProgress(NAME, counter[0], -1, String.valueOf(id));
                                        }
                                    });
                            }
                        } catch (Exception e) {
                            // Skip malformed lines
                        }
                    });
            });
            
            long duration = System.currentTimeMillis() - startTime;
            if (listener != null) listener.onComplete(counter[0]);
            if (progressListener != null) progressListener.onComplete(NAME, counter[0], duration);
            
        } catch (Exception e) {
            if (listener != null) listener.onError(e);
            if (progressListener != null) progressListener.onError(NAME, e);
        }
    }
    
    private String processPath(String path) {
        return "(?i)" + String.join("\\(?i)", path.split("\\\\"));
    }
    
    private ObjectNavMeshData extractObjectNavmesh(int id, JMXFile file) {
        ByteBuffer buffer = ByteBuffer.wrap(Pk2ExtractorUtils.toByteArray(file));
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // BMS file header "JMXVBMS 0110"
        buffer.position(12);
        buffer.getInt(); // vertexOffset
        buffer.getInt(); // skinOffset
        buffer.getInt(); // faceOffset
        buffer.getInt(); // clothVertexOffset
        buffer.getInt(); // clothEdgeOffset
        buffer.getInt(); // boundingBoxOffset
        buffer.getInt(); // occlusionPortals
        
        int navmeshOffset = buffer.getInt();
        buffer.getInt(); // SkinedNavMeshOffset
        buffer.getInt(); // Unknown9Offset
        buffer.getInt(); // unkUInt0
        int navFlag = buffer.getInt();
        
        if (navmeshOffset == 0) return null;
        
        ObjectNavMeshData dto = new ObjectNavMeshData();
        dto.setId(id);
        
        buffer.position(navmeshOffset);
        
        // NavVertices
        int counter = buffer.getInt();
        for (int i = 0; i < counter; i++) {
            float x = buffer.getFloat() / 10;
            float z = buffer.getFloat() / 10;
            float y = buffer.getFloat() / 10;
            dto.getPoints().add(PositionData.builder().x(x).z(z).y(y).build());
            buffer.get(); // unk
        }
        
        // Collision cells (ObjectGround)
        counter = buffer.getInt();
        boolean inCanBlock = false;
        for (int i = 0; i < counter; i++) {
            dto.getObjectGround().add(ObjectGroundTriData.builder()
                .pointA(buffer.getShort())
                .pointB(buffer.getShort())
                .pointC(buffer.getShort())
                .unk(buffer.getShort())
                .build());
            if ((navFlag & 2) != 0) buffer.get();
        }
        
        // NavOutlineEdges
        boolean outCanBlock = false;
        boolean hasEntrance = false;
        counter = buffer.getInt();
        for (int i = 0; i < counter; i++) {
            short pointA = buffer.getShort();
            short pointB = buffer.getShort();
            short neighbourA = buffer.getShort();
            short neighbourB = buffer.getShort();
            byte flag = buffer.get();
            
            dto.getOutLines().add(ObjectLineRefData.builder()
                .pointAIndex(pointA).pointBIndex(pointB)
                .neighbourAIndex(neighbourA & 0xffff)
                .neighbourBIndex(neighbourB & 0xffff)
                .flag(flag).build());
            
            if (flag == 3) outCanBlock = true;
            else if (flag == 0) hasEntrance = true;
            
            if ((navFlag & 1) != 0) buffer.get();
        }
        
        // NavInlineEdges
        counter = buffer.getInt();
        for (int i = 0; i < counter; i++) {
            short pointA = buffer.getShort();
            short pointB = buffer.getShort();
            short neighbourA = buffer.getShort();
            short neighbourB = buffer.getShort();
            byte flag = buffer.get();
            
            dto.getInLines().add(ObjectLineRefData.builder()
                .pointAIndex(pointA).pointBIndex(pointB)
                .neighbourAIndex(neighbourA & 0xffff)
                .neighbourBIndex(neighbourB & 0xffff)
                .flag(flag).build());
            
            if (flag == 7) inCanBlock = true;
            
            if ((navFlag & 1) != 0) buffer.get();
        }
        
        dto.setOutCanBlock(outCanBlock);
        dto.setInCanBlock(inCanBlock);
        dto.setHasEntrance(hasEntrance);
        
        return dto;
    }
    
    private String extractBmsPath(JMXFile bsrFile) {
        ByteBuffer buffer = ByteBuffer.wrap(Pk2ExtractorUtils.toByteArray(bsrFile));
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(12); // file header "JMXVRES 109 "
        
        buffer.getInt(); // bmt
        buffer.getInt(); // bms1
        buffer.getInt(); // unk1-5
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        int bms2 = buffer.getInt();
        buffer.getInt(); // unk6
        
        buffer.position(bms2);
        byte[] b = new byte[buffer.getInt()];
        buffer.get(b);
        String res = new String(b);
        return res.isBlank() ? "" : processPath(res);
    }
}
