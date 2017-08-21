package com.hankcs.my_test;



/**
 * 测试类
 * Created by guoph on 2017/8/18.
 */
public class Any_Test {

    public static void main(String[] args)
    {
        String element = "国";

//        int x = Character.getNumericValue(element.charAt(0));
        int x = (int) (element.charAt(0));
        System.out.println("x=" + x);
        char c=element.charAt(0);
        int b=0;
        int s=b+c;
        System.out.println("s = " + s);
    }
}
