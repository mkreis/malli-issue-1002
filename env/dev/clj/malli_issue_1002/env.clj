(ns malli-issue-1002.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [malli-issue-1002.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[malli-issue-1002 started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[malli-issue-1002 has shut down successfully]=-"))
   :middleware wrap-dev})
