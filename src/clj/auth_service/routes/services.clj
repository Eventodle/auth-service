(ns auth-service.routes.services
  (:require [ring.util.http-response :refer :all]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [buddy.hashers :as hashers]
            [buddy.sign.jwt :as jwt]
            [buddy.core.hash :as hash]
            [auth-service.config :refer [env]]
            [auth-service.db.core :as db]
            [auth-service.middleware :as middleware]))

(defn authenticate-user [user-id pass]
  (when-let [user (db/get-user {:id user-id})]
    (when (hashers/check pass (:pass user))
      (jwt/encrypt (merge {:exp (tc/to-long (time/plus (time/now) (time/days 1)))}
                          (dissoc user :pass)) (hash/sha256 (:jwt-private-key env)) {:alg :dir :enc :a128cbc-hs256}))))
(defn login [req]
  (let [user-id (:user-id (:body-params req))
        pass (:pass (:body-params req))]
    (if-let [token (authenticate-user user-id pass)]
      {:status 200
       :headers {"Content-Type" "application/json" "Authorization" (str "Bearer " token)}
       :body {:logged-in true}}
      (not-found "not found"))))

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}

  (GET "/authenticated" []
    :middleware [middleware/wrap-auth]
    :return {:authenticated s/Bool}
    :header-params [authorization :- String]
    :summary "Authenticate JWT token")

  (POST "/login" []
    :return {:logged-in s/Bool}
    :body-params [user-id :- String pass :- String]
    :summary "User login handler"
    login)

  (context "/api" []
    :tags ["thingie"]))
