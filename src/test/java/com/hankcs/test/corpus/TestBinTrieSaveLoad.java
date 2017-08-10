/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/10/30 16:04</create-date>
 *
 * <copyright file="TestBinTrieSaveLoad.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.test.corpus;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.collection.trie.bintrie.BinTrie;
import com.hankcs.hanlp.corpus.dictionary.item.SimpleItem;
import com.hankcs.hanlp.corpus.util.DictionaryUtil;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import junit.framework.TestCase;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 保存和加载二分数组树BinTrie(dat文件)
 * @author hankcs
 */
public class TestBinTrieSaveLoad extends TestCase
{

    public static final String OUT_BINTRIE_DAT = "data/bintrie.dat";

    public void testSaveAndLoad() throws Exception
    {
        BinTrie<Integer> trie = new BinTrie<Integer>();
        trie.put("haha", 0);
        trie.put("hankcs", 1);
        trie.put("hello", 2);
        trie.put("za", 3);
        trie.put("zb", 4);
        trie.put("zzz", 5);
        System.out.println(trie.save(OUT_BINTRIE_DAT));
        trie = new BinTrie<Integer>();
        Integer[] value = new Integer[100];
        for (int i = 0; i < value.length; ++i)
        {
            value[i] = i;
        }
        System.out.println(trie.load(OUT_BINTRIE_DAT, value));
        Set<Map.Entry<String, Integer>> entrySet = trie.entrySet();
        System.out.println(entrySet);
    }

    public void testLoad() throws Exception
    {
        String OUT_BINTRIE_DAT = "data/test.trie.dat";
        BinTrie<SimpleItem> trie = new BinTrie<SimpleItem>();
        SimpleItem ws = new SimpleItem();
        ws.addLabel("nr",100);
        SimpleItem wk = new SimpleItem();
        ws.addLabel("nt",100);
        SimpleItem sz = new SimpleItem();
        ws.addLabel("ns",100);
        trie.put("王石", ws);
        trie.put("万科集团", wk);
        trie.put("深圳", sz);
        System.out.println(trie.save(OUT_BINTRIE_DAT));
        trie = new BinTrie<SimpleItem>();
        SimpleItem[] value = new SimpleItem[100];
        for (int i = 0; i < value.length; ++i)
        {
            value[i] = new SimpleItem();
        }
        System.out.println(trie.load(OUT_BINTRIE_DAT, value));
        Set<Map.Entry<String, SimpleItem>> entrySet = trie.entrySet();
        System.out.println(entrySet.toString());
    }

    public void testCustomDictionary() throws Exception
    {
        HanLP.Config.enableDebug(true);
        System.out.println("↓词语");
        System.out.println(CustomDictionary.get("老百姓大药房"));
        System.out.println("↑词语");
    }

    public void testSortCustomDictionary() throws Exception
    {
        DictionaryUtil.sortDictionary(HanLP.Config.CustomDictionaryPath[0]);
    }
}
