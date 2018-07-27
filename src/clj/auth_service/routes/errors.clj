(ns auth-service.routes.errors
  (:require [humanize.schema :as humanize]))

(def passwords-do-not-match-error "Password and password confirmation don't match")

(defn humanize-schema-exception [^Exception e]
  (if (instance? schema.utils.ErrorContainer (ex-data e))
    (humanize/explain (:error (ex-data e))
                      (fn [x]
                        (clojure.core.match/match
                         x
                         ['not ['PasswordDoesNotMatch pass]]
                         passwords-do-not-match-error
                         :else x)))))

(defn bad-request-handler
  "Handles bad requests."
  [f]
  (fn [^Exception e data request]
    (let [message (humanize-schema-exception e)]
      (f {:errors message}))))
