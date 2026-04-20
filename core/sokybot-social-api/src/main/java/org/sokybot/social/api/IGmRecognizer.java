package org.sokybot.social.api;

import java.util.Set;

public interface IGmRecognizer {
    boolean isGameMaster(String name);
    Set<String> getKnownGmNames();
}
