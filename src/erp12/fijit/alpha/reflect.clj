(ns ^{:doc "Interface into `scala-reflect` and `scala-compiler`.

> **WARNING:** This is an experimental (alpha) namespace. It is published in order to facilitate community feedback in the
overall design. Expect minor breaking changes in this namespace before a stable design is moved to the non-alpha namespace.

## TypeTag and ClassTag

Scala provides an abstractions for inspecting types.

- The `ClassTag` type can provide information about a runtime class (post type-erasure). All information about
type parameters is lost.
- The `TypeTag` type can provide all type information that the Scala compiler has available at compile time. This
includes all type parameter information.

You can read more about Scala type/class tags on their [Scala documentation page](https://docs.scala-lang.org/overviews/reflection/typetags-manifests.html).

Scala libraries will often expose abstractions that require a type-tag (or class-tag) argument that
corresponds to the data type of another argument. In Scala, the type-tags are typically provided implicitly
but in Clojure they must be provided explicitly.

Class-tag objects are created from Class objects.

```
(class-tag String)
; => #object[scala.reflect.ClassTag$GenericClassTag 0x7c172ce2 \"java.lang.String\"]
```

Fijit exposes two ways to create type-tags.

1. From a class object. This only works for types with no generic parameters.

    ```scala
    (type-tag String)
    ; => #object[scala.reflect.api.TypeTags$TypeTagImpl 0x7c172ce2 \"TypeTag[String]\"]

    (type-tag scala.Product)
    ; => #object[scala.reflect.api.TypeTags$TypeTagImpl 0x1e85e9f0 \"TypeTag[Product]\"]
    ```

2. From a Scala `Type` object (see next section).

    ```
    (type-tag (scala-type scala.Option [Long]))
    ; => #object[scala.reflect.api.TypeTags$TypeTagImpl 0x35f97f3b \"TypeTag[Option[Long]]\"]
    ```

Creating a type-tag can be quite slow in many situations. By default, Fijit will memoize the creation of type-tags
so that any repeated calls with the same type will be faster. In addition, the type-tags for most simple Scala types
(`scala.Int`, `scala.Double`, etc.) are available provided as values in this namespace.

## Scala Types

A Scala `Type` object provides information about a type, including its:

- Members (methods, fields, type aliases, abstract types, nested classes, traits, etc.) either declared directly or inherited.
- Base types
- Erasure
- Conformance
- Equivalence

Fijit provides a function to create Scala type objects.

```
(scala-type java.lang.String)
; => #object[scala.reflect.internal.Types$ClassNoArgsTypeRef 0x29ed2fb4 \"String\"]

(scala-type scala.Option [Integer])
; => => #object[scala.reflect.internal.Types$ClassArgsTypeRef 0x1f8ceb \"Option[Integer]\"]

; Scala types can be parameterized with other Scala types
(scala-type scala.collection.Seq [(scala-type scala.Option [scala.Int])])
; => #object[scala.reflect.internal.Types$ClassArgsTypeRef 0x665c3f13 \"scala.collection.Seq[Option[Int]]\"]
```

### Inspecting Types

Fijit provides 2 ways of inspecting Scala types.

1. The `type-reflect` function inspired by `clojure.reflect/type-reflect`. It returns all information about a type as
data. Unlike Clojure's reflect, the Fijit version allow you to inspect the members as seen by Scala. This means
that some types will have class, trait, and type members according to the path-dependant types. Each member's data
includes a Scala symbol for use in further inspection. See the Fijit `type-inspect` docstring for more information.
2. Functions corresponding to the `Type` methods found in Scala.

## Scala Abstract Syntax Trees

Scala `Tree` objects are the basis of Scala’s abstract syntax which is used to represent programs. See the
official [Scala docs on Trees](https://docs.scala-lang.org/overviews/reflection/symbols-trees-types.html#trees)
for more information.

Fijit provides a suite of functions that each correspond to a different kind of node in the Scala AST. For example,
the `class-def` function is used to create a `ClassDef` node.

Another way to create a `Tree` is to call the `parse` function and pass in a string containing valid Scala code.

### Compilation

After constructing a Scala AST, it can be used in a variety of ways.

- The `compile` function will compile the tree. This will attempt to resolve all type variables, and type check the tree.
- The `define` function will defines a top-level class, trait or module in a uniquely-named package and return
a symbol that references the defined entity.
- The `eval` function will compile and run a tree, returning the resulting value.

"}
  erp12.fijit.alpha.reflect
  (:refer-clojure :exclude [eval compile import apply empty? symbol class? name empty])
  (:require [clojure.core :as core]
            [clojure.string :as str]
            [erp12.fijit.collection :as sc]
            [erp12.fijit.option :as opt]
            [erp12.fijit.core :refer [unit]])
  (:import (scala.reflect.api JavaUniverse TypeTags$TypeTag Mirrors$ClassMirror Mirrors$InstanceMirror Types$TypeApi Names$NameApi Symbols$SymbolApi Mirrors$MethodMirror Mirrors$FieldMirror Printers$BooleanFlag)
           (scala.reflect.runtime JavaMirrors$JavaMirror)
           (scala.reflect.internal Types$Type Trees$Modifiers Names$TermName Trees$Template Trees$Tree Names$TypeName Names$Name Trees$ValDef Trees$RefTree Symbols$TypeSymbol Symbols$ClassSymbol Symbols$MethodSymbol Symbols$TermSymbol Symbols$Symbol Constants$Constant Trees$PackageDef Trees$ClassDef Trees$ModuleDef Trees$DefDef Trees$TypeDef Trees$LabelDef Trees$ImportSelector Trees$Block Trees$CaseDef Trees$Alternative Trees$Star Trees$Import Trees$Bind Trees$UnApply Trees$Function Trees$Assign Trees$If Trees$Annotated Trees$SingletonTypeTree Trees$SelectFromTypeTree Trees$CompoundTypeTree Trees$AppliedTypeTree Trees$TypeBoundsTree Trees$ExistentialTypeTree Trees$Literal Trees$Ident Trees$Select Trees$This Trees$Super Trees$Apply Trees$TypeApply Trees$Match Trees$Return Trees$Try Trees$Throw Trees$New Trees$Typed Trees$TypeTree Symbols$ModuleSymbol)
           (scala.reflect ClassTag)
           (scala.collection.immutable List)))

;; @todo Add more functions from the the `*Api` traits.
;; @todo Can we keep a global atom of type tags to make them "implicit"?

(def ^{:doc "The runtime reflection universe."
       :tag JavaUniverse}
  universe
  (.universe (scala.reflect.runtime.package$/MODULE$)))

(def ^{:doc "The current thread's context class loader."
       :tag ClassLoader}
  class-loader
  (.getContextClassLoader (Thread/currentThread)))

(def ^{:doc "The reflective mirror of the universe. Used for loading Symbols by name, and as an entry point into invoker mirrors."
       :tag JavaMirrors$JavaMirror}
  mirror
  (.runtimeMirror universe class-loader))

(def ^{:doc "A toolbox that can be used to invoke the scala compiler."
       :tag scala.tools.reflect.ToolBox}
  toolbox
  (-> scala.tools.reflect.package$/MODULE$
      (.ToolBox mirror)
      (.mkToolBox (.mkSilentFrontEnd scala.tools.reflect.package$/MODULE$) "")))

;; @todo implement .typeCheck?

(defn parse
  {:doc "Parses Scala code into a Scala AST."
   :tag Trees$Tree}
  [^String code]
  (.parse toolbox code))

(defn compile
  {:doc "Compiles a tree."}
  [^Trees$Tree tree]
  (.compile toolbox tree))

(defn define
  {:doc "Defines a top-level class, trait, or module, putting it into a uniquely-named package and returning a
  symbol that references the defined entity. For a `ClassDef`, a `ClassSymbol` is returned, and for a `ModuleDef`,
  a `ModuleSymbol` is returned (not a module class, but a module itself).

  This method can be used to generate definitions that will later be re-used by subsequent calls to `compile`,
   `define` or `eval`. To refer to the generated definition in a tree, use its symbol."
   :tag Symbols$Symbol}
  [^Trees$Tree tree]
  (.define toolbox tree))

(defn eval
  {:doc "Compiles and runs a tree. Is equivalent to `(.apply (compile tree))`"}
  [^Trees$Tree tree]
  (.eval toolbox tree))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Type

(def ^{:doc "A constant used as a special value to indicate that no meaningful type exists."
       :tag Types$Type}
  no-type
  (-> universe .NoType))

(def ^{:doc "A constant used as a special value denoting the empty prefix in a path dependent type."
       :tag Types$Type}
  no-prefix
  (-> universe .NoPrefix))

(defn term-symbol
  {:doc "The term symbol associated with the type Note that the symbol of the normalized type is returned"
   :tag Symbols$TermSymbol}
  [^Types$Type typ]
  (.termSymbol typ))

(defn type-symbol
  {:doc "The type symbol associated with the type."
   :tag Symbols$Symbol}
  [^Types$Type typ]
  (.typeSymbol typ))

(defn decl
  {:doc "The defined or declared members with name name in this type; an `OverloadedSymbol` if several exist, `NoSymbol` if none exist.

   Alternatives of overloaded symbol appear in the order they are declared."
   :tag Symbols$Symbol}
  [^Types$Type typ ^Names$Name name]
  (.decl typ name))

(defn decls
  {:doc "A list containing directly declared members of this type in sorted order."}
  [^Types$Type typ]
  (sc/->clj (.sorted (.decls typ))))

(defn member
  {:doc "The member with given name, either directly declared or inherited, an `OverloadedSymbol` if several exist,` NoSymbol` if none exist."
   :tag Symbols$Symbol}
  [^Types$Type typ ^Names$Name name]
  (.member typ name))

(defn members
  {:doc "A list containing all members of this type (directly declared or inherited) in sorted order."}
  [^Types$Type typ]
  (sc/->clj (.sorted (.members typ))))

(defn companion
  {:doc "Type signature of the companion of the underlying class symbol.
  `NoType` if the underlying symbol is not a class symbol, or if it doesn't have a companion."
   :tag Types$Type}
  [^Types$Type typ]
  (.companion typ))

(defn takes-type-args?
  {:doc "Return true if the type constructor is missing its type arguments, and false otherwise."
   :tag Boolean}
  [^Types$Type typ]
  (.takesTypeArgs typ))

(defn type-constructor
  {:doc "Returns the corresponding type constructor (e.g. `List` for `List[T]` or `List[String]`)"
   :tag Types$Type}
  [^Types$Type typ]
  (.typeConstructor typ))

(defn conforms?
  {:doc "Tests if `sub-type` conforms to `super-type`."
   :tag Boolean}
  [^Types$Type sub-type ^Types$Type super-type]
  (.$less$colon$less sub-type super-type))

(defn weak-conforms?
  {:doc "Tests if `sub-type` weakly conforms to `super-type`. Returns true or false.

  A type weakly conforms if either:

  - it conform in terms of `(conforms sub-type super-type)
  - both are primitive number types that conform according to \"weak conformance\".

  Example:
  ```
  (weak-conforms? (scala-type scala.Int) (scala-type scala.Long)) ; => true
  ```
  "
   :tag Boolean}
  [^Types$Type sub-type ^Types$Type super-type]
  (.weak_$less$colon$less sub-type super-type))

(defn equivalent?
  {:doc "Tests if `type-1` is equivalent to `type-2`. Returns true or false."
   :tag Boolean}
  [^Types$Type type-1 ^Types$Type type-2]
  (.$eq$colon$eq type-1 type-2))

(defn base-classes
  {:doc "The list of all base classes of this type (including its own type-symbol) in linearization order,
  starting with the class itself and ending in class Any."}
  [^Types$Type typ]
  (sc/->clj (.baseClasses typ)))

(defn base-type
  {:doc "The least type instance of given class which is a super-type of `typ`.

  For examples, see [the scala docs](https://www.scala-lang.org/api/2.13.6/scala-reflect/scala/reflect/api/Types$TypeApi.html#baseType(clazz:Types.this.Symbol):Types.this.Type)."
   :tag Types$Type}
  [^Types$Type typ ^Symbols$Symbol class]
  (.baseType typ class))

(defn as-seen-from
  {:doc "This type as seen from prefix `pre` and class `class`.

  This means: Replace all `ThisTypes` of `class` or one of its subclasses by `pre` and instantiate all parameters by arguments of `pre`.
  Proceed analogously for `ThisType`s referring to outer classes.

  For examples, see [the scala docs](https://www.scala-lang.org/api/2.13.6/scala-reflect/scala/reflect/api/Types$TypeApi.html#asSeenFrom(pre:Types.this.Type,clazz:Types.this.Symbol):Types.this.Type)."
   :tag Types$Type}
  [^Types$Type typ ^Types$Type pre ^Symbols$Symbol class]
  (.asSeenFrom typ pre class))

(defn erasure
  ^{:doc "The erased type corresponding to this type after all transformations from Scala to Java have been performed."
    :tag Types$Type}
  [^Types$Type typ]
  (.erasure typ))

(defn widen
  {:doc "If this is a singleton type, widen it to its nearest underlying non-singleton base type by
  applying one or more `underlying` dereferences. If this is not a singleton type, returns this type itself."
   :tag Types$Type}
  [^Types$Type typ]
  (.widen typ))

(defn dealias
  {:doc "Expands type aliases arising from type members."
   :tag Types$Type}
  [^Types$Type typ]
  (.dealias typ))

(defn type-args
  {:doc "A list of type arguments ingrained in this type reference. Depending on your use case you might or might not want to call `dealias` first.

  Example:
  ```
  (type-args (scala-type scala.collection.immutable.List [scala.Int])) ;; => [scala.Int]
  ```"}
  [^Types$Type typ]
  (sc/->clj (.typeArgs typ)))

(defn param-lists
  {:doc "For a method or polymorphic type, a list of its value parameter sections. An empty list of lists for all other types."}
  [^Types$Type typ]
  (map sc/->clj (sc/->clj (.paramLists typ))))

(defn type-params
  {:doc "For a polymorphic type, its type parameters. An empty list for all other types."}
  [^Types$Type typ]
  (sc/->clj (.typeParams typ)))

(defn scala-type?
  {:doc "Returns `true` if `x` is a Scala `Type` object. `false` otherwise. "
   :tag Boolean}
  [x]
  (instance? Types$TypeApi x))

(defn scala-type
  "Creates a Scala `Type` from the class (`cls`) and `type-args` (if required)."
  ([cls]
   (if (scala-type? cls)
     cls
     (-> mirror (.staticClass (.getName cls)) .asClass .toType type-constructor)))
  ([cls type-args]
   (-> universe
       (.appliedType (scala-type cls)
                     (sc/to-scala-list (map scala-type type-args))))))

(defn full-type-pr
  {:doc "Create a string containing the fully qualify and parameterized type name.

  Example:
  ```
  (full-type-pr (scala-type scala.Option [String])) ; => \"scala.Option[java.lang.String]\"
  ```"
   :tag String}
  [^Types$Type typ]
  (let [base (.fullName (type-symbol typ))
        t-args (type-args typ)]
    (if (core/empty? t-args)
      base
      (format "%s[%s]"
              base
              (str/join "," (map full-type-pr t-args))))))

(defn- eval-tt
  [type-pr]
  (eval (parse (format "scala.reflect.runtime.universe.typeTag[%s]" type-pr))))

(def ^{:private true} -memo-eval-tt
  (memoize eval-tt))

(defn type->tag
  {:doc "Create a `TypeTag` for the given `Type`.

  Requires expensive reflection operations. By default, results will be memoized. To disable
  memoization, use `:memoize? false`."
   :tag TypeTags$TypeTag}
  [^Types$Type typ & {:keys [memoize?] :or {memoize? true}}]
  (let [type-pr (full-type-pr typ)]
    (if memoize?
      (-memo-eval-tt type-pr)
      (eval-tt type-pr))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Type/Class Tags

(def ^{:doc "A `ClassTag` for the `scala.Byte` class."
       :tag ClassTag}
  byte-cls-tag
  (ClassTag/Byte))

(def ^{:doc "A `ClassTag` for the `scala.Short` class."
       :tag ClassTag}
  short-cls-tag
  (ClassTag/Short))

(def ^{:doc "A `ClassTag` for the `scala.Char` class."
       :tag ClassTag}
  char-cls-tag
  (ClassTag/Char))

(def ^{:doc "A `ClassTag` for the `scala.Int` class."
       :tag ClassTag}
  int-cls-tag
  (ClassTag/Int))

(def ^{:doc "A `ClassTag` for the `scala.Long` class."
       :tag ClassTag}
  long-cls-tag
  (ClassTag/Long))

(def ^{:doc "A `ClassTag` for the `scala.Float` class."
       :tag ClassTag}
  float-cls-tag
  (ClassTag/Float))

(def ^{:doc "A `ClassTag` for the `scala.Double` class."
       :tag ClassTag}
  double-cls-tag
  (ClassTag/Double))

(def ^{:doc "A `ClassTag` for the `scala.Boolean` class."
       :tag ClassTag}
  boolean-cls-tag
  (ClassTag/Boolean))

(def ^{:doc "A `ClassTag` for the `scala.Unit` class."
       :tag ClassTag}
  unit-cls-tag
  (ClassTag/Unit))

(def ^{:doc "A `ClassTag` for the `scala.Any` class."
       :tag ClassTag}
  any-cls-tag
  (ClassTag/Any))

(def ^{:doc "A `ClassTag` for the `scala.AnyVal` class."
       :tag ClassTag}
  any-val-cls-tag
  (ClassTag/AnyVal))

(def ^{:doc "A `ClassTag` for the `scala.AnyRef` class."
       :tag ClassTag}
  any-ref-cls-tag
  (ClassTag/AnyRef))

(def ^{:doc "A `ClassTag` for the `java.lang.Object` class."
       :tag ClassTag}
  object-cls-tag
  (ClassTag/Object))

(def ^{:doc "A `ClassTag` for `scala.Noting`."
       :tag ClassTag}
  nothing-cls-tag
  (ClassTag/Nothing))

(def ^{:doc "A `ClassTag` for the `scala.Null`."
       :tag ClassTag}
  null-cls-tag
  (ClassTag/Null))

(defn class-tag
  ^{:doc "Create a `ClassTag` from a `Class` object."
    :tag ClassTag}
  [cls]
  (ClassTag/apply cls))

(defn- cls->tt
  [^Class cls & {:keys [memoize?] :or {memoize? true}}]
  (type->tag (scala-type cls) :memoize? memoize?))

(def ^{:doc "A `TypeTag` for the `scala.Byte` type."
       :tag TypeTags$TypeTag}
  byte-tt
  (-> universe .TypeTag .Byte))

(def ^{:doc "A `TypeTag` for the `scala.Short` type."
       :tag TypeTags$TypeTag}
  short-tt
  (-> universe .TypeTag .Short))

(def ^{:doc "A `TypeTag` for the `scala.Char` type."
       :tag TypeTags$TypeTag}
  char-tt
  (-> universe .TypeTag .Char))

(def ^{:doc "A `TypeTag` for the `scala.Int` type."
       :tag TypeTags$TypeTag}
  int-tt
  (-> universe .TypeTag .Int))

(def ^{:doc "A `TypeTag` for the `scala.Long` type."
       :tag TypeTags$TypeTag}
  long-tt
  (-> universe .TypeTag .Long))

(def ^{:doc "A `TypeTag` for the `scala.Float` type."
       :tag TypeTags$TypeTag}
  float-tt
  (-> universe .TypeTag .Float))

(def ^{:doc "A `TypeTag` for the `scala.Double` type."
       :tag TypeTags$TypeTag}
  double-tt
  (-> universe .TypeTag .Double))

(def ^{:doc "A `TypeTag` for the `scala.Boolean` type."
       :tag TypeTags$TypeTag}
  boolean-tt
  (-> universe .TypeTag .Boolean))

(def ^{:doc "A `TypeTag` for the `scala.Unit` type."
       :tag TypeTags$TypeTag}
  unit-tt
  (-> universe .TypeTag .Unit))

(def ^{:doc "A `TypeTag` for the `scala.Any` type."
       :tag TypeTags$TypeTag}
  any-tt
  (-> universe .TypeTag .Any))

(def ^{:doc "A `TypeTag` for the `scala.AnyVal` type."
       :tag TypeTags$TypeTag}
  any-val-tt
  (-> universe .TypeTag .AnyVal))

(def ^{:doc "A `TypeTag` for the `scala.AnyRef` type."
       :tag TypeTags$TypeTag}
  any-ref-tt
  (-> universe .TypeTag .AnyRef))

(def ^{:doc "A `TypeTag` for the `java.lang.Object` type."
       :tag TypeTags$TypeTag}
  object-tt
  (-> universe .TypeTag .Object))

(def ^{:doc "A `TypeTag` for the `scala.Nothing` type."
       :tag TypeTags$TypeTag}
  nothing-tt
  (-> universe .TypeTag .Nothing))

(def ^{:doc "A `TypeTag` for the `scala.Null` type."
       :tag TypeTags$TypeTag}
  null-tt
  (-> universe .TypeTag .Null))


(def ^{:private true} -simple-cls-tt-map
  {Byte      byte-tt
   Short     short-tt
   Character char-tt
   Integer   int-tt
   Long      long-tt
   Float     float-tt
   Double    double-tt
   Boolean   boolean-tt
   Object    object-tt
   nil       null-tt})

(defn type-tag
  {:doc "Create a `TypeTag` from the given class or `Scala` type."
   :tag TypeTags$TypeTag}
  [cls-or-type & {:keys [memoize?] :or {memoize? true}}]
  (if (scala-type? cls-or-type)
    (type->tag cls-or-type :memoize? memoize?)
    (or (-simple-cls-tt-map cls-or-type)
        (cls->tt cls-or-type :memoize? memoize?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Name

(defn term-name
  ^Names$TermName [s]
  (-> universe .TermName (.apply (core/name s))))

(defn type-name
  ^Names$TypeName [s]
  (-> universe .TypeName (.apply (core/name s))))

(def ^{:doc "The `WILDCARD` term name."
       :tag Names$TermName}
  wildcard-term-name
  (.WILDCARD (.termNames universe)))

(def ^{:doc "The `WILDCARD` type name."
       :tag Names$TypeName}
  wildcard-type-name
  (.WILDCARD (.typeNames universe)))

(def ^{:doc "The `EMPTY` term name."
       :tag Names$TermName}
  empty-term-name
  (.EMPTY (.termNames universe)))

(def ^{:doc "The `EMPTY` type name."
       :tag Names$TypeName}
  empty-type-name
  (.EMPTY (.typeNames universe)))

(def ^{:doc "The `ERROR` term name."
       :tag Names$TermName}
  error-term-name
  (.ERROR (.termNames universe)))

(def ^{:doc "The `ERROR` type name."
       :tag Names$TypeName}
  error-type-name
  (.ERROR (.typeNames universe)))

(def ^{:doc "The `PACKAGE` term name."
       :tag Names$TermName}
  package-term-name
  (.PACKAGE (.termNames universe)))

(def ^{:doc "The `PACKAGE` type name."
       :tag Names$TypeName}
  package-type-name
  (.PACKAGE (.typeNames universe)))

(def ^{:doc "The `CONSTRUCTOR` term name."
       :tag Names$TermName}
  constructor
  (.CONSTRUCTOR (.termNames universe)))

(def ^{:doc "The `ROOTPKG` term name."
       :tag Names$TermName}
  root-pkg
  (.ROOTPKG (.termNames universe)))

(def ^{:doc "The `EMPTY_PACKAGE_NAME` term name."
       :tag Names$TermName}
  empty-package-name
  (.EMPTY_PACKAGE_NAME (.termNames universe)))

(def ^{:doc "The `LOCAL_SUFFIX_STRING` term name."
       :tag Names$TermName}
  local-suffix-string
  (.LOCAL_SUFFIX_STRING (.termNames universe)))

(def ^{:doc "The `WILDCARD_STAR` type name."
       :tag Names$TypeName}
  wildcard-start
  (.WILDCARD_STAR (.typeNames universe)))

(defn term-name?
  {:doc "Checks whether `nm` is a term name."
   :tag boolean}
  [nm]
  (if (not (instance? Names$NameApi nm))
    false
    (.isTermName nm)))

(defn type-name?
  {:doc "Checks whether `nm` is a type name."
   :tag boolean}
  [nm]
  (if (not (instance? Names$NameApi nm))
    false
    (.isTypeName nm)))

(defn to-term-name
  {:doc "Returns a term name that wraps the same string as the name `nm`."
   :tag Names$TermName}
  [nm]
  (.toTermName nm))

(defn to-type-name
  {:doc "Returns a type name that wraps the same string as the name `nm`."
   :tag Names$TypeName}
  [nm]
  (.toTypeName nm))

(defn decoded-name
  {:doc "The decoded name.

  Example:
  ```
  (decoded-name (term-name \"$bang$eq\")) ; => (term-name \"!=\")
  ```
  "
   :tag Names$Name}
  [nm]
  (.decodedName nm))

(defn encoded-name
  {:doc "The encoded name.

  Example:
  ```
  (encoded-name (term-name \"!=\")) ; => (term-name \"$bang$eq\")
  ```"
   :tag Names$Name}
  [nm]
  (.encodedName nm))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tree

(def ^{:doc "The empty tree."
       :tag Trees$Tree}
  empty-tree
  (.EmptyTree universe))

(def ^{:doc "An empty deferred value definition.
This is used as a placeholder in the `self` parameter Template if there is no definition of a self value of self type."
       :tag Trees$ValDef}
  no-self-type
  (.noSelfType universe))

(defn modifiers
  ^Trees$Modifiers
  ([]
   (-> universe .Modifiers (.apply)))
  ([flags private-within annotations]
   (-> universe .Modifiers (.apply flags private-within (sc/to-scala-list annotations)))))

(defn type-tree
  []
  (-> universe .TypeTree (.apply)))

(defn singleton-type-tree
  ^Trees$SingletonTypeTree [^Trees$Tree ref]
  (-> universe .SingletonTypeTree (.apply ref)))

(defn select-from-type-tree
  ^Trees$SelectFromTypeTree [^Trees$Tree qualifier ^Names$TypeName name]
  (-> universe .SelectFromTypeTree (.apply qualifier name)))

(defn compound-type-tree
  ^Trees$CompoundTypeTree [^Trees$Template templ]
  (-> universe .CompoundTypeTree (.apply templ)))

(defn applied-type-tree
  ^Trees$AppliedTypeTree [^Trees$Tree tpt args]
  (-> universe .AppliedTypeTree (.apply tpt (sc/to-scala-list args))))

(defn type-bounds-tree
  ^Trees$TypeBoundsTree [^Trees$Tree lower ^Trees$Tree higher]
  (-> universe .TypeBoundsTree (.apply lower higher)))

(defn existential-type-tree
  ^Trees$ExistentialTypeTree [^Trees$Tree tpt where-clauses]
  (-> universe .ExistentialTypeTree (.apply tpt (sc/to-scala-list where-clauses))))

(defn type->tree
  ^Trees$TypeTree [^Types$Type typ]
  (-> universe (.TypeTree typ)))

(defn import-selector
  ^Trees$ImportSelector [^Names$Name name ^Integer name-pos ^Names$Name rename ^Integer rename-pos]
  (-> universe .ImportSelector (.apply name name-pos rename rename-pos)))

(defn import
  ^Trees$Import [^Trees$Tree expr selectors]
  (-> universe .Import (.apply expr (sc/to-scala-list selectors))))

(defn block
  ^Trees$Block [{:keys [stats ^Trees$Tree expr] :or {stats []}}]
  (-> universe .Block (.apply (sc/to-scala-list stats) expr)))

(defn case-def
  ^Trees$CaseDef [{:keys [^Trees$Tree pat ^Trees$Tree guard ^Trees$Tree body]}]
  (-> universe .CaseDef (.apply pat guard body)))

(defn alternative
  ^Trees$Alternative [trees]
  (-> universe .Alternative (.apply (sc/to-scala-list trees))))

(defn star
  ^Trees$Star [^Trees$Tree elem]
  (-> universe .Star (.apply elem)))

(defn bind
  ^Trees$Bind [^Names$Name name ^Trees$Tree body]
  (-> universe .Bind (.apply name body)))

(defn un-apply
  ^Trees$UnApply [^Trees$Tree fun args]
  (-> universe .UnApply (.apply fun (sc/to-scala-list args))))

(defn function
  ^Trees$Function [^List vparams ^Trees$Tree body]
  (-> universe .Function (.apply (sc/to-scala-list vparams) body)))

(defn assign
  ^Trees$Assign [^Trees$Tree lhs ^Trees$Tree rhs]
  (-> universe .Assign (.apply lhs rhs)))

(defn named-arg
  [^Trees$Tree lhs ^Trees$Tree rhs]
  (-> universe .NamedArg (.apply lhs rhs)))

(defn if
  ^Trees$If [^Trees$Tree cond ^Trees$Tree thenp ^Trees$Tree elsep]
  (-> universe .If (.apply cond thenp elsep)))

(defn match
  ^Trees$Match [^Trees$Tree selector cases]
  (-> universe .Match (.apply selector (sc/to-scala-list cases))))

(defn return
  ^Trees$Return [^Trees$Tree expr]
  (-> universe .Return (.apply expr)))

(defn try
  ^Trees$Try [^Trees$Tree block ^List catches ^Trees$Tree finalizer]
  (-> universe .Try (.apply block (sc/to-scala-list catches) finalizer)))

(defn throw
  ^Trees$Throw [^Trees$Tree expr]
  (-> universe .Throw (.apply expr)))

(defn new
  ^Trees$New [^Trees$Tree tpt]
  (-> universe .New (.apply tpt)))

(defn typed
  ^Trees$Typed [^Trees$Tree expr ^Trees$Tree tpt]
  (-> universe .Typed (.apply expr tpt)))

(defn type-apply
  ^Trees$TypeApply [^Trees$Tree func args]
  (-> universe .TypeApply (.apply func (sc/to-scala-list args))))

(defn apply
  ^Trees$Apply [^Trees$Tree func args]
  (-> universe .Apply (.apply func (sc/to-scala-list args))))

(defn super
  ^Trees$Super [^Trees$Tree qual ^Names$TypeName mix]
  (-> universe .Super (.apply qual mix)))

(defn this
  ^Trees$This [^Names$TypeName qual]
  (-> universe .This (.apply qual)))

(defn select
  ^Trees$Select [^Trees$Tree qualifier ^Names$TypeName name]
  (-> universe .Select (.apply qualifier name)))

(defn ident
  ^Trees$Ident [^Names$TypeName name]
  (-> universe .Ident (.apply name)))

(defn constant
  ^Constants$Constant [value]
  (-> universe .Constant (.apply value)))

(defn literal
  ^Trees$Literal [^Constants$Constant value]
  (-> universe .Literal (.apply value)))

(defn annotated
  ^Trees$Annotated [^Trees$Tree annot ^Trees$Tree arg]
  (-> universe .Annotated (.apply annot arg)))

(defn val-def
  ^Trees$ValDef [{:keys [mods name tpt rhs] :or {mods (modifiers) tpt (type-tree)}}]
  (-> universe .ValDef (.apply mods name tpt rhs)))

(defn def-def
  ^Trees$DefDef [{:keys [^Trees$Modifiers mods
                         ^Names$TermName name
                         tparams
                         vparamss
                         ^Trees$Tree tpt
                         ^Trees$Tree rhs]
                  :or   {mods (modifiers) tparams [] vparamss [] tpt (type-tree)}}]
  (-> universe
      .DefDef
      (.apply mods
              name
              (sc/to-scala-list tparams)
              (sc/to-scala-list (map sc/to-scala-list vparamss))
              tpt
              rhs)))

(defn type-def
  ^Trees$TypeDef [{:keys [^Trees$Modifiers mods ^Names$TypeName name tparams ^Trees$Tree rhs]
                   :or   {mods (modifiers) tparams [] rhs empty-tree}}]
  (-> universe .TypeDef (.apply mods name (sc/to-scala-list tparams) rhs)))

(defn label-def
  ^Trees$LabelDef [^Names$TermName name params ^Trees$Tree rhs]
  (-> universe .LabelDef (.apply name (sc/to-scala-list params) rhs)))

(def default-constructor
  (def-def {:name     constructor
            :vparamss [[]]
            :rhs      (block {:stats [(apply (select (super (this empty-type-name) empty-type-name)
                                                     constructor)
                                             [])]
                              :expr  (literal (constant unit))})}))

(defn template
  ^Trees$Template [{:keys [parents ^Trees$ValDef self body]
                    :or   {parents [] self no-self-type body []}}]
  (-> universe
      .Template
      (.apply (sc/to-scala-list parents)
              self
              (sc/to-scala-list body))))

(defn package-def
  ^Trees$PackageDef [{:keys [^Trees$RefTree pid stats]}]
  (-> universe .PackageDef (.apply pid (sc/to-scala-list stats))))

(defn class-def
  ^Trees$ClassDef [{:keys [^Trees$Modifiers mods ^Names$TypeName name tparams ^Trees$Template impl]
                    :or   {mods    (modifiers)
                           tparams []
                           impl    (template {:body [default-constructor]})}}]
  (-> universe .ClassDef (.apply mods name (sc/to-scala-list tparams) impl)))

(defn module-def
  ^Trees$ModuleDef [{:keys [^Trees$Modifiers mods ^Names$TermName name ^Trees$Template impl]
                     :or   {mods (modifiers) impl (template {})}}]
  (-> universe .ModuleDef (.apply mods name impl)))

(defn def?
  ^Boolean [^Trees$Tree tree]
  (.isDef tree))

(defn empty?
  ^Boolean [^Trees$Tree tree]
  (.isEmpty tree))

(defn non-empty?
  ^Boolean [^Trees$Tree tree]
  (.nonEmpty tree))

(defn can-have-attrs?
  ^Boolean [^Trees$Tree tree]
  (.canHaveAttrs tree))

(defn pos
  [^Trees$Tree tree]
  (.pos tree))

(defn tpe
  ^Types$Type [^Trees$Tree tree]
  (.tpe tree))

(defn symbol
  ^Symbols$Symbol [^Trees$Tree tree]
  (.symbol tree))

(defn structurally-equal?
  ^Boolean [^Trees$Tree tree-a ^Trees$Tree tree-b]
  (.equalsStructure tree-a tree-b))

(defn children
  [^Trees$Tree tree]
  (sc/->clj (.children tree)))

(defn duplicate
  ^Trees$Tree [^Trees$Tree tree]
  (.duplicate tree))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Symbol

(defn scala-symbol?
  {:doc "Checks if `s` is a Scala reflect `Symbol`."
   :tag Boolean}
  [s]
  (instance? Symbols$SymbolApi s))

(defn owner
  {:doc "The owner of this symbol. This is the symbol
  that directly contains the current symbol's definition.
  The `NoSymbol` symbol does not have an owner, and calling this method
  on one causes an internal error.

  The owner of the Scala `RootClass` and `RootPackage` is `NoSymbol`.
  Every other symbol has a chain of owners that ends in `RootClass`."
   :tag Symbols$Symbol}
  [symb]
  (.owner symb))

(defn name
  {:doc "The name of the symbol as a member of the `Name` type.
  Can be either `TermName` or `TypeName` depending on whether the given symbol
  is a `TermSymbol` or a `TypeSymbol`."
   :tag Names$Name}
  [^Symbols$Symbol symb]
  (.name symb))

(defn full-name
  {:doc "The encoded full path name of the symbol, where outer names and inner names are separated by periods."
   :tag String}
  [symb]
  (.fullName symb))

(defn method?
  [symb]
  (.isMethod symb))

(defn constructor?
  [symb]
  (.isConstructor symb))

(defn module?
  [symb]
  (.isModule symb))

(defn class?
  [symb]
  (.isClass symb))

(defn synthetic?
  [symb]
  (.isSynthetic symb))

(defn implementation-artifact?
  [symb]
  (.isImplementationArtifact symb))

(defn private-this?
  [symb]
  (.isPrivateThis symb))

(defn private?
  [symb]
  (.isPrivate symb))

(defn protected-this?
  [symb]
  (.isProtectedThis symb))

(defn protected?
  [symb]
  (.isProtected symb))

(defn public?
  [symb]
  (.isPublic symb))

(defn package?
  [symb]
  (.isPackage symb))

(defn static?
  [symb]
  (.isStatic symb))

(defn final?
  [symb]
  (.isFinal symb))

(defn abstract?
  [symb]
  (.isAbstract symb))

(defn abstract-override?
  [symb]
  (.isAbstractOverride symb))

(defn macro?
  [symb]
  (.isMacro symb))

(defn parameter?
  [symb]
  (.isParameter symb))

(defn specialized?
  [symb]
  (.isSpecialized symb))

(defn java?
  [symb]
  (.isJava symb))

(defn implicit?
  [symb]
  (.isImplicit symb))

(defn java-enum?
  [symb]
  (.isJavaEnum symb))

(defn java-annotation?
  [symb]
  (.isJavaAnnotation symb))

(defn as-type
  {:doc "The symbol cast to a `TypeSymbol`."
   :tag Symbols$TypeSymbol}
  [symb]
  (.asType symb))

(defn as-term
  {:doc "The symbol cast to a `TermSymbol`."
   :tag Symbols$TermSymbol}
  [symb]
  (.asTerm symb))

(defn as-method
  {:doc "The symbol cast to a `MethodSymbol`."
   :tag Symbols$MethodSymbol}
  [symb]
  (.asMethod symb))

(defn as-module
  {:doc "The symbol cast to a `ModuleSymbol` defined by an object definition."
   :tag Symbols$ModuleSymbol}
  [symb]
  (.asModule symb))

(defn as-class
  {:doc "The symbol cast to a `ClassSymbol` representing a class or trait."
   :tag Symbols$ClassSymbol}
  [symb]
  (.asClass symb))

(defn to-type
  {:doc "The \"type\" of the symbol.

   The type of a term symbol is its usual type.

   For type symbols, this is different than the `info` function because returns a `typeRef` to the type symbol.
   `info` returns the type information of the type symbol."
   :tag Types$Type}
  [^Symbols$TypeSymbol symb]
  (.tpe symb))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Shared

(defn type?
  {:doc "Checks if the given `Symbol` or `Tree` is denoting a type."
   :tag Boolean}
  [symb-or-tree]
  ;; Same implementation for `Symbol` and `Tree`.
  (.isType symb-or-tree))

(defn term?
  {:doc "Checks if the given `Symbol` or `Tree` is denoting a term."
   :tag Boolean}
  [symb-or-tree]
  ;; Same implementation for `Symbol` and `Tree`.
  (.isTerm symb-or-tree))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Class Mirror

(defn reflect
  "Creates a reflective mirror for the given object."
  ^Mirrors$InstanceMirror [obj]
  (.reflect mirror obj (class-tag (core/class obj))))

(defn reflect-class
  "Reflects against a static class symbol and returns a mirror that can be used to create instances
   of the class, inspect its companion object or perform further reflections."
  ^Mirrors$ClassMirror [^Symbols$ClassSymbol symb]
  (.reflectClass mirror symb))

(defn reflect-constructor
  "Reflects against a constructor symbol and returns a mirror that can be used to invoke it and construct instances of the mirror's symbols."
  ^Mirrors$MethodMirror [^Mirrors$ClassMirror cm ^Symbols$MethodSymbol ctor]
  (.reflectConstructor cm ctor))

(defn reflect-field
  "Reflects against a field symbol and returns a mirror that can be used to get and, if appropriate, set the value of the field."
  ^Mirrors$FieldMirror [^Mirrors$InstanceMirror im ^Symbols$TermSymbol symb]
  (.reflectField im symb))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Flags

; @todo Idiomatic Clojure for specifying flags.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Printers

(defn- wrap-flags
  [opts]
  (map #(-> universe .BooleanFlag (.optionToBooleanFlag (opt/option %)))
       opts))

(defn pr-raw
  [^Trees$Tree tree {:keys [types? ids? owners? kinds? mirrors? positions?]
                     :or   {types? nil ids? nil owners? nil kinds? nil mirrors? nil positions? nil}}]
  (let [builder (fn [types? ids? owners? kinds? mirrors? positions?]
                  (fn [tree]
                    (-> universe (.showRaw tree types? ids? owners? kinds? mirrors? positions?))))
        f (core/apply builder (wrap-flags [types? ids? owners? kinds? mirrors? positions?]))]
    (f tree)))

(defn pr-code
  [^Trees$Tree tree {:keys [types? ids? owners? positions? root-pkg?]
                     :or   {types? nil ids? nil owners? nil positions? nil root-pkg? false}}]
  (let [builder (fn [types? ids? owners? positions? root-pkg?]
                  (fn [tree]
                    (-> universe (.showCode tree types? ids? owners? positions? root-pkg?))))
        f (core/apply builder (conj (vec (wrap-flags [types? ids? owners? positions?])) root-pkg?))]
    (f tree)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fijit

(defn type-reflect
  "Reflect on a Scala type, returning a map with `:bases`, `:flags`, and `:members`.
  Similar to `clojure.reflect/type-reflect` over Scala types, except that names are
  represented as Scala reflect `Symbol` objects rather than Clojure symbols.

    :bases            a set of symbols of the type's bases
    :flags            a set of keywords naming the boolean attributes of the type.
    :members          a set of the type's members. Each member is a map
                      and can be a constructor, method, field, or path dependant types (ie. nested class).

    Keys common to all members:
    :name             A Clojure symbol denoting the member's name
    :symbol           The Scala Symbol of the member
    :declaring-class  Symbol of the declarer
    :flags            keyword naming boolean attributes of the member

    Keys specific to constructors:
    :parameter-types  vector of parameter type symbols

    Key specific to methods:
    :parameter-types  vector of parameter type symbols
    :return-type      return type symbol

    Keys specific to fields:
    :type             type name

    There are no additional symbols for nested class

    Options:

     :ancestors?    If true, add all ancestor members to `:members`."
  [typ & {:keys [ancestors?] :or {ancestors? false}}]
  (let [symb (type-symbol typ)
        member-finder (if ancestors? members decls)]
    {:bases   (set (base-classes typ))
     :flags   (set (filter some? [(when (public? symb) :public)
                                  (when (private? symb) :private)
                                  (when (abstract? symb) :abstract)
                                  (when (final? symb) :final)
                                  (when (synthetic? symb) :synthetic)]))
     :members (->> typ
                   member-finder
                   (map (fn [member-symb]
                          (merge {:name            (-> member-symb name str (core/symbol))
                                  :symbol          member-symb
                                  :declaring-class (owner member-symb)
                                  :flags           (set (filter some? [(when (public? member-symb) :public)
                                                                       (when (private? member-symb) :private)
                                                                       (when (static? member-symb) :static)
                                                                       (when (abstract? member-symb) :abstract)
                                                                       (when (final? member-symb) :final)
                                                                       (when (synthetic? member-symb) :synthetic)]))}
                                 (cond
                                   (constructor? member-symb) {:parameter-types nil} ;; @todo Finish
                                   (method? member-symb) {:parameter-types nil ;; @todo Finish
                                                          :return-type     nil} ;; @todo Finish
                                   (class? member-symb) {}  ;; @todo What should be put here? Recursive type-reflect?
                                   (module? member-symb) {}
                                   :else {:type nil})))))}))

;;;;;;;;;;;;
;; Dev

(comment



  (let [tree (class-def {:name    (type-name "Box")
                         :tparams [(type-def {:name (type-name "T")})]
                         :impl    (template {:body [default-constructor
                                                    (def-def {:name (term-name "boxed")
                                                              :tpt  (ident (type-name "T"))
                                                              :rhs  empty-tree})]})})]
    (println (pr-raw tree {}))
    (println (pr-code tree {})))

  )

;; @todo Make this entire API easier by automatically converting Clj values to common Scala AST types.
;; Clojure keywords -> TermNames
;; Clojure Symbols -> TypeNames
;; Map -> *Def based on :kind