(ns auth-service.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :refer :all]
            [auth-service.handler :refer :all]
            [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'auth-service.config/env
                 #'auth-service.handler/app)
    (f)))

(deftest test-app
  (testing "redirects to swagger ui"
    (let [response (app (request :get "/swagger-ui"))]
      (is (= 302 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response))))))
