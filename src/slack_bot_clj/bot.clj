(ns slack-bot-clj.bot
	(:require [slack-rtm.core :as rtm]
		      [clojure.string :as str]))

(def API-TOKEN ((read-string (slurp "api-token.json")) "token"))

(def rtm-conn (rtm/start API-TOKEN))

(def events-pub (:events-publication rtm-conn))

(def dispatcher (:dispatcher rtm-conn))

(def pong-receiver #(println "got this:" %))
(rtm/sub-to-event events-pub :pong pong-receiver)

(rtm/send-event dispatcher {:type "ping"})
; got this: {:type pong, :reply_to 429753360}

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

(defn send-handler [text]
	(let [[s send-time channel-name send-text] (str/split text #"\s+" 4)] 
	  (Thread/sleep 10000)
	  (rtm/send-event dispatcher {:type "message"
		                          :channel (:id (find-channel-by-name channel-name))
		                          :text send-text})))

(defn message-handler [message]
  (let [text (:text message)
		channel-id (:channel message)]
    (println message)
    (send-typing-indicator channel-id)
    (cond
      (str/starts-with? text "hello") (rtm/send-event dispatcher {:type "message"
		                                                       :channel channel-id
		                                                       :text "hello to you sir!"})
      (str/starts-with? text "send") (send-handler text))))

(rtm/sub-to-event events-pub :message message-handler)





