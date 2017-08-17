/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/9/10 14:47</create-date>
 *
 * <copyright file="PersonDictionary.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.dictionary.nr;


import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.collection.AhoCorasick.AhoCorasickDoubleArrayTrie;
import com.hankcs.hanlp.corpus.dictionary.item.EnumItem;
import com.hankcs.hanlp.corpus.tag.NR;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;
import com.hankcs.hanlp.dictionary.TransformMatrixDictionary;
import com.hankcs.hanlp.seg.common.Vertex;
import com.hankcs.hanlp.seg.common.WordNet;
import com.hankcs.hanlp.utility.Predefine;

import java.util.*;

import static com.hankcs.hanlp.corpus.tag.NR.*;
import static com.hankcs.hanlp.utility.Predefine.logger;
import static com.hankcs.hanlp.dictionary.nr.NRConstant.*;

//import com.google.common.collect.Iterables;

/**
 * 人名识别用的词典，实际上是对两个词典的包装
 *
 * @author hankcs
 */
public class PersonDictionary
{
    /**
     * 人名词典
     */
    public static NRDictionary dictionary;
    /**
     * 转移矩阵词典
     */
    public static TransformMatrixDictionary<NR> transformMatrixDictionary;
    /**
     * AC算法用到的Trie树
     */
    public static AhoCorasickDoubleArrayTrie<NRPattern> trie;

    public static final CoreDictionary.Attribute ATTRIBUTE = new CoreDictionary.Attribute(Nature.nr, 100);

    static
    {
        long start = System.currentTimeMillis();
        dictionary = new NRDictionary();
        if (!dictionary.load(HanLP.Config.PersonDictionaryPath))
        {
            logger.severe("人名词典加载失败：" + HanLP.Config.PersonDictionaryPath);
            System.exit(-1);
        }
        transformMatrixDictionary = new TransformMatrixDictionary<NR>(NR.class);
        transformMatrixDictionary.load(HanLP.Config.PersonDictionaryTrPath);
        trie = new AhoCorasickDoubleArrayTrie<NRPattern>();
        TreeMap<String, NRPattern> map = new TreeMap<String, NRPattern>();
        for (NRPattern pattern : NRPattern.values())
        {
            map.put(pattern.toString(), pattern);
        }
        trie.build(map);
        logger.info(HanLP.Config.PersonDictionaryPath + "加载成功，耗时" + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * 根据角色标注序列，进行模式匹配，正式识别人名——不过好多主观的规则
     * 例如
     * 人名角色标注：[ /K ,杨/B ,国/C ,福/D ,大/L ,药房/A , /A]
     * 识别出人名：杨国 BC
     * 识别出人名：杨国福 BCD
     * @param nrList         确定的标注序列(带UV)：[ /K ,杨/B ,国/C ,福/D ,大/L ,药房/A , /A]    [ /K ,老百姓/A ,大/K ,药房/A , /A]   [ /K ,邓/B ,颖/C ,超/D ,生前/L , /A]
     * @param vertexList     原始的未加角色标注的序列：[杨/ng, 国/n, 福/n, 大/a, 药房/n]   [老百姓/n, 大/a, 药房/n]
     * @param wordNetOptimum 待优化的图
     * @param wordNetAll     全词图
     * wordNetOptimum：
    * 【始##始】
    * 【老百姓】
    * 【】
    * 【】
    * 【大】
    * 【药房】
    * 【】
    * 【末##末】
     * wordNetAll：
    * 完整wordNet：其中的老、老百姓都是一个Vertex
    * 【始##始】
    * 【老，老百姓】
    * 【百，百姓】
    * 【姓】
    * 【大】
    * 【药，药房】
    * 【房】
    * 【末##末】
     */
    public static void parsePattern(List<NR> nrList, List<Vertex> vertexList, final WordNet wordNetOptimum, final WordNet wordNetAll)
    {
//        先拆分U、V
//        再识别BCD和BE等
        // 拆分UV
        ListIterator<Vertex> listIterator = vertexList.listIterator();
        StringBuilder sbPattern = new StringBuilder(nrList.size());
        System.out.println("nrList.size() = " + nrList.size());
//        新建长度为nrList.size()的char数组？？数字对不上吧？——嗯，初始时候，是这样的，长度就是原来的角色标注序列的长度
//        后来由于U和V的拆分等情况，长度可能会增加。
        NR preNR = NR.A;
        boolean backUp = false; //什么意思？
        int index = 0;
        for (NR nr : nrList)
        {
            ++index;
//            index=1,2,...,7
//            nr=K,B,C,D,L,A,A
//            vertex=始，杨，国，福，大，药房，末
            System.out.println("index = " + index);
            System.out.println("nr = " + nr);
            Vertex current = listIterator.next();
            System.out.println("current.getRealWord() = " + current.getRealWord());
            //            logger.trace("{}/{}", current.realWord, nr);
            switch (nr)
            {
//                现在关心U（上文与姓成词）、V（下文与双名末字成词）、或其他
                case U:
                    if (!backUp)
                    {
//                        就是转一下格式，vertexList和相应的迭代器都转一下格式，设定一下迭代器指针的位置
                        System.out.println("backUp=false");
                        System.out.println("vertexList.toString() = " + vertexList.toString());
                        System.out.println("vertexList.class"+vertexList.getClass());
                        vertexList = new ArrayList<Vertex>(vertexList);
                        System.out.println("vertexList.toString() = " + vertexList.toString());
                        System.out.println("vertexList.class"+vertexList.getClass());
//                        把LinkedList<Vertex>转换为ArrayList?——没错，打印出来形式还是完全一样的。
                        listIterator = vertexList.listIterator(index);
//                        获取上面这个ArrayList的迭代器，并且将下一个要return的元素下标设置为index，最开始index=1，即下面调用next时会返回下标为1的元素
//                        int size =  Iterables.size(listIterator);
//                        System.out.println("listIterator.toString()"+listIterator.next());
                        backUp = true;
                    }
//                    System.out.println("NR.K.toString()="+NR.K.toString());
//                    System.out.println("sbPattern.toString() = " + sbPattern.toString());
//                    原来是sb=KL——再往后出现一个U，把U分解为U=KB：上文+姓
                    sbPattern.append(NR.K.toString());
//                    现在sb=KLK
//                    System.out.println("sbPattern.toString() = " + sbPattern.toString());

                    //这个string就是“K”......
                    sbPattern.append(NR.B.toString());
//                    现在sb=KLKB
//                    System.out.println("sbPattern.toString() = " + sbPattern.toString());
                    preNR = B;
                    System.out.println("-1listIterator.nextIndex() = " + listIterator.nextIndex());
//                      [ /K ,这里/L ,有关/U ,天/C ,培/D ,的/L ,壮烈/A , /A]
//                    index=cursor=3,回退，设list[2]=有K（原来是：有关U），再前进，设list[3]=关B（原来没有值，直接add）
//                      ——这样实际上就比原来的标注序列多一个元素了：原来应该是：始K，这里L，有关U——>始K，这里L，有K，关B
                    listIterator.previous();
                    System.out.println("listIterator.nextIndex() = " + listIterator.nextIndex());
                    System.out.println("2listIterator.nextIndex() = " + listIterator.nextIndex());

//                    迭代器指针回退一步
//                    把当前Vertex的词语分成两部分：最后一个字作为B，前面其他部分作为K
                    String nowK = current.realWord.substring(0, current.realWord.length() - 1);
                    String nowB = current.realWord.substring(current.realWord.length() - 1);
                    System.out.println("nowB = " + nowB);
                    listIterator.set(new Vertex(nowK));
                    listIterator.next();
//                    迭代器指针前进一步
                    listIterator.add(new Vertex(nowB));
                    continue;
                case V:
//                    不仅包括双名末字与下文成词，也包括单名与下文成词
                    if (!backUp)
                    {
                        vertexList = new ArrayList<Vertex>(vertexList);
                        System.out.println("vertexList.listIterator().nextIndex() = " + vertexList.listIterator().nextIndex());
                        listIterator = vertexList.listIterator(index);
                        backUp = true;
                    }
                    if (preNR == B)
                    {
//                        前一个角色是B，当前角色是V，是单名与下文成词的情况。就拆成：EL，连起来是BEL
                        sbPattern.append(NR.E.toString());  //BE
                    }
                    else
                    {
//                        前一个角色不是B，当前角色是V，是双名末字与下文成词的情况，就拆成：DL，连起来是BCDL
                        sbPattern.append(NR.D.toString());  //CD
                    }
//                    把L加上
                    sbPattern.append(NR.L.toString());
                    // 对串也做一些修改
                    listIterator.previous();
//                    下面这两个是不是弄反了。。。龚学平等领导。nowED=等，nowL=平——已改正
                    String nowL = current.realWord.substring(current.realWord.length() - 1);
                    String nowED = current.realWord.substring(0, current.realWord.length() - 1);
                    System.out.println("nowED = " + nowED);
                    System.out.println("nowL = " + nowL);
                    System.out.println("set前listIterator.nextIndex() = " + listIterator.nextIndex());

                    listIterator.set(new Vertex(nowL));
                    System.out.println("set后add前listIterator.nextIndex() = " + listIterator.nextIndex());

                    listIterator.add(new Vertex(nowED));//这个由于插入的操作是在nowL前面的，
                    System.out.println("ddddd"+listIterator.next().getRealWord());
                    listIterator.previous();
//                    add会在当前指针位置处添加元素“平”，当前元素“等”会往后移，然后再把指针前进一步，指到原来被后移的元素上去(等)。
                    System.out.println("add后listIterator.nextIndex() = " + listIterator.nextIndex());

//                    add后指针cursor会前进一步
                    listIterator.next();
                    continue;
                default:
//                    如果不是U或V，就原样添加上去
                    sbPattern.append(nr.toString());
                    break;
            }
            preNR = nr;
        }
        while (listIterator.hasPrevious()){
            System.out.println("listIterator.previous() = " + listIterator.previous());
        }
//        到目前为止，已经完成拆分U和V，全部转为BCD等角色。pattern=KBCDLKA：龚学平等领导——的角色标注序列
        String pattern = sbPattern.toString();
        System.out.println("pattern = " + pattern);
//        logger.trace("模式串：{}", pattern);
//        logger.trace("对应串：{}", vertexList);
//        if (pattern.length() != vertexList.size())
//        {
//            logger.warn("人名识别模式串有bug", pattern, vertexList);
//            return;
//        }
        final Vertex[] wordArray = vertexList.toArray(new Vertex[0]);
//        把vertexList复制到括号里的变量，新类型就是括号里变量指定类型。返回赋值后的括号里的变量
//        其实就是相当于把wordArray=vertexList，只不过直接这样写大概会变成同一个对象？
        System.out.println("wordArray.length = " + wordArray.length);
        System.out.println("wordArray[0] = " + wordArray[0]+"wordArray[1] = " + wordArray[1]+"wordArray[2] = " + wordArray[2]);
        final int[] offsetArray = new int[wordArray.length];
//        记录了每个字组词的位置，比如老百姓大药房：[0,3,4,6]，邓颖超生前：[0,1,2,3,4,6]
        offsetArray[0] = 0;
        for (int i = 1; i < wordArray.length; ++i)
        {
            offsetArray[i] = offsetArray[i - 1] + wordArray[i - 1].realWord.length();
//            下一个坐标要增加一个词的长度：
//            始/老百姓/大/药房/末：[0,1,4,5,7],最后的8不要
//            始/龚/学/平/等/领导/末：[0,1,2,3,4,5,7],最后的8不要
        }
        for (int i=0;i<offsetArray.length;i++){
            System.out.println("i = " + i);
            System.out.println("offsetArray[i] = " + offsetArray[i]);
        }
//        pattern="KFBEGA";
//        根据拆分UV好的角色标注序列去匹配：
        trie.parseText(pattern, new AhoCorasickDoubleArrayTrie.IHit<NRPattern>()
        {
            @Override
            public void hit(int begin, int end, NRPattern value)
            {
//                标注序列KBCDLA在AC自动机中已经匹配到BC和BCD两个
                System.out.println("PersonDictionary.hit");
                System.out.println("begin = [" + begin + "], end = [" + end + "], value = [" + value + "]");
//                vertexList = [始，邓，颖，超，生前，末]
//                begin=1，end=3，NRPattern = BC,邓颖
//                begin=1, end=4, NRPattern = BCD，邓颖超
//            logger.trace("匹配到：{}", keyword);
                StringBuilder sbName = new StringBuilder();
                for (int i = begin; i < end; ++i)
                {
                    sbName.append(wordArray[i].realWord);
//                    wordArray就是当前的vertexList：【始，邓】
                }
                String name = sbName.toString();
                System.out.println("name = " + name);

//            logger.trace("识别出：{}", name);
                // 对一些bad case做出调整
                switch (value)
                {
                    case BCD:
                        if (name.charAt(0) == name.charAt(2)) return; // 姓和最后一个名不可能相等的
//                        String cd = name.substring(1);
//                        if (CoreDictionary.contains(cd))
//                        {
//                            EnumItem<NR> item = PersonDictionary.dictionary.get(cd);
//                            if (item == null || !item.containsLabel(Z)) return; // 三字名字但是后两个字不在词典中，有很大可能性是误命中
//                        }
                        break;
                }
                if (isBadCase(name)) return;
//                如果这个名字在字典中有,且有角色A的可能，就不用管了，说明之前的分析中肯定

                // 正式算它是一个名字
                if (HanLP.Config.DEBUG)
                {
                    System.out.printf("识别出人名：%s %s\n", name, value);
                }
                int offset = offsetArray[begin];
//        记录了每个字组词的位置，比如：始/老百姓/大药房/末：[0,1,4,5,7]，始/邓/颖/超/生前/末：[0,1,2,3,4,6]
                System.out.printf("最优词网：\n%s\n", wordNetOptimum);
//                由vertexList生成的原始最优词网
//                0:[ ]
//                1:[邓]
//                2:[颖]
//                3:[超]
//                4:[生前]
//                5:[]
//                6:[ ]
//                添加了新Vertex：邓颖，之后：
//                0:[ ]
//                1:[邓, 邓颖]
//                2:[颖]
//                3:[超]
//                4:[生前]
//                5:[]
//                6:[ ]
//                添加了新Vertex：邓颖超，之后：
//                0:[ ]
//                1:[邓, 邓颖, 邓颖超]
//                2:[颖]
//                3:[超]
//                4:[生前]
//                5:[前]
//                6:[ ]
//                -------------------龚学平等领导------------
//                0:[ ]
//                1:[龚]
//                2:[学]
//                3:[平等]
//                4:[]
//                5:[领导]
//                6:[]
//                7:[ ]
//
//                识别出人名：龚学 BC
//                最优词网：
//                0:[ ]
//                1:[龚, 龚学]
//                2:[学]
//                3:[平等]
//                4:[等]   ——原来这里是空的，第一次insert的时候检查了连通性，添加上去了
//                5:[领导]
//                6:[]
//                7:[ ]
//                识别出人名：龚学平 BCD
//                最后命名实体识别后的细分词网：
//                0:[ ]
//                1:[龚, 龚学, 龚学平]
//                2:[学]
//                3:[平等]
//                4:[等]
//                5:[领导]
//                6:[]
//                7:[ ]
                wordNetOptimum.insert(offset, new Vertex(Predefine.TAG_PEOPLE, name, ATTRIBUTE, WORD_ID), wordNetAll);
//                表示在第offset=line行添加一个节点，wordNetAll是用来参考的，不会被更改
//                Predefine.TAG_PEOPLE = 人名实体的标记：未##人——Vertex.word(经常是带##的那种)
//                name：就是邓颖，邓颖超，——Vertex.realWord，真实文本，不带#
                System.out.println("ATTRIBUTE.toString() = " + ATTRIBUTE.toString());
//                Attribute：词属性，本页new出来的(Nature.nr,100)——固定了的，不用纠结，就是新的词语：邓颖和邓颖超都是这个词性
//                WORD_ID，也可以唯一确定一个词，是Attribute的下标，就是说每个不同的词有一个Attribute。
//                问题，怎么insert，怎么用wordNet
            }
        });
    }

    /**
     * 因为任何算法都无法解决100%的问题，总是有一些bad case，这些bad case会以“盖公章 A 1”的形式加入词典中<BR>
     * 这个方法返回人名是否是bad case
     *
     * @param name
     * @return
     */
    static boolean isBadCase(String name)
    {
        System.out.println("name = [" + name + "]");
        EnumItem<NR> nrEnumItem = dictionary.get(name);
        System.out.println("nrEnumItem==null = " + Boolean.toString(nrEnumItem == null));
        if (nrEnumItem == null) return false;
        System.out.println("nrEnumItem.toString() = " + nrEnumItem.toString());
        System.out.println("nrEnumItem.containsLabel(NR.A) = " + nrEnumItem.containsLabel(NR.A));
        return nrEnumItem.containsLabel(NR.A);
    }
}
