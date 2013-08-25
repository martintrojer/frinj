# frinj

Frinj is a practical unit-of-measure calculator DSL for Clojure.

Key features;

* Tracks units of measure through all calculations allowing you to mix units of measure transparently
* Comes with a huge database of units and conversion factors
* Supports live unit feeds like currency conversion
* Inspired by the Frink project (http://futureboy.us/frinkdocs/)
* Tries to combine Frink's fluent calculation style with idiomatic Clojure
* Supports infix calculation style

## Usage

Using Leiningen, create a new project

```sh
$ lein new frinj-example
```

Add the following line to the dependency list in `frinj-example/project.clj`:

```clj
(defproject frinj-example "1.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [frinj "0.2.0"]])
```

Pull the dependencies and start the REPL

```sh
$ cd frinj-example
$ lein deps
$ lein repl
```

Reference and initialize the Frinj calculator

```clj
user=> (use 'frinj.ops)
user=> (frinj-init!)
```

Start calculating!

## Examples

* See [example calculations](https://github.com/martintrojer/frinj/blob/master/src/frinj/examples.clj "example calculations") for ideas...

* [Video of a frinj talk](http://skillsmatter.com/podcast/home/frinj-having-fun-with-units-3861)

* Examples using infix calcuation style [here](https://github.com/martintrojer/frinj/blob/master/src/frinj/examples-infix.clj)

* Live units for currencies, precious metals etc, see [simple examples](https://gist.github.com/2036735)

Finally, check out the [wiki](https://github.com/martintrojer/frinj/wiki) from more info.

## License

Copyright (C) 2013 Martin Trojer

Distributed under the Eclipse Public License, the same as Clojure.
