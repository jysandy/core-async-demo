(ns core-async-demo.core
  (:require [clojure.core.async :refer [<! >! go go-loop <!! >!!] :as async]))

(comment
  ;; Define a channel
  (def c (async/chan))
  (def d (async/chan))

  ;; Async take
  async/take!

  ;; Async put
  async/put!

  ;; Take from a channel asynchronously
  (async/take! c #(println %))

  (def buffered-chan (async/chan 2))

  ;; Put into a channel, also asynchronously
  (async/put! c "Hello, world!"
              (fn [& _]
                (println "put completed!")))

  ;; Read and write from channels synchronously

  ;; Blocking take
  (<!! c)

  ;; Blocking put
  (>!! c "Hello world!")

  ;; Read from a channel in a go block, non-blocking
  (go (println (<! c)))

  ;; Put into a channel from a go block
  (go (>! c "foobar"))
  (async/take! c #(println %))

  ;; A go block returns a channel
  (let [returned-channel (go "foobar")]
    (println (<!! returned-channel))
    (println (<!! returned-channel)))

  (<!! (go "foobar"))

  (def foo-channel (async/chan))

  ;; timeout returns a channel which closes after x milliseconds

  (<!! (async/timeout 5000))

  ;; Use alt! to perform multiple channel operations

  (go (async/alt!
        (async/timeout 5000) ([_]
                               (println "Timed out"))
        c ([v] (println "Got" v "from c"))
        d ([v] (println "Got" v "from d"))))

  (go (async/alt!
        (async/timeout 5000) ([_ _]
                               (println "Timed out"))
        [c d] ([v port]
                (cond
                  (= port d) (println "Took from d")
                  (= port c) (println "Took from c")))))

  ;; Putting onto a channel with a timeout
  (do (async/put! d "foobar")
      (async/put! c "Hello world!")
      (go (async/alt!
            (async/timeout 5000) ([_ _]
                                   (println "Timed out, failed to write to d"))
            [[d "foo"]] (println "Wrote to d")
            c ([v ] (println "Received" v "from c")))))

  (async/take! d #(println %))

  (go (async/alt!
        (async/timeout 5000) ([_ _]
                               (println "Timed out, failed to write to d"))
        [[d "foo"]] (println "Wrote to d")
        c ([v ] (println "Received" v "from c"))))

  ;; Transducers
  (def incremented (async/chan 1 (map inc)))
  (async/put! incremented "foobar")
  (async/take! incremented #(println %))

  (def filtered (async/chan 1 (filter even?)))
  (async/put! filtered 4)
  (async/take! filtered #(println %))

  ;; Consume messages from a channel
  (def channel-e (async/chan))
  (def kill-channel (let [kill-ch (async/chan)]
                      (go-loop []
                        (async/alt!
                          kill-ch ([_] (do (println "Kill message received, consumer quitting")
                                           :quit))
                          channel-e ([v] (do (println "Received" v "from channel e")
                                             (recur)))))
                      kill-ch))
  (async/put! channel-e "Hello world!")
  (async/put! kill-channel "die")
  (async/close! kill-channel))
