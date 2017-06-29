(ns slack-bot-clj.bot
    (:require [slack-rtm.core :as rtm]
              [clojure.string :as str]
              [clj-time.coerce :as timec]))

(def API-TOKEN ((read-string (slurp "api-token.json")) "token"))

(def message-stack (atom []))

(def current-timestamp (atom 0))

(def rtm-conn (rtm/start API-TOKEN))

(def events-pub (:events-publication rtm-conn))

(def dispatcher (:dispatcher rtm-conn))

(def pong-receiver #(println "got this:" %))
(rtm/sub-to-event events-pub :pong pong-receiver)

(rtm/send-event dispatcher {:type "ping"})

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
                                 :text core-text})
      (reset! message-stack (sort-by :send-time @message-stack))))

(defn message-handler [message]
  (let [text (:text message)
        channel-id (:channel message)]
    (println message)
    (send-typing-indicator channel-id)
    (cond
      (str/starts-with? text "send") (add-to-stack text))))

(rtm/sub-to-event events-pub :message message-handler)

;;-----------Getting rid of message stack------------

(defn time-watcher
  [keyy watched old-state new-state]
    (when (and (not-empty @message-stack) (> new-state (:send-time (first @message-stack))))
        (rtm/send-event dispatcher (dissoc (first @message-stack) :send-time))
        (swap! message-stack rest)))

(add-watch current-timestamp :time-watch time-watcher)

(while true
    (Thread/sleep 10000)
     (reset! current-timestamp (System/currentTimeMillis)))
