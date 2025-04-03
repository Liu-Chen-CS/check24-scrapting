package com.liuchen;

import com.microsoft.playwright.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        Map<String, List<Map<String, String>>> cityDataMap = new LinkedHashMap<>();

        // 邮编列表
        List<Postcode> postcodes = Arrays.asList(
                new Postcode("65604", "Elz"),
                new Postcode("80331", "München")
        );

        try (Playwright playwright = Playwright.create()) {
            // 启动浏览器(放在循环之前)
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));

            for (Postcode postcode : postcodes) {
                String city = postcode.city;
                String code = postcode.code;
                System.out.println("------------------------");
                System.out.println("正在处理邮编: " + code + " (" + city + ")");

                cityDataMap.putIfAbsent(city, new ArrayList<>());

                // 为每个邮编创建新页面
                try (Page page = browser.newPage()) {
                    // 构建URL
                    String url = String.format(
                            "https://www.check24.de/strom/vergleich/check24/?totalconsumption=2500&zipcode=%s&city=%s" +
                                    "&pid=24&pricecap=no&pricing=month&product_id=1&calculationparameter_id=da21493eee0643df413e3aa705760bb2",
                            postcode.code,
                            URLEncoder.encode(postcode.city, StandardCharsets.UTF_8.toString())
                    );

                    // 导航到页面
                    System.out.println("访问URL: " + url);
                    page.navigate(url);

                    // 等待结果加载(增加超时时间)
                    page.waitForSelector("div.result__items", new Page.WaitForSelectorOptions().setTimeout(30000));

                    System.out.println("------------------------");
                    for (int i = 0; i < 2; i++) {
                        System.out.println("页面滚动: " + i + "次");
                        page.evaluate("window.scrollBy(0, window.innerHeight)");
                        page.waitForTimeout(2000); // 等待2秒
                    }
                    System.out.println("------------------------");

                    // 监听AJAX请求(可选)
                    page.onResponse(response -> {
                        if (response.url().contains("pagination") && response.request().method().equals("GET")) {
                            System.out.println("捕获到分页请求: " + response.url());
                        }
                    });

                    // 获取所有文章元素
                    List<ElementHandle> articles = page.querySelectorAll("div.result__items article");
                    System.out.println("找到文章数量: " + articles.size());
                    System.out.println("------------------------");

                    // 提取每个文章的信息
                    for (int i = 0; i < articles.size(); i++) {
                        Map<String, String> articleData = extractArticleInfo(articles.get(i), i+1);
                        cityDataMap.get(city).add(articleData);
                    }

                    // 处理完成后等待一会儿
                    page.waitForTimeout(3000);
                } catch (Exception e) {
                    System.err.println("处理邮编 " + postcode.code + " 时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 所有邮编处理完成后关闭浏览器
            System.out.println("所有邮编处理完成, 关闭浏览器");
            browser.close();

            //数据清理
            Map<String, List<Map<String, String>>> cityDataMapCleaned = DataCleaning.RemovingNull(cityDataMap);

            // 导出到Excel
            try {
                ExcelGenerator.generateExcelWithSheets(cityDataMapCleaned, "爬虫结果.xlsx");
                System.out.println("Excel文件生成成功！");
            } catch (IOException e) {
                System.err.println("生成Excel文件时出错: " + e.getMessage());
            }
        }
    }

    private static Map<String, String> extractArticleInfo(ElementHandle article, int index) {
        Map<String, String> data = new LinkedHashMap<>();
        try {
            // 提取品牌名称
            ElementHandle brandImage = article.querySelector("img.logo__img");
            String brandName = brandImage != null ? brandImage.getAttribute("alt") != null ? brandImage.getAttribute("alt") : "N/A" : "N/A";

            // 提取品牌名称
            ElementHandle tariffBrandNameHandle = article.querySelector("div.tariff-brand__name");
            String tariffBrandName = tariffBrandNameHandle != null ? tariffBrandNameHandle.innerText() : "N/A";

            // 提取评分
            ElementHandle tariffGrade = article.querySelector("tariff-grade");
            String grade = tariffGrade != null ? tariffGrade.getAttribute("grade") : "N/A";
            String rating = tariffGrade != null ? tariffGrade.getAttribute("rating") : "N/A";

            // 提取价格捕获器
            List<ElementHandle> priceCatchers = article.querySelectorAll("div.result-row__priceCatcher");
            Map<String, String> priceCatcherMap = new LinkedHashMap<>();
            for (ElementHandle priceCatcher : priceCatchers) {
                String priceCatcherName = priceCatcher.innerText();
                if(!priceCatcherName.isEmpty()){
                    String[] priceCatcherNameArr = priceCatcherName.trim().replaceAll("[\u00AD\u200B\u2010-\u2015]", "").split(":");
                    priceCatcherMap.put(priceCatcherNameArr[0], priceCatcherNameArr[1].trim());
                }
            }

            // 提取限时优惠
            ElementHandle limitedTimeCatchers = article.querySelector("div.result-row__limitedTimeCatcher");
            String limitedTimeCatcherText = limitedTimeCatchers == null ? "N/A" :
                    limitedTimeCatchers.innerText().trim().isEmpty() ? "Empty" : limitedTimeCatchers.innerText();

            List<ElementHandle> resultRowSecondaryCatcherElement = article.querySelectorAll("div.result-row__secondaryCatcherRow *");
            Map<String, String> tooltipContentMap = new LinkedHashMap<>();
            for (ElementHandle childElement : resultRowSecondaryCatcherElement) {
                ElementHandle tooltipContent = childElement.querySelector("div.c24-tooltip-content");
                if (tooltipContent != null && !tooltipContent.innerText().trim().isEmpty()) {
                    String text = tooltipContent.innerText().trim();

                    // 1. 按第一个冒号分割 key 和 value
                    int colonIndex = text.indexOf(':');
                    if (colonIndex != -1) {
                        String key = text.substring(0, colonIndex).trim();
                        String value = text.substring(colonIndex + 1).trim();

                        // 2. 清理 value 部分
                        value = value
                                .replaceAll("\\s+", " ")          // 合并多个空格
                                .replaceAll("\\s*\\n\\s*", ": "); // 换行替换为冒号+空格

                        tooltipContentMap.put(key, value);
                    }
                }
            }

            // 提取资费特征
            List<ElementHandle> tariffFeatures = article.querySelectorAll("div.result-row__tariffFeatures div.tariff-feature");
            Map<String, String> c24TooltipTriggerMap = new LinkedHashMap<>();
            for (ElementHandle triggerElement : tariffFeatures) {
                ElementHandle firstElement = triggerElement.querySelector("span.tariff-feature__inner--first");
                ElementHandle secondElement = triggerElement.querySelector("span.tariff-feature__inner--second");
                String secondText = secondElement != null ? secondElement.innerText().trim() : "N/A";
                if(firstElement != null){
                    String firstElementText = firstElement.innerText().trim();
                    if(!firstElementText.isEmpty()){
                        c24TooltipTriggerMap.put(firstElementText, secondText);;
                    }
                }
            }

            // 提取价格
            ElementHandle priceElement = article.querySelector("div.result-row__prices div.tariff-price__price");
            String price = priceElement != null ? priceElement.innerText().trim() : "N/A";

            data.put("品牌名", brandName);
            data.put("等级", grade);
            data.put("评分", rating);
            data.put("Tariff", tariffBrandName);
            data.put("价格",price);
            data.put("限时优惠",limitedTimeCatcherText);
            data.putAll(priceCatcherMap);
            data.putAll(c24TooltipTriggerMap);
            data.putAll(tooltipContentMap);
        } catch (Exception e) {
            System.err.println("提取文章信息时出错: " + e.getMessage());
        }
        return data;
    }
}




