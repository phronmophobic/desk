# Desk

A Desktop viewer for [clerk](https://github.com/nextjournal/clerk).

![Demo gif](/cells.gif?raw=true)

## Dependency

Currently, only available as a git dependency.

```
com.phronemophobic/desk {:git/url "https://github.com/phronmophobic/desk.git"
                         :git/sha "e4b850558076398a7114dad90bd627d73db01aae"}
```

## Usage



```clojure
(require '[com.phronemophobic.desk :as desk])

;; open window and start watching file
;; for local namespace.
(desk/show!)

```

See [examples](/examples).

## License

Copyright © 2022 Adrian

Distributed under the Eclipse Public License version 1.0.
