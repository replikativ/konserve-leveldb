(ns konserve-leveldb.core-test
  (:require [clojure.test :refer :all]
            [konserve.core :as k]
            [konserve-leveldb.core :refer :all]
            [clj-leveldb :as level]
            [clojure.core.async :refer [<!!]]))

(deftest leveldb-store-test
  (testing "Test the leveldb store functionality."
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
      (<!! (k/dissoc store :foo))
      (is (= (<!! (k/get-in store [:foo]))
             nil))
      (<!! (k/bassoc store :binbar (byte-array (range 10))))
      (<!! (k/bget store :binbar (fn [{:keys [input-stream]}]
                                   (is (= (map byte (slurp input-stream))
                                          (range 10))))))
      (release store))))


(comment
  (def path "/tmp/leveldb-test2")

  (when store
    (release store))
  (level/destroy-db path)
  (def store (<!! (new-leveldb-store path)))



  (<!! (k/exists? store :foo)))

