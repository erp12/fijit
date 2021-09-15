# FAQ

## Will fijit ever support Scala 3?

The short answer is: probably not.

The original motivation for building fijit was to facilitate the development of Clojure interfaces into Scala-native
projects like [Apache Spark](https://spark.apache.org/) and [Akka](https://akka.io/). 
At the time of writing, neither of these tools run on Scala 3 and will likely not be switching for a long time. 

That said, fijit already leverages macros to delegate to different implementations depending on the Scala version.
It is possible that a Scala 3 implementation is possible for the entire fijit API, it is possible that one day fijit
could support Scala 3. 

If you are inclined to help with this effort, please see the [contributing guide](https://github.com/erp12/fijit/blob/master/CONTRIBUTING.md).

## Why the name "fijit"?

Clojure and Scala are both:

- **F**unctional.
- **I**mmutable-first.
- **J**vm hosted (primarily).

[Cora Sutton](https://dev.to/corasaurus_hex) of [Clojure Morsels](https://www.clojuremorsels.com/)
also noticed that "figit" is an inflection of the latin word "[figo](https://www.wordsense.eu/figit/)" which means "to fasten".
A happy coincidence that I find too perfect to not write down.

## How to pronounce "fijit"?

Same as "fidget".
