(ns pallet-meetup.crate.shortly
    (:require
     [clojure.string :as s]
     [pallet.phase :as phase]
     [pallet.core :as pallet-core]
     [pallet.action.exec-script :as exec-script]
     [pallet.action.package :as package]
     [pallet.action.remote-file :as remote-file]
     [pallet-meetup.core :as core]))

(def shortly-path "/usr/local/shortly")
(def shortly-config "/usr/local/shortly/examples/shortly/shortly.cfg")
(def shortly-virtualenv "/usr/local/shortly-virtualenv")
(def shortly-git-repo "https://github.com/chunhk/werkzeug.git")
(def shortly-log "/var/log/shortly.log")

(def shortly-port 5000)

(defn setup-virtualenv
  [session]
  (->
    session
    (exec-script/exec-script
      ~(format "virtualenv %s" shortly-virtualenv)
      ~(format "%s/bin/pip install werkzeug redis Jinja2"
               shortly-virtualenv))))
      
(defn install-shortly
  [session]
  (->
    session
    (exec-script/exec-script ~(format "git clone %s %s"
                                      shortly-git-repo
                                      shortly-path))))

(defn configure-shortly
  [redis-master redis-slaves]
  (phase/phase-fn
    (remote-file/remote-file shortly-config
      :mode "664"
      :literal true
      :content (core/lines
                 "[logging]"
                 (format "logfile=%s" shortly-log)
                 "[redis]"
                 (format "master=%s" redis-master)
                 (format "slaves=%s" (s/join "," redis-slaves))))))

(def start-shortly
  (phase/phase-fn
    (exec-script/exec-script
      ~(format "nohup %s/bin/python %s/examples/shortly/shortly.py %s &"
               shortly-virtualenv shortly-path shortly-config))))

(def shortly-spec
  (pallet-core/server-spec
    :phases {
      :bootstrap (phase/phase-fn (package/package "git-core"))
      :configure (phase/phase-fn (package/package "python-virtualenv"))
      :install (phase/phase-fn
                 (setup-virtualenv)
                 (install-shortly))}))
