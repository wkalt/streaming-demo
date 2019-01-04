# streaming-demo

This is a demonstration of a suspected bug/oddity in the Clojure compiler. Run
the demo with

    lein run server-headless

This starts the service with a 64mb heap, to exacerbate the issue.

The curl commands below will demonstrate the inconsistency:

    curl -X GET http://localhost:3000/stream

    curl -X GET http://localhost:3000/nostream

    curl -X GET http://localhost:3000/streaming-restored


The respective definitions are

`/stream`

    (GET "/stream" []
         (let [result-seq (take 10000000 (repeatedly rand-value))]
           (rr/response
             (piped-input-stream
               #(let [w (io/make-writer % {:encoding "UTF-8"})]
                  (json/generate-stream result-seq w {:pretty true}))))))

`/nostream`

    (GET "/nostream" []
         (let [result-seq (take 10000000 (repeatedly rand-value))]
           (rr/response
             (piped-input-stream
               #(let [w (io/make-writer % {:encoding "UTF-8"})]
                  (try
                    (json/generate-stream result-seq w {:pretty true})
                    (catch Exception e
                      (println "yo"))))))))

`/streaming-restored`

    (defn wrapped-generate-stream
      [result-seq writer  opts]
      (try
        (json/generate-stream result-seq writer opts)
        (catch Exception e
          (println "yo"))))

    (GET "/streaming-restored" []
         (let [result-seq (take 10000000 (repeatedly rand-value))]
           (rr/response
             (piped-input-stream
               #(let [w (io/make-writer % {:encoding "UTF-8"})]
                  (wrapped-generate-stream result-seq w {:pretty true}))))))


The `/stream` and `/streaming-restored` endpoints will both stream results
properly. The `/nostream` endpoint will run out of memory. Between the three,
the only difference is the location of the try/catch clauses. The `/stream`
endpoint has none, the `/nostream` endpoint has them in the anonymous function
being passed to piped-input-stream, and the `/streaming-restored` function
splits them out into their own function. In my opinion all three handlers
should behave the same.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2019 FIXME
