(ns auth-service.schemas.user 
  (:require [schema.core :as s]))


(def min-length
  (s/constrained s/Str #(> (count %) 3) 'NotEnoughLength))

(s/defschema RegisterUser
  (s/both
    {:first_name min-length
     :last_name min-length
     :pass s/Str
     :pass_confirmation s/Str
     :email s/Str}
    (s/pred (fn [{:keys [pass pass_confirmation]}]
      (= pass pass_confirmation)) 'PasswordDoesNotMatch)
  )
)


