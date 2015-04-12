#Petrarch
### A travelblog for Ben's trip in SE Asia
#### GPS tracking included

## Install
You must have jre-7, clojure, leiningen, postgres/postgis
```
$ git clone https://github.com/cavedweller/petrarch.git
$ cd petrarch
```

setup a postgres user called dev

````
$ lein ragtime migrate
$ lein figwheel
```
## Running
```
$ lein run
```
point the petrarchmobile client to this IP
