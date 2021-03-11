package com.epam.deltix.tbwg.messages;

public enum SecurityStatus {
  FEED_CONNECTED(0),

  FEED_DISCONNECTED(1),

  TRADING_STARTED(2),

  TRADING_STOPPED(5);

  private final int value;

  SecurityStatus(int value) {
    this.value = value;
  }

  public int getNumber() {
    return this.value;
  }
}
