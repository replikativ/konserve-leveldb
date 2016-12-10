(ns konserve-leveldb.core
  (:require [clj-leveldb :as level :refer [create-db stats]]
            [clojure.core.async :as async :refer [<!! chan close! go put!]]
            [clojure.java.io :as io]
            [hasch.core :refer [uuid]]
            [konserve
             [protocols :refer [-bassoc -bget -deserialize -exists? -get-in -serialize -update-in
                                PBinaryAsyncKeyValueStore PEDNAsyncKeyValueStore]]
             [serializers :as ser]]
            [konserve.core :as k])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defrecord LevelDBStore [ldb serializer read-handlers write-handlers locks]
  PEDNAsyncKeyValueStore
  (-exists? [this key]
    (let [id (str (uuid key))
          res (chan)]
      (put! res (not= (level/get ldb id) nil))
      (close! res)
      res))


  (-get-in [this key-vec]
    (let [[fkey & rkey] key-vec
          id (str (uuid fkey))
          val (level/get ldb id)]
      (if (= val nil)
        (go nil)
        (let [res-ch (chan)]
          (try
            (let [bais (ByteArrayInputStream. val)]
              (put! res-ch
                    (get-in
                     (second (-deserialize serializer read-handlers bais))
                     rkey)))
            res-ch
            (catch Exception e
              (put! res-ch (ex-info "Could not read key."
                                   {:type :read-error
                                    :key fkey
                                    :exception e}))
              res-ch)
            (finally
              (close! res-ch)))))))

  (-update-in [this key-vec up-fn]
    (let [[fkey & rkey] key-vec
          id (str (uuid fkey))]
      (let [res-ch (chan)]
        (try
          (let [old-bin (level/get ldb id)
                old (when old-bin
                      (let [bais (ByteArrayInputStream. old-bin)]
                        (second (-deserialize serializer write-handlers bais))))
                new (if (empty? rkey)
                      (up-fn old)
                      (update-in old rkey up-fn))]
            (if new
              (let [baos (ByteArrayOutputStream.)]
                (-serialize serializer baos write-handlers [key-vec new])
                (level/put ldb id (.toByteArray baos)))
              (level/delete ldb id))
            (put! res-ch [(get-in old rkey)
                          (get-in new rkey)]))
          res-ch
          (catch Exception e
            (put! res-ch (ex-info "Could not write key."
                                  {:type :write-error
                                   :key fkey
                                   :exception e}))
            res-ch)
          (finally
            (close! res-ch))))))

  PBinaryAsyncKeyValueStore
  (-bget [this key locked-cb]
    (let [id (str (uuid key))
          val (level/get ldb id)]
      (if (nil? val)
        (go nil)
        (go
          (try
            (let [bais (ByteArrayInputStream. val)]
              (locked-cb {:input-stream bais
                          :size (count val)}))
            (catch Exception e
              (ex-info "Could not read key."
                       {:type :read-error
                        :key key
                        :exception e})))))))

  (-bassoc [this key input]
    (let [id (str (uuid key))]
      (go
        (try
          (level/put ldb id input)
          nil
          (catch Exception e
            (ex-info "Could not write key."
                     {:type :write-error
                      :key key
                      :exception e})))))))

(defn new-leveldb-store
  [path & {:keys [leveldb-opts serializer read-handlers write-handlers]
           :or {serializer (ser/fressian-serializer)
                read-handlers (atom {})
                write-handlers (atom {})
                leveldb-opts {}}}]
  (go (try
        (level/repair-db path)
        (let [db (create-db path leveldb-opts)]
          (map->LevelDBStore {:ldb db
                              :read-handlers read-handlers
                              :write-handlers write-handlers
                              :serializer serializer
                              :locks (atom {})}))
        (catch Exception e
          e))))

(defn release [{:keys [ldb] :as store}]
  (try
    (do
      (-> ldb :db .close)
      true)
    (catch Exception e
      e)))
  

(comment

  (def store (<!! (new-leveldb-store "/tmp/kleveldb3")))

  (require '[konserve.core :as k])

  (get (:ldb store) (str (uuid "foo")))

  (.close (-> store :ldb :db))
  
  (-> store :ldb :db (.getProperty "leveldb.stats"))

  (:locks store)

  (close-leveldb-store store)

  (<!! (k/get-in store ["foo"]))

  <!! (k/exists? store "foos"))

  (<!! (k/assoc-in store ["foo"] {:foo 42}))

  (<!! (k/update-in store ["foo" :foo] inc))

  (<!! (k/bget store ["foo"] (fn [arg]
                               (let [baos (ByteArrayOutputStream.)]
                                 (io/copy (:input-stream arg) baos)
                                 (prn "cb" (vec (.toByteArray baos)))))))

  (<!! (k/bassoc store ["foo"] (byte-array [42 42 42 42 42])))


  (def db (create-db "/tmp/leveldb" {}))

  (get db "foo")

  (put db "foo" 42)



  )
