/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

package com.opengamma.financial.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.opengamma.OpenGammaRuntimeException;

import au.com.bytecode.opencsv.CSVReader;

/**
 * A class to facilitate importing portfolio data from comma-separated value files
 */
public class CsvSheetReader extends SheetReader {

  private CSVReader _csvReader;

  public CsvSheetReader(InputStream inputStream, String[] columns) {
    
    // Set up CSV reader
    _csvReader = new CSVReader(new InputStreamReader(inputStream));
    
    // Set columns
    setColumns(columns);
  }
  
  public CsvSheetReader(InputStream inputStream) {
    
    // Set up CSV reader
    _csvReader = new CSVReader(new InputStreamReader(inputStream));
    
    // Read in the header row
    String[] rawRow;
    try {
      rawRow = _csvReader.readNext();
    } catch (IOException ex) {
      throw new OpenGammaRuntimeException("Error reading CSV file header row: " + ex.getMessage());
    }
    
    // Normalise read-in headers (to lower case) and set as columns
    String[] columns = new String[rawRow.length];
    for (int i = 0; i < rawRow.length; i++) {
      columns[i] = rawRow[i].trim().toLowerCase();
    }
    setColumns(columns);
  }

  @Override
  public Map<String, String> loadNextRow() {
    
    // Read in next row
    String[] rawRow;
    try {
      rawRow = _csvReader.readNext();
    } catch (IOException ex) {
      throw new OpenGammaRuntimeException("Error reading CSV file data row: " + ex.getMessage());
    }

    // Return null if EOF
    if (rawRow == null) {
      return null;
    }
    
    // Map read-in row onto expected columns
    Map<String, String> result = new HashMap<String, String>();
    for (int i = 0; i < getColumns().length; i++) {
      if (i >= rawRow.length) {
        break;
      }
      result.put(getColumns()[i], rawRow[i]);
    }

    return result;
  }

}
