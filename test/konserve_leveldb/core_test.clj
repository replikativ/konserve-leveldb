(ns konserve-leveldb.core-test
  (:require [clojure.test :refer :all]
            [konserve.core :as k]
            [konserve-leveldb.core :refer :all]
            [clj-leveldb :as level]
            [clojure.core.async :refer [<!!]]))

(deftest carmine-store-test
  (testing "Test the carmine store functionality."
    (let [path "/tmp/konserve-leveldb-test"
          _ (level/destroy-db path)
          store (<!! (new-leveldb-store path))]
      (is (= (<!! (k/exists? store :foo))
             false))
      (<!! (k/assoc-in store [:foo] nil))
      (is (= (<!! (k/get-in store [:foo]))
             nil))
      (<!! (k/assoc-in store [:foo] :bar))
      (is (= (<!! (k/exists? store :foo))
             true))
      (is (= (<!! (k/get-in store [:foo]))
             :bar))
      (<!! (k/bassoc store :binbar (byte-array (range 10))))
      (<!! (k/bget store :binbar (fn [{:keys [input-stream]}]
                                   (is (= (map byte (slurp input-stream))
                                          (range 10)))))))))
