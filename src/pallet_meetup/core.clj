(ns pallet-meetup.core
  (:require
    [clojure.string :as s]
    [pallet.action.exec-script :as exec-script]))

(defn lines [& args]
  (s/join "\n" args))

(defn append-remote-file
  [session filename content]
  (->
    session
    (exec-script/exec-script ~(format "touch %s" filename))
    (exec-script/exec-script
      ~(format "echo -e \"%s\" >> %s" content filename))))

(defn append-remote-user-file
  [session user user-group filename content]
  (->
    session
    (append-remote-file filename content)
    (exec-script/exec-script
      ~(format "chown %s:%s %s" user user-group filename))))
