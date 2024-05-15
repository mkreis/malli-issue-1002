(ns malli-issue-1002.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[malli-issue-1002 started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[malli-issue-1002 has shut down successfully]=-"))
   :middleware identity})
