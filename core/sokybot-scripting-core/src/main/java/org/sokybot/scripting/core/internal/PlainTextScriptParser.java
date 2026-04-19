package org.sokybot.scripting.core.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.osgi.service.component.annotations.Component;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.scripting.api.IScriptParser;
import org.sokybot.scripting.api.ITravelScript;
import org.sokybot.scripting.api.ScriptParseException;
import org.sokybot.scripting.api.TravelCommand;
import org.sokybot.scripting.api.TravelScript;

/**
 * Tokenizer for the Epic #3 plain-text travel DSL.
 */
@Component(service = IScriptParser.class, property = "service.ranking:Integer=0")
public final class PlainTextScriptParser implements IScriptParser {

    @Override
    public ITravelScript parse(String id, Reader source) throws ScriptParseException, IOException {
        List<TravelCommand> commands = new ArrayList<>();
        String description = "";
        boolean descAssigned = false;

        try (BufferedReader br = new BufferedReader(source)) {
            String rawLine;
            int lineNumber = 0;
            while ((rawLine = br.readLine()) != null) {
                lineNumber++;

                String stripped = stripComment(rawLine);
                String trimmed = stripped.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (trimmed.startsWith("#")) {
                    String afterHash = trimmed.substring(1).trim();
                    if (!descAssigned && afterHash.regionMatches(true, 0, "desc:", 0, 5)) {
                        String d = afterHash.substring(5).trim();
                        if (!d.isEmpty()) {
                            description = d;
                            descAssigned = true;
                        }
                    }
                    continue;
                }

                commands.add(parseCommandLine(trimmed, lineNumber));
            }
        }

        return TravelScript.of(id, description, commands);
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        if (hash < 0) {
            return line;
        }
        return line.substring(0, hash);
    }

    private static TravelCommand parseCommandLine(String line, int lineNumber) throws ScriptParseException {
        String lead = firstToken(line);
        if (lead == null) {
            throw new ScriptParseException(lineNumber, "empty command");
        }
        String upper = lead.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "WALK":
                return parseWalk(line, lineNumber);
            case "TELEPORT":
                return parseTeleport(line, lineNumber);
            case "PORTAL":
                return parsePortal(line, lineNumber);
            case "WAIT":
                return parseWait(line, lineNumber);
            case "LOG":
                return parseLog(line, lineNumber);
            default:
                throw new ScriptParseException(lineNumber, "unknown command: " + lead);
        }
    }

    private static String firstToken(String line) {
        String t = line.trim();
        if (t.isEmpty()) {
            return null;
        }
        int sp = -1;
        for (int i = 0; i < t.length(); i++) {
            if (Character.isWhitespace(t.charAt(i))) {
                sp = i;
                break;
            }
        }
        return sp < 0 ? t : t.substring(0, sp);
    }

    private static float[] threeFloatsAfterKeyword(String line, String keyword, int lineNumber)
            throws ScriptParseException {
        String t = line.trim();
        if (!t.regionMatches(true, 0, keyword, 0, keyword.length())) {
            throw new ScriptParseException(lineNumber, "expected " + keyword);
        }
        String rest = t.substring(keyword.length()).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length < 3) {
            throw new ScriptParseException(lineNumber, keyword + " requires three coordinates");
        }
        try {
            float x = Float.parseFloat(parts[0]);
            float y = Float.parseFloat(parts[1]);
            float z = Float.parseFloat(parts[2]);
            return new float[] { x, y, z };
        } catch (NumberFormatException ex) {
            throw new ScriptParseException(lineNumber, "invalid coordinate number", ex);
        }
    }

    private static TravelCommand parseWalk(String line, int lineNumber) throws ScriptParseException {
        float[] xyz = threeFloatsAfterKeyword(line, "WALK", lineNumber);
        return TravelCommand.walk(new WorldPoint(xyz[0], xyz[1], xyz[2]), lineNumber);
    }

    private static TravelCommand parsePortal(String line, int lineNumber) throws ScriptParseException {
        float[] xyz = threeFloatsAfterKeyword(line, "PORTAL", lineNumber);
        return TravelCommand.portal(new WorldPoint(xyz[0], xyz[1], xyz[2]), lineNumber);
    }

    private static TravelCommand parseTeleport(String line, int lineNumber) throws ScriptParseException {
        String t = line.trim();
        if (!t.regionMatches(true, 0, "TELEPORT", 0, "TELEPORT".length())) {
            throw new ScriptParseException(lineNumber, "expected TELEPORT");
        }
        String rest = t.substring("TELEPORT".length()).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length < 2) {
            throw new ScriptParseException(lineNumber, "TELEPORT requires npcRefId and destinationRefId");
        }
        try {
            int npc = Integer.parseInt(parts[0]);
            int dest = Integer.parseInt(parts[1]);
            return TravelCommand.teleport(npc, dest, lineNumber);
        } catch (NumberFormatException ex) {
            throw new ScriptParseException(lineNumber, "invalid TELEPORT integers", ex);
        }
    }

    private static TravelCommand parseWait(String line, int lineNumber) throws ScriptParseException {
        String t = line.trim();
        if (!t.regionMatches(true, 0, "WAIT", 0, "WAIT".length())) {
            throw new ScriptParseException(lineNumber, "expected WAIT");
        }
        String rest = t.substring("WAIT".length()).trim();
        String[] parts = rest.split("\\s+");
        if (parts.length < 1 || parts[0].isEmpty()) {
            throw new ScriptParseException(lineNumber, "WAIT requires millis");
        }
        try {
            long ms = Long.parseLong(parts[0]);
            return TravelCommand.waitMillis(ms, lineNumber);
        } catch (NumberFormatException ex) {
            throw new ScriptParseException(lineNumber, "invalid WAIT millis", ex);
        }
    }

    private static TravelCommand parseLog(String line, int lineNumber) throws ScriptParseException {
        String t = line.trim();
        if (!t.regionMatches(true, 0, "LOG", 0, "LOG".length())) {
            throw new ScriptParseException(lineNumber, "expected LOG");
        }
        String msg = t.substring("LOG".length()).trim();
        return TravelCommand.log(msg, lineNumber);
    }
}
