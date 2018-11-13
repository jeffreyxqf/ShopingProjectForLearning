package com.leyou.goods.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

@Service
public class GoodsHtmlService {

    @Autowired
    private TemplateEngine engine;

    @Autowired
    private GoodsService goodsService;

    public void createHtml(Long spuId){

        // 初始化上下文
        Context context = new Context();
        // 获取数据模型
        Map<String, Object> map = this.goodsService.loadGoods(spuId);
        // 设置数据模型
        context.setVariables(map);

        PrintWriter printWriter = null;
        try {
            File file = new File("C:\\yun7\\tools\\nginx-1.14.0\\html\\item\\" + spuId + ".html");
            printWriter = new PrintWriter(file);

            this.engine.process("item", context, printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printWriter != null){
                printWriter.close();
            }
        }

    }

    public void deleteHtml(Long spuId) {
        File file = new File("C:\\yun7\\tools\\nginx-1.14.0\\html\\item\\" + spuId + ".html");
        file.deleteOnExit();
    }
}
