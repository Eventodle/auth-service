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
            [auth-service.schemas.user :as us]
            [auth-service.middleware :as middleware]
            [compojure.api.exception :as ex]
            [auth-service.routes.errors :refer [bad-request-handler]]))


(defn authenticate-user [email pass]
  (when-let [user (db/get-user-by-email {:email email})]
    (when (hashers/check pass (:pass user))
      (jwt/encrypt (merge {:exp (tc/to-long (time/plus (time/now) (time/days 1)))}
                          (dissoc user :pass)) (hash/sha256 (:jwt-private-key env)) {:alg :dir :enc :a128cbc-hs256}))))
(defn login [req]
  (let [email (:email (:body-params req))
        pass (:pass (:body-params req))]
    (if-let [token (authenticate-user email pass)]
      {:status 200
       :headers {"Content-Type" "application/json" "Authorization" (str "Bearer " token)}
       :body {:logged-in true}}
      (not-found "not found"))))

(defapi service-routes
  {:exceptions {:handlers {::ex/request-validation (bad-request-handler bad-request)}}
   :swagger {:ui "/swagger-ui"
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
    :body-params [email :- String pass :- String]
    :summary "User login handler"
    login)

  (POST "/register" []
    :return us/RegisterUser
    :body [user us/RegisterUser]
    :summary "User login handler"
    (created "url" user))

  (context "/api" []
    :tags ["thingie"]))
