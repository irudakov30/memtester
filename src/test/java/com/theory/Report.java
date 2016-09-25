package com.theory;

import lombok.Builder;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.chart.LineChartBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.HorizontalTextAlignment;
import net.sf.dynamicreports.report.exception.DRException;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import static net.sf.dynamicreports.report.builder.DynamicReports.*;

/**
 * Created by irudakov on 25.09.2016.
 */
@Builder
public class Report {

    private List<Metric> metrics;
    private MemoryAnalizerConfig memoryAnalizerConfig;

    public void generate() throws DRException, FileNotFoundException {
        JasperReportBuilder report = DynamicReports.report();

        report.setDataSource(metrics);

        TextColumnBuilder timeColumn = col.column("Time", "time", type.stringType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT);
        TextColumnBuilder loopColumn = col.column("Loop", "loopCount", type.integerType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT);
        TextColumnBuilder memoryColumn = col.column("Memory", "memory", type.doubleType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT);
        TextColumnBuilder gcInvokedColumn = col.column("GC invoked", "gcInvoke", type.integerType()).setHorizontalTextAlignment(HorizontalTextAlignment.LEFT);

        report.columns(loopColumn, memoryColumn, gcInvokedColumn, timeColumn);

        report.subtotalsAtSummary(sbt.sum(gcInvokedColumn));

        applyStyles(report);

        LineChartBuilder lineChartBuilder = cht.lineChart().setTitle("Memory dynamic chart")
                .setCategory(loopColumn)
                .addSerie(cht.serie(memoryColumn));

        report.summary(lineChartBuilder);

        String reportPath = memoryAnalizerConfig.getReportPath();
        report.toHtml(new FileOutputStream(new File(reportPath + File.separator + "index.html")));
    }

    private void applyStyles(JasperReportBuilder report) {
        StyleBuilder boldStyle = stl.style().bold();
        StyleBuilder boldCenteredStyle = stl.style(boldStyle).setHorizontalAlignment(HorizontalAlignment.CENTER);

        StyleBuilder columnTitleStyle = stl.style(boldCenteredStyle)
                .setBorder(stl.pen1Point())
                .setBackgroundColor(Color.LIGHT_GRAY);
        report
                .setColumnTitleStyle(columnTitleStyle)
                .highlightDetailEvenRows()
                .title(cmp.text("Getting started").setStyle(boldCenteredStyle))
                .pageFooter(cmp.pageXofY().setStyle(boldCenteredStyle));
    }
}
