/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/05/2014/5/26 13:52</create-date>
 *
 * <copyright file="UnknowWord.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.recognition.nr;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.algorithm.Viterbi;
import com.hankcs.hanlp.corpus.dictionary.item.EnumItem;
import com.hankcs.hanlp.corpus.tag.NR;
import com.hankcs.hanlp.dictionary.nr.PersonDictionary;
import com.hankcs.hanlp.seg.common.Vertex;
import com.hankcs.hanlp.seg.common.WordNet;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 人名识别
 * @author hankcs
 */
public class PersonRecognition
{
    /**
     * 1. 角色观察，根据规则把词性标注转化为角色标注序列——这时候每个词可能有多个概率不同的角色（隐藏状态变量）
     * 2. Viterbi算法（简化版），预测每个词最可能的角色——返回最佳的角色标注序列
     * 3. 根据角色标注序列，进行模式匹配（使用AC自动机+BieTrie），正式识别人名
     * @param pWordSegResult：最佳路径词语列表：vertexList
     * @param wordNetOptimum：最优词网，由上面的最佳路径生成的精简版词网
     * @param wordNetAll：原来的完整的词网
     * @return: 测试
     */
    public static boolean Recognition(List<Vertex> pWordSegResult, WordNet wordNetOptimum, WordNet wordNetAll)
    {

//        1. 角色观察，根据规则把词性标注转化为角色标注序列——这时候每个词可能有多个概率不同的角色（隐藏状态变量）
        List<EnumItem<NR>> roleTagList = roleObserve(pWordSegResult);
//        这就是角色观察了，把词性标注信息转换为人名角色标注。注这个输入变量不是训练用的标注样本，没有复合词
//        人名角色观察 roleTagList = [  K 1 A 1 ][老百姓 A 20833310 ][大 C 1262 L 218 K 127 D 52 E 8 M 2 ][药房 A 20833310 ][  A 20833310 ]
//        人名角色观察：[  K 1 A 1 ][杨 B 10602 E 268 C 91 D 55 K 5 ][国 C 2368 D 1390 B 51 E 33 L 4 ][福 C 999 D 552 E 34 B 23 K 18 L 2 ][大 C 1262 L 218 K 127 D 52 E 8 M 2 ][药房 A 20833310 ][  A 20833310 ]
        if (HanLP.Config.DEBUG)
        {
            StringBuilder sbLog = new StringBuilder();
            Iterator<Vertex> iterator = pWordSegResult.iterator();
            for (EnumItem<NR> nrEnumItem : roleTagList)
            {
                sbLog.append('[');
                sbLog.append(iterator.next().realWord);
                sbLog.append(' ');
                sbLog.append(nrEnumItem);
                sbLog.append(']');
            }
            System.out.printf("人名角色观察：%s\n", sbLog.toString());
        }

//        2. Viterbi算法（简化版），预测每个词最可能的角色——返回最佳的角色标注序列
        List<NR> nrList = viterbiComputeSimply(roleTagList);
//        角色观察列表，自带发射矩阵+观测变量。。
//        这是人名角色标注中的HMM的Viterbi算法（的简化形式，也可以改用标准形式）。
//        隐藏状态变量是词语对应的可能角色；观测变量是词语；
//        人名角色标注 nrList = [ /K ,老百姓/A ,大/K ,药房/A , /A]
//        人名角色标注：[ /K ,杨/B ,国/C ,福/D ,大/L ,药房/A , /A]
        if (HanLP.Config.DEBUG)
        {
            StringBuilder sbLog = new StringBuilder();
            Iterator<Vertex> iterator = pWordSegResult.iterator();
            sbLog.append('[');
            for (NR nr : nrList)
            {
                sbLog.append(iterator.next().realWord);
                sbLog.append('/');
                sbLog.append(nr);
                sbLog.append(" ,");
            }
            if (sbLog.length() > 1) sbLog.delete(sbLog.length() - 2, sbLog.length());
            sbLog.append(']');
            System.out.printf("人名角色标注：%s\n", sbLog.toString());
        }

//        3. 根据角色标注序列，进行模式匹配（使用AC自动机+BieTrie），正式识别人名
        PersonDictionary.parsePattern(nrList, pWordSegResult, wordNetOptimum, wordNetAll);
        return true;
    }

    /**
     * 角色观察(从模型中加载所有词语对应的所有角色,允许进行一些规则补充)——PS这里的规则相当主观啊，或者说是大量经验支持的？
     * @param wordSegResult 粗分结果
     * @return tagList：角色的列表，最佳路径词语列表中的每个词（因此不是单字，是词语）一组角色（即一个词语可以保存多个可能角色）
     */
    public static List<EnumItem<NR>> roleObserve(List<Vertex> wordSegResult)
    {
//        对于最佳路径词列表中的一个Vertex，这里用到了三个属性（重要！！↓↓↓）：
//        Vertex.realWord:词语的真实文本，jieba有
//        Vertex.gussNature:词语的最可能词性attribute.nature[0]（频数最大的是第一个词性,已检查确认），jieba好像只能从字典中取？只能是单个词性的，这没问题。
//        Vertex.getAttribute().totalFrequency:一个词语的所有可能词性的频数总和。比如希望，动词7685，名词616，总频数8301。——如果用到jieba就不用求和了。
//        用到人名角色的转移矩阵（用到A的总频数）
//        用不到的：Attribute.frequency:一个词不同词性的频数列表，Attribute.Nature:一个词不同词性的列表

        List<EnumItem<NR>> tagList = new LinkedList<EnumItem<NR>>();
        Iterator<Vertex> iterator = wordSegResult.iterator();
        iterator.next();
        tagList.add(new EnumItem<NR>(NR.A, NR.K)); //  始##始 A K，A其他角色，K姓名的上文
//        创建Map<E, Integer> labelMap，增加两个词性，默认频数为1，即：{A:1,K:1}
        while (iterator.hasNext())
        {
            Vertex vertex = iterator.next();
//            for (int i=0;i<vertex.getAttribute().frequency.length;i++){
//                System.out.println("vertex.getAttribute().frequency[ "+i+"] = "+ + vertex.getAttribute().frequency[i]);
//            }
//            System.out.println("vertex.getAttribute().totalFrequency = " + vertex.getAttribute().totalFrequency);
            EnumItem<NR> nrEnumItem = PersonDictionary.dictionary.get(vertex.realWord);
//            System.out.println(vertex.realWord+".nrEnumItem = " + nrEnumItem);
//            杨.nrEnumItem = B 10602 E 268 C 91 D 55 K 5
//            国.nrEnumItem = C 2368 D 1390 B 51 E 33 L 4
//            福.nrEnumItem = C 999 D 552 E 34 B 23 K 18 L 2
//            大.nrEnumItem = C 1262 L 218 K 127 D 52 E 8 M 2
//            药房.nrEnumItem = null
//            或者
//            老百姓.nrEnumItem = null
//            大.nrEnumItem = C 1262 L 218 K 127 D 52 E 8 M 2
//            药房.nrEnumItem = null
//            目测单字有角色，词组经常没有角色字典
//            如果上面取到了角色，就添加到该词的角色里就行了，否则下面继续处理：
            if (nrEnumItem == null)
            {
                switch (vertex.guessNature())
                {
//                    读取词性，nr人名、nnt职务职称、或其他，三种
                    case nr:
                    {
//                        词性是人名的
                        // 有些双名实际上可以构成更长的三名
                        if (vertex.getAttribute().totalFrequency <= 1000 && vertex.realWord.length() == 2)
                        {
//                            词性是人名的，但该词的总频数<=1000,不常用的双字词，词性设为X（姓与双名首字成词）和G（人名后缀）
                            nrEnumItem = new EnumItem<NR>(NR.X, NR.G);
                        }
//                        词性是人名，但不符上面的条件，就设为A，其他非人名相关角色，频数设为：人名角色转移矩阵中A出现的总频数
                        else nrEnumItem = new EnumItem<NR>(NR.A, PersonDictionary.transformMatrixDictionary.getTotalFrequency(NR.A));
                    }break;
                    case nnt:
                    {
//                        词性为职务职称
                        // 姓+职位
//                        设置角色为G人名后缀、和 K人名上文：
//                        万科董事长王石，董事长是K
//                        王校长，校长是G
                        nrEnumItem = new EnumItem<NR>(NR.G, NR.K);
                    }break;
                    default:
                    {
//                        如果不是人名和职务职称，就设置角色为A其他与人名无关的角色，频数设为：人名角色转移矩阵中A出现的总频数
                        nrEnumItem = new EnumItem<NR>(NR.A, PersonDictionary.transformMatrixDictionary.getTotalFrequency(NR.A));
                    }break;
                }
            }
//            把一个词语的角色添加到角色列表里去
            tagList.add(nrEnumItem);
        }
        return tagList;
    }

    /**
     * 维特比算法求解最优标签
     * @param roleTagList
     * @return
     */
    public static List<NR> viterbiCompute(List<EnumItem<NR>> roleTagList)
    {
        return Viterbi.computeEnum(roleTagList, PersonDictionary.transformMatrixDictionary);
    }

    /**
     * 简化的"维特比算法"求解最优标签
     * @param roleTagList
     * @return
     */
    public static List<NR> viterbiComputeSimply(List<EnumItem<NR>> roleTagList)
    {
//        roleTagList里包含了每个角色的频数，等于自带发射矩阵，只需要另外传转移矩阵即可
        return Viterbi.computeEnumSimply(roleTagList, PersonDictionary.transformMatrixDictionary);
    }
}
