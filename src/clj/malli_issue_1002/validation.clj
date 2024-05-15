(ns malli-issue-1002.validation
  (:require cljs.pprint
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [malli.core :as mc]
            [malli.registry :as mr]
            [malli.transform :as mt]
            [malli.util :as mu]))




(defn string-trimmer []
  (mt/transformer
   {:decoders
    {:string
     {:compile (fn [schema _]
                 (let [{:string/keys [trim]} (mc/properties schema)]
                   (when trim #(do
                                 (println (str "trimming '" % "'"))
                                 (cond-> % (string? %) str/trim)))))}}}))

(def string-transformer (mt/transformer mt/string-transformer
                                        string-trimmer
                                         ;mt/strip-extra-keys-transformer
                                        mt/default-value-transformer))

(defn optional-keys
  "Makes map keys recursively optional."
  [?schema]
  (mc/walk
   ?schema
   (mc/schema-walker
    (fn [schema]
      (if (= :map (mc/type schema))
        (mu/optional-keys schema)
        (if (= :fn (mc/type schema)) ; disable custom validations as well
          (let [f (fn [_] ;replace previous validation with function always returning true
                    [(fn [_] true)])]
            (mc/into-schema (mc/-parent schema) (mc/-properties schema) (f (mc/-children schema)) (mc/options schema)))
          schema))))))

(def decimal
  (mc/-simple-schema
   {:type :lohnica/amount
    :compile (fn [_properties [min max] _options]
               {:pred (fn [v] (and (decimal? v)
                                   (if min (> v min) true)
                                   (if max (< v max) true)))
                :type-properties {;:error/message {:en "Invalid decimal amount"
                                  ;                 :de "Ungültige Zahl"}
                                  :error/fn (fn [error _] (cond (and min (= min max)) (str "Muss " min " sein")
                                                                (and min max) (str "Muss zwischen " min " und " max " liegen")
                                                                min (str "Mindestens " min)
                                                                max (str "Maximal " max)
                                                                :else "Ungültiger Wert"))
                                  :decode/string (fn [s]
                                                   (cond (and (string? s) (str/blank? s)) nil
                                                         (nil? s) nil
                                                         :else (->
                                                                (if (number? s) s (-> s
                                                                                      (str/replace "," ".")
                                                                                      (edn/read-string)))
                                                                (bigdec))))
                                  :decode/json (fn [s]
                                                 (cond (and (string? s) (str/blank? s)) nil
                                                       (nil? s) nil
                                                       :else (->
                                                              (if (number? s) s (-> s
                                                                                    (str/replace "," ".")
                                                                                    (edn/read-string)))
                                                              (bigdec))))
                                  :json-schema (merge {:type "number"}
                                                      (when min {:minimum min})
                                                      (when max {:maximum max}))
                                  :gen/gen (gen/double* (merge (when min {:min (inc min)})
                                                               (when max {:max max})))}})}))



(def employee-schema
  [:map
   [:finalized {:optional true} boolean?]
   [:active {:optional true} boolean?]
   [:personnel-number {:optional true} [:int {:min 1}]]
   [:establishment-id {:optional true} :uuid]
   [:salutation {:optional true} [:enum :male :female]]
   [:gender [:enum :male :female :diverse :unknown]]
   [:firstname [:string {:min 1}]]
   [:lastname [:string {:min 1}]]
   [:title {:optional true} [:string {:min 1}]]
   [:name-prefix {:optional true} [:string {:min 1}]]
   [:name-extension {:optional true} [:string {:min 1}]]
   [:birthdate :string]

   [:phone {:optional true} :string]
   [:mobile-phone {:optional true} :string]
   [:email {:optional true} :string]
   [:address {:optional true} [:map
                               [:street {:optional true} :string]
                               [:street-number {:optional true} :string]
                               [:supplement {:optional true} :string]
                               [:city {:optional true} :string]
                               [:postcode {:optional true} :string]]]

   [:tax-details [:map
                  [:employment-type [:enum :primary :secondary]]
                  [:mini-job-taxation-method {:optional true} [:enum :lump-sum :tax-card]]
                  [:income-tax-class [:enum "1" "2" "3" "4" "5" "6"]]
                  [:tax-id :string]
                  [:child-allowance [:enum
                                     "0" "0.5" "1" "1.5" "2" "2.5" "3" "3.5" "4" "4.5" "5" "5.5" "6" "6.5"
                                     "7" "7.5" "8" "8.5" "9"]]
                  [:income-tax-factor {:optional true} [:maybe [decimal {:min 0 :max 1}]]]
                  [:tax-exempt-or-additional-amount {:optional true} [:maybe [:enum "exempt" "additional"]]]
                  [:tax-exempt-amount-monthly {:optional true} decimal]
                  [:tax-exempt-amount-yearly {:optional true} decimal]
                  [:tax-additional-amount-monthly {:optional true} decimal]
                  [:tax-additional-amount-yearly {:optional true} decimal]]]
   [:social-insurance
    [:and
     [:map

      [:type {:title "Type"
              :description "Type of social insurance"
              :json-schema/default :compulsory}
       [:enum :compulsory :private :voluntary :voluntary-self-payer]]
      [:health-insurance-company-number [:string {:min 1}]]


      [:social-insurance-number {:optional true} [:string {:min 1}]]
      [:social-insurance-number-unknown boolean?]
      [:birth-name {:optional true} [:string {:min 1}]]
      [:birth-name-prefix {:optional true} [:string {:min 1}]]
      [:birth-name-extension {:optional true} [:string {:min 1}]]
      [:birth-place {:optional true} [:string {:min 1}]]
      [:eu-social-insurance-number {:optional true} string?]
      [:private-health-insurance-premium {:optional true} number?]
      [:private-nursing-care-insurance-premium {:optional true} number?]
      [:private-health-insurance-base-amount {:optional true} number?]
      [:private-nursing-care-insurance-base-amount {:optional true} number?]
      [:has-children {:optional true} boolean?]
      [:minijob-pension-insurance-exemption {:optional true} boolean?]
      [:subsidy-regulation-voluntarily-insured {:optional true} [:enum :subsidy-regulation-contribution-ceiling :subsidy-regulation-actual-income]]
      [:u1-u2-contribution-obligation {:optional true} [:enum :u1-u2-contribution :u2-only :exempt]]]]]])


(def registry
  (merge
   (mc/default-schemas)
   {
    ::decimal decimal
    ::employee employee-schema
    ::foo (optional-keys employee-schema)
    ::bar (optional-keys employee-schema)

}))

(mr/set-default-registry! registry)