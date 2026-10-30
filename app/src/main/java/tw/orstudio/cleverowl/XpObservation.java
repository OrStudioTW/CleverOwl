package tw.orstudio.cleverowl;

/** Diagnostic output deliberately excludes identifiers, URLs, headers and request bodies. */
public final class XpObservation {
    private XpObservation() {}
    public static boolean sessionWrite(String method, String host, String path) {
        if (!("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) return false;
        if (host == null || path == null) return false;
        if (!(host.equals("duolingo.com") || host.endsWith(".duolingo.com")
                || host.equals("duolingo.cn") || host.endsWith(".duolingo.cn"))) return false;
        for (String segment : path.split("/"))
            if (segment.equals("sessions") || segment.equals("session")) return true;
        return false;
    }
    private static String scalar(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Integer || value instanceof Long) return value.toString();
        if (value instanceof Double && Double.isFinite((Double) value)) return value.toString();
        return "unsupported";
    }
    public static String submission(Object multiplier, Object hasBoost, Object promised, Object expected) {
        return "XP serialization completed: multiplier=" + scalar(multiplier) + ", hasBoost=" + scalar(hasBoost)
                + ", xpPromised=" + scalar(promised) + ", expectedXpGain=" + scalar(expected)
                + "; encoding alone does not prove network submission.";
    }
    public static String summary(Object gainedXp, Object dayKey) {
        return "XP summary decoded: gainedXp=" + scalar(gainedXp) + ", dayKey=" + scalar(dayKey)
                + "; decoder source may be network or cache.";
    }
}
