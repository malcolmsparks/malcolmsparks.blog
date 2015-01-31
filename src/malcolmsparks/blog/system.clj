(ns malcolmsparks.blog.system
  "Components and their dependency relationships"
  (:refer-clojure :exclude (read))
  (:require
   [clojure.java.io :as io]
   [clojure.tools.reader :refer (read)]
   [clojure.string :as str]
   [clojure.tools.reader.reader-types :refer (indexing-push-back-reader)]
   [com.stuartsierra.component :refer (system-map system-using using)]
   [tangrammer.component.co-dependency :refer (co-using system-co-using)]
   [modular.maker :refer (make)]
   [malcolmsparks.blog.pages :refer (new-pages)]
   [modular.bidi :refer (new-redirect new-router new-static-resource-service)]
   [modular.clostache :refer (new-clostache-templater)]
   [modular.http-kit :refer (new-webserver)]
   [modular.less :refer (new-less-compiler)]))

(defn ^:private read-file
  [f]
  (read
   ;; This indexing-push-back-reader gives better information if the
   ;; file is misconfigured.
   (indexing-push-back-reader
    (java.io.PushbackReader. (io/reader f)))))

(defn ^:private config-from
  [f]
  (if (.exists f)
    (read-file f)
    {}))

(defn ^:private user-config
  []
  (config-from (io/file (System/getProperty "user.home") ".malcolmsparks.blog.edn")))

(defn ^:private config-from-classpath
  []
  (if-let [res (io/resource "malcolmsparks.blog.edn")]
    (config-from (io/file res))
    {}))

(defn config
  "Return a map of the static configuration used in the component
  constructors."
  []
  (merge (config-from-classpath)
         (user-config)))

(defn http-listener-components
  [system config]
  (assoc system
         :http-listener-listener
         (->
          (make new-webserver config :port :modular.maker/required)
          (using [])
          (co-using []))))

(defn modular-bidi-router-components
  [system config]
  (assoc system
    :modular-bidi-router-webrouter
    (->
      (make new-router config)
      (using [])
      (co-using []))))


(defn clostache-templater-components
  [system config]
  (assoc system
    :clostache-templater-templater
    (->
      (make new-clostache-templater config)
      (using [])
      (co-using []))))

(defn twitter-bootstrap-components
  "Serve Twitter Bootstrap CSS, Javascript and other resources from a
   web-jar."
  [system config]
  (assoc system
    :twitter-bootstrap-service
    (->
      (make new-static-resource-service config :uri-context "/bootstrap" :resource-prefix "META-INF/resources/webjars/bootstrap/3.3.0")
      (using [])
      (co-using []))))

(defn jquery-components
  "Serve JQuery resources from a web-jar."
  [system config]
  (assoc system
    :jquery-resources
    (->
      (make new-static-resource-service config :uri-context "/jquery" :resource-prefix "META-INF/resources/webjars/jquery/2.1.0")
      (using [])
      (co-using []))))

(defn less-compiler-components
  "Compile LESS files to CSS."
  [system config]
  (assoc system
    :less-compiler-compiler
    (->
      (make new-less-compiler config :source-dir "resources/less" :source-path "clean-blog.less")
      (using [])
      (co-using []))))

(defn clean-blog-resources-components
  [system config]
  (assoc system
    :clean-blog-resources-static
    (->
      (make new-static-resource-service config :uri-context "/static/" :resource-prefix "public/")
      (using [])
      (co-using []))))

(defn clean-blog-website-components
  "A redirect component is added which ensures that / will redirect to
   the blog index. Remove this component (and this docstring) if you
   don't want this."
  [system config]
  (assoc system
         :clean-blog-website-pages
         (->
          (make new-pages config
                :title "Extreme moderation"
                :subtitle "Striving for balance and harmony in crafting software")
          (using [])
          (co-using []))
         :clean-blog-website-redirect
         (->
          (make new-redirect config :to :malcolmsparks.blog.pages/index)
          (using [])
          (co-using []))))


(defn new-system-map
  [config]
  (apply system-map
    (apply concat
      (-> {}

          (http-listener-components config)
          (modular-bidi-router-components config)
          (clostache-templater-components config)
          (twitter-bootstrap-components config)
          (jquery-components config)
          (less-compiler-components config)
          (clean-blog-resources-components config)
          (clean-blog-website-components config)))))

(defn new-dependency-map
  []
  {:http-listener-listener {:request-handler :modular-bidi-router-webrouter}, :modular-bidi-router-webrouter {:twitter-bootstrap :twitter-bootstrap-service, :jquery :jquery-resources, :compiler :less-compiler-compiler, :static :clean-blog-resources-static, :redirect :clean-blog-website-redirect, :pages :clean-blog-website-pages}, :clean-blog-website-pages {:templater :clostache-templater-templater, :resources :clean-blog-resources-static}})

(defn new-co-dependency-map
  []
  {:clean-blog-website-pages {:router :modular-bidi-router-webrouter}})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map (merge (config) opts))
     (system-using (new-dependency-map))
     (system-co-using (new-co-dependency-map))))
  ([] (new-production-system {})))
