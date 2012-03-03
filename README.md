# frinj

Frinj is a practical unit-of-measure calculator DSL for Clojure.

Key features;

* Tracks units of measure allowing you to mix units of measure transparently
* Comes with a huge database of units and conversion factors
* Inspired by the Frink project (http://futureboy.us/frinkdocs/)
* Tries to combine Frink's fluent calculation style with idiomatic Clojure

## Usage

Using Leiningen, create a new project

```sh
$ lein new frinj-example
```

Add the following line to the dependency list in `frinj-example/project.clj`:

```clj
(defproject example "1.0"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [frinj "0.1.0"]])
```

Pull the dependencies and start the REPL

```sh
$ lein deps
$ lein repl
```

Reference and initialize the Frinj calculator

```clj
user=> (use 'frinj.calc)
user=> (frinj-init!)          ;; this will reset all units to the defaults
```

Start calculating! See the [example calculations](https://github.com/martintrojer/frinj/blob/master/src/frinj/examples.clj "example calculations") for ideas...

## License

Copyright (C) 2012 Martin Trojer

Distributed under the Eclipse Public License, the same as Clojure.
