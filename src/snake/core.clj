(ns snake.core
  (:require [org.baznex.imports :refer [import-static]])
  (:import (java.awt Color Dimension)
           (javax.swing JPanel JFrame Timer JOptionPane)
           (java.awt.event ActionListener KeyListener)))

(import-static java.awt.event.KeyEvent VK_LEFT VK_RIGHT VK_UP VK_DOWN)

(def width 75)
(def height 50)
(def point-size 10)
(def turn-millis 75)
(def win-length 5)
(def dirs {VK_LEFT [-1 0]
           VK_RIGHT [1 0]
           VK_UP [0 -1]
           VK_DOWN [0 1]})

;; ------------------ FUNCTIONAL MODEL ---------------------------

(defn add-points [& pts]
  (vec (apply map + pts)))

(defn point-to-screen-rect [pt]
  (map #(* point-size %)
       [(pt 0) (pt 1) 1 1]))

(defn create-apple [pt]
  {:location pt
   :color (Color. 210 50 90)
   :type :apple})

(defn create-snake []
  {:body (list [1 1])
   :dir [1 0]
   :color (Color. 15 160 70)
   :type :snake})

(defn move [{:keys [body dir] :as snake} & grow]
  (assoc snake :body (cons (add-points (first body) dir)
                           (if grow body (butlast body)))))

(defn win? [{:keys [body]}]
  (>= (count body) win-length))

(defn head-overlaps-body? [{[head & body] :body}]
  (contains? (set body) head))

(def lose? head-overlaps-body?)

(defn eats? [{[snake-head] :body} {apple :location}]
  (= snake-head apple))

(defn turn [snake newdir]
  (assoc snake :dir newdir))

(comment
  (create-apple [(rand-int width) (rand-int height)])
  (create-snake)
  (move (move (move (create-snake) :grow) :grow))
  (win? (move (move (move (move (create-snake) :grow) :grow) :grow) :grow))
  (lose? {:body '([1 1] [2 0])})
  (lose? {:body '([1 1] [2 0] [1 1])})
  (eats? (create-snake) (create-apple [1 1]))
  (turn (create-snake) [0 -1])
  )
;; ------------------ FUNCTIONAL MODEL ---------------------------

;; ------------------ MUTABLE MODEL  -----------------------------

(defn reset-game! [snake apple]
  (dosync
    (ref-set apple (create-apple [(rand-int width) (rand-int height)]))
    (ref-set snake (create-snake)))
  nil)

(defn update-direction! [snake newdir]
  (when newdir (dosync (alter snake turn newdir))))

(defn update-positions! [snake apple]
  (dosync
    (if (eats? @snake @apple)
      (do
        (ref-set apple (create-apple [(rand-int width) (rand-int height)]))
        (alter snake move :grow))
      (alter snake move)))
  nil)

(comment
  (def test-snake (ref nil))
  (def test-apple (ref nil))

  (dosync (ref-set test-apple (create-apple [3 1])))

  (deref test-snake)
  (deref test-apple)

  (reset-game! test-snake test-apple)
  (update-direction! test-snake [0 -1])
  (update-positions! test-snake test-apple))

;; ------------------ MUTABLE MODEL  -----------------------------

;; ------------------ SNAKE GUI  ---------------------------------

(defn fill-point [g pt color]
  (let [[x y width height] (point-to-screen-rect pt)]
    (.setColor g color)
    (.fillRect g x y width height)))

(defmulti paint (fn [g object & _] (:type object)))

(defmethod paint :apple [g {:keys [location color]}]
  (fill-point g location color))

(defmethod paint :snake [g {:keys [body color]}]
  (doseq [point body]
    (fill-point g point color)))

(defn game-panel [frame snake apple]
  (proxy [JPanel ActionListener KeyListener] []
    (paintComponent [g]
      (proxy-super paintComponent g)
      (paint g @snake)
      (paint g @apple))
    (actionPerformed [e]
      (update-positions! snake apple)
      (when (lose? @snake)
        (reset-game! snake apple)
        (JOptionPane/showMessageDialog frame "You lose!"))
      (when (win? @snake)
        (reset-game! snake apple)
        (JOptionPane/showMessageDialog frame "You win!"))
      (.repaint this))
    (keyPressed [e]
      (update-direction! snake (dirs (.getKeyCode e))))
    (getPreferredSize []
      (Dimension. (* (inc width) point-size)
                  (* (inc height) point-size)))
    (keyReleased [e])
    (keyTyped [e])))

(defn game []
  (let [snake (ref (create-snake))
        apple (ref (create-apple [(rand-int width) (rand-int height)]))
        frame (JFrame. "Snake")
        panel (game-panel frame snake apple)
        timer (Timer. turn-millis panel)]
    (doto panel
      (.setFocusable true)
      (.addKeyListener panel))
    (doto frame
      (.add panel)
      (.pack)
      (.setVisible true))
    (.start timer)
    [snake apple timer]))
;; ------------------ SNAKE GUI  ---------------------------------

#_
    (defn eats? [snake apple]
      (let [{[snake-head] :body} snake
            {apple-location :location} apple]
        (= snake-head apple-location)))

#_
    (defn eats? [snake apple]
      (let [snake-head (first (:body snake))
            apple-location (:location apple)]
        (= snake-head apple-location)))




