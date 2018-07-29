(ns auth-service.routes.errors
  (:require [humanize.schema :as humanize]
            [clojure.string :as str]
            [clojure.test :as ct]))

(def passwords-do-not-match "Password and password confirmation don't match")

(def email-invalid "E-mail invalid")

(def password-invalid "Password invalid")

(defn not-enough-length [field]
  (str (str/capitalize (str/join " " (str/split (name field) #"_"))) " does not have the minimum size"))

(defn humanize-schema-exception [^Exception e]
  (if (instance? schema.utils.ErrorContainer (ex-data e))
    (humanize/explain (:error (ex-data e))
                      (fn [x]
                        (clojure.core.match/match
                         x
                         ['not ['NotEnoughLength value]]
                           ;;(not-enough-length (first (map key (:error (ex-data e)))))
                          not-enough-length
                         ['not ['PasswordDoesNotMatch pass]]
                           passwords-do-not-match
                         ['not ['InvalidEmail email]]
                           email-invalid
                         ['not ['InvalidPassword pass]]
                           password-invalid
                         :else x)))))

(defn build-error-response [message]
  {:source {:pointer "/data/attributes/"} :title "Invalid Attribute" :detail message})

(defn unprocessable-entity-handler
  "Handles bad requests."
  [f]
  (fn [^Exception e data request]
    (let [message (humanize-schema-exception e)]
      (f {:errors (cond
                    (map? message) (map (fn [[key val]] (build-error-response (cond (ct/function? val) (val key) :else val))) message)
                    :else [(build-error-response message)])}))))
