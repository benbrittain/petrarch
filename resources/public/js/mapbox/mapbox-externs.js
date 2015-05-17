"use strict";
/**
 *  * Leaflet map.
 *   *
 *    * @see http://leaflet.cloudmade.com/
 *     * @namespace
 *      * @externs
 *       */
var L = {};

/**
 *   * @constructor
 *     * @param {string} id
 *       * @param {Object} options
 *         */
L.mapbox.map = function(id, options){};

/**
 *   * @param {L.Layer} layer
 *     */
L.Map.prototype.addLayer = function(layer){};
/**
 *   * @param {L.Layer} layer
 *     */
L.Map.prototype.removeLayer = function(layer){};
L.Map.prototype.setView = function(a, b){};

/** @constructor */
L.LatLng = function(a, b){};

/**
 *  * @constructor
 *   * @param {L.LatLng} latLng
 *    * @param {Object} options
 *     **/
L.Marker = function(latLng, options){};
L.Marker.prototype.bindPopup = function(a){};
L.Marker.prototype.openPopup = function(){};

/** @constructor */
L.TileLayer = function(a, b){};

/** @constructor */
L.Icon = function(a){};
L.Icon.prototype.iconSize = null;
L.Icon.prototype.shadowSize = null;
L.Icon.prototype.iconAnchor = null;
L.Icon.prototype.popupAnchor = null;

/** @constructor */
L.Point = function(a, b){};

/** @constructor */
L.Layer = function () {};

/**
 *   * @constructor
 *     * @extends {L.Layer}
 *       */
L.StamenTileLayer = function (name) {};

L.Util = {};

/**
 *  * @param {Object} obj
 *   * @param {Object} options
 *    */
L.Util.setOptions = function (obj, options) {};
