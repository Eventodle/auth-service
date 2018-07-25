(ns auth-service.middleware
  (:require [auth-service.env :refer [defaults]]
            [auth-service.config :refer [env]]
            [ring-ttl-session.core :refer [ttl-memory-store]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [auth-service.db.core :as db]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [buddy.core.hash :as hash]
            [buddy.sign.jwt :as jwt]))

(defn on-auth-error []
  {:status 403
   :headers {"Content-Type" "application/json"}
   :body {:error "Forbidden"}})

(defn on-auth-success [token]
  { :status 200
    :headers {"Content-Type" "application/json" "Authorization" (str "Bearer " token)}
    :body { :authenticated true }
  })

(defn wrap-auth [handler]
  (fn [request]
    (try
      (let [auth-token (get-in (:headers request) ["authorization"])
            data (jwt/decrypt auth-token (hash/sha256 (:jwt-private-key env)) {:alg :dir :enc :a128cbc-hs256})
            valid-user? (not (nil? (db/get-user {:id (:id data)})))
            expired? (> (tc/to-long (time/now)) (:exp data))]
        (if (and valid-user? (not expired?)) (on-auth-success auth-token) (on-auth-error)))
      (catch Exception ex
        (on-auth-error)))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))))
