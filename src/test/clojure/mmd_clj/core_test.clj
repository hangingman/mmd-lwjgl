(ns mmd-clj.core-test
  (:require
   [clojure.test :refer :all]
   [speclj.core :refer :all]
   [clojure.java.io :as io]
   [mmd-clj.core :refer :all])
  (:use
   [clojure.tools.logging]))

(describe "mmd-clj can load header file"
          (it "can return"
              (let [ans (load-pmd-file "HatsuneMiku.pmd")]
                (debug ans))))

(run-specs)
