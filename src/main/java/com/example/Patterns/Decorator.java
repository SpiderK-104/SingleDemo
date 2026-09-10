package main.java.com.example.Patterns;

/**
 * 适配器模式
 */
public class Decorator {

    interface Gun {
        void shoot();   //射击

        void reload(); //装载
    }


    abstract class GunDecorator implements Gun {
        protected Gun gun;

        public GunDecorator(Gun gun) {
            this.gun = gun;
        }

        @Override
        public void shoot() {
            System.out.println("射击");
        }

        @Override
        public void reload() {
            gun.reload();
        }
    }

    class AK47 extends GunDecorator {
        public AK47(Gun gun) {
            super(gun);
        }

        @Override
        public void shoot() {
            System.out.println("AK47射击");
            gun.shoot();
        }

        @Override
        public void reload() {
            gun.reload();
            System.out.println("AK47装载子弹");
        }
    }
}
