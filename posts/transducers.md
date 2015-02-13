Title: Transducers in action
Subtitle: Using transducers with Clojure's core.async
Date: 2015-02-13
Keywords: clojure core.async
Background: img/industry.jpg

Imagine we have some messages that we wish to process.

Let's define the message. Here's a way we can create one.

```clojure
(require '[cheshire.core :as json])
(require '[byte-streams :refer (to-byte-array)]

(defn temperature-message [msgid device topic temperature]
  {:id msgid
   :device device
   :topic topic
   :payload {:encoding encoding
             :content-type "application/json"
             :bytes (-> {:temperature temperature}
                        json/encode
                        (to-byte-array {:encoding encoding}))}})
```

Our message has a binary payload. When receiving a message like this, we'll want to decode the byte array into a JSON string, and perhaps decode that JSON into a map.

Good design is about breaking things apart, so let's try to break this into steps: first we'll decode the bytes into a body string, second we'll decode that body string in json.

Ideally, we'd like to start with a channel of messages and arrive at a
channel of decoded messages.

Let's start with the source channel, and create one with 4 messages.

```clojure
(require '[clojure.core.async :refer (to-chan pipe)])

(def source
  (to-chan
    (for [msgid (range 4)]
      (temperature-message msgid "1001" "/users/malcolm/temperature" 50)))
```

Now we can write functions that take a source channel and return a channel with completely decoded messages.

```clojure
(-> source (pipe (chan)) payload-decoder payload-json-decoder)
```

Here's my first attempt at writing `payload-decoder` and `payload-json-decoder`.

```clojure
(defn decode-payload [{:keys [bytes] :as payload} encoding]
  (assoc payload :body (to-string bytes)))

(defn payload-decoder
  ([in encoding]
   (let [out (chan)]
     (go-loop []
       (let [v (<! in)]
         (if v
           (do
             (>! out (update-in v [:payload] decode-payload encoding))
             (recur))
           (a/close! out))))
     out))
  ([in]
  (payload-decoder in "UTF-8")))

(defn payload-json-decoder
  [in]
  (let [out (chan)]
    (go-loop []
      (let [v (<! in)]
        (if v
          (do
            (>! out (update-in v [:payload :body] json/decode keyword))
            (recur))
          (a/close! out))))
    out))
```

Then I thought to myself, why not use [_transducers_](http://blog.cognitect.com/blog/2014/8/6/transducers-are-coming)?


![Ouch](img/transducer.png)

Here's the code I ended up with :-

```clojure
(require '[clojure.core.async :refer (to-chan pipe buffer pipeline)])

(defn ncpus []
  (.availableProcessors (Runtime/getRuntime)))

(defn parallelism []
  (+ (ncpus) 1))

(defn add-transducer
  [in xf]
  (let [out (chan (buffer 16))]
    (pipeline (parallelism) out xf in)
    out))

(defn decode-payload
  [{:keys [bytes] :as payload} encoding]
  (assoc payload :body (to-string bytes)))

(defn payload-decoder
  ([in encoding]
   (add-transducer in (map #(update-in % [:payload] decode-payload encoding))))
  ([in]
  (payload-decoder in "UTF-8")))

(defn payload-json-decoder
  [in]
  (add-transducer in (map #(update-in % [:payload :body] json/decode keyword))))

```

By creating an `add-transducer` function, both `payload-decoder` and `payload-json-decoder` collapsed into one-line implementations.

In these transducers, I'm using map, which applies a function to every message in the channel. However, there are numerous advantages with the code that use transducers.

* Messages can be filtered out, and even new messages inserted.
* Using core.async's pipeline function, increased parallelism can be exploited.
* Transducers can be composed together using `comp`.

This design helps give me the most flexibility and allows me to quickly create pipelines for processing a diverse set of message streams.
