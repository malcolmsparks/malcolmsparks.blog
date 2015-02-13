Title: Prefer data over functions
Subtitle: Functional programming is great, but always prefer data when you can - warning: functions are opaque!
Date: 2015-01-31
Keywords: clojure
Background: img/redirect.jpg

Clojure is often described as a __functional programming__ language. To me,
it's more accurate to say it's a __data processing__ language. Of course,
you can do functional programming in Clojure (with functional
composition, higher-order functions, monads and so on), but doing that can hide important parts that should really be kept visible.

Consider a common Clojure function to handle Ring requests (this
one causes a browser to redirect to another URL)

```clojure
(fn [req] {:status 302 :headers {"location" "/index.html"}})
```

Let's print it out

```clojure-repl
user> (println (fn [req] {:status 302 :headers {"location" "/index.html"}}))
```

This is the sort of thing you'll see on the console

```clojure-repl
#<user$eval7294$fn__7295 user$eval7294$fn__7295@74310632>
```

Ouch. That doesn't tell us anything!

The only way we can find out information from the function is to _call_
it. That's like checking to see if a gun is loaded by holding it to your
head and firing.

![Ouch](img/gun.jpg)

And in order to call this function, I need to create the request data (and there's a great library for doing just this, called [ring-mock](https://github.com/weavejester/ring-mock), I digress)

But it's far better to create a data record of the re-direct using
a Clojure record.

```clojure
(defrecord Redirect [status location])
```

Let's print it out

```clojure-repl
user> (println (->Redirect 302 "/index.html"))
```

Now we get something like this

```
#Redirect{:status 302, :location "/index.html"}
```

That's _so_ much more useful!

All have lost is the ability to treat it as a function. But Clojure's
protocols make it easy to restore this lost functionality.

First we define a protocol, containing the function signature we want
our Redirect record to support.

```clojure
(defprotocol Ring
  (request [_ req]))
```

Now we declare how our Redirect record satisfies this protocol.

```clojure
(extend-protocol Ring
  Redirect
  (request [this req]
    {:status (:status this)
     :headers {"location" (:location this)}
     })
```

Whenever we are required to create a Ring handler, it is simply matter
of creating a function wrapper.

```clojure
(let [redirect (map->Redirect {:status 302 :location "/index.html"})]
  (fn [req] (request redirect req))
```

While there is a bit more code to write, there are many benefits of
data-composition compared to function-composition.

## Bidi

My [bidi routing library](https://github.com/juxt/bidi) embraces the concept
of using data as much as possible. Bidi contains its own version of our
`Ring` protocol, supported by its `bidi.ring/make-handler` function. (And
yes, bidi comes with a Redirect record, and lots more.)

Bidi allows you to keep your routing data as data, and still use it to
dispatch Ring requests, just as you would a normal Ring handler
function. The difference is, your routing data remains transparent, and
you can use it for other things (creating a site-map, for example)
rather than for the single purpose of dispatching an HTTP request.
