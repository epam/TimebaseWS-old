# TimeBase Gateway websocket API

## 1. STOMP API

### 1.1 Authentication
The purpose of authentication and authorization is to deny public access to private resources and 
minimize the possibility of access leak.

#### 1.1.1 Tokens
Our security model uses two types of tokens: access token and refresh token. This is the default security 
model for Spring MVC.

Access token is used to sign each REST API request and websocket STOMP CONNECT request. Requests without 
access token or with expired access token will be denied with AccessDenied error code. Access token is 
usually valid only for a short period of time, so that if someone who has stolen it could not use it for 
a long period of time.

Refresh token is used for retrieving new access token, once previous was expired. Refresh token is 
usually valid for a long period of time, since it cannot be updated. Expiration of the refresh token 
will result in user logout.

Since websocket works over a single TCP connection we suppose that it is secured enough and it’s 
messages don’t need to be signed, once the first authentication succeeds. That is why we only require 
STOMP CONNECT request to be signed by a valid access token. This request is sent each time a new 
websocket connection is established, so on reconnecting you need to make sure you have a valid 
access token, and update it using refresh token if needed.

Tokens can be requested via REST API (see REST API documentation).

#### 1.1.2 Connect
The connection endpoint path is `/stomp/v0` (ex, `ws://localhost:8099/stomp/v0`).

Websocket requires authentication token after connection. To use: add authentication token 
(access_token) for CONNECT STOMP message to headers. For example:

```
CONNECT
authorization:f99b9f57-cf69-4715-ad42-5d6c92d23e06
accept-version:1.0,1.1,1.2
heart-beat:10000,10000
```
Field accept-version is required by STOP specification. Supported versions are 1.0, 1.1 and 1.2.

Field heart-beat specifies outgoing and incoming heart-beats rate (the smallest number of milliseconds 
between heart-beats). If this is not specified, websocket connection can be disconnected if no 
outgoing/incoming messages are send during a significant amount of time. 10000 is default and 
it is advisable not to set timeouts less than that.

If Access Denied error is received, client should update access token, using provided refresh 
oken and recreate websocket connection, since any STOMP ERROR breaks the connection.

#### 1.1.3 Keep Alive
In websocket session is kept alive by STOMP HEARTBEAT messages. Since default timeout for 
HEARTBEAT websocket messages is 10 seconds it is strongly advised not to set keepalive timeout 
less then 10 seconds.

Why not use refresh token validity time for this purpose? On the one hand, we don’t want to logout 
active users too often, so refresh token timeout is usually wanted to be large - at least several days, 
or even months. On the other hand, we don’t want to keep user’s data in memory during such long period 
of time. Usually logouting user that left several hours or even minutes ago won’t have any negative effects.

### 1.2 Topics
 
#### 1.2.1 Stream states

Stream states are posted to `/topic/streams` topic.

New messages are sent **once in a second** at max.

##### Subscribe to stream states updates:
```
SUBSCRIBE
ack:auto
id:sub-0
destination:/topic/streams
```

##### Unsubscribe:
```
UNSUBSCRIBE
ack:auto
id:sub-0
destination:/topic/streams
```

##### Message:
```
MESSAGE
destination:/topic/streams
content-type:application/json;charset=UTF-8
subscription:sub-0
message-id:4-18708
content-length:217

{
    "messageType": "DB_STATE",
    "id": 42,
    "added": ["GDAX"],
    "deleted": ["tempStream"],
    "changed": []
}
```

#### 1.2.2 Monitor user topics

Monitoring is provided on `/user/topic/monitor/{stream-key}` destination.

New messages are sent **once a 500 ms** by default, but generally this
period could be configured via deltix.tbwg.webapp.services.monitorFlushPeriod system
property, that consumes milliseconds.

Parameters:
* **`stream-key`** - destination parameter, means stream key you want to subscribe.
* **`fromTimestamp`** - header parameter, means timestamp you want to subscribe starting from in 
`yyyy-MM-ddThh-mm-ss.SSSZ` form, default: `Long.MIN_VALUE`.
* **`types`** - header parameter, types, you want to subscribe in json string format.
* **`symbols`** - header parameter, symbols, you want to subscribe in json string format.

If no symbols and/or types set, subscription will be for all symbols and/or types.

##### Subscribe to monitor:
```
SUBSCRIBE
ack:auto
id:sub-0
destination:/user/topic/monitor/GDAX
fromTimestamp:2018-06-28T09\c30\c00.123Z
types:["deltix.timebase.api.messages.BarMessage","deltix.timebase.api.messages.MarketMessage"]
symbols:["TEST","BTC/USD"]
```

##### Unsubscribe:
```
UNSUBSCRIBE
ack:auto
id:sub-0
destination:/user/topic/monitor/GDAX
```

##### Message:
```
MESSAGE
destination:/user/topic/monitor/GDAX
content-type:application/json;charset=UTF-8
subscription:sub-0
message-id:4-18708
content-length:217
```
```json5
[
  {
    "type":"deltix.timebase.api.messages.BarMessage",
    "symbol":"BTC/USD",
    "timestamp":"2020-04-21T17:12:03.585Z",
    "instrumentType":"FX",
    "open":"5647.355",
    "close":"4646.3553",
    ...
  },
  ...
]
```
