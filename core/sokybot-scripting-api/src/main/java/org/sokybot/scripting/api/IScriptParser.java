package org.sokybot.scripting.api;

import java.io.IOException;
import java.io.Reader;

/**
 * Parses a character stream into a structured {@link ITravelScript}.
 */
public interface IScriptParser {

    ITravelScript parse(String id, Reader source) throws ScriptParseException, IOException;
}
