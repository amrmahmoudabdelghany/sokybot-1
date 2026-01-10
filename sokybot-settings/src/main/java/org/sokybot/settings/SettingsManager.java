package org.sokybot.settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.osgi.service.component.annotations.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component(service = ISettingsManager.class)
public class SettingsManager implements ISettingsManager {

    private static final String DATA_DIR = "sokybot-data";
    private static final String SETTINGS_DIR = "settings";

    public SettingsManager() {
        // Ensure base directories exist
        try {
            Files.createDirectories(Paths.get(DATA_DIR, SETTINGS_DIR));
        } catch (IOException e) {
            e.printStackTrace(); // Use proper logging in real app
        }
    }

    private File getSettingsFile(String groupName, String machineName, String scope) {
        // Sanitize names to avoid path traversal or invalid chars
        String safeGroup = groupName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String safeMachine = machineName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String safeScope = scope.replaceAll("[^a-zA-Z0-9.-]", "_");

        // Structure: settings/Group/Machine/scope.xml
        Path machinePath = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, safeMachine);
        return machinePath.resolve(safeScope + ".xml").toFile();
    }

    @Override
    public <T> T loadSettings(String groupName, String machineName, String scope, Class<T> type) {
        File file = getSettingsFile(groupName, machineName, scope);

        if (file.exists()) {
            try {
                JAXBContext context = JAXBContext.newInstance(type);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return type.cast(unmarshaller.unmarshal(file));
            } catch (JAXBException e) {
                e.printStackTrace();
                // Fallback to default
            }
        }

        // Return default instance
        try {
            // If the type is Settings.class, we might want to inject ID/Group/Name if possible,
            // but for generic T, we can only call no-arg constructor.
            // Special handling for legacy Settings class to preserve behavior?
            // "Legacy" behavior passed name/group to constructor.
            // But generic design cleaner to use no-args and letting caller set them if needed.
            // However, Settings.java has fields that need initialization.
            T instance = type.getDeclaredConstructor().newInstance();
            
            // Optional: If T is Settings, try to set metadata. 
            // Better to let the caller handle initialization of new objects if specific logic needed.
            return instance;
        } catch (Exception e) {
             throw new RuntimeException("Failed to create default instance of " + type.getName(), e);
        }
    }

    @Override
    public void saveSettings(String groupName, String machineName, String scope, Object settingsObject) {
        if (settingsObject == null) return;
        
        File file = getSettingsFile(groupName, machineName, scope);

        try {
            // Ensure machine directory exists
            file.getParentFile().mkdirs();

            JAXBContext context = JAXBContext.newInstance(settingsObject.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(settingsObject, file);
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save settings", e);
        }
    }

    @Override
    public boolean exists(String groupName, String machineName, String scope) {
        return getSettingsFile(groupName, machineName, scope).exists();
    }

    @Override
    public void delete(String groupName, String machineName, String scope) {
        File file = getSettingsFile(groupName, machineName, scope);
        if (file.exists()) {
            file.delete();
        }
    }
}
