package org.sokybot.settings.internal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class RawSettingsTest {
    public static void main(String[] args) throws Exception {
        System.out.println("== Starting test ==");
        SettingsRegistryImpl registry = new SettingsRegistryImpl();

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetGateway", "1.2.3.4");
        payload.put("autoLogin", true);
        payload.put("username", "myuser");
        payload.put("password", "supersecret2026");

        registry.writeRawSettings("TestGroup", "DynamicTestBot", "login", payload);

        Path settingsPath = Paths.get("sokybot-data", "settings", "TestGroup", "DynamicTestBot", "login.json");
        if (Files.exists(settingsPath)) {
            System.out.println("SUCCESS: File created at " + settingsPath);
            System.out.println("File contents:");
            System.out.println(new String(Files.readAllBytes(settingsPath)));
        } else {
            System.out.println("ERROR: File not found");
        }
    }
}
