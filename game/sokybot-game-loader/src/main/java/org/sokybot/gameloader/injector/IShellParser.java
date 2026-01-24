package org.sokybot.gameloader.injector;

import java.io.File;
import java.nio.ByteBuffer;

public interface IShellParser {

	ByteBuffer parse(File shellFile) ; 
}
