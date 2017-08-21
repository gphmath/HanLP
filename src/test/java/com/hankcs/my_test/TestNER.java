/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/12/7 19:25</create-date>
 *
 * <copyright file="DemoChineseNameRecoginiton.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014+ 上海林原信息科技有限公司. All Right Reserved+ http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.my_test;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

import java.util.List;


/**
 * 命名实体识别测试
 * @author hankcs
 */
public class TestNER
{
    public static void main(String[] args)
    {
        String[] testCase = new String[]{
//                "签约仪式前，秦光荣、李纪恒、仇和等一同会见了参加签约的企业家。",
//                "区长庄木弟新年致辞",
//                "朱立伦：两岸都希望共创双赢 习朱历史会晤在即",
//                "陕西首富吴一坚被带走 与令计划妻子有交集",
//                "据美国之音电台网站4月28日报道，8岁的凯瑟琳·克罗尔（凤甫娟）和很多华裔美国小朋友一样，小小年纪就开始学小提琴了。她的妈妈是位虎妈么？",
//                "凯瑟琳和露西（庐瑞媛），跟她们的哥哥们有一些不同。",
//                "王国强、高峰、汪洋、张朝阳光着头、韩寒、小四",
//                "张浩和胡健康复员回家了",
//                "王总和小丽结婚了",
//                "编剧邵钧林和稽道青说",
//                "这里有关天培的有关事迹",
//                "龚学平等领导说,邓颖超生前杜绝超生",
//                "金三胖是中国一拖集团有限责任公司的董事长",
//            "据记者尹同飞报道，是融创董事长李石与王石关于宝能系和华润集团准备正式签约的时间，但戏剧性一幕出现了，",
//            "过去一周出现了彩虹",
            "秦光荣、李纪恒、仇和等一同会见了参加签约的企业家",
//            "他去了北一环路" ,
//            "万科董事长王石与经理郁亮表示，公司目前和碧桂园还有恒大一起合作建造深圳地铁，目前共有10公里，整个项目包括了两个海底隧道",

        };
        Segment segment = HanLP.newSegment().enableNameRecognize(true);
        //        创建初始化的只有Config类对象（所有Segment类都有的成员），里面存放了一些设置：是否开启人名识别地名识别等，线程数等


        HanLP.Config.enableDebug(true);
        for (String sentence : testCase)
        {
            List<Term> termList = segment.seg(sentence);
//            Term类里有词语本身，词性还有在句子中的位置，输出时重载了函数toString，所以直接输出为：词语/nr
//            返回一个list：[区长/n, 庄木弟/nr, 新年/t, 致辞/v]
            System.out.println(termList);
        }

//        System.out.println("\n\n开启命名实体识别\n\n");
//        Segment segment2 = HanLP.newSegment().enableAllNamedEntityRecognize(true);
//        //        创建初始化的只有Config类对象（所有Segment类都有的成员），里面存放了一些设置：是否开启人名识别地名识别等，线程数等
//
//
////        HanLP.Config.enableDebug(true);
//        for (String sentence : testCase)
//        {
//            List<Term> termList = segment2.seg(sentence);
////            Term类里有词语本身，词性还有在句子中的位置，输出时重载了函数toString，所以直接输出为：词语/nr
////            返回一个list：[区长/n, 庄木弟/nr, 新年/t, 致辞/v]
//            System.out.println(termList);
//        }
    }
}
