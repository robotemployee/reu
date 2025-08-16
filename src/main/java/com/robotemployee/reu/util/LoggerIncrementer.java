package com.robotemployee.reu.util;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class LoggerIncrementer {

    // this class is just a logging utility that i can use to like,,, print numbers at different lines easily according to where a certain function has reached

    // just make one and pass a reference to the class you're making it in
    // or if it's in a static context just give it a String label directly
    // then whenever you call increment() it'll output a number that goes up with each output
    // so you can see where the code is

    static final Logger LOGGER = LogUtils.getLogger();
    final String label;
    long index;

    static long amountOfThese = 0;

    public LoggerIncrementer(Object object) {
        this(object.toString());
    }

    public LoggerIncrementer(String label) {
        LOGGER.info(getHeader() + label + "+");
        index = amountOfThese++;
        this.label = label;
    }

    int displayedNumber = 1;
    public void increment() {
        LOGGER.info(String.format(getHeader() + "%s reached %s", label, displayedNumber++));
    }

    public String getHeader() {
        return "LI" + index % 11 + ": ";
    }
}
