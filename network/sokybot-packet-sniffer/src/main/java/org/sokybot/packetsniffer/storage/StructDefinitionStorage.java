package org.sokybot.packetsniffer.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sokybot.packetsniffer.struct.StructDefinition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class StructDefinitionStorage {
    private final ObjectMapper mapper;
    private final File dataFile;

    public StructDefinitionStorage(String filePath) {
        this.dataFile = new File(filePath);
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<StructDefinition> load() {
        if (!dataFile.exists()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(dataFile, new TypeReference<List<StructDefinition>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void save(List<StructDefinition> definitions) {
        try {
            mapper.writeValue(dataFile, definitions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
