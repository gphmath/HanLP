/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/05/23 17:09</create-date>
 *
 * <copyright file="MathTools.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.utility;

import com.hankcs.hanlp.dictionary.CoreBiGramTableDictionary;
import com.hankcs.hanlp.seg.common.Vertex;

import static com.hankcs.hanlp.utility.Predefine.*;

/**
 * @author hankcs
 */
public class MathTools
{
    /**
     * 从一个词到另一个词的词的花费
     *
     * @param from 前面的词
     * @param to   后面的词
     * @return 分数
     */
    public static double calculateWeight(Vertex from, Vertex to)
    {
        int frequency = from.getAttribute().totalFrequency;
        if (frequency == 0)
        {
//            如果这个词在核心词典中没有，就当做频数为1
            frequency = 1;  // 防止发生除零错误
        }
//        int nTwoWordsFreq = BiGramDictionary.getBiFrequency(from.word, to.word);
        int nTwoWordsFreq = CoreBiGramTableDictionary.getBiFrequency(from.wordID, to.wordID);
        double value = -Math.log(dSmoothingPara * frequency / (MAX_FREQUENCY) + (1 - dSmoothingPara) * ((1 - dTemp) * nTwoWordsFreq / frequency + dTemp));
//        MAX_FREQUENCY:总词频25146057
//        dTemp:平滑因子：1/(总词频+0.00001)
//        t*词的频数/总词频25146057  + （1-t）*  [(1-temp)*转移频数(from->to)/词的频数  + temp ]
//        tmp约等于0，就当做0，t=0.1
//        0.1 * F(from)/F总  + 0.9 * F转移(from->to)/F(from)
        if (value < 0.0)
        {
            value = -value;
        }
//        logger.info(String.format("%5s frequency:%6d, %s nTwoWordsFreq:%3d, weight:%.2f", from.word, frequency, from.word + "@" + to.word, nTwoWordsFreq, value));
        return value;
    }
}
