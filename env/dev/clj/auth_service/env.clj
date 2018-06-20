(ns auth-service.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [auth-service.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[auth-service started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[auth-service has shut down successfully]=-"))
   :middleware wrap-dev})
