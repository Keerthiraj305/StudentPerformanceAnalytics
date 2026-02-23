import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartPanel;

public class DashboardUI extends JFrame {
    private final List<Student> students;
    private final Map<String, AnalyticsUtil.RankInfo> ranks;
    private final Map<String, AnalyticsUtil.Stats> stats;
    private final Map<String, Integer> gradeBands;
    private final AnalyticsUtil.QualityMetrics qualityMetrics;

    private ChartPanel overviewAverageChart;
    private ChartPanel overviewPassFailChart;
    private ChartPanel studentChart;
    private ChartPanel distributionChart;
    private ChartPanel gradeBandChart;
    private ChartPanel correlationChart;

    public DashboardUI(List<Student> students){
        this.students = new ArrayList<>(students);
        this.ranks = AnalyticsUtil.computeRanks(students);
        this.stats = AnalyticsUtil.computeSubjectStats(students);
        this.gradeBands = AnalyticsUtil.computeGradeBands(students);
        this.qualityMetrics = AnalyticsUtil.computeQualityMetrics(students);
        applyTheme();

        SwingUtilities.invokeLater(this::initUI);
    }

    private void applyTheme() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }

        Font base = new Font("Georgia", Font.PLAIN, 13);
        UIManager.put("defaultFont", base);
        UIManager.put("Label.font", base);
        UIManager.put("Button.font", base);
        UIManager.put("TabbedPane.font", base.deriveFont(Font.BOLD, 13f));
        UIManager.put("Table.font", base);
        UIManager.put("TableHeader.font", base.deriveFont(Font.BOLD));
    }

    private void initUI(){
        setTitle("Student Performance Dashboard");
        setSize(1200, 760);
        setMinimumSize(new Dimension(980, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(246, 242, 234));

        root.add(buildSummaryPanel(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);

        setContentPane(root);
        setVisible(true);
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 12, 12));
        panel.setOpaque(false);

        SummaryMetrics metrics = computeMetrics();

        panel.add(createCard("Class Avg (Total)", String.format("%.1f", metrics.avgTotal), new Color(255, 230, 204)));
        panel.add(createCard("Topper", metrics.topperName + " (" + metrics.topperTotal + ")", new Color(210, 235, 255)));
        panel.add(createCard("Pass Rate", String.format("%.0f%%", metrics.passRate), new Color(220, 247, 222)));

        return panel;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Overview", buildOverviewTab());
        tabs.addTab("Students", buildStudentsTab());
        tabs.addTab("Rankings", wrapChart(ChartUtil.createTopBottomChart(students, 5)));
        tabs.addTab("Distribution", buildDistributionTab());
        tabs.addTab("Correlation", buildCorrelationTab());
        tabs.addTab("Statistics", buildStatsTab());
        tabs.addTab("Data Quality", buildQualityTab());
        tabs.addTab("Insights", buildInsightsTab());

        return tabs;
    }

    private JPanel buildOverviewTab() {
        SummaryMetrics metrics = computeMetrics();

        overviewAverageChart = ChartUtil.createAverageChart(metrics.avgMathRounded, metrics.avgSciRounded, metrics.avgEngRounded);
        overviewPassFailChart = ChartUtil.createPassFailChart(students, 120);

        overviewAverageChart.setPreferredSize(new Dimension(500, 300));
        overviewPassFailChart.setPreferredSize(new Dimension(400, 300));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wrapChart(overviewAverageChart), wrapChart(overviewPassFailChart));
        split.setResizeWeight(0.6);
        split.setDividerSize(6);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.add(split, BorderLayout.CENTER);
        panel.add(buildAveragesStrip(metrics), BorderLayout.SOUTH);
        panel.add(buildOverviewActions(), BorderLayout.NORTH);

        return panel;
    }

    private JPanel buildOverviewActions() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel left = new JPanel(new GridLayout(1, 2, 12, 12));
        left.setOpaque(false);
        left.add(buildWhatIfPanel());
        left.add(buildExportPanel("Export Overview Charts", e -> exportOverviewCharts()));

        panel.add(left, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildWhatIfPanel() {
        JPanel panel = createSoftPanel();
        panel.setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("What-if Simulator (Add/Subtract Points)");
        title.setFont(title.getFont().deriveFont(Font.BOLD));

        JComboBox<String> subjectSelect = new JComboBox<>(new String[] {"Maths", "Science", "English"});
        JSlider slider = new JSlider(-10, 10, 0);
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        JLabel avgLabel = new JLabel();
        JLabel passLabel = new JLabel();
        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        statsPanel.setOpaque(false);
        statsPanel.add(createLabeledPanel("Updated Avg", avgLabel));
        statsPanel.add(createLabeledPanel("Updated Pass %", passLabel));

        Runnable update = () -> {
            String subject = (String) subjectSelect.getSelectedItem();
            int delta = slider.getValue();
            WhatIfMetrics whatIf = computeWhatIf(subject, delta);
            avgLabel.setText(String.format("%.1f", whatIf.subjectAvg));
            passLabel.setText(String.format("%.0f%%", whatIf.passRate));
        };

        subjectSelect.addActionListener(e -> update.run());
        slider.addChangeListener(e -> update.run());
        update.run();

        JPanel top = new JPanel(new GridLayout(1, 2, 12, 12));
        top.setOpaque(false);
        top.add(createLabeledPanel("Subject", subjectSelect));
        top.add(createLabeledPanel("Adjustment", slider));

        panel.add(title, BorderLayout.NORTH);
        panel.add(top, BorderLayout.CENTER);
        panel.add(statsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildAveragesStrip(SummaryMetrics metrics) {
        JPanel strip = new JPanel(new GridLayout(1, 3, 12, 12));
        strip.setOpaque(false);

        strip.add(createCard("Avg Maths", String.format("%.1f", metrics.avgMath), new Color(255, 244, 218)));
        strip.add(createCard("Avg Science", String.format("%.1f", metrics.avgSci), new Color(233, 247, 255)));
        strip.add(createCard("Avg English", String.format("%.1f", metrics.avgEng), new Color(238, 250, 230)));

        return strip;
    }

    private JPanel buildStudentsTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JComboBox<String> studentSelect = new JComboBox<>();
        for (Student s : students) {
            studentSelect.addItem(s.name);
        }

        JLabel totalLabel = new JLabel();
        JLabel gradeLabel = new JLabel();
        JLabel rankLabel = new JLabel();
        JLabel percentileLabel = new JLabel();

        JPanel info = new JPanel(new GridLayout(1, 4, 12, 12));
        info.setOpaque(false);
        info.add(createLabeledPanel("Student", studentSelect));
        info.add(createLabeledPanel("Total", totalLabel));
        info.add(createLabeledPanel("Grade", gradeLabel));
        info.add(createLabeledPanel("Rank", rankLabel));

        JPanel infoBottom = new JPanel(new GridLayout(1, 2, 12, 12));
        infoBottom.setOpaque(false);
        infoBottom.add(createLabeledPanel("Percentile", percentileLabel));
        infoBottom.add(buildExportPanel("Export Student Chart", e -> exportStudentChart()));

        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setOpaque(false);
        header.add(info, BorderLayout.NORTH);
        header.add(infoBottom, BorderLayout.SOUTH);

        studentChart = ChartUtil.createStudentChart(students.get(0));
        JPanel chartHolder = wrapChart(studentChart);

        JTable table = buildStudentTable();
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(100, 220));

        updateStudentInfo(0, totalLabel, gradeLabel, rankLabel, percentileLabel, chartHolder);

        studentSelect.addActionListener(e -> {
            int index = studentSelect.getSelectedIndex();
            if (index >= 0) {
                updateStudentInfo(index, totalLabel, gradeLabel, rankLabel, percentileLabel, chartHolder);
            }
        });

        JPanel exportTablePanel = buildExportPanel("Export Table (CSV)", e -> exportStudentTable(table));

        panel.add(header, BorderLayout.NORTH);
        panel.add(chartHolder, BorderLayout.CENTER);
        panel.add(tableScroll, BorderLayout.SOUTH);
        panel.add(exportTablePanel, BorderLayout.EAST);

        return panel;
    }

    private JTable buildStudentTable() {
        String[] columns = {"Name", "Maths", "Science", "English", "Total", "Average", "Grade", "Rank", "Percentile"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Student s : students) {
            AnalyticsUtil.RankInfo rankInfo = ranks.get(s.name);
            model.addRow(new Object[] {
                    s.name,
                    s.maths,
                    s.science,
                    s.english,
                    s.getTotal(),
                    String.format("%.1f", s.getAverage()),
                    s.getGrade(),
                    rankInfo == null ? "-" : rankInfo.rank,
                    rankInfo == null ? "-" : String.format("%.1f", rankInfo.percentile)
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(22);
        return table;
    }

    private void updateStudentInfo(int index, JLabel totalLabel, JLabel gradeLabel, JLabel rankLabel, JLabel percentileLabel, JPanel chartHolder) {
        Student s = students.get(index);
        totalLabel.setText(String.valueOf(s.getTotal()));
        gradeLabel.setText(s.getGrade());
        AnalyticsUtil.RankInfo rankInfo = ranks.get(s.name);
        rankLabel.setText(rankInfo == null ? "-" : String.valueOf(rankInfo.rank));
        percentileLabel.setText(rankInfo == null ? "-" : String.format("%.1f", rankInfo.percentile));

        chartHolder.removeAll();
        studentChart = ChartUtil.createStudentChart(s);
        chartHolder.add(studentChart, BorderLayout.CENTER);
        chartHolder.revalidate();
        chartHolder.repaint();
    }

    private JPanel buildDistributionTab() {
        distributionChart = ChartUtil.createSubjectDistributionChart(students);
        gradeBandChart = ChartUtil.createGradeBandChart(gradeBands);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wrapChart(distributionChart), wrapChart(gradeBandChart));
        split.setResizeWeight(0.6);
        split.setDividerSize(6);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.add(split, BorderLayout.CENTER);
        panel.add(buildExportPanel("Export Distribution Charts", e -> exportDistributionCharts()), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCorrelationTab() {
        String xSubject = "Maths";
        String ySubject = "Science";
        correlationChart = ChartUtil.createCorrelationChart(students, xSubject, ySubject);

        List<Integer> xValues = AnalyticsUtil.getSubjectScores(students, xSubject);
        List<Integer> yValues = AnalyticsUtil.getSubjectScores(students, ySubject);
        double corr = AnalyticsUtil.correlation(xValues, yValues);

        JLabel corrLabel = new JLabel(String.format("Correlation (r): %.3f", corr));
        corrLabel.setFont(corrLabel.getFont().deriveFont(Font.BOLD));

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.add(wrapChart(correlationChart), BorderLayout.CENTER);
        panel.add(corrLabel, BorderLayout.NORTH);
        panel.add(buildExportPanel("Export Correlation Chart", e -> exportCorrelationChart()), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatsTab() {
        String[] columns = {"Metric", "Maths", "Science", "English", "Total"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        addStatRow(model, "Mean");
        addStatRow(model, "Median");
        addStatRow(model, "Std Dev");
        addStatRow(model, "Variance");
        addStatRow(model, "Min");
        addStatRow(model, "Max");
        addStatRow(model, "IQR");

        JTable table = new JTable(model);
        table.setRowHeight(22);

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildQualityTab() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 12));
        panel.setOpaque(false);

        panel.add(createCard("Records", String.valueOf(qualityMetrics.totalRecords), new Color(240, 240, 255)));
        panel.add(createCard("Missing Names", String.valueOf(qualityMetrics.missingNames), new Color(255, 232, 232)));
        panel.add(createCard("Duplicate Names", String.valueOf(qualityMetrics.duplicateNames), new Color(255, 240, 210)));
        panel.add(createCard("Invalid Scores", String.valueOf(qualityMetrics.invalidScores), new Color(255, 220, 220)));

        return panel;
    }

    private JPanel buildInsightsTab() {
        JTextArea area = new JTextArea(buildInsightsText());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFont(area.getFont().deriveFont(13f));
        area.setBackground(Color.WHITE);
        area.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildExportPanel(String label, java.awt.event.ActionListener handler) {
        JPanel panel = createSoftPanel();
        JButton button = new JButton(label);
        button.addActionListener(handler);
        panel.add(button);
        return panel;
    }

    private void addStatRow(DefaultTableModel model, String metric) {
        AnalyticsUtil.Stats math = stats.get("Maths");
        AnalyticsUtil.Stats sci = stats.get("Science");
        AnalyticsUtil.Stats eng = stats.get("English");
        AnalyticsUtil.Stats total = stats.get("Total");

        model.addRow(new Object[] {
                metric,
                formatStat(metric, math),
                formatStat(metric, sci),
                formatStat(metric, eng),
                formatStat(metric, total)
        });
    }

    private String formatStat(String metric, AnalyticsUtil.Stats stats) {
        switch (metric) {
            case "Mean":
                return String.format("%.2f", stats.mean);
            case "Median":
                return String.format("%.2f", stats.median);
            case "Std Dev":
                return String.format("%.2f", stats.stdDev);
            case "Variance":
                return String.format("%.2f", stats.variance);
            case "Min":
                return String.format("%.0f", stats.min);
            case "Max":
                return String.format("%.0f", stats.max);
            case "IQR":
                return String.format("%.2f", stats.iqr);
            default:
                return "-";
        }
    }

    private SummaryMetrics computeMetrics() {
        int totalMath = 0;
        int totalSci = 0;
        int totalEng = 0;
        int passCount = 0;
        Student topper = students.get(0);

        for (Student s : students) {
            totalMath += s.maths;
            totalSci += s.science;
            totalEng += s.english;

            if (s.getTotal() >= 120) {
                passCount++;
            }

            if (s.getTotal() > topper.getTotal()) {
                topper = s;
            }
        }

        double avgMath = totalMath / (double) students.size();
        double avgSci = totalSci / (double) students.size();
        double avgEng = totalEng / (double) students.size();
        double avgTotal = (avgMath + avgSci + avgEng);
        double passRate = (passCount * 100.0) / students.size();

        return new SummaryMetrics(avgMath, avgSci, avgEng, avgTotal, passRate, topper.name, topper.getTotal());
    }

    private WhatIfMetrics computeWhatIf(String subject, int delta) {
        int totalSubject = 0;
        int passCount = 0;

        for (Student s : students) {
            int adjusted = clamp(AnalyticsUtil.getSubjectScore(s, subject) + delta);
            int total = s.getTotal() - AnalyticsUtil.getSubjectScore(s, subject) + adjusted;
            totalSubject += adjusted;
            if (total >= 120) {
                passCount++;
            }
        }

        double subjectAvg = totalSubject / (double) students.size();
        double passRate = (passCount * 100.0) / students.size();
        return new WhatIfMetrics(subjectAvg, passRate);
    }

    private int clamp(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }

    private void exportOverviewCharts() {
        exportChart(overviewAverageChart, "average-chart");
        exportChart(overviewPassFailChart, "pass-fail-chart");
    }

    private void exportStudentChart() {
        exportChart(studentChart, "student-chart");
    }

    private void exportDistributionCharts() {
        exportChart(distributionChart, "distribution-chart");
        exportChart(gradeBandChart, "grade-band-chart");
    }

    private void exportCorrelationChart() {
        exportChart(correlationChart, "correlation-chart");
    }

    private void exportChart(ChartPanel panel, String prefix) {
        if (panel == null) return;
        File file = chooseFile(prefix + "-" + timestamp() + ".png");
        if (file == null) return;
        try {
            ChartUtil.saveChartAsPng(panel, file, 900, 600);
        } catch (Exception ignored) {
        }
    }

    private void exportStudentTable(JTable table) {
        File file = chooseFile("students-" + timestamp() + ".csv");
        if (file == null) return;
        try (FileWriter writer = new FileWriter(file)) {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            for (int col = 0; col < model.getColumnCount(); col++) {
                writer.append(model.getColumnName(col));
                writer.append(col == model.getColumnCount() - 1 ? "\n" : ",");
            }
            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    writer.append(String.valueOf(model.getValueAt(row, col)));
                    writer.append(col == model.getColumnCount() - 1 ? "\n" : ",");
                }
            }
        } catch (Exception ignored) {
        }
    }

    private File chooseFile(String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        int result = chooser.showSaveDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private String buildInsightsText() {
        AnalyticsUtil.Stats math = stats.get("Maths");
        AnalyticsUtil.Stats sci = stats.get("Science");
        AnalyticsUtil.Stats eng = stats.get("English");

        String strongest = maxSubject(math.mean, sci.mean, eng.mean);
        String weakest = minSubject(math.mean, sci.mean, eng.mean);
        String mostVariable = maxSubject(math.stdDev, sci.stdDev, eng.stdDev);

        SummaryMetrics metrics = computeMetrics();
        return "Key Insights\n"
                + "- Strongest subject average: " + strongest + "\n"
                + "- Weakest subject average: " + weakest + "\n"
                + "- Highest variability: " + mostVariable + "\n"
                + "- Pass rate: " + String.format("%.0f%%", metrics.passRate) + "\n"
                + "- Topper: " + metrics.topperName + " (" + metrics.topperTotal + ")\n"
                + "- Data quality flags: " + qualityMetrics.invalidScores + " invalid scores, "
                + qualityMetrics.duplicateNames + " duplicate names\n";
    }

    private String maxSubject(double m, double s, double e) {
        if (m >= s && m >= e) return "Maths";
        if (s >= m && s >= e) return "Science";
        return "English";
    }

    private String minSubject(double m, double s, double e) {
        if (m <= s && m <= e) return "Maths";
        if (s <= m && s <= e) return "Science";
        return "English";
    }

    private JPanel wrapChart(ChartPanel panel) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(8, 8, 8, 8)
        ));
        container.setBackground(Color.WHITE);
        container.add(panel, BorderLayout.CENTER);
        return container;
    }

    private JPanel createCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(color);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 18f));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createSoftPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                new EmptyBorder(8, 8, 8, 8)
        ));
        return panel;
    }

    private JPanel createLabeledPanel(String title, java.awt.Component component) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private static class SummaryMetrics {
        final double avgMath;
        final double avgSci;
        final double avgEng;
        final double avgTotal;
        final double passRate;
        final String topperName;
        final int topperTotal;
        final int avgMathRounded;
        final int avgSciRounded;
        final int avgEngRounded;

        SummaryMetrics(double avgMath, double avgSci, double avgEng, double avgTotal, double passRate, String topperName, int topperTotal) {
            this.avgMath = avgMath;
            this.avgSci = avgSci;
            this.avgEng = avgEng;
            this.avgTotal = avgTotal;
            this.passRate = passRate;
            this.topperName = topperName;
            this.topperTotal = topperTotal;
            this.avgMathRounded = (int) Math.round(avgMath);
            this.avgSciRounded = (int) Math.round(avgSci);
            this.avgEngRounded = (int) Math.round(avgEng);
        }
    }

    private static class WhatIfMetrics {
        final double subjectAvg;
        final double passRate;

        WhatIfMetrics(double subjectAvg, double passRate) {
            this.subjectAvg = subjectAvg;
            this.passRate = passRate;
        }
    }
}