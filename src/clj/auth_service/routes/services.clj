(ns auth-service.routes.services
  (:require [ring.util.http-response :refer :all]
            [clj-time.core :as time]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.hashers :as hashers]
            [buddy.auth :refer [authenticated?]]
            [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [auth-service.db.core :as db]
            [dotenv.core :as dotenv]))

(def secret (hash/sha256 (dotenv/env :jwt-private-key)))

(defn authenticate-user [user-id pass]
  (when-let [user (db/get-user {:id user-id})]
    (when (hashers/check pass (:pass user))
      (jwt/encrypt (merge {:exp (time/plus (time/now) (time/days 1))}
                          (dissoc user :pass)) secret {:alg :dir :enc :a128cbc-hs256}))))

(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}

  (GET "/authenticated" []
    :return {:authenticated s/Bool}
    :header-params [authorization :- String]
    :summary "Authenticate JWT token"
    (fn [req]
      (let [auth-token (get-in (:headers req) ["authorization"])]
        { :status 200
          :headers {"Content-Type" "application/json" "Authorization" (str "Bearer " auth-token)}
          :body { :authenticated true }
        })))

  (POST "/login" req
    :return {:logged s/Bool}
    :body-params [user-id :- String pass :- String]
    :summary "User login handler"
    (if-let [token (authenticate-user user-id pass)]
      { :status 200
        :headers {"Content-Type" "application/json" "Authorization" (str "Bearer " token)}
        :body { :logged true }
      }
      (not-found "not found")))

  (context "/api" []
    :tags ["thingie"]))
