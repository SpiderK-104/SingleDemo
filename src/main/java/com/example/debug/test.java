package main.java.com.example.debug;

import java.lang.reflect.Array;
import java.util.Arrays;

public class test {
    static void main() {
        char []arr={'a','b','c','d'};

        String cp = Arrays.toString(Arrays.copyOfRange(arr,0,2));
        System.out.println(cp); // 只会输出 a b ，不包括 end.
    }


}
