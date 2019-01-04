(ns streaming-demo.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [ring.util.response :as rr]
            [ring.util.io :refer [piped-input-stream]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
  (:import [java.io IOException]))

(defn rand-str []
  (apply str (take 10 (repeatedly #(char (+ (rand 26) 65))))))

(defn rand-value []
  {:foo (rand-str) :bar (rand-str) :baz (rand-str)})

(defn wrapped-generate-stream
  [result-seq writer opts]
  (try
    (json/generate-stream result-seq writer opts)
    (catch Exception e
      (println "yo"))))

(defroutes app-routes
  (GET "/stream" []
       (let [result-seq (take 10000000 (repeatedly rand-value))]
         (rr/response
           (piped-input-stream
             #(let [w (io/make-writer % {:encoding "UTF-8"})]
                (json/generate-stream result-seq w {:pretty true}))))))
  (GET "/nostream" []
       (let [result-seq (take 10000000 (repeatedly rand-value))]
         (rr/response
           (piped-input-stream
             #(let [w (io/make-writer % {:encoding "UTF-8"})]
                (try
                  (json/generate-stream result-seq w {:pretty true})
                  (catch Exception e
                    (println "yo"))))))))
  (GET "/streaming-restored" []
       (let [result-seq (take 10000000 (repeatedly rand-value))]
         (rr/response
           (piped-input-stream
             #(let [w (io/make-writer % {:encoding "UTF-8"})]
                (wrapped-generate-stream result-seq w {:pretty true}))))))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
