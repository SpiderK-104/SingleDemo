package main.java.com.example.Patterns;

public class Adapter {
    // 目标接口（电脑能兼容的）
    interface USB {
        void readData();
    }

    // 被适配者（SD卡，接口不兼容）
    static class SDCard {
        void readFromSDCard() {
            System.out.println("从SD卡读取数据");
        }
    }

    // 适配器（适配SD卡，使其兼容USB接口）
    static class CardReaderAdapter implements USB {
        private SDCard sdCard;

        public CardReaderAdapter(SDCard sdCard) {
            this.sdCard = sdCard;
        }

        @Override
        public void readData() {
            // 在这里完成接口转换
            sdCard.readFromSDCard();
        }
    }

    static void main() {
        // 1️⃣ 有一个 SD 卡
        SDCard sdCard = new SDCard();
        // 2️⃣ 用适配器包装它
        USB usbDevice = new CardReaderAdapter(sdCard);
        // 3️⃣ 电脑只认 USB 接口
        usbDevice.readData();
    }
}
