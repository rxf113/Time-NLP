package com.time.nlp.unit;

import com.time.nlp.CusTimeModel;
import com.time.nlp.TimeUnit;
import com.time.util.DateUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.time.util.DateUtil.ISO_8601_FORMATTER_START;

public enum YearProcess {
    INSTANCE;

    public static YearProcess getInstance() {
        return INSTANCE;
    }

    private final Pattern NUM_YEAR_PATTERN = Pattern.compile(".*([前后上下])(\\d+)(年).*");

    public CusTimeModel process(String time, TimeUnit[] units) {

        CusTimeModel cusTimeModel = new CusTimeModel();

        Matcher matcher = NUM_YEAR_PATTERN.matcher(time);
        if (matcher.find()) {
            String dir = matcher.group(1);
            String interval = matcher.group(2);

            LocalDateTime now = LocalDateTime.now();
            if (dir.equals("前") || dir.equals("上")) {
                cusTimeModel.setTimeEnd(now.plusDays(1).format(ISO_8601_FORMATTER_START));
                LocalDateTime preDay = now.minusYears(Integer.parseInt(interval));
                cusTimeModel.setTime(preDay.format(ISO_8601_FORMATTER_START));
            } else {
                cusTimeModel.setTime(now.format(ISO_8601_FORMATTER_START));
                LocalDateTime preDay = now.plusYears(Integer.parseInt(interval));
                cusTimeModel.setTimeEnd(preDay.plusDays(1).format(ISO_8601_FORMATTER_START));
            }
            return cusTimeModel;
        }

        //其它时间，单天，添加首位
        Date date = units[0].getTime();
        cusTimeModel.setTime(DateUtil.formatDateStartDefault(date));

        Instant instant = date.toInstant();
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        cusTimeModel.setTimeEnd(localDateTime.plusYears(1).format(ISO_8601_FORMATTER_START));
        //将 Time_Expression 做为originTime
        cusTimeModel.setOriginTime(units[0].Time_Expression);
        return cusTimeModel;
    }
}
