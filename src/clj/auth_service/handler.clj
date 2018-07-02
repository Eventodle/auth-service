(ns auth-service.handler
  (:require
            [auth-service.routes.services :refer [service-routes]]
            [auth-service.routes.oauth :refer [oauth-routes]]
            [compojure.core :refer [routes wrap-routes]]
            [compojure.route :as route]
            [auth-service.env :refer [defaults]]
            [mount.core :as mount]
            [auth-service.middleware :as middleware]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
    (routes
          #'oauth-routes
          #'service-routes
          (route/not-found
             "page not found"))))

