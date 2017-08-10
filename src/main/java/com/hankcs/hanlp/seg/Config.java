/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/10/30 10:06</create-date>
 *
 * <copyright file="Config.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.seg;

/**
 * 分词器配置项
 */
public class Config
{
    /**
     * 是否是索引分词（合理地最小分割）
     */
    public boolean indexMode = false;
    /**
     * 是否识别中国人名
     */
    public boolean nameRecognize = true;
    /**
     * 是否识别音译人名
     */
    public boolean translatedNameRecognize = true;
    /**
     * 是否识别日本人名
     */
    public boolean japaneseNameRecognize = false;
    /**
     * 是否识别地名
     */
    public boolean placeRecognize = false;
    /**
     * 是否识别机构
     */
    public boolean organizationRecognize = false;
    /**
     * 是否加载用户词典
     */
    public boolean useCustomDictionary = true;
    /**
     * 词性标注
     */
    public boolean speechTagging = false;
    /**
     * 命名实体识别是否至少有一项被激活
     */
    public boolean ner = true;
    /**
     * 是否计算偏移量
     */
    public boolean offset = false;
    /**
     * 是否识别数字和量词
     */
    public boolean numberQuantifierRecognize = false;
    /**
     * 并行分词的线程数
     */
    public int threadNumber = 1;


    /**
     * 更新命名实体识别总开关
     */
    public void updateNerConfig()
    {
        ner = nameRecognize || translatedNameRecognize || japaneseNameRecognize || placeRecognize || organizationRecognize;
    }

    public void displayConfig(){
        System.out.println("Config.displayConfig打印参数内容：");
        System.out.println("indexMode索引分词 = " + indexMode);
        System.out.println("nameRecognize人名识别 = " + nameRecognize);
        System.out.println("translatedNameRecognize译名识别 = " + translatedNameRecognize);
        System.out.println("japaneseNameRecognize日文名识别 = " + japaneseNameRecognize);
        System.out.println("placeRecognize地点识别 = " + placeRecognize);
        System.out.println("organizationRecognize机构识别 = " + organizationRecognize);
        System.out.println("useCustomDictionary使用自定义词典 = " + useCustomDictionary);
        System.out.println("speechTagging词性标注 = " + speechTagging);
        System.out.println("ner开启命名实体识别了 = " + ner);
        System.out.println("offset计算偏移量 = " + offset);
        System.out.println("numberQuantifierRecognize识别数量词 = " + numberQuantifierRecognize);
        System.out.println("threadNumber使用的线程数 = " + threadNumber);
    }
}
