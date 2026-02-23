import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ChartUtil {

    public static ChartPanel createAverageChart(int m, int s, int e) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(m, "Marks", "Maths");
        dataset.addValue(s, "Marks", "Science");
        dataset.addValue(e, "Marks", "English");

        JFreeChart chart = ChartFactory.createBarChart(
                "Subject Wise Average",
                "Subjects",
                "Marks",
                dataset
        );

        return new ChartPanel(chart);
    }

    public static ChartPanel createStudentChart(Student student) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(student.maths, "Marks", "Maths");
        dataset.addValue(student.science, "Marks", "Science");
        dataset.addValue(student.english, "Marks", "English");

        JFreeChart chart = ChartFactory.createBarChart(
                "Student Performance: " + student.name,
                "Subjects",
                "Marks",
                dataset
        );

        return new ChartPanel(chart);
    }

    public static ChartPanel createTopBottomChart(List<Student> students, int count) {
        ArrayList<Student> sorted = new ArrayList<>(students);
        sorted.sort(Comparator.comparingInt(Student::getTotal).reversed());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        int topCount = Math.min(count, sorted.size());
        for (int i = 0; i < topCount; i++) {
            Student s = sorted.get(i);
            dataset.addValue(s.getTotal(), "Top " + topCount, s.name);
        }

        ArrayList<Student> bottom = new ArrayList<>(sorted);
        Collections.reverse(bottom);
        int bottomCount = Math.min(count, bottom.size());
        for (int i = 0; i < bottomCount; i++) {
            Student s = bottom.get(i);
            dataset.addValue(s.getTotal(), "Bottom " + bottomCount, s.name);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Top/Bottom Students (Total Score)",
                "Students",
                "Total",
                dataset
        );

        return new ChartPanel(chart);
    }

    public static ChartPanel createPassFailChart(List<Student> students, int passThresholdTotal) {
        int pass = 0;
        int fail = 0;
        for (Student s : students) {
            if (s.getTotal() >= passThresholdTotal) {
                pass++;
            } else {
                fail++;
            }
        }

        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Pass", pass);
        dataset.setValue("Fail", fail);

        JFreeChart chart = ChartFactory.createPieChart(
                "Pass vs Fail",
                dataset,
                true,
                true,
                false
        );

        return new ChartPanel(chart);
    }

    public static ChartPanel createSubjectDistributionChart(List<Student> students) {
        String[] bins = new String[] {"0-39", "40-59", "60-79", "80-100"};
        int[] mathBins = new int[bins.length];
        int[] sciBins = new int[bins.length];
        int[] engBins = new int[bins.length];

        for (Student s : students) {
            mathBins[binIndex(s.maths)]++;
            sciBins[binIndex(s.science)]++;
            engBins[binIndex(s.english)]++;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < bins.length; i++) {
            dataset.addValue(mathBins[i], "Maths", bins[i]);
            dataset.addValue(sciBins[i], "Science", bins[i]);
            dataset.addValue(engBins[i], "English", bins[i]);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Score Distribution by Subject",
                "Score Bands",
                "Students",
                dataset
        );

        return new ChartPanel(chart);
    }

    public static ChartPanel createGradeBandChart(Map<String, Integer> gradeBands) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Integer> entry : gradeBands.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Grade Distribution",
                dataset,
                true,
                true,
                false
        );

        return new ChartPanel(chart);
    }

    public static ChartPanel createCorrelationChart(List<Student> students, String xSubject, String ySubject) {
        XYSeries series = new XYSeries(xSubject + " vs " + ySubject);
        for (Student s : students) {
            int x = AnalyticsUtil.getSubjectScore(s, xSubject);
            int y = AnalyticsUtil.getSubjectScore(s, ySubject);
            series.add(x, y);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createScatterPlot(
                "Correlation: " + xSubject + " vs " + ySubject,
                xSubject,
                ySubject,
                dataset
        );

        return new ChartPanel(chart);
    }

    public static void saveChartAsPng(ChartPanel panel, File file, int width, int height) throws IOException {
        ChartUtilities.saveChartAsPNG(file, panel.getChart(), width, height);
    }

    private static int binIndex(int score) {
        if (score < 40) return 0;
        if (score < 60) return 1;
        if (score < 80) return 2;
        return 3;
    }
}