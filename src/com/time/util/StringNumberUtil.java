package com.time.util;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理字符串中的数字
 *
 * @author 2018-08-07 12:36
 */

public class StringNumberUtil {

    private static final Map<String, Integer> capitalNumberMap = new HashMap<>();

    private static final Map<String, Integer> unitMap = new HashMap<>();

    //单位的unicode编码 十拾百佰千仟万萬亿億
    private static final String UNICODE_UNIT = "\\u5341\\u62fe\\u767e\\u4f70\\u5343\\u4edf\\u4e07\\u842c\\u4ebf\\u5104";

    //数字的unicode编码 一二三四五六七八九壹幺贰叁肆伍陆柒捌玖
    private static final String UNICODE_NUM_WITHOUT_ZERO = "\\u4e00\\u4e8c\\u4e09\\u56db\\u4e94\\u516d\\u4e03\\u516b\\u4e5d\\u58f9\\u5e7a\\u8d30\\u53c1\\u8086\\u4f0d\\u9646\\u67d2\\u634c\\u7396";

    //数字序列的unicode编码 壹幺贰叁肆伍陆柒捌玖零一二三四五六七八九
    private static final String UNICODE_NUM = "\\u58f9\\u5e7a\\u8d30\\u53c1\\u8086\\u4f0d\\u9646\\u67d2\\u634c\\u7396\\u96f6\\u4e00\\u4e8c\\u4e09\\u56db\\u4e94\\u516d\\u4e03\\u516b\\u4e5d";

    static {
        capitalNumberMap.put("一", 1);
        capitalNumberMap.put("二", 2);
        capitalNumberMap.put("三", 3);
        capitalNumberMap.put("四", 4);
        capitalNumberMap.put("五", 5);
        capitalNumberMap.put("六", 6);
        capitalNumberMap.put("七", 7);
        capitalNumberMap.put("八", 8);
        capitalNumberMap.put("九", 9);

        capitalNumberMap.put("壹", 1);
        capitalNumberMap.put("贰", 2);
        capitalNumberMap.put("叁", 3);
        capitalNumberMap.put("肆", 4);
        capitalNumberMap.put("伍", 5);
        capitalNumberMap.put("陆", 6);
        capitalNumberMap.put("柒", 7);
        capitalNumberMap.put("捌", 8);
        capitalNumberMap.put("玖", 9);

        capitalNumberMap.put("幺", 1);
        capitalNumberMap.put("零", 0);

        //十拾百佰千仟万萬亿億
        unitMap.put("十", 10);
        unitMap.put("拾", 10);
        unitMap.put("百", 100);
        unitMap.put("佰", 100);
        unitMap.put("千", 1000);
        unitMap.put("仟", 1000);
        unitMap.put("万", 10000);
        unitMap.put("萬", 10000);
        unitMap.put("亿", 100000000);
        unitMap.put("億", 100000000);
    }

    /**
     * 将输入中包含的大写数字转换为小写
     * input 十九楼
     * output 19楼
     *
     * @param inputText
     * @return
     */
    public static String replaceCapitalNumberWithNumber(String inputText) {
        String tmp = new String(inputText);

        if (tmp != null && tmp.length() > 0) {
            if (containNumIgnoreCase(tmp)) {
                //包含数字

                //小写数字+大写单位 如250万
                tmp = handleNumWithUnit(tmp);

                //纯大写数字 如二百五十万
                tmp = handleCapitalNumber(tmp);

                //数字序列 如二五八
                tmp = handleCapitalNumSeq(tmp);

            }
        }
        return tmp;
    }

    /**
     * 处理大写数字序列 二五零
     *
     * @param inputText
     * @return
     */
    public static String handleCapitalNumSeq(String inputText) {
        Pattern numSeqRegex = getCapitalNumberSequenceRegex();
        Matcher numSeqMatcher = numSeqRegex.matcher(inputText);

        while (numSeqMatcher.find()) {
            String matchStr = numSeqMatcher.group("cns");
            if (matchStr != null && matchStr.length() > 0) {
                //将匹配到的大写数字字符串转为小写
                String numSeq = convertCapitalNumSeqToNum(matchStr);
                if (numSeq != null && numSeq.length() > 0) {
                    //替换匹配的字符串
                    inputText = inputText.substring(0, numSeqMatcher.start()) + numSeq + inputText.substring(numSeqMatcher.end());
                    numSeqMatcher = numSeqRegex.matcher(inputText);
                }
            }
        }
        return inputText;
    }

    /**
     * 处理纯大写数字 二百五十万
     *
     * @param inputText
     * @return
     */
    public static String handleCapitalNumber(String inputText) {
        Pattern capitalNumRegex = getCapitalNumberRegex();
        Matcher capitalNumMatcher = capitalNumRegex.matcher(inputText);

        while (capitalNumMatcher.find()) {
            String matchStr = capitalNumMatcher.group();
            if (matchStr != null && matchStr.length() > 0) {
                //将匹配的大写数字字符串转为小写
                Long number = convertCapitalNumberToLower(matchStr);
                if (number != null) {
                    //替换匹配的字符串
                    inputText = inputText.substring(0, capitalNumMatcher.start()) + String.valueOf(number) + inputText.substring(capitalNumMatcher.end());
                    capitalNumMatcher = capitalNumRegex.matcher(inputText);
                }
            }
        }
        return inputText;
    }

    /**
     * 处理数字加单位 250万
     *
     * @param inputText
     * @return
     */
    public static String handleNumWithUnit(String inputText) {
        Pattern numUnitRegex = getNumWithUnitRegex();
        Matcher numUnitMatcher = numUnitRegex.matcher(inputText);
        while (numUnitMatcher.find()) {
            String matchStr = numUnitMatcher.group();
            if (matchStr != null && matchStr.length() > 0) {
                //将匹配的字符串转为数字
                Long number = convertNumWithUnitToNum(matchStr);
                if (number != null) {
                    //替换匹配的字符串
                    inputText = inputText.substring(0, numUnitMatcher.start()) + String.valueOf(number) + inputText.substring(numUnitMatcher.end());
                    numUnitMatcher = numUnitRegex.matcher(inputText);
                }
            }
        }
        return inputText;
    }

    /**
     * 输入的字符串中是否包含数字, 忽略大小写
     *
     * @param input
     * @return
     */
    public static boolean containNumIgnoreCase(String input) {
        String pattern = "^.*" +
                "(" +
                "(" +
                "(" +
                "[" + UNICODE_NUM_WITHOUT_ZERO + "]" + //[一二三四五六七八九壹幺贰叁肆伍陆柒捌玖]
                "[" + UNICODE_UNIT + "]" + //[十拾百佰千仟万萬亿億]
                ")+" +
                "\\u96f6?" + //[零]
                "[" + UNICODE_NUM_WITHOUT_ZERO + "]?" + //[一二三四五六七八九壹幺贰叁肆伍陆柒捌玖]
                "|" +
                "[\\u5341\\u62fe]" + //[十拾]
                "[" + UNICODE_NUM_WITHOUT_ZERO + "]" + //[一二三四五六七八九壹幺贰叁肆伍陆柒捌玖]
                "|" +
                "[0-9]+" +
                ")" +
                "[\\u4e07\\u842c\\u4ebf\\u5104]?" + //[万萬亿億]
                "|" +
                "[" + UNICODE_NUM + "]+" +
                ")" +
                ".*$";
        Pattern regex = Pattern.compile(pattern);
        return regex.matcher(input).matches();
    }

    /**
     * 将大写数字转换为小写
     *
     * @param capitalNum
     * @return
     */
    public static Long convertCapitalNumberToLower(String capitalNum) {
        if (capitalNum != null && capitalNum.length() > 0) {
            String cn = new String(capitalNum);
            long resultNumber = 0L;

            /**
             * 处理亿
             */
            int yIndex = cn.lastIndexOf("亿");
            if (yIndex == -1)
                yIndex = cn.lastIndexOf("億");

            String yi = yIndex != -1 ? cn.substring(0, yIndex + 1) : "";

            if (!"".equals(yi))
                //计算亿前面的值
                resultNumber = convertCapitalNumberToLower(yi.substring(0, yi.length() - 1)) * 100000000;

            cn = yIndex != -1 ? cn.substring(yIndex + 1) : cn;

            int wIndex = cn.lastIndexOf("万");
            if (wIndex == -1)
                wIndex = cn.lastIndexOf("萬");

            String wan = wIndex != -1 ? cn.substring(0, wIndex + 1) : "";
            cn = wIndex != -1 ? cn.substring(wIndex + 1) : cn;

            long wanNum = !"".equals(wan) ? convertNumToLower(wan.substring(0, wan.length() - 1), 10000) : 0;
            long geNum = !"".equals(cn) ? convertNumToLower(cn, 1) : 0;

            return resultNumber + wanNum + geNum;
        } else {
            return null;
        }
    }

    /**
     * 转换亿以下的大写数字
     *
     * @param numStr 要转换的大写数字
     * @param unit   单位的阿拉伯数值,如万, 则为10000, 个 则为1
     * @return
     */
    private static long convertNumToLower(String numStr, int unit) {
        long num = 0L;
        String text = new String(numStr);
        /**
         * 分割千,百,十,个
         */
        //千
        int thousandIndex = text.lastIndexOf("千");
        if (-1 == thousandIndex)
            thousandIndex = text.lastIndexOf("仟");

        String thousand = thousandIndex != -1 ? text.substring(0, thousandIndex) : "";
        if (!"".equals(thousand))
            num += numCharToLower(thousand.charAt(thousand.length() - 1)) * 1000;

        text = thousandIndex != -1 ? text.substring(thousandIndex + 1) : text;

        //百
        int hundredIndex = text.lastIndexOf("百");
        if (-1 == hundredIndex)
            hundredIndex = text.lastIndexOf("佰");

        String hundred = hundredIndex != -1 ? text.substring(0, hundredIndex) : "";
        if (!"".equals(hundred))
            num += numCharToLower(hundred.charAt(hundred.length() - 1)) * 100;

        text = hundredIndex != -1 ? text.substring(hundredIndex + 1) : text;

        //十
        int tenIndex = text.lastIndexOf("十");
        if (-1 == tenIndex)
            tenIndex = text.lastIndexOf("拾");

        String ten = tenIndex != -1 ? text.substring(0, tenIndex) : "";
        if (-1 != tenIndex && ten.equals(""))
            ten = "一";
        if (!"".equals(ten))
            num += numCharToLower(ten.charAt(ten.length() - 1)) * 10;

        text = tenIndex != -1 ? text.substring(tenIndex + 1) : text;
        //个
        if (!"".equals(text)) {
            if (unit == 1 && (numStr.length() - text.length() - 1) >= 0) {
                //取个位的前一位单位
                char pUnit = numStr.charAt(numStr.length() - text.length() - 1);
                Integer pUnitNum = unitMap.get(String.valueOf(pUnit));
                if (pUnitNum != null && !(text.startsWith("零") || text.startsWith("0")))
                    num += (numCharToLower(text.charAt(text.length() - 1)) * (pUnitNum / 10));
                else
                    num += numCharToLower(text.charAt(text.length() - 1));
            } else {
                num += numCharToLower(text.charAt(text.length() - 1));
            }
        }

        return num * unit;
    }

    /**
     * 大写数字字符转成小写数字
     *
     * @param numChar
     * @return
     */
    private static long numCharToLower(char numChar) {
        long lower = 0L;
        Integer targetNum = capitalNumberMap.get(String.valueOf(numChar));
        if (targetNum != null)
            lower = targetNum;
        return lower;
    }

    /**
     * 大写数字序列转为小写序列
     *
     * @param capitalNumSeq
     * @return
     */
    private static String convertCapitalNumSeqToNum(String capitalNumSeq) {
        if (capitalNumSeq != null && capitalNumSeq.length() > 0) {
            String text = new String(capitalNumSeq);
            for (int i = 0; i < text.length(); i++) {
                char cn = text.charAt(i);
                long cn_l = numCharToLower(cn);
                text = (i == 0 ? "" : text.substring(0, i))
                        + String.valueOf(cn_l)
                        + ((i + 1) < text.length() ? text.substring(i + 1) : "")
                ;
            }
            return text;
        } else {
            return capitalNumSeq;
        }
    }

    /**
     * 匹配大写数字的正则
     * 二十一
     * 贰拾壹
     *
     * @return
     */
    public static Pattern getCapitalNumberRegex() {
        String pattern = "(" +
                "(" +
                "(" +
                "[" + UNICODE_NUM_WITHOUT_ZERO + "]" + //[一二三四五六七八九壹幺贰叁肆伍陆柒捌玖]
                "[" + UNICODE_UNIT + "]" + //[十拾百佰千仟万萬亿億]
                ")+" +
                "\\u96f6?" + //[零]
                "[" + UNICODE_NUM_WITHOUT_ZERO + "]?" + //[一二三四五六七八九壹幺贰叁肆伍陆柒捌玖]
                "|" +
                "[\\u5341\\u62fe]" + //[十拾]
                "[" + UNICODE_NUM_WITHOUT_ZERO + "]" + //[一二三四五六七八九壹幺贰叁肆伍陆柒捌玖]
                ")" +
                "[\\u4e07\\u842c\\u4ebf\\u5104]?" +
                ")";
        return Pattern.compile(pattern);
    }

    /**
     * 匹配数字+单位
     * 25万亿
     *
     * @return
     */
    public static Pattern getNumWithUnitRegex() {
        String pattern = "(?<number>[0-9]+)" +
                "(?<unit>" +
                "(" +
                "[\\u5341\\u62fe\\u767e\\u4f70\\u5343\\u4edf]" + //十拾百佰千仟
                "[\\u4e07\\u842c]?" + //万萬
                "[\\u4ebf\\u5104]?" +  //亿億
                "|" +
                "[\\u4e07\\u842c]?[\\u4ebf\\u5104]" + //万亿, 亿
                "|" +
                "[\\u4e07\\u842c]" + //万
                ")" +
                ")";
        return Pattern.compile(pattern);
    }

    /**
     * 匹配大写的数字序列的正则 如一二五, 壹贰伍, 幺贰伍
     * 匹配的结果存在group cns 中
     *
     * @return
     */
    public static Pattern getCapitalNumberSequenceRegex() {
        String regexStr = "(?<cns>[" + UNICODE_NUM + "]+)";
        return Pattern.compile(regexStr);
    }

    /**
     * 将数字+单位的值转换为纯数字
     * 25万 ---> 250000
     *
     * @param input
     * @return
     */
    public static Long convertNumWithUnitToNum(String input) {
        if (input != null && input.length() > 0) {
            Pattern regex = getNumWithUnitRegex();
            Matcher matcher = regex.matcher(input);
            if (matcher.matches()) {
                Long num = 0L;
                String numStr = matcher.group("number");
                String unitStr = matcher.group("unit");
                num = Long.valueOf(numStr);

                if (unitStr != null && unitStr.length() > 0) {
                    for (int i = 0; i < unitStr.length(); i++) {
                        char c = unitStr.charAt(i);
                        Integer scale = unitMap.get(String.valueOf(c));
                        if (scale != null && scale != 0)
                            num *= scale;
                    }
                }
                return num;
            }
        }
        return null;
    }

    //提取 "4.6分以上 5分左右" 里的数字
    static Pattern compile = Pattern.compile("\\d+(\\.\\d+)?");

    public static String extractNumber(String originStr) {
        if (StringUtils.isBlank(originStr)) {
            return "";
        }
        Matcher matcher = compile.matcher(originStr);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

}
