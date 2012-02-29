(ns pallet-meetup.crate.redis
  (:require
   [pallet.core :as pallet-core]
   [pallet.phase :as phase]
   [pallet.action.exec-script :as exec-script]
   [pallet.action.package :as package]
   [pallet-meetup.core :as core]))

(def redis-port 6379)

(defn redis-start
  [session]
  (exec-script/exec-script session "/etc/init.d/redis-server start"))

(defn redis-stop
  [session]
  (exec-script/exec-script session "/etc/init.d/redis-server stop"))

(defn redis-restart
  [session]
  (exec-script/exec-script session "/etc/init.d/redis-server restart"))

(defn configure-redis
  [session]
  (->
    session
    (exec-script/exec-script
      "sed -i s/\"^bind 127\\.0\\.0\\.1\"/\"#bind 127.0.0.1\"/ /etc/redis/redis.conf")
    (redis-restart)))

(defn configure-slave
  [master]
  (phase/phase-fn
    (core/append-remote-file "/etc/redis/redis.conf"
                             (format "slaveof %s %s" master redis-port))
    (redis-restart)))

(def redis-spec
  (pallet-core/server-spec
    :phases {
      :install (phase/phase-fn (package/package "redis-server")
                               (configure-redis))}))

