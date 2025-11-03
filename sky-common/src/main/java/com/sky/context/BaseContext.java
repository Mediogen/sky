package com.sky.context;

public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        // 如果不remove，tomcat底层使用线程池，线程不会结束，threadLocal会有内存泄漏的风险
        // 因为 ThreadLocalMap 中的 Entry 对 value 强引用，线程池的线程如果一直处理请求，threadLocal不会被回收
        threadLocal.remove();
    }

}
