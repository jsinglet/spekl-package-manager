(ns spekl-package-manager.progress)

;;
;; Where's a Monad when you need one!
;;


(defn fill [s n]
  (if (= n 0)
    ""
  (if (> n 1)
    (fill (str s (first s)) (- n 1))
    s)))

(defn line [label steps step]
  (format "%-20s : [%s] %-3s" label (str (fill "=" step) (fill " " (- steps step))) "0%"))

(defn progress [label steps step]
  (let [pct (format "%d%s" (int (* (/ step steps) 100)) "%")]
  (format "%s] %-3s" (str (fill "=" step) (fill " " (- steps step))) pct)))

(defn init [label steps]
  (do
    (print (line label steps 0))
    (flush)
    ))


(defn create [label steps]
  (do
    (println "")
    (init label steps)
    (def _l {:label label :steps steps :step 0})
    ))

(defn delete-chars [n]
  (if (not (= n 0))
    (do 
      (print "\b \b")
      (delete-chars (- n 1)))))

(defn step []
  (do
    ;; backspace 1 + total progress
    (delete-chars (+ (_l :steps) 5))
    (print (progress (_l :label) (_l :steps) (+ (_l :step) 1)))
    (def _l {:label  (_l :label) :steps (_l :steps) :step (+ (_l :step) 1)})
    (flush)
    ))

(defn done []
  (do
    (delete-chars (+ (_l :steps) 5))
    (print (progress (_l :label) (_l :steps)  (_l :steps)))
    (def _l {:label  (_l :label) :steps (_l :steps) :step (_l :steps)})
    (flush)
    (println "")
    ))

;; this is how you use the progress bar
(defn silly-loop []
  (do
    (create "silly-1.1.1" 50)
    (loop [x 50]
      (when (> x 40)    
        (Thread/sleep 300)
        (step)
        (recur (- x 1))
        ))
    (done)
    (println (_l :step))
    ))
