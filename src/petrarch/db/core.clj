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

(defn insert-entry!
  "creates an entry in the sql database"
  [title post-text location]
  (jdbc/execute! @db ["INSERT INTO entries (title, timestamp, point, post)
                      VALUES (?::text, ?::timestamptz, ?::geometry, ?::text)"
                      title
                      (java.sql.Timestamp. (.getTime (java.util.Date.)))
                      (geo/point (:lat location) (:long location))
                      post-text]))

(defn get-entry
  "retrieves the text of a specific entry"
  [id]
  (jdbc/query @db ["SELECT post
                     FROM entries
                     WHERE id = ?::int"
                   id]))

(defn get-entries
  "retrieve the indexes, title, ts & location of the entries"
  []
  (jdbc/query @db ["SELECT id, title, timestamp, point
                   FROM entries
                   ORDER BY id DESC; "]))

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
                     )
                   ORDER BY timestamp DESC,
                            ST_Distance(point, ?::geography) DESC
                   LIMIT 3000;"
                   (geo/point (:lat center-point) (:long center-point))
                   radius
                   (geo/point (:lat center-point) (:long center-point))
                   ]))

(defn get-1000-points
  "retrieves 1000 points - testing"
  []
  (jdbc/query @db ["SELECT point FROM locations LIMIT 1000"]))
