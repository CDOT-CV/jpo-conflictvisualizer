package us.dot.its.jpo.ode.api;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.HeatMapChart;
import org.knowm.xchart.HeatMapChartBuilder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.style.AxesChartStyler.TextAlignment;
import org.knowm.xchart.style.Styler.LegendLayout;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.geom.Point;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import us.dot.its.jpo.conflictmonitor.monitor.models.assessments.LaneDirectionOfTravelAssessment;
import us.dot.its.jpo.conflictmonitor.monitor.models.assessments.LaneDirectionOfTravelAssessmentGroup;
import us.dot.its.jpo.ode.api.accessors.map.ProcessedMapRepository;
import us.dot.its.jpo.ode.api.models.ChartData;
import us.dot.its.jpo.ode.api.models.IDCount;
import us.dot.its.jpo.ode.api.models.LaneConnectionCount;

import java.io.FileNotFoundException;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

public class ReportBuilder {

    private Document document;
    private PdfWriter writer;
    DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter secondsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:SS");

    // Creates a new PDF report builder to add components to.
    public ReportBuilder(OutputStream stream) {
        document = new Document();
        try {
            writer = PdfWriter.getInstance(document, stream);

            document.open();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }

    // Writes PDF to File System if Output Stream, allows getting PDF as ByteStream
    // for ByteOutputStreams
    public void write() {
        document.close();

    }

    public void addTitlePage(String reportTitle, long startTime, long endTime) {

        String startTimeString = Instant.ofEpochMilli(startTime).toString();
        String endTimeString = Instant.ofEpochMilli(endTime).toString();

        try {
            Font f = new Font(FontFamily.TIMES_ROMAN, 42.0f, Font.BOLD, BaseColor.BLACK);
            Paragraph p = new Paragraph(reportTitle, f);
            p.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(p);

            Paragraph dates = new Paragraph(startTimeString + "     -     " + endTimeString);
            dates.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(dates);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public void addTitle(String title) {
        try {
            document.newPage();
            Font f = new Font(FontFamily.TIMES_ROMAN, 36.0f, Font.BOLD, BaseColor.BLACK);
            Paragraph p = new Paragraph(title, f);
            document.add(p);

        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public void addPageBreak() {
        document.newPage();
    }

    public void addMapBroadcastRate(List<IDCount> data) {

        List<Date> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        DateFormat sdf = new SimpleDateFormat("yyyy-mm-dd-HH:mm:ss");

        Date lastDate = null;
        for (IDCount elem : data) {
            try {
                Date newDate = sdf.parse(elem.getId());

                if (lastDate != null) {
                    if (newDate.toInstant().toEpochMilli() - lastDate.toInstant().toEpochMilli() > 1000) {
                        times.add(Date.from(lastDate.toInstant().plusSeconds(1)));
                        values.add(0.0);
                        times.add(Date.from(newDate.toInstant().minusSeconds(1)));
                        values.add(0.0);
                    }
                }

                times.add(newDate);
                values.add((double) elem.getCount());

                lastDate = newDate;

            } catch (ParseException e) {

            }
        }

        int width = (int) (document.getPageSize().getWidth() * 0.9);

        // Create Chart
        XYChart chart = new XYChartBuilder().width(width).height(400).title("Map Broadcast Rate").xAxisTitle("Date")
                .yAxisTitle("Broadcast Rate (msg/second)").build();

        if(times.size() > 0 && values.size() > 0){
            XYSeries series = chart.addSeries("Map Broadcast Rate", times, values);
            series.setSmooth(true);
            series.setMarker(SeriesMarkers.NONE);

            ArrayList<Date> markerDates = new ArrayList<>();
            markerDates.add(times.get(0));
            markerDates.add(times.get(times.size() - 1));

            ArrayList<Double> bottomMarkerValues = new ArrayList<>();
            bottomMarkerValues.add(9.0);
            bottomMarkerValues.add(9.0);

            ArrayList<Double> topMarkerValues = new ArrayList<>();
            topMarkerValues.add(11.0);
            topMarkerValues.add(11.0);

            addMarkerLine(chart, markerDates, bottomMarkerValues);
            addMarkerLine(chart, markerDates, topMarkerValues);
        }
        

        chart.getStyler().setShowWithinAreaPoint(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendVisible(false);

        BufferedImage chartImage = BitmapEncoder.getBufferedImage(chart);

        try {
            document.add(Image.getInstance(chartImage, null));
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

    }

    public void addSpatBroadcastRate(List<IDCount> data) {

        List<Date> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        DateFormat sdf = new SimpleDateFormat("yyyy-mm-dd-HH:mm:ss");

        Date lastDate = null;
        for (IDCount elem : data) {
            try {
                Date newDate = sdf.parse(elem.getId());
                if (lastDate != null) {
                    if (newDate.toInstant().toEpochMilli() - lastDate.toInstant().toEpochMilli() > 1000) {
                        times.add(Date.from(lastDate.toInstant().plusSeconds(1)));
                        values.add(0.0);
                        times.add(Date.from(newDate.toInstant().minusSeconds(1)));
                        values.add(0.0);
                    }
                }
                times.add(newDate);
                values.add((double) elem.getCount());
                lastDate = newDate;
            } catch (ParseException e) {

            }
        }

        int width = (int) (document.getPageSize().getWidth() * 0.9);

        // Create Chart
        XYChart chart = new XYChartBuilder().width(width).height(400).title("SPaT Broadcast Rate").xAxisTitle("Date")
                .yAxisTitle("Broadcast Rate (msg/second)").build();

        if(times.size() > 0 && values.size() > 0){
            XYSeries series = chart.addSeries("Spat Broadcast Rate", times, values);
            series.setSmooth(true);
            series.setMarker(SeriesMarkers.NONE);

            ArrayList<Date> markerDates = new ArrayList<>();
            markerDates.add(times.get(0));
            markerDates.add(times.get(times.size() - 1));

            ArrayList<Double> bottomMarkerValues = new ArrayList<>();
            bottomMarkerValues.add(9.0);
            bottomMarkerValues.add(9.0);

            ArrayList<Double> topMarkerValues = new ArrayList<>();
            topMarkerValues.add(11.0);
            topMarkerValues.add(11.0);

            addMarkerLine(chart, markerDates, bottomMarkerValues);
            addMarkerLine(chart, markerDates, topMarkerValues);
        }
        

        

        chart.getStyler().setShowWithinAreaPoint(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendVisible(false);

        BufferedImage chartImage = BitmapEncoder.getBufferedImage(chart);

        try {
            document.add(Image.getInstance(chartImage, null));
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

    }

    public void addMarkerLine(XYChart chart, ArrayList<Date> startEndDate, ArrayList<Double> startEndValue) {
        if(startEndDate.size() > 2 && startEndValue.size() > 2){
            XYSeries series = chart.addSeries("Map Minimum Marker" + startEndValue.hashCode(), startEndDate, startEndValue);
            series.setSmooth(true);
            series.setMarker(SeriesMarkers.NONE);
            series.setLineWidth(0.125f);
            series.setLineColor(Color.BLACK);
            series.setShowInLegend(false);
        }
        
    }

    public void addSignalStateEvents(ChartData data) {
        try {
            document.add(getBarGraph(data, "Signal State Passage Events Per Day", "Day", "Event Count"));
        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }

    public void addSignalStateStopEvents(ChartData data) {
        try {
            document.add(getBarGraph(data, "Signal State Stop Events Per Day", "Day", "Event Count"));

        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }

    public void addLaneDirectionOfTravelEvent(ChartData data) {
        try {
            document.add(getBarGraph(data, "Lane Direction of Travel Events Per Day", "Day", "Event Count"));

        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }

    public void addConnectionOfTravelEvent(ChartData data) {
        try {
            document.add(getBarGraph(data, "Connection of Travel Events Per Day", "Day", "Event Count"));

        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }

    public void addSignalStateConflictEvent(ChartData data) {
        try {
            document.add(getBarGraph(data, "Signal State Conflict Events Per Day", "Day", "Event Count"));

        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }

    public void addSpatTimeChangeDetailsEvent(ChartData data) {
        try {
            document.add(getBarGraph(data, "Time Change Details Events Per Day", "Day", "Event Count"));

        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public void addLaneDirectionOfTravelMedianDistanceDistribution(ChartData data) {
        try {
            document.add(getBarGraph(data, "Distance from Centerline Distribution (ft)", "Feet", "Event Count"));
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public void addLaneDirectionOfTravelMedianHeadingDistribution(ChartData data) {
        try {
            document.add(getBarGraph(data, "Heading Error Distribution (deg)", "Degrees", "Event Count"));
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public Image getLineGraph(ChartData data, String title, String xAxisLabel, String yAxislabel) {
        int width = (int) (document.getPageSize().getWidth() * 0.9);

        // Create Chart
        XYChart chart = new XYChartBuilder().width(width).height(400).title("Test Report").xAxisTitle("X")
                .yAxisTitle("Y").build();

        if(data.getLabels().size() > 0 && data.getValues().size() > 0){
            XYSeries series = chart.addSeries("Fake Data", data.getLabels(), data.getValues());
        }
        

        chart.getStyler().setShowWithinAreaPoint(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendVisible(false);

        BufferedImage chartImage = BitmapEncoder.getBufferedImage(chart);

        try {
            return Image.getInstance(chartImage, null);
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
            return null;
        }

    }

    public Image getBarGraph(ChartData data, String title, String xAxisLabel, String yAxislabel) {
        int width = (int) (document.getPageSize().getWidth() * 0.9);

        CategoryChart chart = new CategoryChartBuilder()
                .width(width)
                .height(300)
                .title(title)
                .xAxisTitle(xAxisLabel)
                .yAxisTitle(yAxislabel)
                .build();

        if(data.getLabels().size() > 0 && data.getValues().size() > 0){
            CategorySeries series = chart.addSeries("series", data.getLabels(), data.getValues());
            series.setFillColor(Color.BLUE);
        }

        chart.getStyler().setShowWithinAreaPoint(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendVisible(false);

        chart.getStyler().setLabelsVisible(false);
        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setXAxisMaxLabelCount(31);
        chart.getStyler().setXAxisLabelAlignmentVertical(TextAlignment.Centre);
        chart.getStyler().setXAxisLabelRotation(90);

        BufferedImage chartImage = BitmapEncoder.getBufferedImage(chart);

        try {
            return Image.getInstance(chartImage, null);
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
            return null;
        }

    }

    public void addHeadingOverTime(List<LaneDirectionOfTravelAssessment> assessments) {
        int width = (int) (document.getPageSize().getWidth() * 0.9);
        Map<String, ArrayList<Double>> distancesFromCenterline = new HashMap<>();
        Map<String, ArrayList<Date>> timestamps = new HashMap<>();

        for (LaneDirectionOfTravelAssessment assessment : assessments) {
            for (LaneDirectionOfTravelAssessmentGroup group : assessment.getLaneDirectionOfTravelAssessmentGroup()) {
                String hash = "Lane: " + group.getLaneID() + " Segment: " + group.getSegmentID();
                if (distancesFromCenterline.containsKey(hash)) {
                    distancesFromCenterline.get(hash).add(group.getMedianHeading() - group.getExpectedHeading());
                    timestamps.get(hash).add(Date.from(Instant.ofEpochMilli(assessment.getTimestamp())));
                } else {
                    ArrayList<Double> distances = new ArrayList<>();
                    distances.add(group.getMedianHeading() - group.getExpectedHeading());
                    distancesFromCenterline.put(hash, distances);

                    ArrayList<Date> times = new ArrayList<>();
                    times.add(Date.from(Instant.ofEpochMilli(assessment.getTimestamp())));
                    timestamps.put(hash, times);
                }
            }
        }

        XYChart chart = new XYChartBuilder().width(width).height(400).title("Vehicle Heading Error Delta")
                .xAxisTitle("Time")
                .yAxisTitle("Heading Delta (Degrees)").build();

        if (assessments.size() > 0) {
            Date minDate = Date.from(Instant.ofEpochMilli(assessments.get(0).getTimestamp()));
            Date maxDate = Date.from(Instant.ofEpochMilli(assessments.get(assessments.size() - 1).getTimestamp()));
            for (String key : distancesFromCenterline.keySet()) {
                ArrayList<Double> distances = distancesFromCenterline.get(key);
                ArrayList<Date> times = timestamps.get(key);

                distances.add(0, distances.get(0));
                times.add(0, minDate);

                distances.add(distances.size(), distances.get(distances.size() - 1));
                times.add(maxDate);

                if(times.size() > 0 && distances.size() > 0){
                    XYSeries series = chart.addSeries(key, times, distances);
                    series.setSmooth(true);
                    series.setMarker(SeriesMarkers.NONE);
                }
            }
        }

        chart.getStyler().setShowWithinAreaPoint(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setLegendPosition(LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(LegendLayout.Horizontal);

        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setXAxisMaxLabelCount(31);
        chart.getStyler().setXAxisLabelAlignmentVertical(TextAlignment.Centre);
        chart.getStyler().setXAxisLabelRotation(90);

        BufferedImage chartImage = BitmapEncoder.getBufferedImage(chart);

        try {
            document.add(Image.getInstance(chartImage, null));
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
    }

    public void addDistanceFromCenterlineOverTime(List<LaneDirectionOfTravelAssessment> assessments) {
        int width = (int) (document.getPageSize().getWidth() * 0.9);
        Map<String, ArrayList<Double>> distancesFromCenterline = new HashMap<>();
        Map<String, ArrayList<Date>> timestamps = new HashMap<>();

        for (LaneDirectionOfTravelAssessment assessment : assessments) {
            for (LaneDirectionOfTravelAssessmentGroup group : assessment.getLaneDirectionOfTravelAssessmentGroup()) {
                String hash = "Lane: " + group.getLaneID() + " Segment: " + group.getSegmentID();
                if (distancesFromCenterline.containsKey(hash)) {
                    distancesFromCenterline.get(hash).add(group.getMedianCenterlineDistance());
                    timestamps.get(hash).add(Date.from(Instant.ofEpochMilli(assessment.getTimestamp())));
                } else {
                    ArrayList<Double> distances = new ArrayList<>();
                    distances.add(group.getMedianCenterlineDistance());
                    distancesFromCenterline.put(hash, distances);

                    ArrayList<Date> times = new ArrayList<>();
                    times.add(Date.from(Instant.ofEpochMilli(assessment.getTimestamp())));
                    timestamps.put(hash, times);
                }
            }
        }

        XYChart chart = new XYChartBuilder().width(width).height(400).title("Distance From Centerline")
                .xAxisTitle("Time")
                .yAxisTitle("Distance from Centerline (cm)").build();

        if (assessments.size() > 0) {
            Date minDate = Date.from(Instant.ofEpochMilli(assessments.get(0).getTimestamp()));
            Date maxDate = Date.from(Instant.ofEpochMilli(assessments.get(assessments.size() - 1).getTimestamp()));
            for (String key : distancesFromCenterline.keySet()) {
                ArrayList<Double> distances = distancesFromCenterline.get(key);
                ArrayList<Date> times = timestamps.get(key);

                distances.add(0, distances.get(0));
                times.add(0, minDate);

                distances.add(distances.size(), distances.get(distances.size() - 1));
                times.add(maxDate);

                if(times.size() > 0 && distances.size() > 0){
                    XYSeries series = chart.addSeries(key, times, distances);
                series.setSmooth(true);
                series.setMarker(SeriesMarkers.NONE);
                }
                
            }
        }

        chart.getStyler().setShowWithinAreaPoint(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setLegendPosition(LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(LegendLayout.Horizontal);

        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setXAxisMaxLabelCount(31);
        chart.getStyler().setXAxisLabelAlignmentVertical(TextAlignment.Centre);
        chart.getStyler().setXAxisLabelRotation(90);

        BufferedImage chartImage = BitmapEncoder.getBufferedImage(chart);

        try {
            document.add(Image.getInstance(chartImage, null));
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }

    }

    public void addLaneConnectionOfTravelMap(List<LaneConnectionCount> laneConnectionCounts) {

        Set<Integer> ingressLanes = new HashSet<>();
        Set<Integer> egressLanes = new HashSet<>();
        Map<String, Integer> laneLookup = new HashMap<>();

        for (LaneConnectionCount count : laneConnectionCounts) {
            ingressLanes.add(count.getIngressLaneID());
            egressLanes.add(count.getEgressLaneID());
            laneLookup.put(count.getIngressLaneID() + "_" + count.getEgressLaneID(), count.getCount());
        }

        Integer[] ingressLaneLabels = new Integer[ingressLanes.size()];
        ingressLaneLabels = ingressLanes.toArray(ingressLaneLabels);

        Integer[] egressLaneLabels = new Integer[egressLanes.size()];
        egressLaneLabels = egressLanes.toArray(egressLaneLabels);

        Arrays.sort(ingressLaneLabels);
        Arrays.sort(egressLaneLabels);

        int[][] pairMappings = new int[ingressLaneLabels.length][egressLaneLabels.length];

        for (int i = 0; i < ingressLaneLabels.length; i++) {
            for (int j = 0; j < egressLaneLabels.length; j++) {
                int ingressLane = ingressLaneLabels[i];
                int egressLane = egressLaneLabels[j];
                String hash = ingressLane + "_" + egressLane;
                if (laneLookup.containsKey(hash)) {
                    pairMappings[i][j] = laneLookup.get(hash);
                }

            }
        }

        int[] ingressLaneLabelsInt = Arrays.stream(ingressLaneLabels).mapToInt(Integer::intValue).toArray();
        int[] egressLaneLabelsInt = Arrays.stream(egressLaneLabels).mapToInt(Integer::intValue).toArray();

        int width = (int) (document.getPageSize().getWidth() * 0.9);

        HeatMapChart chart = new HeatMapChartBuilder().width(width).height(600).title("Ingress Egress Lane Pairings")
                .xAxisTitle("Ingress Lane ID").yAxisTitle("Egress Lane ID").build();

        chart.getStyler().setPlotContentSize(1);
        chart.getStyler().setShowValue(true);

        chart.addSeries("Ingress, Egress Lane Pairings", ingressLaneLabelsInt, egressLaneLabelsInt, pairMappings);

        chart.getStyler().setShowWithinAreaPoint(false);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setLegendVisible(false);

        chart.getStyler().setPlotGridLinesVisible(false);

        // Color[] rangeColors = {Color.WHITE, Color.BLUE, Color.GREEN, Color.YELLOW,
        // Color.ORANGE, Color.RED};
        // chart.getStyler().setRangeColors(rangeColors);

        BufferedImage chartImage = BitmapEncoder.getBufferedImage(chart);
        Image iTextImage;
        try {
            iTextImage = Image.getInstance(chartImage, null);
            document.add(iTextImage);
        } catch (IOException | DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getZonedDateTimeDayString(ZonedDateTime zonedDateTime) {
        return zonedDateTime.format(dayFormatter);
    }

    public String getZonedSecondsString(ZonedDateTime zonedDateTime) {
        return zonedDateTime.format(secondsFormatter);
    }

    public ZonedDateTime utcMillisToDay(long utcMillis) {
        ZonedDateTime day = ZonedDateTime.ofInstant(Instant.ofEpochMilli(utcMillis), ZoneOffset.UTC);

        ZonedDateTime dayStart = ZonedDateTime.of(day.getYear(), day.getMonthValue(), day.getDayOfMonth(), 0, 0, 0, 0,
                ZoneOffset.UTC);
        return dayStart;
    }

    public ZonedDateTime utcSecondsToDay(long utcMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(utcMillis / 1000), ZoneOffset.UTC);
    }

    public String utcMillisToDayString(long utcMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(utcMillis / 1000), ZoneOffset.UTC).format(dayFormatter);
    }

    public List<String> getDayStringsInRange(long startTimeMillis, long endTimeMillis) {
        ZonedDateTime startDayTime = utcMillisToDay(startTimeMillis);
        ZonedDateTime endDayTime = utcMillisToDay(endTimeMillis);

        List<String> dateRange = new ArrayList<>();

        int daysAdded = 0;

        while (startDayTime.plusDays(daysAdded).compareTo(endDayTime) <= 0) {
            dateRange.add(getZonedDateTimeDayString(startDayTime.plusDays(daysAdded)));
            daysAdded += 1;
        }

        return dateRange;
    }

    public List<Long> getSecondsStringInRange(long startTimeMillis, long endTimeMillis) {
        long time = startTimeMillis / 1000;
        long endTime = endTimeMillis / 1000;
        List<Long> secondRange = new ArrayList<>();

        while (time < endTime) {
            secondRange.add(time);
            time += 3600;
        }

        return secondRange;
    }

}
