# pallet-meetup

Sample Project for Los Angeles Clojure Meetup on Pallet.
Deploys two url shortener webapp servers and redis master-slaves

## Usage

### Create ~/.pallet/config.clj

(defpallet
  :services
    {:aws {:provider "aws-ec2" 
           :identity "\<access-key-id\>"
           :credential "\<secret-access-key\>"}})

### Run Project

lein deps

lein repl

=> (use 'pallet-meetup.demo)

=> (def c (cluster))

=> (start-cluster c)

### Get External Shortly IP

=> (shortly-ips c :external)

point browser to http://\<ip-address\>:5000

### Shutdown Cluster

=> (shutdown-cluster c)
