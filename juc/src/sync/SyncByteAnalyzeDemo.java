package sync;

/**
 * 本demo主要用于同步字节码分析
 */
public class SyncByteAnalyzeDemo {
    public static void main(String[] args) {
        Object lock = new Object();
        synchronized (lock) {
            System.out.println("同步块");
        }
    }
}
