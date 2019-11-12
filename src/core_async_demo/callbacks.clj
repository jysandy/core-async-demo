(ns core-async-demo.callbacks
  "Problem: find the number of branches across all repos in a Github organization.
  Implemented using callbacks, just to demonstrate how ugly it can get..."
  (:require [cheshire.core :as json]
            [org.httpkit.client :as http]
            [camel-snake-kebab.core :as csk]))

(def github-username "fill-in-username-here")
(def github-api-token "fill-in-api-token-here")

(defn http-get-sync
  [url]
  (-> @(http/get url {:basic-auth [github-username github-api-token]})
      :body
      (json/decode csk/->kebab-case-keyword)))

(defn http-get-callback
  [url callback]
  (http/get url {:basic-auth [github-username github-api-token]}
            (fn [response]
              (callback (-> response
                            :body
                            (json/decode csk/->kebab-case-keyword))))))

(defn get-repos
  [org-name callback]
  (http-get-callback (format "https://api.github.com/orgs/%s/repos" org-name) callback))

(defn get-branches
  [full-repo-name callback]
  (http-get-callback (format "https://api.github.com/repos/%s/branches?per_page=100" full-repo-name) callback))

(defn number-of-branches
  [org-name]
  (get-repos "clojure" (fn [repos]
                         (let [repo-names (map :full-name repos)
                               branches   (atom [])]
                           (doseq [repo-name repo-names]
                             (get-branches repo-name (fn [repo-branches]
                                                       (swap! branches concat repo-branches))))
                           (Thread/sleep 5000)
                           (println (count @branches))))))
