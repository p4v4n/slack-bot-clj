# slack-bot-clj

A slack bot to store messages and post them on behalf of user at a later time.

## Installation

#### Software requirements :

- [Leiningen](https://leiningen.org/)
- [slack-rtm](https://github.com/casidiablo/slack-rtm)

## Usage

Create a slack-token for your bot [here](https://api.slack.com/tokens) and pass it to the _API-TOKEN_ variable in bot.clj.

    lein run

## Example

Send a private message to the bot with the following syntax

```
send [target-time] [target-channel] [target-text]
```

The target-text will be posted by the bot in the target-channel at the target-time.

*Note*: The target-time is currently set to match GMT.

ex:

```
send 2017-07-15-08-10-15 bot-test Happy Independence Day
```

### Bugs

Please file any issues you find on github with minimal sample code that demonstrates the problem.

## License

Copyright © 2017 p4v4n

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
