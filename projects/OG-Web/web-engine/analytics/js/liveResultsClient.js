/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * Provides live results to the web client. Maintains a continuous request/response cycle.
 */
(function($) {
  
  /** @constructor */
  function LiveResultsClient() {
    
    var self = this;
    
    var _logger = new Logger("LiveResultsClient", "debug");
    var _cometd = $.cometd;
    
    var _isConnected = false;
    
    var _inBatch = false;
    var _batchMetadata = null;
    var _batchPortfolioUpdate;
    var _batchPrimitivesUpdate;
    
    var _primitivesEventHandler = null;
    var _portfolioEventHandler = null;
    
    var _awaitingImmediateUpdate = false;
    var _immediateUpdateNextCycle = false;
    
    var _changingView = false;
    var _isRunning = false;
    var _gridStructure;
    
    function init() {
      // Kick everything off
      var cometURL = location.protocol + "//" + location.host + config.contextPath + "/cometd";
      _cometd.configure({
        url : cometURL,
        logLevel : 'warn'
      });
      _cometd.addListener('/meta/connect', handleMetaConnect);
    }
    
    //-----------------------------------------------------------------------
    // Response handlers
    
    function handleMetaConnect(message) {
      if (!_isConnected && message.successful) {
        _isConnected = true;
        _cometd.batch(function() {
          _cometd.subscribe('/status', handleStatusUpdate);
          _cometd.subscribe('/views', handleViewListUpdate);
          _cometd.subscribe('/initialize', handleViewInitialized);          
          
          _cometd.subscribe('/updates/portfolio', handlePortfolioUpdate);
          _cometd.subscribe('/updates/primitives', handlePrimitivesUpdate);
          
          // Updates to the columns
          _cometd.subscribe('/gridStructure/portfolio/columns', handlePortfolioGridColumns);
          _cometd.subscribe('/gridStructure/primitives/columns', handlePrimitivesGridColumns);
          
          _cometd.subscribe('/updates/control/**', handleUpdatesControlMessage);
        });
        self.onConnected.fire();
      } else if (_isConnected && !message.successful) {
        _isConnected = false;
        self.onDisconnected.fire();
      }
    }
    
    function handleStatusUpdate(message) {
      if (_changingView) {
        // An update from the last view
        return;
      }
      
      self.onStatusUpdateReceived.fire(message.data);
      
      var isRunning = message.data.isRunning;
      if (_isRunning != isRunning) {
        _isRunning = isRunning;
        if (isRunning) {
          // Kick off the update cycle
          sendUpdateRequest(true);
        }
      }
    }
    
    function handleViewListUpdate(message) {
      self.onViewListReceived.fire(message.data.availableViewNames);
    }
    
    function handleViewInitialized(message) {
      _changingView = false;
      
      if (message.data.primitives) {
        assignFormatters(message.data.primitives.columns);
      }
      if (message.data.portfolio) {
        assignFormatters(message.data.portfolio.columns);
      }
      
      _gridStructures = message.data;
      self.onViewInitialized.fire(_gridStructures);
    }
    
    function assignFormatters(columns) {
      $.each(columns, function(index, column) {
        column.typeFormatter = ColumnFormatter.getTypeFormatter(column.dataType);
      });
    }
    
    function handlePortfolioGridColumns(message) {
      assignFormatters(message.data);
      $.extend(true, _gridStructures.portfolio.columns, message.data);
    }
    
    function handlePrimitivesGridColumns(message) {
      assignFormatters(message.data);
      $.extend(true, _gridStructures.primitives.columns, message.data);
    }
    
    function handleUpdatesControlMessage(message) {
      if (message.channel == '/updates/control/start') {
        if (_inBatch) {
          _logger.warn('Already in batch when received start message');
        }
        _inBatch = true;
        _batchMetadata = message.data;
        _batchPortfolioUpdate = new Array();
        _batchPrimitivesUpdate = new Array();
      } else if (message.channel == '/updates/control/end') {
        if (!_inBatch) {
          _logger.warn('Batch flag already cleared before END message');
        }
        
        handleUpdate(_primitivesEventHandler, _batchPrimitivesUpdate, _batchMetadata.timestamp, _batchMetadata.latency);
        handleUpdate(_portfolioEventHandler, _batchPortfolioUpdate, _batchMetadata.timestamp, _batchMetadata.latency);
        
        self.afterUpdateReceived.fire(_batchMetadata);
        
        _inBatch = false;
        _batchMetadata = null;
        _batchPortfolioUpdate = null;
        _batchPrimitivesUpdate = null;
        _awaitingImmediateUpdate = false;

        if (_isRunning) {
          if (_immediateUpdateNextCycle) {
            _immediateUpdateNextCycle = false;
            sendUpdateRequest(true);
          } else {
            sendUpdateRequest(false);
          }
        }
      }
    }
    
    function handleUpdate(eventHandler, update, timestamp, latency) {
      if (!eventHandler) {
        return;
      }
      eventHandler.updateReceived(update, timestamp, latency);
    }
    
    function handlePortfolioUpdate(message) {
      if (!_inBatch) {
        _logger.warn('Update received when not in batch');
      }
      _batchPortfolioUpdate.push(message.data);
    }
    
    function handlePrimitivesUpdate(message) {
      if (!_inBatch) {
        _logger.warn('Update received when not in batch');
      }
      _batchPrimitivesUpdate.push(message.data);
    }
    
    //-----------------------------------------------------------------------
    // Request senders
        
    function sendUpdateRequest(immediateResponse) {
      if (_awaitingImmediateUpdate) {
        // Already waiting for an immediate update - queue another one
        // Prevents the client from being flooded with update responses
        _immediateUpdateNextCycle = true;
        return;
      }
      
      if (immediateResponse) {
        _awaitingImmediateUpdate = true;
      }
      
      var updateMetadata = {
          portfolioViewport : {},
          primitiveViewport : {},
      }
      
      // Get the UI components to complete the metadata
      self.beforeUpdateRequested.fire(updateMetadata);
      
      // Send the request
      updateMetadata.immediateResponse = immediateResponse;
      _cometd.publish('/service/updates', updateMetadata);
    }
    
    function sendUpdateMode(gridName, rowId, colId, mode) {
      _cometd.publish('/service/updates/mode', {
        gridName: gridName,
        rowId: rowId,
        colId: colId,
        mode: mode
      });
    }
    
    //-----------------------------------------------------------------------
    // Public API
    
    this.changeView = function(viewName) {
      _logger.info("Sending initialize request");
      _portfolioDetails = null;
      _primitiveDetails = null;
      _changingView = true;
      _isRunning = false;
      
      _cometd.publish('/service/initialize', {
        viewName : viewName
      });
    }
    
    this.pause = function() {
      _cometd.publish('/service/currentview/pause', {});
    }
    
    this.resume = function() {
      _cometd.publish('/service/currentview/resume', {});
    }
    
    this.getViews = function() {
      _cometd.publish('/service/views', {});
    }
    
    this.triggerImmediateUpdate = function() {
      sendUpdateRequest(true);
    }
    
    this.startDetailedCellUpdates = function(gridName, rowId, colId) {
      sendUpdateMode(gridName, rowId, colId, "FULL");
    }
    
    this.stopDetailedCellUpdates = function(gridName, rowId, colId) {
      sendUpdateMode(gridName, rowId, colId, "SUMMARY");
    }
    
    this.connect = function() {
      _cometd.handshake();
    }
    
    this.disconnect = function() {
      _cometd.disconnect();
    }
    
    this.onConnected = new EventManager();
    this.onDisconnected = new EventManager();
    
    /**
     * Fired before a update is requested to allow UI components to contribute to the request, for example by inserting
     * viewport details. This way, multiple UI components can consume the same type of data while maintaining requests
     * for the minimum possible set of data.
     */
    this.beforeUpdateRequested = new EventManager();
    
    this.onViewListReceived = new EventManager();
    this.onViewInitialized = new EventManager();
    this.onStatusUpdateReceived = new EventManager();
    this.afterUpdateReceived = new EventManager();
    
    this.setPortfolioEventHandler = function(portfolioEventHandler) {
      _portfolioEventHandler = portfolioEventHandler;
    }
    
    this.setPrimitivesEventHandler = function(primitivesEventHandler) {
      _primitivesEventHandler = primitivesEventHandler;
    }
    
    //-----------------------------------------------------------------------
    
    init();
  }
    
  $.extend(true, window, {
      LiveResultsClient : LiveResultsClient
  });
    
}(jQuery));