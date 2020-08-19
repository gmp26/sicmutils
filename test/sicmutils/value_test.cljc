;
; Copyright © 2017 Colin Smith.
; This work is based on the Scmutils system of MIT/GNU Scheme:
; Copyright © 2002 Massachusetts Institute of Technology
;
; This is free software;  you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation; either version 3 of the License, or (at
; your option) any later version.
;
; This software is distributed in the hope that it will be useful, but
; WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
; General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this code; if not, see <http://www.gnu.org/licenses/>.
;

(ns sicmutils.value-test
  (:require [clojure.test :refer [is deftest testing]]
            [sicmutils.util :as u]
            [sicmutils.value :as v])
  #?(:clj
     (:import (clojure.lang PersistentVector))))

(deftest vector-value-impl
  (testing "nullity?"
    (is (v/nullity? []))
    (is (v/nullity? [0 0]))
    (is (not (v/nullity? [1 2 3]))))

  (testing "zero-like"
    (is (= [0 0 0] (v/zero-like [1 2 3])))
    (is (= [] (v/zero-like [])))
    (is (= [0 [0 0] [0 0]] (v/zero-like [1 [2 3] [4 5]])))
    (is (= [(u/long 0) (u/int 0) 0]
           (v/zero-like [(u/long 1) (u/int 2) 3]))))

  (is (thrown? #?(:clj UnsupportedOperationException :cljs js/Error)
               (v/one-like [1 2 3])))

  (testing "exact?"
    (is (v/exact? [1 2 3 4]))
    (is (not (v/exact? [1.2 3 4])))
    #?(:clj (is (v/exact? [0 1 3/2])))
    (is (not (v/exact? [0 0 0.00001]))))

  (testing "freeze"
    (is (= [1 2 3] (v/freeze [1 2 3]))))

  (testing "kind"
    (is (= PersistentVector (v/kind [1 2])))))

(deftest value-protocol-numbers
  ;; These really want to be generative tests.
  ;;
  ;; TODO convert, once we sort out the cljs test.check story.
  (is (v/nullity? 0))
  (is (v/nullity? 0.0))
  (is (not (v/nullity? 1)))
  (is (not (v/nullity? 1.0)))
  (is (v/nullity? (v/zero-like 100)))
  (is (= 0 (v/zero-like 2)))
  (is (= 0 (v/zero-like 3.14)))

  (is (v/unity? 1))
  (is (v/unity? 1.0))
  (is (v/unity? (v/one-like 100)))
  (is (not (v/unity? 2)))
  (is (not (v/unity? 0.0)))

  (is (= 10 (v/freeze 10)))
  (is (v/numerical? 10))
  (is (isa? (v/kind 10) v/numtype))
  (is (v/exact? 10))
  (is (not (v/exact? 10.1))))

#?(:cljs
   (deftest exposed-arities-test
     (is (= [1] (v/exposed-arities (fn [x] (* x x)))))
     (is (= [1 3] (v/exposed-arities (fn ([x] (* x x)) ([x y z] (+ x y))))))))

(deftest arities
  (is (= [:exactly 0] (v/arity (fn [] 42))))
  (is (= [:exactly 1] (v/arity (fn [x] (+ x 1)))))
  (is (= [:exactly 2] (v/arity (fn [x y] (+ x y)))))
  (is (= [:exactly 3] (v/arity (fn [x y z] (* x y z)))))
  (is (= [:at-least 0] (v/arity (fn [& xs] (reduce + 0 xs)))))
  (is (= [:at-least 1] (v/arity (fn [x & xs] (+ x (reduce * 1 xs))))))
  (is (= [:at-least 2] (v/arity (fn [x y & zs] (+ x y (reduce * 1 zs))))))
  (is (= [:exactly 0] (v/arity 'x)))
  (is (= [:at-least 0] (v/arity (constantly 42))))
  ;; the following is dubious until we attach arity metadata to MultiFns
  (is (= [:exactly 1] (v/arity [1 2 3])))
  (let [f (fn [x] (+ x x))
        g (fn [y] (* y y))]
    (is (= [:exactly 1] (v/arity (comp f g))))))

#?(:cljs
   (deftest arities-cljs
     ;; in cljs, we can figure out that a fn accepts some bounded number of
     ;; arguments.
     (is (= [:between 1 3] (v/arity (fn ([x] (inc x))
                                      ([x y] (+ x y))
                                      ([x y z] (* x y z))))))

     ;; Noting the case here where we're missing the arity-2, but we still
     ;; return a :between.
     (is (= [:between 1 3] (v/arity (fn ([x] (inc x)) ([x y z] (* x y z))))))

     ;; Adding a variadic triggers :at-least...
     (is (= [:at-least 1] (v/arity (fn ([x] (inc x)) ([x y z & xs] (* x y z))))))

     ;; Unless you add ALL arities from 0 to 3 and variadic. Then we assume you were
     ;; generated by comp.
     (is (= [:exactly 1] (v/arity (fn ([] 10) ([x] (inc x)) ([x y]) ([x y z & xs] (* x y z))))))

     ;; A single variadic with lots of args works too.
     (is (= [:at-least 4] (v/arity (fn [x y z a & xs] (* x y z a)))))))

(deftest nullity
  (is (v/nullity? 0))
  (is (v/nullity? 0.0))
  (is (not (v/nullity? 1)))
  (is (not (v/nullity? 0.1))))

(deftest unity
  (is (v/unity? 1))
  (is (v/unity? 1.0))
  (is (not (v/unity? 0)))
  (is (not (v/unity? 0.0))))

(deftest kinds
  (is (= #?(:clj Long :cljs ::v/native-integral) (v/kind 1)))
  (is (= #?(:clj Double :cljs ::v/native-integral) (v/kind 1.0)))
  (is (= PersistentVector (v/kind [1 2]))))

(deftest exactness
  (is (v/exact? 1))
  (is (v/exact? 4N))
  (is (not (v/exact? 1.1)))
  (is (not (v/exact? 'a)))
  (is (not (v/exact? :a)))
  (is (not (v/exact? "a"))))

#?(:clj
   (deftest exactness-clj
     (is (v/exact? 3/2))
     (is (v/exact? (BigInteger/valueOf 111)))))

(deftest argument-kinds
  (let [L #?(:clj Long :cljs ::v/native-integral)
        V PersistentVector]
    (is (= [L] (v/argument-kind 1)))
    (is (= [L L L] (v/argument-kind 1 2 3)))
    (is (= [V] (v/argument-kind [2 3])))
    (is (= [V V] (v/argument-kind [1] [3 4])))))

(defn illegal? [f]
  (is (thrown? #?(:clj IllegalArgumentException :cljs js/Error)
               (f))))

(deftest joint-arities
  (is (= [:exactly 1] (v/joint-arity [[:exactly 1] [:exactly 1]])))
  (is (= [:exactly 5] (v/joint-arity [[:exactly 5] [:exactly 5]])))
  (is (illegal? #(v/joint-arity [[:exactly 2] [:exactly 1]])))
  (is (illegal? #(v/joint-arity [[:exactly 1] [:exactly 2]])))
  (is (= [:exactly 3] (v/joint-arity [[:exactly 3] [:at-least 2]])))
  (is (= [:exactly 3] (v/joint-arity [[:exactly 3] [:at-least 3]])))
  (is (= [:exactly 3] (v/joint-arity [[:at-least 1] [:exactly 3]])))
  (is (= [:exactly 3] (v/joint-arity [[:at-least 3] [:exactly 3]])))
  (is (illegal? #(v/joint-arity [[:exactly 1] [:at-least 2]])))
  (is (illegal? #(v/joint-arity [[:at-least 2] [:exactly 1]])))
  (is (= [:at-least 3] (v/joint-arity [[:at-least 2] [:at-least 3]])))
  (is (= [:at-least 3] (v/joint-arity [[:at-least 3] [:at-least 2]])))
  (is (= [:between 2 3] (v/joint-arity [[:between 1 3] [:between 2 5]])))
  (is (= [:between 2 3] (v/joint-arity [[:between 2 5] [:between 1 3]])))
  (is (illegal? #(v/joint-arity [[:between 1 3] [:between 4 6]])))
  (is (illegal? #(v/joint-arity [[:between 4 6] [:between 1 3]])))
  (is (= [:exactly 3] (v/joint-arity [[:between 1 3] [:between 3 4]])))
  (is (= [:exactly 3] (v/joint-arity [[:between 3 4] [:between 1 3]])))
  (is (= [:between 2 4] (v/joint-arity [[:at-least 2] [:between 1 4]])))
  (is (= [:between 2 4] (v/joint-arity [[:between 1 4] [:at-least 2]])))
  (is (illegal? #(v/joint-arity [[:at-least 4] [:between 1 3]])))
  (is (illegal? #(v/joint-arity [[:between 1 3] [:at-least 4]])))
  (is (= [:exactly 2] (v/joint-arity [[:exactly 2] [:between 2 3]])))
  (is (= [:exactly 2] (v/joint-arity [[:between 2 3] [:exactly 2]])))
  (is (illegal? #(v/joint-arity [[:between 2 3] [:exactly 1]])))
  (is (illegal? #(v/joint-arity [[:exactly 1] [:between 2 3]]))))
