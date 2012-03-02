(ns pallet-meetup.crate.redis
  (:require
   [pallet.core :as pallet-core]
   [pallet.compute :as compute]
   [pallet.phase :as phase]
   [pallet.session :as sess]
   [pallet.action.exec-script :as exec-script]
   [pallet.action.service :as service]
   [pallet.action.package :as package]
   [pallet-meetup.core :as core]))

(def redis-port 6379)
(def redis-config "/etc/redis/redis.conf")

(def master-role :redis-master)
(def slave-role :redis-slave)

(defn redis-start
  [session]
  (service/service session "redis-server" :action :start))

(defn redis-stop
  [session]
  (service/service session "redis-server" :action :stop))

(defn redis-restart
  [session]
  (service/service session "redis-server" :action :restart))

(def configure-redis
  (phase/phase-fn
    (exec-script/exec-script
      ~(format "sed -i s/\"^bind 127\\.0\\.0\\.1\"/\"#bind 127.0.0.1\"/ %s" redis-config))
    (redis-restart)))

(defn redis-slave-config
  [session]
  (let [master-group (str (:prefix session)
                       (name (first (sess/groups-with-role session master-role))))
        master-ip (compute/private-ip
                    (first (sess/nodes-in-group session master-group)))]
    (exec-script/exec-script session
      ~(format "sed -i s/\"^# slaveof <masterip> <masterport>\"/\"slaveof %s %s\"/ %s"
               master-ip redis-port redis-config))))
 
(def configure-slave
  (phase/phase-fn
    (redis-slave-config)
    (redis-restart)))

(def redis-spec
  (pallet-core/server-spec
    :phases {
      :install (phase/phase-fn (package/package "redis-server")
                               configure-redis)}))

(def redis-slave-spec
  (pallet-core/server-spec
    :extends [redis-spec]
    :phases {
      :configure-redis-slaves (phase/phase-fn configure-slave)}))
