(ns slack-bot-clj.bot
    (:require [slack-rtm.core :as rtm]
              [clojure.string :as str]
              [clj-time.coerce :as timec]
              [clj-slack.chat :as chat]
              [clj-slack.channels :as channels]))

(def API-TOKEN (System/getenv "SLACK_API_TOKEN"))

(def message-stack (atom []))

(def current-timestamp (atom 0))

(def rtm-conn (rtm/start API-TOKEN))

(def events-pub (:events-publication rtm-conn))

(def dispatcher (:dispatcher rtm-conn))

(def pong-receiver #(println "got this:" %))
(rtm/sub-to-event events-pub :pong pong-receiver)

(def rest-connection {:api-url "https://slack.com/api" :token (System/getenv "SLACK_API_TOKEN")})
;(rtm/send-event dispatcher {:type "ping"})

;;------------ Functions -------------- 

(defn gmt-to-utc-timestamp [t]
    (- t (* 330 60 1000)))

(defn datestring-to-timestamp [date-str]
  (->> (str/split date-str #"\-")
       (map #(Integer/parseInt %))
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

(defn find-user-by-id [user-id]
  (->> (get-in rtm-conn [:start :users])
       (filter #(= user-id (:id %)))
       first))

;;------------Input Sanitation---------

(defn channel-exists? [channel-name]
    (find-channel-by-name channel-name))

(defn is-bot-channel-member? [channel-name]
    (:is_member (find-channel-by-name channel-name)))

(defn time-format-valid? [time-str]
    (if (re-find #"^(\d+\-){0,6}\d+$" time-str)
         true
         false))
;;---------------------------

(defn send-typing-indicator [channel-id]
    (rtm/send-event dispatcher {:type "typing"
                                :channel channel-id}))

(defn send-message [channel-id text]
    (rtm/send-event dispatcher {:type "message"
                                :channel channel-id
                                :text text}))
;;------------------------------

(defn add-to-stack [text user-id]
    (let [user-info (find-user-by-id user-id)
          [keyw send-time channel-name core-text] (str/split text #"\s+" 4)]
      (swap! message-stack conj {:send-time (datestring-to-timestamp send-time)
                                 :type "message"
                                 :channel (:id (find-channel-by-name channel-name))
                                 :text core-text
                                 :username (:name user-info)
                                 :icon_url (:image_512 (:profile user-info))})
      (reset! message-stack (sort-by :send-time @message-stack))))

(defn message-handler [message]
  (let [text (:text message)
        channel-id (:channel message)
        user-id (:user message)
        [keyw send-time channel-name core-text] (str/split text #"\s+" 4)]
    (println message)
    (send-typing-indicator channel-id)
    (cond
      (and (not= keyw "") (not= keyw "send")) 
          (send-message channel-id (format "The command '%s' doesn't exist" keyw))
      (nil? (channel-exists? channel-name)) 
          (send-message channel-id (format "The channel '%s' is not in the list of public channels." channel-name))
      (false? (is-bot-channel-member? channel-name)) 
          (send-message channel-id (format "Sorry.I am not a member of the channel '%s'" channel-name))
      (false? (time-format-valid? send-time)) 
          (send-message channel-id "The specified time format is invalid.")
      :else 
          (do
            (add-to-stack text user-id)))))

(rtm/sub-to-event events-pub :message message-handler)

;;-----------Clearing the message-stack------------

(defn time-watcher
  [keyy watched old-state new-state]
    (when (and (not-empty @message-stack) (> new-state (:send-time (first @message-stack))))
        (chat/post-message rest-connection (:channel (first @message-stack)) (:text (first @message-stack)) (dissoc (first @message-stack) :send-time :channel :text :type))
        (swap! message-stack rest)))

(add-watch current-timestamp :time-watch time-watcher)

(while true
    (Thread/sleep 10000)
     (reset! current-timestamp (System/currentTimeMillis)))
