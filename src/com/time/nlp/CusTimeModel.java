package com.time.nlp;

public  class CusTimeModel {
        private String originTime;
        private String repeat;
        private String time;
        private String timeEnd;

        public String getOriginTime() {
            return originTime;
        }

        public void setOriginTime(String originTime) {
            this.originTime = originTime;
        }

        public String getRepeat() {
            return repeat;
        }

        public void setRepeat(String repeat) {
            this.repeat = repeat;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getTimeEnd() {
            return timeEnd;
        }

        public void setTimeEnd(String timeEnd) {
            this.timeEnd = timeEnd;
        }

        @Override
        public String toString() {
            return "CusTimeModel{" +
                    "originTime='" + originTime + '\'' +
                    ", repeat='" + repeat + '\'' +
                    ", time='" + time + '\'' +
                    ", timeEnd='" + timeEnd + '\'' +
                    '}';
        }
    }