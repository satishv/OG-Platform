/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.examples.marketdata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.time.calendar.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import au.com.bytecode.opencsv.CSVReader;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundleWithDates;
import com.opengamma.id.ExternalIdWithDates;
import com.opengamma.master.historicaltimeseries.HistoricalTimeSeriesInfoDocument;
import com.opengamma.master.historicaltimeseries.ManageableHistoricalTimeSeriesInfo;
import com.opengamma.masterdb.historicaltimeseries.DbHistoricalTimeSeriesMaster;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.PlatformConfigUtils;
import com.opengamma.util.PlatformConfigUtils.RunMode;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.timeseries.localdate.LocalDateDoubleTimeSeries;
import com.opengamma.util.timeseries.localdate.MapLocalDateDoubleTimeSeries;
import com.opengamma.util.tuple.Pair;

/**
 * An ultra-simple historical data simulator, we load the initial values from a CSV file (with a header row)
 * and the format 
 * <identification-scheme>, <identifier-value>, <datafield>, <value>
 */
public class SimulatedHistoricalDataGenerator {
  
  /**
   * OG Simulated data provider name
   */
  public static final String OG_DATA_PROVIDER = "OG_DATA_PROVIDER";

  /**
   * OG Simulated data source name
   */
  public static final String OG_DATA_SOURCE = "OG_DATA_SOURCE";

  private static final Logger s_logger = LoggerFactory.getLogger(SimulatedHistoricalDataGenerator.class);
  
  private final DbHistoricalTimeSeriesMaster _htsMaster;
  private Map<Pair<ExternalId, String>, Double> _initialValues = new HashMap<Pair<ExternalId, String>, Double>();

  private static final int NUM_FIELDS = 4;
  private static final double SCALING_FACTOR = 0.005; // i.e. 0.5% * 1SD
  private static final int TS_LENGTH = 2; // length of timeseries in years
  
  public SimulatedHistoricalDataGenerator(DbHistoricalTimeSeriesMaster htsMaster, Resource initialValuesFile) {
    ArgumentChecker.notNull(htsMaster, "htsMaster");
    ArgumentChecker.notNull(initialValuesFile, "initialValuesFile");
    _htsMaster = htsMaster;
    readInitialValues(initialValuesFile);
  }
  
  public void readInitialValues(Resource initialValuesFile) {
    try {
      CSVReader reader = new CSVReader(new BufferedReader(new InputStreamReader(initialValuesFile.getInputStream())));
      // Read header row
      @SuppressWarnings("unused")
      String[] headers = reader.readNext();
      String[] line;
      int lineNum = 1;
      while ((line = reader.readNext()) != null) {
        lineNum++;
        if (line.length != NUM_FIELDS) {
          s_logger.error("Not enough fields in CSV on line " + lineNum);
        } else {
          String scheme = line[0];
          String identifier = line[1];
          String fieldName = line[2];
          String valueStr = line[3];
          Double value = Double.parseDouble(valueStr);
          ExternalId id = ExternalId.of(scheme, identifier);
          _initialValues.put(Pair.of(id, fieldName), value);
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }    
  }
  
  public void run() {
    Random random = new Random(); // noMarket need for SecureRandom here..
    StringBuilder buf = new StringBuilder("loading ").append(_initialValues.size()).append(" timeseries");
    for (Entry<Pair<ExternalId, String>, Double> entry : _initialValues.entrySet()) {
      ExternalId identifier = entry.getKey().getFirst();
      String dataField = entry.getKey().getSecond();
      Double startValue = entry.getValue();
      ManageableHistoricalTimeSeriesInfo info = new ManageableHistoricalTimeSeriesInfo();
      info.setName(dataField + " " + identifier);
      info.setDataField(dataField);
      info.setDataSource(OG_DATA_SOURCE);
      info.setDataProvider(OG_DATA_PROVIDER);
      info.setObservationTime("LONDON_CLOSE");
      ExternalIdWithDates id = ExternalIdWithDates.of(identifier, null, null);
      ExternalIdBundleWithDates bundle = ExternalIdBundleWithDates.of(id);
      info.setExternalIdBundle(bundle);
      buf.append("\t").append(identifier).append(" ").append(dataField).append("\n");
      HistoricalTimeSeriesInfoDocument addedDoc = _htsMaster.add(new HistoricalTimeSeriesInfoDocument(info));
      LocalDateDoubleTimeSeries timeSeries = getHistoricalDataPoints(random, startValue, TS_LENGTH);
      _htsMaster.updateTimeSeriesDataPoints(addedDoc.getInfo().getTimeSeriesObjectId(), timeSeries);
    }
    s_logger.info(buf.toString());
  }
  
  private LocalDateDoubleTimeSeries getHistoricalDataPoints(Random random, Double startValue, int tsLength) {
    MapLocalDateDoubleTimeSeries result = new MapLocalDateDoubleTimeSeries();
    LocalDate date = DateUtils.previousWeekDay(LocalDate.now().minusYears(tsLength));
    double currentValue = startValue;
    do {
      currentValue = wiggleValue(random, currentValue);
      result.putDataPoint(date, currentValue);
      date = DateUtils.nextWeekDay(date);
    } while (date.isBefore(LocalDate.now()));
    return result;
  }

  private double wiggleValue(Random random, double value) {
    double result = value + (random.nextGaussian() * (value * SCALING_FACTOR));
    //s_logger.warn("wiggleValue = {}", result);
    return result;
  }
  
  //-------------------------------------------------------------------------
  /**
   * Sets up and loads the database.
   * <p>
   * This loader requires a Spring configuration file that defines the security,
   * position and portfolio masters, together with an instance of this bean
   * under the name "simulatedHistoricalDataGenerator".
   * 
   * @param args  the arguments, unused
   */
  public static void main(String[] args) {  // CSIGNORE
    try {
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(lc);
      lc.reset(); 
      configurator.doConfigure("src/com/opengamma/examples/server/logback.xml");
      
      // Set the run mode to EXAMPLE so we use the HSQLDB example database.
      PlatformConfigUtils.configureSystemProperties(RunMode.EXAMPLE);
      System.out.println("Starting connections");
      AbstractApplicationContext appContext = new ClassPathXmlApplicationContext("demoPortfolioLoader.xml");
      appContext.start();
      
      try {
        SimulatedHistoricalDataGenerator loader = appContext.getBean("simulatedHistoricalDataGenerator", SimulatedHistoricalDataGenerator.class);
        System.out.println("Loading data");
        loader.run();
      } finally {
        appContext.close();
      }
      System.out.println("Finished");
      
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    System.exit(0);
  }
}
