# Guide to Fijit

Clojure is a great language, but some people use Scala... and that's okay!

## Rationale

Should you create a hybrid Clojure and Scala project? ... probably not. Each language assumes a 
wildly different opinion on what makes an effective program.

What if you are *forced* to call out to a Scala project? Maybe you want to sneak some Clojure into your
company's Scala stack! Luckily, Clojure's standard JVM interop features usually do a good job.

So what is the value of Fijit?

**Fijit makes interop easier by providing an idiomatic Clojure API into Scala constructs that commonly appear in 
Scala interfaces.**

In particular, Fijit was created to facilitate the development of idiomatic Clojure wrappers around the popular
information processing frameworks that are available in Scala. For example, 
[Apache Spark](https://spark.apache.org/) and [Akka](https://akka.io/). 
Clojure is a natural fit for these domains, but it can be difficult to leverage given the differences between
Clojure and Scala (the host language of these frameworks). 

In addition, Fijit provides some useful macros that leverage Clojure's compile-on-load architecture to help you write
code that targets multiple Scala versions. Scala is famous for having an interesting relationship with binary 
incompatibility [1].

is in a constant war with 
[source and binary incompatibility](https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html)
(which stands in stark contrast to Clojure :smile:) and it isn't uncommon for Scala projects to stay change the upgrade Scala to the next

## Installation

Fijit does not assume a specific Scala version, thus you must declare both Fijit and Scala as dependencies to 
your project. In particular, you need to depend on `scala-library`, `scala-reflect`, and `scala-compiler` 
all at the same version.

If we wanted to build our project against Scala 2.13.6 we would specify the following dependencies:

```clojure
{:deps {io.github.erp12/fijit {:git/tag "1.0.7" :git/sha "5009b4d"}
        org.scala-lang/scala-library  {:mvn/version "2.13.6"}
        org.scala-lang/scala-reflect  {:mvn/version "2.13.6"}
        org.scala-lang/scala-compiler {:mvn/version "2.13.6"}}}
```

If we wanted to target multiple versions of Scala, we would use an alias for each version, 
and depend on fijit at the project level.

```clojure
{:deps {io.github.erp12/fijit {:git/tag "1.0.7" :git/sha "5009b4d"}}
 :aliases {:2.12  {:extra-deps {org.scala-lang/scala-library  {:mvn/version "2.12.13"}
                                org.scala-lang/scala-reflect  {:mvn/version "2.12.13"}
                                org.scala-lang/scala-compiler {:mvn/version "2.12.13"}}}
           :2.13  {:extra-deps {org.scala-lang/scala-library  {:mvn/version "2.13.6"}
                                org.scala-lang/scala-reflect  {:mvn/version "2.13.6"}
                                org.scala-lang/scala-compiler {:mvn/version "2.13.6"}}}}}
```

We recommend declaring your fijit dependency with git coordinates. Future releases will likely also be 
published to Clojars.

## Usage Overview

The following sections will demonstrate some core features provided by fijit. 
For the complete API, see [the documentation site](https://erp12.github.io/fijit/).

### Collections

Using the `erp12.fijit.collection` namespace, Scala collections can be created with `scala-*` functions. 
These functions mirror their Clojure counterparts.

> Note: Exact return types may change depending on your Scala version.

```clojure
(use 'erp12.fijit.collection)

(scala-list :a :b)
; => #object[scala.collection.immutable.$colon$colon 0x58acad38 "List(:a, :b)"]

(scala-vector :a :b)
; => #object[scala.collection.immutable.Vector1 0x27571648 "Vector(:a, :b)"]

(scala-set :a :b :a)
; => #object[scala.collection.immutable.Set$Set2 0x40104fb7 "Set(:a, :b)"]

(scala-map :a 1
           :b 2
           :c 3)
; => #object[scala.collection.convert.JavaCollectionWrappers$JMapWrapper 0x28a5f7e1 "Map(:a -> 1, :b -> 2, :c -> 3)"]
```

Clojure collections can be converted to Scala collections using the `to-scala-*` functions.
Many of these functions are flexible with respect to the exact collection type passed as input.

```clojure
(to-scala-list [:a :b])
; => #object[scala.collection.immutable.$colon$colon 0x1320cfab "List(:a, :b)"]

(to-scala-vector [:a :b])
; => #object[scala.collection.immutable.Vector1 0x54816c33 "Vector(:a, :b)"]

(to-scala-set #{1 1 1})
; => #object[scala.collection.immutable.Set$Set1 0x48b97f32 "Set(:a)"]

(to-scala-map {:a 1 :b 2})
; => #object[scala.collection.convert.JavaCollectionWrappers$JMapWrapper 0x64be80db "Map(:a -> 1, :b -> 2)"]
```

Scala collections can be converted to Clojure collections. These functions mostly assume that the
type of the collection is compatible with the collection type mentioned in the function name. For
example, the `map->clj` function expects to be passed an instance of `scala.collection.Map`.

```clojure
(seq->clj (scala-seq :a :b))
; => (:a :b)

(vector->clj (scala-vector :a :b))
; => [:a :b]

(set->clj (scala-set :a :b))
; => #{:b :a}

(map->clj (scala-map :a 1 :b 2))
; => {:a 1, :b 2}
```

In some scenarios, there is no need to be explicit about the collection type we want to create, but
rather we would like the logical collection type for the other host language. This is more commonly
the case when converting Scala collections to Clojure collections. 

In these cases, the `->clj` and `->scala` functions can be used to generically convert collections
using a logically associated types. For example, Scala vectors to Clojure vectors and Scala maps to
Clojure maps.

These mappings are not applies recursively because this could result in significant extra work
in scenarios where the inner collections are intended to be passed back to an abstraction written
in their types host language. For example, if we have a Scala `Seq[Map[String,Int]]` and would 
like to write a some Clojure code that passed each `Map` to a Scala function, only the outer `Seq`
should be converted to a Clojure collection.

### Functions

Scala provides a different interface (trait) for function types of each arity. For example, a function
that takes 1 parameters will implement `scala.Function1`. Fijit provides a suite of `deftype` that implement
Scala's function interfaces, as well as `java.io.Serializable` and `clojure.lang.IFn`. These types simply
wrap a Clojure function that is assumed to have the same arity as the desired Scala function type. These 
types can be constructed useing the `->fn*` functions.

```clojure
(require '[erp12.fijit.function :as sf])

(def add-one 
  (sf/->fn1 #(+ 1 %))) 

add-one
; =>  #object[erp12.fijit.function.Function1 0x29429270 "erp12.fijit.function.Function1@29429270"]

(instance? scala.Function1 add-one)
; => true

; Invoked as a Scala function.
(.apply add-one 10) ; => 11

; Invoked as a Clojure function.
(add-one 10) ; => 11 

(def bad-fn2
  ; The `identity` function takes 1 parameter, but we are building a `scala.Function2`.
  (sf/->fn2 identity))

; The bad-fn2 is creatable...
bad-fn2
; => #object[erp12.fijit.function.Function2 0x67539f2 "erp12.fijit.function.Function2@67539f2"]

; ... but it cannot be invoked.
(.apply bad-fn2 :A) ; throws IllegalArgumentException
(bad-fn2 :A) ; throws AbstractMethodError
```

Fijit also provides a macro for declaring Scala functions that will implicitly create a Clojure funciton
and wrap it in the correct fijit funciton wrapper. The `scala-fn` macro can be called very similarly to the
Clojure core `fn` macro.

```clojure
(def plus (sf/scala-fn [a b] (+ a b)))

(instance? scala.Function2 plus)
; => true

; Invoked as a Scala function.
(.apply plus 1 2) ; => 3

; Invoked as a Clojure function.
(plus 1 2) ; => 3
```

### Option and Try

Scala represents optional values with the wrapper type `scala.Option`. Many Scala interfaces
require `Option` objects as input and provide `Option` objects as returned values.

The object for non-existing values (`scala.None`) is a singleton. Fijit provides
this as a `def`.

```clojure
(use 'erp12.fijit.option)

none
;=> #object[scala.None$ 0x14e04246 "None"]
```

Creating `Option`s can be done with `option` function. If provided `nil` or no arguments, it will
return `None`. Otherwise it will return a `Some` that wrapps the given value.

```clojure
(option)
; => #object[scala.None$ 0x14e04246 "None"]

(option nil)
; => #object[scala.None$ 0x14e04246 "None"]

(option :A)
; => #object[scala.Some 0x6e742b26 "Some(:A)"]
```

Fijit also provides a variety of ways to handle `Option` values in our Clojure projects.
The `emtpy?` and `defined?` predicates can be used check if the `Option` is holding a 
value. 

```clojure
(require '[erp12.fijit.option :as opt])

(opt/empty? opt/none) ; => true
(opt/empty? (opt/option :A)) ; => false

(opt/defined? opt/none) ; => false
(opt/defined? (opt/option :A)) ; => true
```

Option objects can be unpacked with the `get` macro. If not passed an `or-else` form, a `NoSuchElementException`
will be thrown. 

```clojure
(opt/get (opt/option :A)) ; => :A
(opt/get none) ; throws NoSuchElementException
(opt/get none :not-found) ; => :not-found
```

Side effects and custom error handling can be implemented in the `or-else` form of the `get` macro because
it will not be evaluated unless the option is empty.

```clojure
(opt/get opt/none (throw (ex-info "Oh no!" {}))) ; throws ExceptionInfo
```

The idiomatic Clojure representation of a missing value is `nil`, thus it is common to unpack options into 
their held value or `nil` if the option is `None`. Fijit provides a function with this behavior. 

```clojure
(opt/get-or-nil (opt/option :A)) ; => :A
(opt/get-or-nil opt/none) ; => nil
```

### Try

The Scala `Try` type represents the result of a computation that either raised an exception, or successfully
returned a computed value. 

We can create `Try` objects using the `scala-try` macro.

```clojure
(require '[erp12.fijit.try :as t])

(t/scala-try (/ 4 2))
; => #object[scala.util.Success 0x5c1bfc1c "Success(2)"]

(t/scala-try (/ 4 0))
; => #object[scala.util.Failure 0x1488471 "Failure(java.lang.ArithmeticException: Divide by zero)"]
```

The `failure?` and `success?` predicates can be used check if a `Try` is representing a raised exception or
a successfully computed value.

```clojure
(t/failure? (t/scala-try (/ 4 2))) ; => false
(t/failure? (t/scala-ty (/ 4 0))) ; => true

(t/success? (t/scala-try (/ 4 2))) ; => true
(t/success? (t/scala-ty (/ 4 0))) ; => false
```

Try objects can be unpacked with the `get` macro (different from the `get` macro in other fijit namespaces).

If no `or-else` form is provided and the `Try` is a `Failure`, the exception held by the try will be thrown.
If an `or-else` is provided, it will be evaluated in the event the that try is a `Failure` instead of raising
the exception. Some use cases for this behavior include: throwing a more specific error or performing a "retry".

```clojure
(t/get (t/scala-try (/ 4 2))) ; => 2
(t/get (t/scala-try (/ 4 0))) ; throws ArithmeticException
(t/get (t/scala-try (/ 4 0)) 0) ; => 0

(defn my-div
  [n d] 
  (t/get (t/scala-try (/ n d)) 
         (throw (ex-info "Failed division!" {:numerator n :denominator d}))))

(my-div 3 0) ; throws clojure.lang.ExceptionInfo: Failed division! {:numerator 3, :denominator 0}
```

### Tuple

Tuples are the canonical representation of Scala `Product` types. Scala uses a different type to denote a scala
of each size. For exaple, 2 element tuples are represented as instances of `Tuple2`.

We can create Scala tuples with fijit in a couple ways. The first is the `scala-tuple` function, which will 
accept a variable number of elements and return a Scala tuple. The second is the `to-tuple` function, which will
converts a sequential Clojure collection to a tuple.

```clojure
(require '[erp12.fijit.tuple :as tup])

(tup/scala-tuple :a 1)
; => #object[scala.Tuple2 0x4fc03728 "(:a,1)"]

(tup/scala-tuple :a)
; => #object[scala.Tuple1 0x5dbab3c5 "(:a)"]

(tup/to-tuple [:a 1])
; => #object[scala.Tuple2 0x6c62f09e "(:a,1)"]
```

Scala tuples can be converted to a Clojure vector using the `product->vec` function. This function can be used on 
any product, including instances of case classes.

```clojure
(tup/product->vec (tup/scala-tuple :a 1)) 
; => [:a 1]
```

### Targeting multiple Scala versions

Fijit helps us write Clojure code that safely runs on top multiple Scala versions!

First, it can be helpful to know at runtime which exact version of Scala we are running. The `scala-version`
symbol will provide a map of version-parts and their numeric value.

```clojure
(require '[erp12.fijit.version :as sv])

sv/scala-version
; => {:major 2, :minor 13, :patch 6}
```

More importantly, the `by-scala-version` macro will deligate to a different form depending on the Scala version.
We can supply pairs of scala versions and their implementations and let Clojure's compile-on-load architecture
sort out how to compile the byte code.

Consider the following Clojure code:

```clojure
(sv/by-scala-version :2.12    :A
                     :2.12.10 :B
                     :2.13.4  :C)
```

The behavior will be different depending on the Scala version. Specifically the above code will...

- Return `:A` on Scala 2.12.0 through 2.13.9.
- Return `:B` for :2.12.10 and all other 2.12.x versions.
- Throw and exception for scala 2.13.0 through 2.13.3.
- Return `:C` for 2.13.4 and all other 2.13.x versions.

In other words, the form that corresponds to the highest compatible version will be executed. A compatible version
is one that has the same major and minor versions as the current version of Scala on the classpath. If a form's
version keyword contains a patch version, it will only be valid if the patch version of the active Scala version
is at least the same number.

______________
 
[1] Want to read more about Scala and binary incompatibility? There are some official docs on the subject 
    [here](https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html) and
    [here](https://docs.scala-lang.org/overviews/core/binary-compatibility-of-scala-releases.html). 
    The Apache Spark project has also served as a good example of how difficult upgrading to a new minor version
    (or supporting multiple minor versions) can be. See [this discussion](https://contributors.scala-lang.org/t/spark-as-a-scala-gateway-drug-and-the-2-12-failure/1747) 
    about migrating from 2.11 to 2.12 as well as [this ticket](https://issues.apache.org/jira/browse/SPARK-25075)
    about making Spark compatible with 2.13.
