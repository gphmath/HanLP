/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/9/10 17:12</create-date>
 *
 * <copyright file="Viterbi.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.algorithm;

import com.hankcs.hanlp.corpus.dictionary.item.EnumItem;
import com.hankcs.hanlp.corpus.tag.NR;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.TransformMatrixDictionary;
import com.hankcs.hanlp.seg.common.Vertex;

import java.util.*;

/**
 * 维特比算法
 *
 * @author hankcs
 */
public class Viterbi
{
    /**
     * 求解HMM模型，所有概率请提前取对数
     *
     * @param obs     观测序列
     * @param states  隐状态
     * @param start_p 初始概率（隐状态）
     * @param trans_p 转移概率（隐状态）
     * @param emit_p  发射概率 （隐状态表现为显状态的概率）
     * @return 最可能的序列
     */
    public static int[] compute(int[] obs, int[] states, double[] start_p, double[][] trans_p, double[][] emit_p)
    {
        int _max_states_value = 0;
        for (int s : states)
        {
            _max_states_value = Math.max(_max_states_value, s);
        }
        ++_max_states_value;
        double[][] V = new double[obs.length][_max_states_value];
        int[][] path = new int[_max_states_value][obs.length];

        for (int y : states)
        {
            V[0][y] = start_p[y] + emit_p[y][obs[0]];
            path[y][0] = y;
        }

        for (int t = 1; t < obs.length; ++t)
        {
            int[][] newpath = new int[_max_states_value][obs.length];

            for (int y : states)
            {
                double prob = Double.MAX_VALUE;
                int state;
                for (int y0 : states)
                {
                    double nprob = V[t - 1][y0] + trans_p[y0][y] + emit_p[y][obs[t]];
                    if (nprob < prob)
                    {
                        prob = nprob;
                        state = y0;
                        // 记录最大概率
                        V[t][y] = prob;
                        // 记录路径
                        System.arraycopy(path[state], 0, newpath[y], 0, t);
                        newpath[y][t] = y;
                    }
                }
            }

            path = newpath;
        }

        double prob = Double.MAX_VALUE;
        int state = 0;
        for (int y : states)
        {
            if (V[obs.length - 1][y] < prob)
            {
                prob = V[obs.length - 1][y];
                state = y;
            }
        }

        return path[state];
    }

    /**
     * 特化版的求解HMM模型
     *
     * @param vertexList                包含Vertex.B节点的路径
     * @param transformMatrixDictionary 词典对应的转移矩阵
     */
    public static void compute(List<Vertex> vertexList, TransformMatrixDictionary<Nature> transformMatrixDictionary)
    {
        int length = vertexList.size() - 1;
        double[][] cost = new double[2][];  // 滚动数组
        Iterator<Vertex> iterator = vertexList.iterator();
        Vertex start = iterator.next();
        Nature pre = start.attribute.nature[0];
        // 第一个是确定的
//        start.confirmNature(pre);
        // 第二个也可以简单地算出来
        Vertex preItem;
        Nature[] preTagSet;
        {
            Vertex item = iterator.next();
            cost[0] = new double[item.attribute.nature.length];
            int j = 0;
            int curIndex = 0;
            for (Nature cur : item.attribute.nature)
            {
                cost[0][j] = transformMatrixDictionary.transititon_probability[pre.ordinal()][cur.ordinal()] - Math.log((item.attribute.frequency[curIndex] + 1e-8) / transformMatrixDictionary.getTotalFrequency(cur));
                ++j;
                ++curIndex;
            }
            preTagSet = item.attribute.nature;
            preItem = item;
        }
        // 第三个开始复杂一些
        for (int i = 1; i < length; ++i)
        {
            int index_i = i & 1;
            int index_i_1 = 1 - index_i;
            Vertex item = iterator.next();
            cost[index_i] = new double[item.attribute.nature.length];
            double perfect_cost_line = Double.MAX_VALUE;
            int k = 0;
            Nature[] curTagSet = item.attribute.nature;
            for (Nature cur : curTagSet)
            {
                cost[index_i][k] = Double.MAX_VALUE;
                int j = 0;
                for (Nature p : preTagSet)
                {
                    double now = cost[index_i_1][j] + transformMatrixDictionary.transititon_probability[p.ordinal()][cur.ordinal()] - Math.log((item.attribute.frequency[k] + 1e-8) / transformMatrixDictionary.getTotalFrequency(cur));
                    if (now < cost[index_i][k])
                    {
                        cost[index_i][k] = now;
                        if (now < perfect_cost_line)
                        {
                            perfect_cost_line = now;
                            pre = p;
                        }
                    }
                    ++j;
                }
                ++k;
            }
            preItem.confirmNature(pre);
            preTagSet = curTagSet;
            preItem = item;
        }
    }

    /**
     * 标准版的Viterbi算法，查准率高，效率稍低——这才是正确的Viterbi算法
     *
     * @param roleTagList               观测序列
     * @param transformMatrixDictionary 转移矩阵
     * @param <E>                       EnumItem的具体类型
     * @return 预测结果
     */
    public static <E extends Enum<E>> List<E> computeEnum(List<EnumItem<E>> roleTagList, TransformMatrixDictionary<E> transformMatrixDictionary)
    {
        int length = roleTagList.size() - 1;
        List<E> tagList = new ArrayList<E>(roleTagList.size());
        double[][] cost = new double[2][];  // 滚动数组
        Iterator<EnumItem<E>> iterator = roleTagList.iterator();
        EnumItem<E> start = iterator.next();
        E pre = start.labelMap.entrySet().iterator().next().getKey();
        // 第一个是确定的
        tagList.add(pre);
        // 第二个也可以简单地算出来
        Set<E> preTagSet;
        {
            EnumItem<E> item = iterator.next();
            cost[0] = new double[item.labelMap.size()];
            int j = 0;
            for (E cur : item.labelMap.keySet())
            {
                cost[0][j] = transformMatrixDictionary.transititon_probability[pre.ordinal()][cur.ordinal()] - Math.log((item.getFrequency(cur) + 1e-8) / transformMatrixDictionary.getTotalFrequency(cur));
                ++j;
            }
            preTagSet = item.labelMap.keySet();
        }
        // 第三个开始复杂一些
        for (int i = 1; i < length; ++i)
        {
            int index_i = i & 1;
            int index_i_1 = 1 - index_i;
            EnumItem<E> item = iterator.next();
            cost[index_i] = new double[item.labelMap.size()];
            double perfect_cost_line = Double.MAX_VALUE;
            int k = 0;
            Set<E> curTagSet = item.labelMap.keySet();
            for (E cur : curTagSet)
            {
                cost[index_i][k] = Double.MAX_VALUE;
                int j = 0;
                for (E p : preTagSet)
                {
                    double now = cost[index_i_1][j] + transformMatrixDictionary.transititon_probability[p.ordinal()][cur.ordinal()] - Math.log((item.getFrequency(cur) + 1e-8) / transformMatrixDictionary.getTotalFrequency(cur));
                    if (now < cost[index_i][k])
                    {
                        cost[index_i][k] = now;
                        if (now < perfect_cost_line)
                        {
                            perfect_cost_line = now;
                            pre = p;
                        }
                    }
                    ++j;
                }
                ++k;
            }
            tagList.add(pre);
            preTagSet = curTagSet;
        }
        tagList.add(tagList.get(0));    // 对于最后一个##末##
        return tagList;
    }

    /**
     * 仅仅利用了转移矩阵的“维特比”算法
     *
     * @param roleTagList               观测序列（也自带了发射矩阵）[  K 1 A 1 ][杨 B 10602 E 268 C 91 D 55 K 5 ][国 C 2368 D 1390 B 51 E 33 L 4 ][福 C 999 D 552 E 34 B 23 K 18 L 2 ][大 C 1262 L 218 K 127 D 52 E 8 M 2 ][药房 A 20833310 ][  A 20833310 ]
     * @param transformMatrixDictionary 转移矩阵，只用到真正的转移概率矩阵：transititon_probability是一个double[][]的数组。是真实概率的负对数吧。
     * @param <E>                       EnumItem的具体类型
     * @return 预测结果
     */
    public static <E extends Enum<E>> List<E> computeEnumSimply(List<EnumItem<E>> roleTagList, TransformMatrixDictionary<E> transformMatrixDictionary)
    {
//        用到的数据：
//        transititon_probability角色转移矩阵：OK
//        词语.getFrequency(某角色)：词语属于某角色的频数，OK
//        transformMatrixDictionary中某角色的总频数。首先需要转移频数矩阵OK，然后对于某个角色，计算它转出和转入的次数总和。所以有转移频数矩阵就行了，这是nr.tr.txt里的
        System.out.println("B到C的转移概率 = " + transformMatrixDictionary.transititon_probability[NR.B.ordinal()][NR.C.ordinal()]);
        System.out.println("B到A的转移概率 = " + transformMatrixDictionary.transititon_probability[NR.B.ordinal()][NR.A.ordinal()]);
//        nr.tr.txt的左边第一列是出发节点，上面第一行是到达节点，目测是概率的负对数（待确认）
        int length = roleTagList.size() - 1;
//        减一是因为第一个始的角色作者认为不用计算，直接取为K。始，杨，国，福，大，药房，末，共7个元素，去掉“始”，length=6
        System.out.println("length = " + length);
        List<E> tagList = new LinkedList<E>(); //初始化要返回的角色列表
        Iterator<EnumItem<E>> iterator = roleTagList.iterator();
        EnumItem<E> start = iterator.next();
        System.out.println("start = " + start);
//        start = K 1 A 1
        E pre = start.labelMap.entrySet().iterator().next().getKey();
        E perfect_tag = pre;
        System.out.println("perfect_tag = " + perfect_tag);
        // 第一个是确定的，指的是始##始。为什么不能是A？一开始不是标注为始[K 1 A 1 ]的吗？,好吧暂时无视，反正第一个字标注为人名上文。
        tagList.add(pre);
//        已经把这个【始，K】添加到角色列表的第一个位置
        for (int i = 0; i < length; ++i)
        {
            double perfect_cost = Double.MAX_VALUE;
            EnumItem<E> item = iterator.next();
//          当前item = [杨 B 10602 E 268 C 91 D 55 K 5 ]
            for (E cur : item.labelMap.keySet())
            {
//                遍历当前词语所有可能词性，选一个乘上去之后概率最大的，
// 但这不是真正的Viterbi算法！！！真正的Viterbi是全局最大概率路径，而这只是每一步选最大概率，全局不一定最大
//                cur依次=B，E,C,D,K
//                item.getFrequency(cur),当前词语属于当前角色的频数
//                transformMatrixDictionary.getTotalFrequency(cur)，当前角色的总频数
//                前者除以后者，表示当前角色能发射到当前的概率——这就是发射概率
//                下式=-log[ P_tran(s0->s1) * P_emit(s1->o1) ]
                double now = transformMatrixDictionary.transititon_probability[pre.ordinal()][cur.ordinal()] - Math.log((item.getFrequency(cur) + 1e-8) / transformMatrixDictionary.getTotalFrequency(cur));
                if (perfect_cost > now)
                {
//                    如果有负对数更小的（即概率更大的路径），就更新，选出这一步概率最大（不是全局最大）的路径
                    perfect_cost = now;
                    perfect_tag = cur;
                }
            }
//            继续下一步
            pre = perfect_tag;
            tagList.add(pre);
        }
        return tagList;
    }
}
