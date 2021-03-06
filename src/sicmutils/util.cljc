(ns sicmutils.util
  "Shared utilities between clojure and clojurescript."
  (:refer-clojure :rename {bigint core-bigint
                           biginteger core-biginteger
                           int core-int
                           long core-long
                           #?@(:clj [ratio? core-ratio?
                                     denominator core-denominator
                                     numerator core-numerator])}
                  #?@(:cljs [:exclude [bigint long int]]))
  (:require #?(:clj [clojure.math.numeric-tower :as nt])
            #?(:cljs goog.math.Integer)
            #?(:cljs goog.math.Long))
  #?(:clj
     (:import [java.util.concurrent TimeUnit TimeoutException])))

(defmacro import-def
  "import a single fn or var
   (import-def a b) => (def b a/b)
  "
  [from-ns def-name]
  (let [from-sym# (symbol (str from-ns) (str def-name))]
    `(def ~def-name ~from-sym#)))

(defmacro import-vars
  "import multiple defs from multiple namespaces
   works for vars and fns. not macros.
   (same syntax as potemkin.namespaces/import-vars)
   (import-vars
     [m.n.ns1 a b]
     [x.y.ns2 d e f]) =>
   (def a m.n.ns1/a)
   (def b m.n.ns1/b)
    ...
   (def d m.n.ns2/d)
    ... etc
  "
  [& imports]
  (let [expanded-imports (for [[from-ns & defs] imports
                               d defs]
                           `(import-def ~from-ns ~d))]
    `(do ~@expanded-imports)))

(def compute-sqrt #?(:clj nt/sqrt :cljs Math/sqrt))
(def compute-expt #?(:clj nt/expt :cljs Math/pow))
(def compute-abs #?(:clj nt/abs :cljs Math/abs))
(def inttype #?(:clj Integer :cljs goog.math.Integer))
(def longtype #?(:clj Long :cljs goog.math.Long))

(defn bigint [x]
  #?(:clj (core-bigint x)
     :cljs (js/BigInt x)))

(def ratio?
  #?(:clj core-ratio?
     :cljs (constantly false)))

(def numerator
  #?(:clj core-numerator
     :cljs (fn [x] (throw (js/Error "Ratio doesn't exist in cljs.")))))

(def denominator
  #?(:clj core-denominator
     :cljs (fn [x] (throw (js/Error "Ratio doesn't exist in cljs.")))))

(defn biginteger [x]
  #?(:clj (core-biginteger x)
     :cljs (js/BigInt x)))

(defn int [x]
  #?(:clj (core-int x)
     :cljs (.fromNumber goog.math.Integer x)))

(defn long [x]
  #?(:clj (core-long x)
     :cljs (.fromNumber goog.math.Long x)))

(defn unsupported [s]
  (throw
   #?(:clj (UnsupportedOperationException. s)
      :cljs (js/Error s))))

(defn exception [s]
  (throw
   #?(:clj (Exception. s)
      :cljs (js/Error s))))

(defn illegal [s]
  (throw
   #?(:clj (IllegalArgumentException. s)
      :cljs (js/Error s))))

(defn illegal-state [s]
  (throw
   #?(:clj (IllegalStateException. s)
      :cljs (js/Error s))))

(defn arithmetic-ex [s]
  (throw
   #?(:clj (ArithmeticException. s)
      :cljs (js/Error s))))

(defn timeout-ex [s]
  (throw
   #?(:clj (TimeoutException. s)
      :cljs (js/Error s))))
