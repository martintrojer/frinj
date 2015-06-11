# Frinj

Frinj is a practical unit-of-measure calculator DSL for Clojure / ClojureScript.

Key features;

* Tracks units of measure through all calculations allowing you to mix units of measure transparently
* Comes with a huge database of units and conversion factors
* Supports live unit feeds like currency conversion
* Inspired by the Frink project (http://futureboy.us/frinkdocs/)
* Tries to combine Frink's fluent calculation style with idiomatic Clojure
* Supports infix calculation style

## Usage

Add the following line into your Leiningen :dependencies `[frinj "0.2.5"]`.

### Clojure

```clojure
user=> (use 'frinj.repl)
user=> (frinj-init!)
user=> (override-operators!)
user=> (fj 2000 :Calories :per :day :to :watts)
;; "1163/12 (approx. 96.91666666666667) [dimensionless]"
```

### ClojureScript

There are a few differences when running Frinj on Node / in a browser. See [this wiki page](https://github.com/martintrojer/frinj/wiki/ClojureScript) for details.

#### Browser demo

[Visit the browser demo here](http://martintrojer.github.io/frinj-demo/)

To generate the browser demo, run `lein cljsbuild once`. Then start a web server in the [browser-example](browser-example) directory, and visit it. You should see the examples page.

For example;

```
$ cd browser-demo
$ python -m SimpleHTTPServer
```

then visit `http://localhost:8000` in a browser and click on the `demo.html` file.

## Examples

* See [example calculations](https://github.com/martintrojer/frinj/blob/master/examples/examples.clj) for ideas...

* [Video of a frinj talk](http://skillsmatter.com/podcast/home/frinj-having-fun-with-units-3861)

* Examples using infix calcuation style [here](https://github.com/martintrojer/frinj/blob/master/examples/examples-infix.clj)

* Live units for currencies, precious metals etc, [some examples](https://github.com/martintrojer/frinj/wiki/Live-Unit-Feeds#examples)

Finally, check out the [wiki](https://github.com/martintrojer/frinj/wiki) from more info.

## License

Copyright (C) 2013 Martin Trojer

Distributed under the Eclipse Public License, the same as Clojure.
