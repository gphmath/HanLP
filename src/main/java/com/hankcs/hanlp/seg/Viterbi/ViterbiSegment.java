/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2015/1/19 20:51</create-date>
 *
 * <copyright file="ViterbiSegment.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.seg.Viterbi;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.recognition.nr.JapanesePersonRecognition;
import com.hankcs.hanlp.recognition.nr.PersonRecognition;
import com.hankcs.hanlp.recognition.nr.TranslatedPersonRecognition;
import com.hankcs.hanlp.recognition.ns.PlaceRecognition;
import com.hankcs.hanlp.recognition.nt.OrganizationRecognition;
import com.hankcs.hanlp.seg.WordBasedGenerativeModelSegment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.seg.common.Vertex;
import com.hankcs.hanlp.seg.common.WordNet;

import java.util.LinkedList;
import java.util.List;

/**
 * Viterbi分词器<br>
 * 也是最短路分词，最短路求解采用Viterbi算法
 *
 * @author hankcs
 */
public class ViterbiSegment extends WordBasedGenerativeModelSegment
{
    /**
     *
     * @param sentence 待分词句子:字符数组，一个元素是一个字
     * @return
     */
    @Override
    protected List<Term> segSentence(char[] sentence)
    {
//        config.displayConfig();
//        对，调用到这里的segSentence函数
//        System.out.println("ViterbiSegment.segSentence");
//        long start = System.currentTimeMillis();
        WordNet wordNetAll = new WordNet(sentence);
        ////////////////生成词网////////////////////
//    * 初始化相应长度的空wordNet：
//    * 【始##始】
//    * 【 】
//    * 【 】
//    * 【 】
//    * 【 】
//    * 【 】
//    * 【 】
//    * 【末##末】
        GenerateWordNet(wordNetAll);
        ///////////////生成词图////////////////////
//    * 完整wordNet：其中的老、老百姓都是一个Vertex
//    * 【始##始】
//    * 【老，老百姓】
//    * 【百，百姓】
//    * 【姓】
//    * 【大】
//    * 【药，药房】
//    * 【房】
//    * 【末##末】
//        System.out.println("构图：" + (System.currentTimeMillis() - start));
        if (HanLP.Config.DEBUG)
        {
//            System.out.println("DEBUG=True");
            System.out.printf("粗分词网：\n%s\n", wordNetAll);
        }
//        start = System.currentTimeMillis();
        List<Vertex> vertexList = viterbi(wordNetAll);
//        System.out.println("最短路：" + (System.currentTimeMillis() - start));
//        System.out.println("直接用Viterbi算法的粗分结果" + convert(vertexList, false));
        System.out.println("用了Viterbi算法的粗分结果" + convert(vertexList, false));
        config.useCustomDictionary=false;
        if (config.useCustomDictionary)
        {
//            System.out.println("useCustomDictionary=True");
            if (config.indexMode){
                System.out.println("indexMode=T");
                combineByCustomDictionary(vertexList, wordNetAll);

            }
            else combineByCustomDictionary(vertexList);
        }

        if (HanLP.Config.DEBUG)
        {
            System.out.println("用了Viterbi算法结合自定义词典后的粗分结果" + convert(vertexList, false));
        }

        // 数字识别
        if (config.numberQuantifierRecognize)
        {
            System.out.println("数字识别numberQuantifierRecognize=T");
            mergeNumberQuantifier(vertexList, wordNetAll, config);
        }

        // 实体命名识别
        if (config.ner)
        {
//            这个词网是使用了NER的最优词网，vertexList是前面生成的。
            WordNet wordNetOptimum = new WordNet(sentence, vertexList);
//            System.out.println("用了vertexList生成的初始最优词网" + convert(vertexList, false));
            if (HanLP.Config.DEBUG)
            {
                System.out.printf("用了vertexList生成的初始最优词网：\n%s\n", wordNetOptimum);
            }
            int preSize = wordNetOptimum.size();
//            这个size是不计算空链表的，比如下面size=5，其中始和末也算2个
//            * 【始##始】
//            * 【老百姓】
//            * 【】
//            * 【】
//            * 【大】
//            * 【药房】
//            * 【】
//            * 【末##末】
            System.out.println("preSize = " + preSize);
            if (config.nameRecognize)
            {
                PersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (config.translatedNameRecognize)
            {
                TranslatedPersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (config.japaneseNameRecognize)
            {
                JapanesePersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (config.placeRecognize)
            {
                PlaceRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (config.organizationRecognize)
            {
                // 层叠隐马模型——生成输出作为下一级隐马输入
                vertexList = viterbi(wordNetOptimum);
                wordNetOptimum.clear();
                wordNetOptimum.addAll(vertexList);
                preSize = wordNetOptimum.size();
                OrganizationRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (wordNetOptimum.size() != preSize)
            {
//                if (HanLP.Config.DEBUG)
//                {
//                    System.out.printf("第二次Viterbi之前的细分词网：\n%s\n", wordNetOptimum);
//                }
//                说明加了命名实体识别后分词的词数和原来的最佳路径链表不同了，词数肯定变少了，因为有的词合并成命名实体了，
//                  不会增多，因为不会拆分原来的词语。
                vertexList = viterbi(wordNetOptimum);
//                不改变词网,只是更新了vertexList
//                ！！！重新调用了Viterbi算法。。。。。。。。能重新用一次jieba的最大概率Viterbi吗？
//                  。
                if (HanLP.Config.DEBUG)
                {
                    System.out.printf("最后命名实体识别后的细分词网：\n%s\n", wordNetOptimum);
                }
            }
        }

        // 如果是索引模式则全切分
        if (config.indexMode)
        {
            return decorateResultForIndexMode(vertexList, wordNetAll);
        }

        // 是否标注词性
        if (config.speechTagging)
        {
            speechTagging(vertexList);
        }

        return convert(vertexList, config.offset);
//        没什么新意,就是打印出来。
    }

    /**
     * 广义的维特比算法，原来不只是HMM里的那个Viterbi。
     * 维特比算法是一个特殊但应用最广的动态规划算法，利用动态规划，可以解决任何一个图中的最短路径问题。
     * 而维特比算法是针对一个特殊的图——篱笆网络的有向图（Lattice)的最短路径问题而提出的。
     * 它之所以重要，是因为凡是使用隐含马尔可夫模型描述的问题都可以用它来解码，包括今天的数字通信、语音识别、机器翻译、拼音转汉字、分词等。
     * 给定维特比算法，只要规定好2点：
     * 1. 怎么根据前面的节点分数，计算下一节点的分数
     * 2. 以什么分数标准选择最佳路径、局部最佳路径
     * HMM中，1. 前面节点s0的分数*trans(s0->s1)*emit(s1->o1)
     * @param wordNet：词网：[[Vertex]]
     * @return vertexList：节点列表：[Vertex]
     * 输入wordNet：
    * 【始##始】
    * 【老，老百姓】
    * 【百，百姓】
    * 【姓】
    * 【大】
    * 【药，药房】
    * 【房】
    * 【末##末】
     *
     * 输出vertexList：
     * 【老百姓/n，大/a，药房/n】
     */
    private static List<Vertex> viterbi(WordNet wordNet)
    {
        /**
         * 看看这个Viterbi是干嘛的
         * Viterbi并不改变词网本身
         */
        // 避免生成对象，优化速度
        LinkedList<Vertex>[] wordNetVertexes = wordNet.getVertexes(); // 这就是词网
        LinkedList<Vertex> vertexList = new LinkedList<Vertex>();

        for (Vertex vertex : wordNetVertexes[1])
        {
//          搜索：hankcs 维特比算法在分词中的应用
//          wordNetVertexes[0].getFirst() 就是Vertex(始##始)
            vertex.updateFrom(wordNetVertexes[0].getFirst());
        }
        for (int i = 1; i < wordNetVertexes.length - 1; ++i)
        {
            LinkedList<Vertex> nodeArray = wordNetVertexes[i];
            if (nodeArray == null) continue;
            for (Vertex node : nodeArray)
            {
                if (node.from == null) continue;
                for (Vertex to : wordNetVertexes[i + node.realWord.length()])
                {
                    to.updateFrom(node);
                }
            }
        }
        Vertex from = wordNetVertexes[wordNetVertexes.length - 1].getFirst();
        while (from != null)
        {
            vertexList.addFirst(from);
            from = from.from;
        }
        return vertexList;
    }

    /**
     * 第二次维特比，可以利用前一次的结果，降低复杂度
     *
     * @param wordNet
     * @return
     */
//    private static List<Vertex> viterbiOptimal(WordNet wordNet)
//    {
//        LinkedList<Vertex> nodes[] = wordNet.getVertexes();
//        LinkedList<Vertex> vertexList = new LinkedList<Vertex>();
//        for (Vertex node : nodes[1])
//        {
//            if (node.isNew)
//                node.updateFrom(nodes[0].getFirst());
//        }
//        for (int i = 1; i < nodes.length - 1; ++i)
//        {
//            LinkedList<Vertex> nodeArray = nodes[i];
//            if (nodeArray == null) continue;
//            for (Vertex node : nodeArray)
//            {
//                if (node.from == null) continue;
//                if (node.isNew)
//                {
//                    for (Vertex to : nodes[i + node.realWord.length()])
//                    {
//                        to.updateFrom(node);
//                    }
//                }
//            }
//        }
//        Vertex from = nodes[nodes.length - 1].getFirst();
//        while (from != null)
//        {
//            vertexList.addFirst(from);
//            from = from.from;
//        }
//        return vertexList;
//    }
}
