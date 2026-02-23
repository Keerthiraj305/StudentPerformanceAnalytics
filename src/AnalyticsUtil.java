import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalyticsUtil {

    public static class Stats {
        public final double mean;
        public final double median;
        public final double variance;
        public final double stdDev;
        public final double min;
        public final double max;
        public final double iqr;

        public Stats(double mean, double median, double variance, double stdDev, double min, double max, double iqr) {
            this.mean = mean;
            this.median = median;
            this.variance = variance;
            this.stdDev = stdDev;
            this.min = min;
            this.max = max;
            this.iqr = iqr;
        }
    }

    public static class RankInfo {
        public final int rank;
        public final double percentile;

        public RankInfo(int rank, double percentile) {
            this.rank = rank;
            this.percentile = percentile;
        }
    }

    public static class QualityMetrics {
        public final int totalRecords;
        public final int missingNames;
        public final int duplicateNames;
        public final int invalidScores;

        public QualityMetrics(int totalRecords, int missingNames, int duplicateNames, int invalidScores) {
            this.totalRecords = totalRecords;
            this.missingNames = missingNames;
            this.duplicateNames = duplicateNames;
            this.invalidScores = invalidScores;
        }
    }

    public static Map<String, Stats> computeSubjectStats(List<Student> students) {
        Map<String, Stats> stats = new HashMap<>();
        stats.put("Maths", computeStats(getSubjectScores(students, "Maths")));
        stats.put("Science", computeStats(getSubjectScores(students, "Science")));
        stats.put("English", computeStats(getSubjectScores(students, "English")));
        stats.put("Total", computeStats(getTotalScores(students)));
        return stats;
    }

    public static Map<String, Integer> computeGradeBands(List<Student> students) {
        Map<String, Integer> bands = new HashMap<>();
        bands.put("A", 0);
        bands.put("B", 0);
        bands.put("C", 0);
        bands.put("D", 0);
        bands.put("F", 0);
        for (Student s : students) {
            String grade = s.getGrade();
            bands.put(grade, bands.get(grade) + 1);
        }
        return bands;
    }

    public static Map<String, RankInfo> computeRanks(List<Student> students) {
        ArrayList<Student> sorted = new ArrayList<>(students);
        sorted.sort((a, b) -> Integer.compare(b.getTotal(), a.getTotal()));

        Map<String, RankInfo> ranks = new HashMap<>();
        int n = sorted.size();
        int rank = 1;
        for (int i = 0; i < n; i++) {
            if (i > 0 && sorted.get(i).getTotal() < sorted.get(i - 1).getTotal()) {
                rank = i + 1;
            }
            double percentile = (n == 1) ? 100.0 : 100.0 * (n - rank) / (n - 1.0);
            ranks.put(sorted.get(i).name, new RankInfo(rank, percentile));
        }
        return ranks;
    }

    public static double correlation(List<Integer> x, List<Integer> y) {
        if (x.isEmpty() || y.isEmpty() || x.size() != y.size()) {
            return 0.0;
        }

        int n = x.size();
        double meanX = mean(x);
        double meanY = mean(y);
        double num = 0.0;
        double denX = 0.0;
        double denY = 0.0;

        for (int i = 0; i < n; i++) {
            double dx = x.get(i) - meanX;
            double dy = y.get(i) - meanY;
            num += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }

        double denom = Math.sqrt(denX * denY);
        return denom == 0.0 ? 0.0 : num / denom;
    }

    public static QualityMetrics computeQualityMetrics(List<Student> students) {
        int missingNames = 0;
        int invalidScores = 0;
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();

        for (Student s : students) {
            if (s.name == null || s.name.trim().isEmpty()) {
                missingNames++;
            }
            if (!isValidScore(s.maths)) invalidScores++;
            if (!isValidScore(s.science)) invalidScores++;
            if (!isValidScore(s.english)) invalidScores++;

            String key = s.name == null ? "" : s.name.trim().toLowerCase();
            if (!key.isEmpty()) {
                if (!seen.add(key)) {
                    duplicates.add(key);
                }
            }
        }

        return new QualityMetrics(students.size(), missingNames, duplicates.size(), invalidScores);
    }

    public static List<Integer> getSubjectScores(List<Student> students, String subject) {
        ArrayList<Integer> values = new ArrayList<>();
        for (Student s : students) {
            values.add(getSubjectScore(s, subject));
        }
        return values;
    }

    public static List<Integer> getTotalScores(List<Student> students) {
        ArrayList<Integer> values = new ArrayList<>();
        for (Student s : students) {
            values.add(s.getTotal());
        }
        return values;
    }

    public static int getSubjectScore(Student s, String subject) {
        switch (subject) {
            case "Maths":
                return s.maths;
            case "Science":
                return s.science;
            case "English":
                return s.english;
            default:
                return 0;
        }
    }

    public static Stats computeStats(List<Integer> values) {
        if (values.isEmpty()) {
            return new Stats(0, 0, 0, 0, 0, 0, 0);
        }

        ArrayList<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        double mean = mean(sorted);
        double median = median(sorted);
        double variance = variance(sorted, mean);
        double stdDev = Math.sqrt(variance);
        double min = sorted.get(0);
        double max = sorted.get(sorted.size() - 1);
        double iqr = percentile(sorted, 75) - percentile(sorted, 25);

        return new Stats(mean, median, variance, stdDev, min, max, iqr);
    }

    private static double mean(List<Integer> values) {
        double sum = 0.0;
        for (int v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private static double median(List<Integer> sorted) {
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static double variance(List<Integer> values, double mean) {
        double sum = 0.0;
        for (int v : values) {
            double diff = v - mean;
            sum += diff * diff;
        }
        return sum / values.size();
    }

    private static double percentile(List<Integer> sorted, int percentile) {
        if (sorted.isEmpty()) return 0.0;
        double index = (percentile / 100.0) * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = index - lower;
        return sorted.get(lower) * (1 - weight) + sorted.get(upper) * weight;
    }

    private static boolean isValidScore(int score) {
        return score >= 0 && score <= 100;
    }
}
