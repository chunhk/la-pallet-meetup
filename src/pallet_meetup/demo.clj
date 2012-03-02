(ns pallet-meetup.demo
  (:use pallet.core)
  (:use pallet.compute)
  (:use pallet.crate.automated-admin-user)
  (:require
    [clojure.string :as s]
    [pallet.phase :as phase]
    [pallet.compute.node-list :as node-list]
    [pallet.action.package :as package]
    [pallet-meetup.core :as core]
    [pallet-meetup.environ :as environ]
    [pallet-meetup.crate.redis :as redis]
    [pallet-meetup.crate.shortly :as shortly]))

;; phase order
;; :bootstrap, :configure, :install

(def shortly-node
  (node-spec
    :image {:image-id "us-east-1/ami-809a48e9"}
    :network {:inbound-ports [22 shortly/shortly-port]}))

(def redis-node
  (node-spec
    :image {:image-id "us-east-1/ami-809a48e9"}
    :network {:inbound-ports [22 redis/redis-port]}))

(def base-spec
  (server-spec
    :phases {
      :bootstrap (phase/phase-fn automated-admin-user)}))
 
(def shortly-server
  (server-spec
    :extends [base-spec
              shortly/shortly-spec]
    :node-spec shortly-node))

(def redis-server
  (server-spec
    :extends [base-spec
              redis/redis-spec]
    :node-spec redis-node))

(def redis-slave-server
  (server-spec
    :extends [base-spec
              redis/redis-slave-spec]
    :node-spec redis-node))

(def shortly-group 
  (group-spec "shortly-group" 
    :extends shortly-server))

(def redis-slave-group 
  (group-spec "redis-slave-group" 
    :roles redis/slave-role
    :extends redis-slave-server))

(def redis-master-group
  (group-spec "redis-master-group"
    :roles redis/master-role
    :extends redis-server))

(defn cluster
  ([]
    (cluster (str "c" (str (rand-int 10000)))))
  ([prefix]
  (let [_ (println "cluster group:" prefix)
        corrected-prefix (str prefix "-")]
    {:shortly-tag (keyword (str corrected-prefix "shortly-group"))
     :redis-master-tag (keyword (str corrected-prefix "redis-master-group"))
     :redis-slave-tag (keyword (str corrected-prefix "redis-slave-group"))
     :prefix corrected-prefix
     :compute-service (service environ/aws-provider)
     :compute-service-id environ/aws-provider})))

(defn redis-master-ip
  [cluster ip-type]
  (core/get-first-ip (:redis-master-tag cluster) (:compute-service cluster) ip-type))

(defn redis-slave-ips
  [cluster ip-type]
  (core/get-ips (:redis-slave-tag cluster) (:compute-service cluster) ip-type))

(defn shortly-ips
  [cluster ip-type]
  (core/get-ips (:shortly-tag cluster) (:compute-service cluster) ip-type))

(defn cluster-converge
  ([cluster node-map phases]
    (converge node-map
      :compute (:compute-service cluster)
      :prefix (:prefix cluster)
      :phase phases))
  ([cluster node-map]
    (converge node-map
      :compute (:compute-service cluster)
      :prefix (:prefix cluster))))

(defn cluster-lift
  [cluster groups phases]
  (lift groups
    :compute (:compute-service cluster)
    :prefix (:prefix cluster)
    :phase phases))

(defn startup-instances
  [cluster]
  (cluster-converge 
    cluster
    {shortly-group 2 redis-master-group 1 redis-slave-group 2}
    [:configure]))

(defn instance-setup
  [cluster]
  (cluster-lift cluster
    [shortly-group redis-master-group redis-slave-group] [:install :configure-redis-slaves]))
 
(defn configure-shortly
  [cluster]
  (let [prefix (:prefix cluster)
        redis-master (redis-master-ip cluster :internal)
        redis-slaves (redis-slave-ips cluster :internal)
        configure-shortly (shortly/configure-shortly redis-master redis-slaves)]
    (cluster-lift cluster [shortly-group] [configure-shortly])))

(defn start-shortly
  [cluster]
  (cluster-lift cluster [shortly-group] [shortly/start-shortly]))

(defn start-cluster
  [cluster]
  (startup-instances cluster)
  (instance-setup cluster)
  (configure-shortly cluster)
  (start-shortly cluster))
  
(defn shutdown-cluster
  [cluster]
  (cluster-converge cluster {redis-master-group 0
                             redis-slave-group 0
                             shortly-group 0}))
