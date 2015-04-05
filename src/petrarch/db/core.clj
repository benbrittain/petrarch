(ns petrarch.db.core
  (:require [clojure.java.jdbc :as jdbc] [clj-postgresql.core :as pg]
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
        point (geo/point (:lat point) (:long point))]
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
  ; TODO make the LIMIT a sample of the points
  (jdbc/query @db ["SELECT point
                     FROM locations
                     WHERE ST_DWithin(
                       point,
                       ?::geography,
                       ?::real
                     ) LIMIT 1000"
                   (geo/point (:lat center-point) (:long center-point))
                   radius
                   ]))

(defn get-1000-points
  "retrieves 1000 points - testing"
  []
  (jdbc/query @db ["SELECT point FROM locations LIMIT 1000"]))
