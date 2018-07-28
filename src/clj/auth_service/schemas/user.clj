(ns auth-service.schemas.user 
  (:require [schema.core :as s]))

(defn length-greater [l x]
  (> (count x) l))

(defn length-greater-pred [l pred-name]
  (s/pred #(length-greater l %) pred-name))

(defn matches [r s]
  (re-matches r s))

(defn matches-pred [r pred-name]
  (s/pred #(matches r %) pred-name))

(def min-length
  (s/constrained s/Str #(length-greater 3 %) 'NotEnoughLength))

(def email
  (let [pred-name 'InvalidEmail]
    (s/both (length-greater-pred 5 pred-name) (matches-pred #".+\@.+\..+" pred-name))))

(def password
  (let [pred-name 'InvalidPassword]
    (s/both (length-greater-pred 7 pred-name) (matches-pred #"^(?=.*[A-Za-z])(?=.*\d)(?=.*[$@$!%*#?&])[A-Za-z\d$@$!%*#?&]{8,}$" pred-name))))

(s/defschema RegisterUser
  (s/both
    {:first_name min-length
     :last_name min-length
     :pass password
     :pass_confirmation s/Str
     :email email}
    (s/pred (fn [{:keys [pass pass_confirmation]}]
      (= pass pass_confirmation)) 'PasswordDoesNotMatch)
  )
)


