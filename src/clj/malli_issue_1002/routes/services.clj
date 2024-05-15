(ns malli-issue-1002.routes.services
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.malli]
   [reitit.coercion.malli]
   [malli.util :as mu]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [malli-issue-1002.middleware.formats :as formats]
   [ring.util.http-response :refer :all]
   [clojure.java.io :as io]
   [malli-issue-1002.validation :as validation]))

(defn service-routes []
  ["/api"
   {:coercion (reitit.coercion.malli/create
               {:transformers {:body {:default reitit.coercion.malli/string-transformer-provider
                                      :formats {"application/json" reitit.coercion.malli/json-transformer-provider}}
                               :string {:default reitit.coercion.malli/string-transformer-provider}
                               :response {:default reitit.coercion.malli/default-transformer-provider}}
                    ;; set of keys to include in error messages
                    ;;:error-keys #{:type :coercion :in :schema :value :errors :humanized #_:transformed}
                :error-keys #{:errors :humanized}
                    ;; :error-key #{:humanized}
                    ;; schema identity function (default: close all map schemas)
                :compile mu/closed-schema
                               ;; strip-extra-keys (effects only predefined transformers)
                :strip-extra-keys true
                               ;; add/set default values
                :default-values true
                :enabled true
                               ;; malli options
                :options nil})
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]
   
   ["/employees/{year-month}"
    {:get
     {:parameters {
                   
              ;;;; uncomment the following lines to make the definition of 'foo' disappear in the swagger.json
              ;;      :path [:map
              ;;             [:year-month [:re #"\d{4}-([0]\d|1[0-2])"]]]
              ;;      :query [:map
              ;;              [:something
              ;;               {:description "foo"
              ;;                :optional true}
              ;;               boolean?]]
                   :body [:map [:employee ::validation/foo]]}
      :responses
      {200
       {:body [:map [:employee ::validation/bar]]}}
      :handler (fn [_]
                (ok {:data {}}))}}]

])
