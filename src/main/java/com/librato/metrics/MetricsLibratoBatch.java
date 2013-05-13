package com.librato.metrics;

import com.codahale.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * User: mihasya
 * Date: 6/17/12
 * Time: 10:57 PM
 * a LibratoBatch that understand Metrics-specific types
 */
public class MetricsLibratoBatch extends LibratoBatch {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsLibratoBatch.class);

    /**
     * a string used to identify the library
     */
    private static final String agentIdentifier;

    static {
        InputStream pomIs = null;
        BufferedReader b = null;
        String version = "unknown";
        try {
            pomIs = LibratoReporter.class.getClassLoader().getResourceAsStream("META-INF/maven/com.librato.metrics/metrics-librato/pom.properties");
            b = new BufferedReader(new InputStreamReader(pomIs));
            String line = b.readLine();
            while (line != null)  {
                if (line.startsWith("version")) {
                    version = line.split("=")[1];
                    break;
                }
                line = b.readLine();
            }
        } catch (Throwable e) {
            LOG.error("Failure reading package version for librato-java", e);
        }

        // now coda!
        String codaVersion = "unknown";
        try {
            pomIs = Metric.class.getClassLoader().getResourceAsStream("META-INF/maven/com.codahale.metrics/metrics-core/pom.properties");
            b = new BufferedReader(new InputStreamReader(pomIs));
            String line = b.readLine();
            while (line != null)  {
                if (line.startsWith("version")) {
                    codaVersion = line.split("=")[1];
                    break;
                }
                line = b.readLine();
            }
        } catch (Throwable e) {
            LOG.error("Failure reading package version for librato-java", e);
        }

        agentIdentifier = String.format("metrics-librato/%s metrics/%s", version, codaVersion);
    }

    public MetricsLibratoBatch(int postBatchSize, APIUtil.Sanitizer sanitizer, long timeout, TimeUnit timeoutUnit) {
        super(postBatchSize, sanitizer, timeout, timeoutUnit, agentIdentifier);
    }

    public void addSampling(String name, Sampling sampling) {
        Snapshot snapshot = sampling.getSnapshot();
        addMeasurement(new SingleValueGaugeMeasurement(name+".median", snapshot.getMedian()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".75th", snapshot.get75thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".95th", snapshot.get95thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".98th", snapshot.get98thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".99th", snapshot.get99thPercentile()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".999th", snapshot.get999thPercentile()));
        Double sum = snapshot.size() * snapshot.getMean();
        addMeasurement(new MultiSampleGaugeMeasurement(
                name,
                (long) snapshot.size(),
                sum,
                snapshot.getMax(),
                snapshot.getMin(),
                null
        ));
    }

    public void addMetered(String name, Metered meter) {
        // TODO: possible that 1/5/15min rates can be calculated server-side
        addMeasurement(new CounterMeasurement(name+".count", meter.getCount()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".meanRate", meter.getMeanRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".1MinuteRate", meter.getOneMinuteRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".5MinuteRate", meter.getFiveMinuteRate()));
        addMeasurement(new SingleValueGaugeMeasurement(name+".15MinuteRate", meter.getFifteenMinuteRate()));
    }
}
