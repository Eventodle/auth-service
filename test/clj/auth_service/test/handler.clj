(ns auth-service.test.handler
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [ring.mock.request :refer :all]
            [auth-service.handler :refer :all]
            [auth-service.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [auth-service.config :refer [env]]
            [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'auth-service.config/env
                 #'auth-service.db.core/*db*
                 #'auth-service.handler/app)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-app
  (testing "redirects to swagger ui"
    (let [response (app (request :get "/swagger-ui"))]
      (is (= 302 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "login successful with user and password"
    (let [response (app (-> (request :post "/login")
                            (content-type "application/json")
                            (body (json/write-str {:user-id "first" :pass "admin1234"}))))
          response-body (slurp (:body response))]
      (is (= {:logged-in true} (json/read-str response-body :key-fn keyword)))
      (is (= 200 (:status response)))))

  (testing "login unsuccessful user does not exist"
    (let [response (app (-> (request :post "/login")
                            (content-type "application/json")
                            (body (json/write-str {:user-id "second" :pass "foo"}))))]
      (is (= 404 (:status response)))))

  (testing "login unsuccessful user exists with wrong password"
    (let [response (app (-> (request :post "/login")
                            (content-type "application/json")
                            (body (json/write-str {:user-id "first" :pass "foo"}))))]
      (is (= 404 (:status response)))))


  )
