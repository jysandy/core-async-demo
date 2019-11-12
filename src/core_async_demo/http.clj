(ns core-async-demo.http
  "Problem: find the number of branches across all repos in a Github organization.
  Implemented using core.async."
  (:require [clojure.core.async :refer [<! >! go <!! >!!] :as async]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [camel-snake-kebab.core :as csk]))

(def github-username "fill-in-username-here")
(def github-api-token "fill-in-api-token-here")

(defn http-get-async
  [url]
  ;; chan takes a transducer
  (let [return-chan (async/chan 1 (map (fn [response]
                                         (-> response
                                             :body
                                             (json/decode csk/->kebab-case-keyword)))))]
    (http/get url {:basic-auth [github-username github-api-token]}
              (fn [response]
                (async/put! return-chan response)
                (async/close! return-chan)))
    return-chan))

(defn get-repos
  [org-name]
  (http-get-async (format "https://api.github.com/orgs/%s/repos" org-name)))

(defn get-branches
  [full-repo-name]
  (http-get-async (format "https://api.github.com/repos/%s/branches?per_page=100" full-repo-name)))

(defmacro go-catch
  "Don't throw uncaught exceptions!"
  [& body]
  `(go (try
         ~@body
         (catch Exception e#
           e#))))

(defn number-of-branches
  [org-name]
  (go-catch (let [repos      (<! (get-repos org-name))
                  repo-names (map :full-name repos)
                  branches   (<! (->> repo-names
                                      (map get-branches)
                                      (async/map concat)))]
              (count branches))))

(comment
  (<!! (number-of-branches "clojure"))

  ;; macroexpand
  (macroexpand-1 '(go "foobar"))

  (clojure.core/let
    [c__6659__auto__                 (clojure.core.async/chan 1)
     captured-bindings__6660__auto__ (clojure.lang.Var/getThreadBindingFrame)]
    (clojure.core.async.impl.dispatch/run
      (fn* []
        (clojure.core/let [f__6661__auto__     (clojure.core/fn state-machine__6422__auto__
                                                 ([]
                                                  (clojure.core.async.impl.ioc-macros/aset-all!
                                                    (java.util.concurrent.atomic.AtomicReferenceArray. 7)
                                                    0
                                                    state-machine__6422__auto__
                                                    1
                                                    1))
                                                 ([state_15760]
                                                  (clojure.core/let
                                                    [old-frame__6423__auto__
                                                     (clojure.lang.Var/getThreadBindingFrame)
                                                     ret-value__6424__auto__
                                                     (try
                                                       (clojure.lang.Var/resetThreadBindingFrame (clojure.core.async.impl.ioc-macros/aget-object state_15760 3))
                                                       (clojure.core/loop
                                                         []
                                                         (clojure.core/let
                                                           [result__6425__auto__
                                                            (clojure.core/case
                                                              (clojure.core/int (clojure.core.async.impl.ioc-macros/aget-object state_15760 1))
                                                              1
                                                              (clojure.core/let []
                                                                (clojure.core.async.impl.ioc-macros/return-chan state_15760 "foobar")))]
                                                           (if (clojure.core/identical? result__6425__auto__ :recur) (recur) result__6425__auto__)))
                                                       (catch
                                                         java.lang.Throwable ex__6426__auto__
                                                         (clojure.core.async.impl.ioc-macros/aset-all! state_15760 2 ex__6426__auto__)
                                                         (if
                                                           (clojure.core/seq (clojure.core.async.impl.ioc-macros/aget-object state_15760 4))
                                                           (clojure.core.async.impl.ioc-macros/aset-all!
                                                             state_15760
                                                             1
                                                             (clojure.core/first (clojure.core.async.impl.ioc-macros/aget-object state_15760 4))
                                                             4
                                                             (clojure.core/rest (clojure.core.async.impl.ioc-macros/aget-object state_15760 4)))
                                                           (throw ex__6426__auto__))
                                                         :recur)
                                                       (finally (clojure.lang.Var/resetThreadBindingFrame old-frame__6423__auto__)))]
                                                    (if (clojure.core/identical? ret-value__6424__auto__ :recur) (recur state_15760) ret-value__6424__auto__))))

                           state__6662__auto__ (clojure.core/->
                                                 (f__6661__auto__)
                                                 (clojure.core.async.impl.ioc-macros/aset-all!
                                                   clojure.core.async.impl.ioc-macros/USER-START-IDX
                                                   c__6659__auto__
                                                   clojure.core.async.impl.ioc-macros/BINDINGS-IDX
                                                   captured-bindings__6660__auto__))]
          (clojure.core.async.impl.ioc-macros/run-state-machine-wrapped state__6662__auto__))))
    c__6659__auto__)

  ;; This breaks
  (<!! (go (let [repos      (<! (get-repos "clojure"))
                 repo-names (map :full-name repos)
                 branches   (->> repo-names
                                 (map get-branches)
                                 (mapcat <!))]
             branches)))

  async/thread

  ;; Don't:
  ;; 1. Block in a go block (no IO / long running computation). Put IO onto a separate bounded threadpool, and computation onto a work-stealing thread pool.
  ;; 2. Throw uncaught exceptions in a go block. This will cause threads to crash and be recreated. The exception will not be propagated.
  ;; 3. Call <! or >! across function boundaries. Use core.async functions for help.
  ;; 4. Have a huge number of pending puts or takes on one channel (max 1024).
  ;; 5. Use unbounded threadpools (for example async/thread) for high throughput. Set up your own bounded thread pool and write your own thread macro instead.
  ;; 6. Try to put nil on a channel. It doesn't work.

  ;; Further reading
  ;; https://www.infoq.com/presentations/clojure-core-async/
  ;; https://github.com/clojure/core.async/wiki/Go-Block-Best-Practices
  ;; https://www.youtube.com/watch?v=enwIIGzhahw
  ;; https://github.com/clojure/core.async
  )
