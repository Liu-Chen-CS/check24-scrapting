package com.liuchen;

import java.util.*;

public class DataCleaning {
    public static Map<String, List<Map<String,String>>> RemovingNull(Map<String, List<Map<String,String>>> cityDataMap) {
        for (Map.Entry<String, List<Map<String, String>>> entry : cityDataMap.entrySet()) {
            List<Map<String, String>> articles = entry.getValue();

            // 1. 收集该城市的所有列（keys）
            Set<String> columns = new HashSet<>();
            for (Map<String, String> article : articles) {
                columns.addAll(article.keySet());
            }

            // 2. 检查哪些列在该城市的所有文章里全是 "N/A"
            Set<String> columnsToRemove = new HashSet<>();
            for (String column : columns) {
                boolean allNA = true;
                for (Map<String, String> article : articles) {
                    String value = article.get(column);
                    if (value != null && !value.equals("N/A") && !value.equalsIgnoreCase("empty")) {
                        allNA = false;
                        break;  // 只要有一个不是N/A，就不用继续检查该列
                    }
                }
                if (allNA) {
                    columnsToRemove.add(column);
                }
            }

            // 3. 删除该城市中全为N/A的列
            if (!columnsToRemove.isEmpty()) {
                for (Map<String, String> article : articles) {
                    for (String column : columnsToRemove) {
                        article.remove(column);
                    }
                }
            }
        }
        return cityDataMap;
    }
}
