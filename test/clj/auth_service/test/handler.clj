(ns auth-service.test.handler
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [ring.mock.request :refer :all]
            [auth-service.handler :refer :all]
            [auth-service.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [auth-service.config :refer [env]]
            [auth-service.routes.errors :refer [passwords-do-not-match not-enough-length email-invalid password-invalid]]
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
                            (body (json/write-str {:email "foo@bar.com" :pass "admin1234"}))))
          response-body (slurp (:body response))]
      (is (= {:logged-in true} (json/read-str response-body :key-fn keyword)))
      (is (= 200 (:status response)))))

  (testing "login unsuccessful user does not exist"
    (let [response (app (-> (request :post "/login")
                            (content-type "application/json")
                            (body (json/write-str {:email "first@bar.com" :pass "foo"}))))]
      (is (= 404 (:status response)))))

  (testing "login unsuccessful user exists with wrong password"
    (let [response (app (-> (request :post "/login")
                            (content-type "application/json")
                            (body (json/write-str {:email "foo@bar.com" :pass "foo"}))))]
      (is (= 404 (:status response)))))

  (testing "register successful with user data"
    (let [response (app (-> (request :post "/register")
                            (content-type "application/json")
                            (body (json/write-str {:first_name "first"
                                                   :last_name "last"
                                                   :email "first@gmail.com"
                                                   :pass "@Admin1234"
                                                   :pass_confirmation "@Admin1234"}))))
         response-body (slurp (:body response))]
      (is (= 201 (:status response)))))

  (testing "register unsuccessful when password confirmation is wrong"
    (let [response (app (-> (request :post "/register")
                            (content-type "application/json")
                            (body (json/write-str {:first_name "first"
                                                   :last_name "last"
                                                   :email "first@gmail.com"
                                                   :pass "@Admin1234"
                                                   :pass_confirmation "@Fussy1234"}))))
         response-body (slurp (:body response))]
      (is (= {:errors passwords-do-not-match} (json/read-str response-body :key-fn keyword)))
      (is (= 400 (:status response)))))

  (testing "register unsuccessful when first name is empty"
    (let [response (app (-> (request :post "/register")
                            (content-type "application/json")
                            (body (json/write-str {:first_name ""
                                                   :last_name "last"
                                                   :email "first@gmail.com"
                                                   :pass "@Admin1234"
                                                   :pass_confirmation "@Admin1234"}))))
         response-body (slurp (:body response))]
      (is (= {:errors (not-enough-length :first_name)} (json/read-str response-body :key-fn keyword)))
      (is (= 400 (:status response)))))

  (testing "register unsuccessful when last name is empty"
    (let [response (app (-> (request :post "/register")
                            (content-type "application/json")
                            (body (json/write-str {:first_name "first"
                                                   :last_name "la"
                                                   :email "first@gmail.com"
                                                   :pass "@Admin1234"
                                                   :pass_confirmation "@Admin1234"}))))
         response-body (slurp (:body response))]
      (is (= {:errors (not-enough-length :last_name)} (json/read-str response-body :key-fn keyword)))
      (is (= 400 (:status response)))))

  (testing "register unsuccessful when email is smaller than 5"
    (let [response (app (-> (request :post "/register")
                            (content-type "application/json")
                            (body (json/write-str {:first_name "first"
                                                   :last_name "la"
                                                   :email "f@g.e"
                                                   :pass "@Admin1234"
                                                   :pass_confirmation "@Admin1234"}))))
         response-body (slurp (:body response))]
      (is (= {:errors email-invalid} (json/read-str response-body :key-fn keyword)))
      (is (= 400 (:status response)))))

  (testing "register unsuccessful when email with invalid format"
    (let [response (app (-> (request :post "/register")
                            (content-type "application/json")
                            (body (json/write-str {:first_name "first"
                                                   :last_name "last"
                                                   :email "invalid"
                                                   :pass "@Admin1234"
                                                   :pass_confirmation "@Admin1234"}))))
         response-body (slurp (:body response))]
      (is (= {:errors email-invalid} (json/read-str response-body :key-fn keyword)))
      (is (= 400 (:status response)))))

  (testing "register unsuccessful when password is smaller than 7"
    (let [response (app (-> (request :post "/register")
                            (content-type "application/json")
                            (body (json/write-str {:first_name "first"
                                                   :last_name "last"
                                                   :email "first@google.com"
                                                   :pass "@A1234"
                                                   :pass_confirmation "@A1234"}))))
         response-body (slurp (:body response))]
      (is (= {:errors password-invalid} (json/read-str response-body :key-fn keyword)))
      (is (= 400 (:status response)))))

  (testing "register unsuccessful when password with invalid format"
    (let [response (app (-> (request :post "/register")
                            (content-type "application/json")
                            (body (json/write-str {:first_name "first"
                                                   :last_name "last"
                                                   :email "first@google.com"
                                                   :pass "admin1234"
                                                   :pass_confirmation "admin1234"}))))
         response-body (slurp (:body response))]
      (is (= {:errors password-invalid} (json/read-str response-body :key-fn keyword)))
      (is (= 400 (:status response)))))
  )
