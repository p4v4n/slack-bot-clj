(ns slack-bot-clj.bot
	(:require [slack-rtm.core :as rtm]
		      [clojure.string :as str]))

(def API-TOKEN ((read-string (slurp "api-token.json")) "token"))

(def rtm-conn (rtm/connect API-TOKEN))

(def events-pub (:events-publication rtm-conn))

(def dispatcher (:dispatcher rtm-conn))

(def pong-receiver #(println "got this:" %))
(rtm/sub-to-event events-pub :pong pong-receiver)

(rtm/send-event dispatcher {:type "ping"})
; got this: {:type pong, :reply_to 429753360}

(defn send-typing-indicator [channel-id]
  (rtm/send-event dispatcher {:id 1
		                      :type "typing"
		                      :channel channel-id}))

(defn message-handler [message]
  (let [text (:text message)
		channel-id (:channel message)]
    (println message)
    (send-typing-indicator channel-id)
    (cond
      (str/includes? text "hello") (rtm/send-event dispatcher {:type "message"
		                                                       :channel channel-id
		                                                       :text "hello to you sir!"})
      (str/includes? text "?") (rtm/send-event dispatcher {:type "message"
		                                                   :channel channel-id
		                                                   :text "No"}))))

(rtm/sub-to-event events-pub :message message-handler)




