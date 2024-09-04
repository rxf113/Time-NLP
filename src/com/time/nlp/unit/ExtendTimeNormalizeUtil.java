package com.time.nlp.unit;

import com.time.nlp.CusTimeModel;
import com.time.nlp.CusTimeNormalizer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.time.util.StringNumberUtil.replaceCapitalNumberWithNumber;
import static com.time.util.DateUtil.ISO_8601_FORMATTER;

/**
 * 拓展 特殊case等
 */
public enum ExtendTimeNormalizeUtil {
    INSTANCE;

    public static ExtendTimeNormalizeUtil getInstance() {
        return INSTANCE;
    }

    final Pattern lastPat = Pattern.compile("(?:这|近|最近|[未将]来)?(\\d+)(天|周|月|年)");

    Set<String> nowWords = new HashSet<>(Arrays.asList("实时", "接下来", "现在", "当前", "近期", "最近", "后面"));

    ExtendTimeNormalizeUtil() {
    }

    private String requiredReplace(String originTime) {
        originTime = replaceCapitalNumberWithNumber(originTime);
        return originTime
                .replace("几天", "3天")
                .replace("几周", "2周")
                .replace("星期", "周")
                .replace("个周", "周")

                .replace("今夜", "今晚")
                .replace("明夜", "明晚")
                .replace("暑假", "6月到9月");
    }

    public CusTimeModel rangeTime(String time) {
        time = requiredReplace(time);

        //1. 最近的
        Matcher matcher = lastPat.matcher(time);
        if (matcher.find()) {
            int count = matcher.groupCount();
            String number = matcher.group(count - 1);
            String unit = matcher.group(count);
            String pre = "下";
            String mappingTime = pre + number + unit;
            return CusTimeNormalizer.getINSTANCE().rangeTimeProcess("", mappingTime);
        }
        //2. 现在
        if (nowWords.contains(time)) {
            CusTimeModel cusTimeModel = new CusTimeModel();
            cusTimeModel.setOriginTime(time);
            cusTimeModel.setTime(LocalDateTime.now().format(ISO_8601_FORMATTER));
            return cusTimeModel;
        }
        return CusTimeNormalizer.getINSTANCE().rangeTimeProcess("", time);
    }
}
