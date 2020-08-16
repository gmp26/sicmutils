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

(ns sicmutils.calculus.vector-field-test
  (:refer-clojure :exclude [+ - * / partial])
  (:require #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test :as t :refer-macros [is deftest testing use-fixtures]])
            #?(:clj  [sicmutils.calculus.coordinate :refer [let-coordinates]]
               :cljs [sicmutils.calculus.coordinate :refer-macros [let-coordinates]])
            [sicmutils.calculus.vector-field :as vf]
            [sicmutils.operator :as o]
            [sicmutils.simplify :refer [hermetic-simplify-fixture]]
            [sicmutils.value :as v]))

(use-fixtures :once hermetic-simplify-fixture)

(deftest vectorfield
  (testing "literal"
    (let [f (literal-manifold-function 'f-rect R2-rect)
          v (literal-vector-field 'b R2-rect)
          R2-rect-chi-inverse (point R2-rect)
          p (R2-rect-chi-inverse (up 'x0 'y0))]
      (is (= '(+ (* (((partial 0) f-rect) (up x0 y0)) (b↑0 (up x0 y0)))
                 (* (((partial 1) f-rect) (up x0 y0)) (b↑1 (up x0 y0))))
             (simplify ((v f) p))))
      (is (= '(up (b↑0 (up x0 y0)) (b↑1 (up x0 y0)))
             (simplify ((v (chart R2-rect)) p))))
      (is (= ::vf/vector-field (v/kind v)))))

  (testing "exponential"
    (let-coordinates [[x y] R2-rect]
      (let [circular (- (* x d:dy) (* y d:dx))]
        (is (= '(up (+ (* -1/720 (expt a 6)) (* 1/24 (expt a 4)) (* -1/2 (expt a 2)) 1)
                    (+ (* 1/120 (expt a 5)) (* -1/6 (expt a 3)) a))
               (simplify
                ((((evolution 6) 'a circular) (chart R2-rect))
                 ((point R2-rect) (up 1 0)))))))))

  (testing "naming"
    (let-coordinates [[x y] R2-rect]
      (is (= 0 ))))

  (testing "gjs-examples"
    (let-coordinates [[x y z] R3-rect]
      (is (= '(+ (* -1 a b (cos a) (cos b)) (* -2 a (cos a) (sin b)))
             (simplify (((* (expt d:dy 2) x y d:dx) (* (sin x) (cos y)))
                        ((point R3-rect) (up 'a 'b 'c))))))
      (let [counter-clockwise (- (* x d:dy) (* y d:dx))
            outward (+ (* x d:dx) (* y d:dy))
            mr ((point R3-rect) (up 'x0 'y0 'z0))]
        (is (= 0 (simplify ((counter-clockwise (sqrt (+ (square x) (square y)))) mr))))
        (is (= '(+ (expt x0 2) (* -1 (expt y0 2)))
               (simplify ((counter-clockwise (* x y)) mr))))
        (is (= '(* 2 x0 y0) (simplify ((outward (* x y)) mr))))))

    (let-coordinates [[r theta zeta] R3-cyl
                      [x y z] R3-rect]
      (testing "McQuistan"
        (let [A (+ (* 'A_r d:dr) (* 'A_theta d:dtheta) (* 'A_z d:dzeta))
              p ((point R3-rect) (up 'x 'y 'z))]
          ;; TODO: simplification isn't all it could be here (?)
          (is (= '(up (/ (+ (* -1N A_theta y (sqrt (+ (expt x 2) (expt y 2)))) (* A_r x)) (sqrt (+ (expt x 2) (expt y 2))))
                      (/ (+ (* A_theta x (sqrt (+ (expt x 2) (expt y 2)))) (* A_r y)) (sqrt (+ (expt x 2) (expt y 2))))
                      A_z)
                 (simplify ((vector-field->components A R3-rect) (up 'x 'y 'z)))))
          (is (= '(up (* -1 y) x 0) (simplify ((d:dtheta (up x y z)) p))))
          (is (= '(up (/ x (sqrt (+ (expt x 2) (expt y 2))))
                      (/ y (sqrt (+ (expt x 2) (expt y 2))))
                      0)
                 (simplify ((d:dr (up x y z)) p))))
          (is (= '(up 0 0 1) (simplify ((d:dzeta (up x y z)) p))))
          (is (= '(up 0 0 1) (simplify ((d:dz (up x y z)) p)))) ;; suspicious. GJS has d:dz but I think d:dzeta was meant here (as above)
          ;; "so introduce..."
          (let [e-theta (* (/ 1 r) d:dtheta)
                e-r d:dr
                e-z d:dzeta
                A (+ (* 'A_r e-r) (* 'A_theta e-theta) (* 'A_z e-z))]
            (is (= '(up (/ (+ (* A_r x) (* -1 A_theta y)) (sqrt (+ (expt x 2) (expt y 2))))
                        (/ (+ (* A_r y) (* A_theta x)) (sqrt (+ (expt x 2) (expt y 2))))
                        A_z)
                   (simplify ((vector-field->components A R3-rect) (up 'x 'y 'z))))))))
      (is (= (up 0 1 0) ((vector-field->components d:dy R3-rect) (up 'x0 'y0 'z0))))
      (is (= (up 0 1 0) ((vector-field->components d:dy R3-rect) (up 'r0 'theta0 'z0))))
      (is (= (up 1. 0 0) ((vector-field->components d:dy R3-cyl) (up 1 (/ Math/PI 2) 0))))
      (is (= (up 0 1 0) ((vector-field->components d:dy R3-cyl) (up 1 0 0))))
      (is (= '(up (sin theta0) (/ (cos theta0) r0) 0)
             (simplify ((vector-field->components d:dy R3-cyl) (up 'r0 'theta0 'z)))))

      (testing "coordinatize"
        (let [coordinatize (fn [sfv coordsys]
                             (let [v (fn [f]
                                       (fn [x]
                                         (let [b (compose (sfv (chart coordsys))
                                                          (point coordsys))]
                                           (* ((D f) x) (b x)))))]
                               (o/make-operator v 'coordinatize)))]
          (is (= '(+ (* (((partial 0) f) (up x0 y0 z0)) (v↑0 (up x0 y0 z0)))
                     (* (((partial 1) f) (up x0 y0 z0)) (v↑1 (up x0 y0 z0)))
                     (* (((partial 2) f) (up x0 y0 z0)) (v↑2 (up x0 y0 z0))))
                 (simplify (((coordinatize (literal-vector-field 'v R3-rect) R3-rect)
                             (literal-function 'f (up 1 2 3) 1))
                            (up 'x0 'y0 'z0))))))))))
