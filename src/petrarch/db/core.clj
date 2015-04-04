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

(defn insert-point! [point]
  (let [ts (:time point)
        altitude (:alt point)
        accuracy (:acc point)
        speed (:speed point)
        point (geo/point (:long point) (:lat point))]
    (jdbc/execute! @db ["INSERT INTO locations (timestamp, point, altitude, accuracy, speed)
                        VALUES (?::timestamptz, ?::geometry, ?::int, ?::int, ?::int)"
                        (java.sql.Timestamp. (.getTime (java.util.Date. ts)))
                        point
                        altitude
                        accuracy
                        speed])))
