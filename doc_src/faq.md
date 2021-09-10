# Frequently Asked Questions

## Will Fijit ever support Scala 3?

The short answer is: probably not.

The original motivation for building Fijit was to facilitate the development of Clojure interfaces into Scala-native
projects like [Apache Spark](https://spark.apache.org/) and [Akka](https://akka.io/). 
At the time of writing, neither of these tools run on Scala 3 and will likely not be switching for a long time. 

That said, Fijit already leverages macros to delegate to different implementations depending on the Scala version.
It is possible that a Scala 3 implementation is possible for the entire Fijit API, it is possible that one day Fijit
could support Scala 3. 

If you are inclinded to help with this effort, please see the [contributing guide](erp12.github.io/fijit/contributing.html).

## Why the name "Fijit"?

Clojure and Scala are both:
- **F**unctional.
- **I**mmutable-first.
- **J**vm hosted (primarily).
