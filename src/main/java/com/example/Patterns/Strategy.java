package main.java.com.example.Patterns;

// 策略接口：定义算法规范
interface PaymentStrategy {
    void pay(double amount);
}

// 具体策略：支付宝支付
class AlipayStrategy implements PaymentStrategy {
    private String userId;

    public AlipayStrategy(String userId) {
        this.userId = userId;
    }

    @Override
    public void pay(double amount) {
        System.out.println(userId + "使用支付宝支付" + amount + "元");
    }
}

// 具体策略：微信支付
class WechatPayStrategy implements PaymentStrategy {
    private String openId;

    public WechatPayStrategy(String openId) {
        this.openId = openId;
    }

    @Override
    public void pay(double amount) {
        System.out.println(openId + "使用微信支付" + amount + "元");
    }
}

// 上下文：使用策略的客户端
class PaymentContext {
    private PaymentStrategy strategy;

    // 设置策略
    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    // 执行支付
    public void executePay(double amount) {
        strategy.pay(amount);
    }
}

// 测试类
class StrategyTest {
    public static void main(String[] args) {
        PaymentContext context = new PaymentContext();

        // 选择支付宝支付
        context.setStrategy(new AlipayStrategy("zhangsan@163.com"));
        context.executePay(100.0); // 输出：zhangsan@163.com使用支付宝支付100.0元

        // 切换为微信支付
        context.setStrategy(new WechatPayStrategy("wx123456789"));
        context.executePay(50.0); // 输出：wx123456789使用微信支付50.0元
    }
}