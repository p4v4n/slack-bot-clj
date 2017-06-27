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




