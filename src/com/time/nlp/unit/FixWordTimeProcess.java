package com.time.nlp.unit;

import com.time.nlp.CusTimeModel;
import com.time.nlp.TimeUnit;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import static com.time.util.DateUtil.ISO_8601_FORMATTER;
import static com.time.util.DateUtil.ISO_8601_FORMATTER_START;

public enum FixWordTimeProcess {

    INSTANCE;

    public static FixWordTimeProcess getInstance() {
        return INSTANCE;
    }

    private static class FixWordTime {
        public String time;
        public List<Pattern> patterns;

        public FixWordTime(String time, List<Pattern> patterns) {
            this.time = time;
            this.patterns = patterns;
        }
    }

    List<FixWordTime> fixWordTimes;

    FixWordTimeProcess() {
        fixWordTimes = new ArrayList<>();
        //现在
        String time = LocalDateTime.now().format(ISO_8601_FORMATTER);
        List<Pattern> patterns = Arrays.asList(
                Pattern.compile(".*现在.*"),
                Pattern.compile(".*此[时刻].*"),
                Pattern.compile(".*当前.*"));

        fixWordTimes.add(new FixWordTime(time, patterns));
    }


    public CusTimeModel process(String time) {
        CusTimeModel timeModel = new CusTimeModel();
        for (FixWordTime fixWordTime : fixWordTimes) {
            for (Pattern pattern : fixWordTime.patterns) {
                if (pattern.matcher(time).matches()) {
                    timeModel.setTime(fixWordTime.time);
                    return timeModel;
                }
            }
        }
        return timeModel;
    }

}
