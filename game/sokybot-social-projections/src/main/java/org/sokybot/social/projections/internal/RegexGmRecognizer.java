package org.sokybot.social.projections.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.sokybot.social.api.IGmRecognizer;

@Component(service = IGmRecognizer.class, property = "service.ranking:Integer=0")
public class RegexGmRecognizer implements IGmRecognizer {

    private final List<Pattern> patterns;
    private final ConcurrentHashMap<String, Long> knownGms = new ConcurrentHashMap<>();
    private static final long TTL_MS = 10 * 60 * 1000L;

    public RegexGmRecognizer() {
        String defaultPatterns = "^\\[GM\\].*,^\\[GameMaster\\].*,^GM_.*,.*_GM$";
        String prop = System.getProperty("sokybot.social.gmPatterns", defaultPatterns);
        this.patterns = Arrays.stream(prop.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isGameMaster(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (Pattern p : patterns) {
            if (p.matcher(name).matches()) {
                knownGms.put(name, System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getKnownGmNames() {
        long cutoff = System.currentTimeMillis() - TTL_MS;
        knownGms.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        return Collections.unmodifiableSet(knownGms.keySet());
    }
}
