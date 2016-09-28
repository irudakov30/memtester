package com.theory;

import lombok.Builder;
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
        return MemoryAnalizerConfig.reportPath + File.separator + "index.html";
    }

    public void generate() throws DRException, FileNotFoundException {
        JasperReportBuilder report = createReport("", metrics);

        String reportPath = MemoryAnalizerConfig.reportPath;
        report.toHtml(new FileOutputStream(new File(getReportPath())));
    }

    private JasperReportBuilder createReport(String testDisplayName, List<Metric> metrics) {
        JasperReportBuilder report = DynamicReports.report();

        List<String> columnNames = prepareColumnNames(metrics);

        JRDataSource jrDataSource = createDataSource(columnNames, metrics);
        report.setDataSource(jrDataSource);

        TextColumnBuilder loopColumn = col.column(LOOP_COLUMN, LOOP_COLUMN, type.integerType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT);

        List<TextColumnBuilder> memoryColumns = columnNames.subList(1, columnNames.size() - 1).stream().map(n -> col.column(n, n, type.doubleType())
                .setHorizontalTextAlignment(HorizontalTextAlignment.LEFT)).collect(toList());

        TextColumnBuilder heapDumpPath = col.column("Heap dump path", "heapDumpFile", type.stringType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT);

        report.columns(loopColumn).columns(memoryColumns.toArray(new ColumnBuilder[memoryColumns.size()])).columns(heapDumpPath);

        applyStyles(report);

        List<XyChartSerieBuilder> series = new ArrayList<>(memoryColumns.size());
        for(TextColumnBuilder col: memoryColumns) {
            series.add(cht.xySerie(col));
        }

        ComponentBuilder lineChartBuilder = cht.xyLineChart()
                .setTitle("XY line chart")
                .setXValue(loopColumn)
                .series((XyChartSerieBuilder[]) series.toArray(new XyChartSerieBuilder[series.size()]))
                .setXAxisFormat(
                        cht.axisFormat().setLabel("X"))
                .setYAxisFormat(
                        cht.axisFormat().setLabel("Y"));

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
        columnNames.add("loop"); //X column
        columnNames.addAll(metrics.stream().map(Metric::getTestName).collect(toSet()));
        return columnNames;
    }


    private void applyStyles(JasperReportBuilder report) {
        StyleBuilder boldStyle = stl.style().bold();
        StyleBuilder boldCenteredStyle = stl.style(boldStyle).setHorizontalAlignment(HorizontalAlignment.CENTER);

        StyleBuilder columnTitleStyle = stl.style(boldCenteredStyle)
                .setBorder(stl.pen1Point())
                .setBackgroundColor(Color.LIGHT_GRAY);
        report.setColumnTitleStyle(columnTitleStyle)
              .highlightDetailEvenRows()
              .title(cmp.text("Getting started").setStyle(boldCenteredStyle))
              .pageFooter(cmp.pageXofY().setStyle(boldCenteredStyle));
    }
}
