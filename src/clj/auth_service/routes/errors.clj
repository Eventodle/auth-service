(ns auth-service.routes.errors
  (:require [humanize.schema :as humanize]
            [clojure.string :as str]))

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
                         (not-enough-length (first (map key (:error (ex-data e)))))
                         ['not ['PasswordDoesNotMatch pass]]
                         passwords-do-not-match
                         ['not ['InvalidEmail email]]
                         email-invalid
                         ['not ['InvalidPassword pass]]
                         password-invalid
                         :else x)))))

(defn bad-request-handler
  "Handles bad requests."
  [f]
  (fn [^Exception e data request]
    (let [message (humanize-schema-exception e)]
      (f (cond
           (map? message) {:errors (first (vals message))}
           :else {:errors message})))))
