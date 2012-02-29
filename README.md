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

=> (stop-cluster c)

