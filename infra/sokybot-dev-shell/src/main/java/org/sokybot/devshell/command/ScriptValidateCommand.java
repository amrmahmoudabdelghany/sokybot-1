package org.sokybot.devshell.command;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.sokybot.engine.api.scripting.IScriptEngine;

@Service
@Command(scope = "dev", name = "script-validate", description = "Validate Groovy scripts without executing them")
public class ScriptValidateCommand extends DevCommand {

    @Reference
    private IScriptEngine scriptEngine;

    @Argument(index = 0, name = "path", description = "Script file or directory to validate (defaults to scripts/)", required = false)
    private String targetPath;

    @Override
    public Object execute() throws Exception {
        Path target;
        if (targetPath != null && !targetPath.isEmpty()) {
            target = Paths.get(targetPath);
        } else {
            target = Paths.get("scripts");
        }

        if (!Files.exists(target)) {
            error("Path does not exist: %s", target);
            return null;
        }

        if (Files.isDirectory(target)) {
            validateDirectory(target);
        } else {
            validateFile(target);
        }
        return null;
    }

    private void validateDirectory(Path dir) throws IOException {
        int total = 0, passed = 0, failed = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    validateDirectory(entry);
                } else if (entry.toString().endsWith(".groovy")) {
                    total++;
                    if (validateFile(entry)) {
                        passed++;
                    } else {
                        failed++;
                    }
                }
            }
        }

        if (total > 0) {
            println("\n--- %s: %d scripts, %d passed, %d failed ---", dir, total, passed, failed);
        }
    }

    private boolean validateFile(Path file) {
        try {
            String content = Files.readString(file);
            List<String> errors = scriptEngine.validate(content);
            if (errors.isEmpty()) {
                println("  OK  %s", file.getFileName());
                return true;
            } else {
                println("  FAIL %s", file.getFileName());
                for (String err : errors) {
                    println("       %s", err);
                }
                return false;
            }
        } catch (IOException e) {
            println("  ERR  %s: %s", file.getFileName(), e.getMessage());
            return false;
        }
    }
}
