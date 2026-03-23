package org.sokybot.packetsniffer.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lightweight packet query parser/evaluator.
 *
 * Supported:
 * - field:value clauses (opcode, source, name, size, encoding, payload_contains)
 * - operators: AND, OR, NOT
 * - parentheses
 */
public final class PacketFilter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private PacketFilter() {
    }

    @FunctionalInterface
    public interface Predicate {
        boolean test(Map<String, Object> row);
    }

    public static Predicate parse(String query) {
        if (query == null || query.trim().isEmpty()) {
            return row -> true;
        }
        Parser parser = new Parser(tokenize(query));
        return parser.parseExpression();
    }

    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }
            if (!inQuotes && (c == '(' || c == ')')) {
                flush(current, tokens);
                tokens.add(String.valueOf(c));
                continue;
            }
            if (!inQuotes && Character.isWhitespace(c)) {
                flush(current, tokens);
                continue;
            }
            current.append(c);
        }
        flush(current, tokens);
        return tokens;
    }

    private static void flush(StringBuilder current, List<String> tokens) {
        if (current.length() > 0) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }

    private static final class Parser {
        private final List<String> tokens;
        private int pos = 0;

        private Parser(List<String> tokens) {
            this.tokens = tokens;
        }

        private Predicate parseExpression() {
            Predicate left = parseTerm();
            while (match("OR")) {
                Predicate right = parseTerm();
                left = or(left, right);
            }
            return left;
        }

        private Predicate parseTerm() {
            Predicate left = parseFactor();
            while (match("AND")) {
                Predicate right = parseFactor();
                left = and(left, right);
            }
            return left;
        }

        private Predicate parseFactor() {
            if (match("NOT")) {
                return not(parseFactor());
            }
            if (match("(")) {
                Predicate nested = parseExpression();
                expect(")");
                return nested;
            }
            String token = next();
            if (token == null) {
                return row -> true;
            }
            if (token.startsWith("-")) {
                return not(parseClause(token.substring(1)));
            }
            return parseClause(token);
        }

        private Predicate parseClause(String token) {
            int idx = token.indexOf(':');
            if (idx <= 0) {
                String needle = token.toLowerCase(Locale.ROOT);
                return row -> stringify(row.get("name")).toLowerCase(Locale.ROOT).contains(needle);
            }
            String field = token.substring(0, idx).toLowerCase(Locale.ROOT);
            String rawValue = stripQuotes(token.substring(idx + 1));
            switch (field) {
                case "opcode":
                    return opcodePredicate(rawValue);
                case "source":
                    return row -> stringify(row.get("source")).equalsIgnoreCase(rawValue);
                case "name":
                    return row -> stringify(row.get("name")).toLowerCase(Locale.ROOT)
                            .contains(rawValue.toLowerCase(Locale.ROOT));
                case "size":
                    return sizePredicate(rawValue);
                case "encoding":
                    return row -> stringify(row.get("encoding")).equalsIgnoreCase(rawValue);
                case "payload_contains":
                    String normalized = rawValue.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
                    return row -> stringify(row.get("payload")).toUpperCase(Locale.ROOT).contains(normalized);
                case "payload_regex":
                    Pattern pattern = Pattern.compile(rawValue);
                    return row -> pattern.matcher(stringify(row.get("payload"))).find();
                case "time":
                    return timePredicate(rawValue);
                default:
                    return row -> true;
            }
        }

        private Predicate timePredicate(String value) {
            String v = value.trim();
            String operator = "=";
            if (v.startsWith(">=") || v.startsWith("<=")) {
                operator = v.substring(0, 2);
                v = v.substring(2).trim();
            } else if (v.startsWith(">") || v.startsWith("<") || v.startsWith("=")) {
                operator = v.substring(0, 1);
                v = v.substring(1).trim();
            }
            final LocalTime expected = parseTime(v);
            final String op = operator;
            return row -> {
                String rowTimeRaw = stringify(row.get("time")).trim();
                if (rowTimeRaw.isEmpty()) {
                    return false;
                }
                String hhmmss = rowTimeRaw.length() >= 8 ? rowTimeRaw.substring(0, 8) : rowTimeRaw;
                LocalTime actual = parseTime(hhmmss);
                int cmp = actual.compareTo(expected);
                switch (op) {
                    case ">":
                        return cmp > 0;
                    case "<":
                        return cmp < 0;
                    case ">=":
                        return cmp >= 0;
                    case "<=":
                        return cmp <= 0;
                    default:
                        return cmp == 0;
                }
            };
        }

        private LocalTime parseTime(String value) {
            try {
                return LocalTime.parse(value, TIME_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid time filter value '" + value + "', expected HH:mm:ss");
            }
        }

        private Predicate opcodePredicate(String value) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.endsWith("*")) {
                String prefix = normalized.substring(0, normalized.length() - 1).replace("0x", "");
                return row -> {
                    String opcode = stringify(row.get("opcode")).toLowerCase(Locale.ROOT).replace("0x", "");
                    return opcode.startsWith(prefix);
                };
            }
            int expected = parseHexOrDec(normalized);
            return row -> parseHexOrDec(stringify(row.get("opcode"))) == expected;
        }

        private Predicate sizePredicate(String value) {
            String v = value.trim();
            if (v.startsWith(">=")) {
                int n = parseHexOrDec(v.substring(2));
                return row -> parseHexOrDec(stringify(row.get("size"))) >= n;
            }
            if (v.startsWith("<=")) {
                int n = parseHexOrDec(v.substring(2));
                return row -> parseHexOrDec(stringify(row.get("size"))) <= n;
            }
            if (v.startsWith(">")) {
                int n = parseHexOrDec(v.substring(1));
                return row -> parseHexOrDec(stringify(row.get("size"))) > n;
            }
            if (v.startsWith("<")) {
                int n = parseHexOrDec(v.substring(1));
                return row -> parseHexOrDec(stringify(row.get("size"))) < n;
            }
            if (v.startsWith("=")) {
                int n = parseHexOrDec(v.substring(1));
                return row -> parseHexOrDec(stringify(row.get("size"))) == n;
            }
            int n = parseHexOrDec(v);
            return row -> parseHexOrDec(stringify(row.get("size"))) == n;
        }

        private boolean match(String token) {
            if (pos >= tokens.size()) {
                return false;
            }
            if (!tokens.get(pos).equalsIgnoreCase(token)) {
                return false;
            }
            pos++;
            return true;
        }

        private void expect(String token) {
            if (!match(token)) {
                throw new IllegalArgumentException("Expected '" + token + "' in filter query");
            }
        }

        private String next() {
            if (pos >= tokens.size()) {
                return null;
            }
            return tokens.get(pos++);
        }
    }

    private static Predicate and(Predicate a, Predicate b) {
        return row -> a.test(row) && b.test(row);
    }

    private static Predicate or(Predicate a, Predicate b) {
        return row -> a.test(row) || b.test(row);
    }

    private static Predicate not(Predicate a) {
        return row -> !a.test(row);
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static int parseHexOrDec(String value) {
        String v = stringify(value).trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) {
            return 0;
        }
        if (v.startsWith("0x")) {
            return Integer.parseInt(v.substring(2), 16);
        }
        return Integer.parseInt(v);
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static String supportedSyntaxHint() {
        return List.of(
                "opcode:0x5000",
                "opcode:0x30*",
                "source:SERVER",
                "name:AUTH",
                "size:>20",
                "encoding:PLAIN",
                "payload_contains:\"0F 3A\"",
                "time:>10:30:00",
                "payload_regex:\"[0-9A-F]{4}00\"",
                "Use AND/OR/NOT with parentheses")
                .stream()
                .collect(Collectors.joining(", "));
    }
}
