package com.theory.junit.runner;

import com.theory.junit.runner.MemoryAnalizerConfig;
import com.theory.junit.runner.Metric;
import lombok.Builder;
import net.sf.dynamicreports.jasper.builder.JasperConcatenatedReportBuilder;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.chart.XyChartSerieBuilder;
import net.sf.dynamicreports.report.builder.column.ColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.ComponentBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRDataSource;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;
import static net.sf.dynamicreports.report.builder.DynamicReports.*;

/**
 * Created by irudakov on 25.09.2016.
 */
@Builder
public class Report {

    private List<Metric> metrics;
    private MemoryAnalizerConfig memoryAnalizerConfig;

    private Map<String, TextColumnBuilder> memoryColumns = new HashMap<>();

    private static final String LOOP_COLUMN = "loop";

    public String getReportPath() {
        return MemoryAnalizerConfig.reportPath + File.separator + "report." + MemoryAnalizerConfig.reportType;
    }

    public void generate(String reportName) throws DRException, FileNotFoundException {
        JasperReportBuilder report = createReport(reportName, metrics);
        JasperReportBuilder generalMetricsReport = createGeneralMetricsReport();

        JasperConcatenatedReportBuilder concatenatedReport = concatenatedReport().concatenate(report, generalMetricsReport);

        if(MemoryAnalizerConfig.reportType.equalsIgnoreCase("pdf")) {
            concatenatedReport.toPdf(new FileOutputStream(new File(getReportPath())));
        } else {
            concatenatedReport.toHtml(new FileOutputStream(new File(getReportPath())));
        }
    }

    private JasperReportBuilder createGeneralMetricsReport() {
        JasperReportBuilder generalMetricsReport = DynamicReports.report();

        DRDataSource dataSource = new DRDataSource("Test name", "Gc hits");
        metrics.stream().collect(groupingBy(Metric::getTestName)).entrySet().forEach(e -> dataSource.add(e.getKey(), e.getValue().stream().mapToInt(Metric::getGcInvoke).sum()));

        generalMetricsReport.setDataSource(dataSource);
        TextColumnBuilder testNameColumn = col.column("Test name", "Test name", type.stringType()).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);
        TextColumnBuilder gcHitsColumn = col.column("Gc hits", "Gc hits", type.integerType()).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);

        applyStyles(generalMetricsReport, "General metrics report");

        generalMetricsReport.columns(testNameColumn, gcHitsColumn);
        return generalMetricsReport;
    }

    private JasperReportBuilder createReport(String reportName, List<Metric> metrics) {
        JasperReportBuilder report = DynamicReports.report();

        List<String> columnNames = prepareColumnNames(metrics);

        JRDataSource jrDataSource = createDataSource(columnNames, metrics);
        report.setDataSource(jrDataSource);

        TextColumnBuilder loopColumn = col.column(LOOP_COLUMN, LOOP_COLUMN, type.integerType()).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);

        List<TextColumnBuilder> memoryColumns = columnNames.subList(1, columnNames.size()).stream().map(n -> col.column(n, n, type.doubleType())
                .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER)).collect(toList());

        report.columns(loopColumn).columns(memoryColumns.toArray(new ColumnBuilder[memoryColumns.size()]));

        applyStyles(report, reportName);

        List<XyChartSerieBuilder> series = new ArrayList<>(memoryColumns.size());
        for(TextColumnBuilder col: memoryColumns) {
            series.add(cht.xySerie(col));
        }

        ComponentBuilder lineChartBuilder = cht.xyLineChart()
                .setTitle("Memory consumption chart")
                .setXValue(loopColumn)
                .series((XyChartSerieBuilder[]) series.toArray(new XyChartSerieBuilder[series.size()]))
                .setXAxisFormat(
                        cht.axisFormat().setLabel(LOOP_COLUMN))
                .setYAxisFormat(
                        cht.axisFormat().setLabel("Memory (Mb)"));

        report.summary(lineChartBuilder);
        return report;
    }

    protected JRDataSource createDataSource(List<String> columnNames, List<Metric> metrics) {
        DRDataSource dataSource = new DRDataSource(columnNames.toArray(new String[columnNames.size()]));

        Map<Integer, List<Metric>> groupedMetrics = metrics.stream().collect(groupingBy(Metric::getLoopCount));

        for (Map.Entry<Integer, List<Metric>> entry: groupedMetrics.entrySet()) {
            Integer loop = entry.getKey();
            List<Metric> values = entry.getValue();
            List<Double> memoryValues = values.stream().map(Metric::getMemory).collect(toList());

            List<Object> res = new ArrayList<>();
            res.add(loop);
            res.addAll(memoryValues);

            dataSource.add(res.toArray(new Object[res.size()]));
        }

        return dataSource;
    }

    private List<String> prepareColumnNames(List<Metric> metrics) {
        List<String> columnNames = new ArrayList<>();
        columnNames.add(LOOP_COLUMN); //X column
        columnNames.addAll(metrics.stream().map(Metric::getTestName).collect(toSet()));
        return columnNames;
    }


    private void applyStyles(JasperReportBuilder report, String reportName) {
        StyleBuilder boldStyle = stl.style().bold();

        StyleBuilder boldCenteredStyle = stl.style(boldStyle).setHorizontalAlignment(HorizontalAlignment.CENTER).setFont(stl.font().setFontSize(18));

        StyleBuilder columnTitleStyle = stl.style(boldCenteredStyle)
                .setBorder(stl.pen1Point())
                .setBackgroundColor(Color.LIGHT_GRAY);
        report.setColumnTitleStyle(columnTitleStyle)
              .highlightDetailEvenRows()
              .title(cmp.text(reportName).setStyle(boldCenteredStyle))
              .pageFooter(cmp.pageXofY().setStyle(boldCenteredStyle));

        report.setTextStyle(stl.style().setFont(stl.font().setFontSize(16)));

        report.setChartStyle(stl.style().setFont(stl.font().setFontSize(14)));
    }
}
