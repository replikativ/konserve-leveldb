(defproject io.replikativ/konserve-leveldb "0.1.2-SNAPSHOT"
  :description "A LevelDB backend for konserve."
  :url "http://github.com/replikativ/konserve-leveldb"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [io.replikativ/konserve "0.5-beta1"]
                 [byte-streams "0.2.2"]
                 [factual/clj-leveldb "0.1.1" :exclusions [byte-streams]]])
