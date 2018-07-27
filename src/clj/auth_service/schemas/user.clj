(ns auth-service.schemas.user 
  (:require [schema.core :as s]))

(s/defschema RegisterUser
  (s/both
    {:first_name s/Str
     :last_name s/Str
     :pass s/Str
     :pass_confirmation s/Str
     :email s/Str}
    (s/pred (fn [{:keys [pass pass_confirmation]}]
      (= pass pass_confirmation)) "Password confirmation does not match.")
  )
)


