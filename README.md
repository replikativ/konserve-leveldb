# konserve-leveldb

A LevelDB backend for [konserve](https://github.com/replikativ/konserve) implemented with [clj-leveldb](https://github.com/Factual/clj-leveldb). 

## Usage

Add to your leiningen dependencies:
[![Clojars Project](http://clojars.org/io.replikativ/konserve-leveldb/latest-version.svg)](http://clojars.org/io.replikativ/konserve-leveldb)

The purpose of konserve is to have a unified associative key-value interface for
edn datastructures and binary blobs. Use the standard interface functions of konserve.

You can provide leveldb configuration options to the `new-leveldb-store`
constructor as an `:config` argument. We do not require additional settings
beyond the konserve serialization protocol for the store, so you can still
access the store through `clj-leveldb` directly wherever you need (e.g. for
store deletion).

~~~clojure
  (require '[konserve-leveldb.core :refer :all]
           '[konserve.core :as k)
  (def leveldb-store (<!! (new-leveldb-store "/tmp/konserve-leveldb-test")))

  (<!! (k/exists? leveldb-store  "john"))
  (<!! (k/get-in leveldb-store ["john"]))
  (<!! (k/assoc-in leveldb-store ["john"] 42))
  (<!! (k/update-in leveldb-store ["john"] inc))
  (<!! (k/get-in leveldb-store ["john"]))
  
  (<!! (release store)) ;; there is only one instance per store allowed at a time
~~~



## License

Copyright © 2016-2017 Christian Weilbach

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
