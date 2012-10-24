# frinj

Frinj is a practical unit-of-measure calculator DSL for Clojure.

Key features;

* Tracks units of measure through all calculations allowing you to mix units of measure transparently
* Comes with a huge database of units and conversion factors
* Supports live unit feeds like currency conversion
* Inspired by the Frink project (http://futureboy.us/frinkdocs/)
* Tries to combine Frink's fluent calculation style with idiomatic Clojure
* Supports infix calculation style (in 0.1.4-SNAPSHOT)

## Usage

Using Leiningen, create a new project

```sh
$ lein new frinj-example
```

Add the following line to the dependency list in `frinj-example/project.clj`:

```clj
(defproject frinj-example "1.0"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [frinj "0.1.3"]])
```

Pull the dependencies and start the REPL

```sh
$ cd frinj-example
$ lein deps
$ lein repl
```

Reference and initialize the Frinj calculator

```clj
user=> (use 'frinj.repl)
```

Start calculating! 

## Examples

* See [example calculations](https://github.com/martintrojer/frinj/blob/master/src/frinj/examples.clj "example calculations") for ideas...

* Examples using infix calcuation style (0.1.4-SNAPSHOT) [here](https://github.com/martintrojer/frinj/blob/master/src/frinj/examples-infix.clj)

* Live units for currencies, precious metals etc, see [simple examples](https://gist.github.com/2036735)

Finally, check out the [wiki](https://github.com/martintrojer/frinj/wiki) from more info.

## License

Copyright (C) 2012 Martin Trojer

Distributed under the Eclipse Public License, the same as Clojure.
