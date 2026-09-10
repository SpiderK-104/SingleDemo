package main.java.com.example.Patterns;


/**
 * 单例模式demo
 * 2026-07-24
 */
public class Singleton {
    private Singleton() {
    }

    ; //禁止实例化。

    void test() {
        System.out.println("气死我了");
    }

    public static Singleton getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final Singleton INSTANCE = new Singleton();
    }

    static void main() {
        Singleton singleton = new Singleton();
        singleton.test();
    }
}


