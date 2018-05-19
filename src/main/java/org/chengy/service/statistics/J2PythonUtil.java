package org.chengy.service.statistics;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class J2PythonUtil {
    static ObjectMapper objectMapper = new ObjectMapper();


    public static PythonRes callPythonProcess(String[] args) {
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
        Process process = null;
        PythonRes pythonRes = new PythonRes();
        try {
            process = Runtime.getRuntime().exec(args);
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s;
            String ss = null;
            while ((s = stdOut.readLine()) != null) {
                ss = s;
            }
            try {
                Map<String, Object> map = objectMapper.readValue(ss, new TypeReference<Map<String, Object>>() {
                });
                pythonRes.scoreMap = map;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 0是调用进程正常
            int result = 1;
            result = process.waitFor();
            pythonRes.code = result;
            process.destroy();
        } catch (InterruptedException | IOException e) {
            // e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return pythonRes;
    }

    public static class PythonRes {
        private int code;
        private Map<String, Object> scoreMap;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }


        public Map<String, Object> getScoreMap() {
            return scoreMap;
        }

        public void setScoreMap(Map<String, Object> scoreMap) {
            this.scoreMap = scoreMap;
        }
    }

}
