(ns petrarch.db.core
  (:require [clojure.java.jdbc :as jdbc]
            [clj-postgresql.core :as pg]
            [clj-postgresql.spatial :as geo])
  (:import [org.postgis PGgeometry Point PGgeometryLW]))

(def db
  (delay
    (pg/pool
      :host "127.0.0.1"
      :user "dev"
      :dbname "petrarch"
      :password "")))

(defn insert-point!
  "puts a point into the sql database"
  [point]
  (let [ts (:time point)
        altitude (:alt point)
        accuracy (:acc point)
        speed (:speed point)
        point (geo/point (:long point) (:lat point))]
    (jdbc/execute! @db ["INSERT INTO locations (timestamp, point, altitude, accuracy, speed)
                         VALUES (?::timestamptz, ?::geometry, ?::real, ?::real, ?::real)"
                        (java.sql.Timestamp. (.getTime (java.util.Date. ts)))
                        point
                        altitude
                        accuracy
                        speed])))

(defn get-points-in-region
  "retrieves all points in a circle from point & radius"
  [center-point radius]
  (jdbc/query @db ["SELECT timestamp
                     FROM locations
                     WHERE ST_DWithin(
                       point,
                       ?::geometry,
                       ?::real
                     );"
                   (geo/point (:lat center-point) (:long center-point))
                   radius
                   ]))

(defn get-500-points
  "retrieves 500 points - testing"
  []
  (jdbc/query @db ["SELECT point FROM locations LIMIT 500"]))
