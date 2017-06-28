(ns slack-bot-clj.bot
    (:require [slack-rtm.core :as rtm]
              [clojure.string :as str]
              [clj-time.coerce :as timec]))

(def API-TOKEN ((read-string (slurp "api-token.json")) "token"))

(def message-stack (atom []))

(def rtm-conn (rtm/start API-TOKEN))

(def events-pub (:events-publication rtm-conn))

(def dispatcher (:dispatcher rtm-conn))

(def pong-receiver #(println "got this:" %))
(rtm/sub-to-event events-pub :pong pong-receiver)

(rtm/send-event dispatcher {:type "ping"})

(def current-timestamp (System/currentTimeMillis))

;;------------ Functions -------------- 
(defn gmt-to-utc-timestamp [t]
    (- t (* 330 60 1000)))

(defn datestring-to-timestamp [date-str]
  (->> (str/split date-str #"\-")
       (map read-string)
       (apply clj-time.core/date-time)
       timec/to-long
       gmt-to-utc-timestamp))

(defn find-channel-by-name [channel-name]
  (->> (get-in rtm-conn [:start :channels])
       (filter #(= channel-name (:name_normalized %)))
       first))

(defn find-user-by-name [user-name]
  (->> (get-in rtm-conn [:start :users])
       (filter #(= user-name (:name %)))
       first))

(defn send-typing-indicator [channel-id]
    (rtm/send-event dispatcher {:id 1
                                :type "typing"
                                :channel channel-id}))

(defn add-to-stack [text]
    (let [[s send-time channel-name core-text] (str/split text #"\s+" 4)]
      (swap! message-stack conj {:send-time (datestring-to-timestamp send-time)
                                 :type "message"
                                 :channel (:id (find-channel-by-name channel-name))
                                 :text core-text})))

(defn message-handler [message]
  (let [text (:text message)
        channel-id (:channel message)]
    (println message)
    (send-typing-indicator channel-id)
    (cond
      (str/starts-with? text "send") (add-to-stack text))))

(rtm/sub-to-event events-pub :message message-handler)

