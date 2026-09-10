package main.java.com.example.Patterns;

// 手机接口
interface Phone {
    void call();
}

// 华为手机
class HuaweiPhone implements Phone {
    @Override
    public void call() {
        System.out.println("华为打电话");
    }
}

// 小米手机
class XiaomiPhone implements Phone {
    @Override
    public void call() {
        System.out.println("小米打电话");
    }
}

// 手机工厂
class PhoneFactory {
    // 静态方法：根据类型创建手机
    public static Phone createPhone(String type) {
        if (type == null) {
            return null;
        }
        if (type.equals("huawei")) {
            return new HuaweiPhone();
        } else if (type.equals("xiaomi")) {
            return new XiaomiPhone();
        }
        return null;
    }

    // 测试
    static void main(String[] args) {
        // 不用自己 new HuaweiPhone()
        // 只告诉工厂要什么
        Phone phone = PhoneFactory.createPhone("xiaomi");
        phone.call();
    }
}