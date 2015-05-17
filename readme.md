#Petrarch
### A travelblog for Ben's trip in SE Asia
#### GPS tracking included

```
 “Tourist, Rincewind decided, meant 'idiot'.” -- Terry Pratchet
```
## About
This blog was a bit of a (mis)adventure. It uses clojure throughout it's stack, a language that I never had done
anything substantial in before. While I like clojure(script), I definitly would not use it in the same way
this site was built. This site is so web 18.0, it's a single page app that transpiles to JS... overkill and rough around the edges. But... It'll work well enough for this trip.


## Install
You must have jre-7, clojure, leiningen, postgres/postgis
```
$ git clone https://github.com/cavedweller/petrarch.git
$ cd petrarch
```

setup a postgres user called dev

````
$ lein ragtime migrate
$ lein cljsbuild once release
```
## Running
```
$ lein run
```
when sending GPS traces, point the petrarchmobile client to this IP


