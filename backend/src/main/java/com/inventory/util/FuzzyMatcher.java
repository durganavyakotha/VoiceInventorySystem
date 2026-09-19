package com.inventory.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Free fuzzy string matching: Levenshtein distance + Jaro-Winkler similarity
 * for near-word / misspelled product recognition.
 */
public final class FuzzyMatcher {

    public record Match(String value, double score) {}

    private FuzzyMatcher() {}

    public static String bestMatch(String input, Collection<String> candidates, double minScore) {
        Match m = bestMatchDetailed(input, candidates, minScore);
        return m != null ? m.value() : null;
    }

    public static Match bestMatchDetailed(String input, Collection<String> candidates, double minScore) {
        if (input == null || input.isBlank() || candidates == null || candidates.isEmpty()) {
            return null;
        }
        String needle = normalize(input);
        Match best = null;
        for (String c : candidates) {
            if (c == null || c.isBlank()) continue;
            String cand = normalize(c);
            double score = similarity(needle, cand);
            // boost exact / contains
            if (cand.equals(needle)) {
                score = 1.0;
            } else if (cand.contains(needle) || needle.contains(cand)) {
                score = Math.max(score, 0.92);
            }
            if (score >= minScore && (best == null || score > best.score())) {
                best = new Match(c, score);
            }
        }
        return best;
    }

    public static List<Match> topMatches(String input, Collection<String> candidates, double minScore, int limit) {
        List<Match> list = new ArrayList<>();
        if (input == null || candidates == null) return list;
        for (String c : candidates) {
            Match m = bestMatchDetailed(input, List.of(c), minScore);
            if (m != null) list.add(m);
        }
        list.sort(Comparator.comparingDouble(Match::score).reversed());
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    /** Combined similarity in [0,1]. */
    public static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        double jw = jaroWinkler(a, b);
        double lev = 1.0 - ((double) levenshtein(a, b) / Math.max(a.length(), b.length()));
        return 0.55 * jw + 0.45 * Math.max(0, lev);
    }

    public static int levenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[n][m];
    }

    public static double jaroWinkler(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int matchDistance = Math.max(len1, len2) / 2 - 1;
        if (matchDistance < 0) matchDistance = 0;

        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];
        int matches = 0;
        int transpositions = 0;

        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }
        if (matches == 0) return 0.0;

        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }

        double jaro = ((matches / (double) len1)
                + (matches / (double) len2)
                + ((matches - transpositions / 2.0) / matches)) / 3.0;

        int prefix = 0;
        for (int i = 0; i < Math.min(4, Math.min(len1, len2)); i++) {
            if (s1.charAt(i) == s2.charAt(i)) prefix++;
            else break;
        }
        return jaro + prefix * 0.1 * (1 - jaro);
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\u0C00-\\u0C7F\\u0900-\\u097F\\u0B80-\\u0BFF\\u0C80-\\u0CFF]+", "");
    }
}
