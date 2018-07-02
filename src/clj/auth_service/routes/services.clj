(ns auth-service.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.hashers :as hashers]
            [buddy.auth :refer [authenticated?]]
            [buddy.sign.jwt :as jwt]
            [auth-service.db.core :as db]))

(defn authenticate-user [user-id pass]
  (when-let [user (db/get-user {:id user-id})]
    (when (hashers/check pass (:pass user))
      (jwt/sign (dissoc user :pass) "secret"))))

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
       :auth-rules authenticated?
       :current-user user
       (ok {:user user}))

  (POST "/login" req
    :return {:token s/Str}
    :body-params [user-id :- String pass :- String]
    :summary "User login handler"
    (if-let [token (authenticate-user user-id pass)]
      (ok {:token token})
      (not-found "not found")))

  (context "/api" []
    :tags ["thingie"]))
