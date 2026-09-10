public interface Car {
    void drive();
}

static class Link implements Car {
    public void drive() {
        System.out.println("领克GT07 在路上飞驰");
    }
}

static class Audi implements Car {
    public void drive() {
        System.out.println("奥迪 在路上飞驰");
    }
}

interface CarFactory {
    Car createCar();
}

static class AudiCar implements CarFactory {
    public Car createCar() {
        return new Audi();
    }
}

static class LinkCar implements CarFactory {
    public Car createCar() {
        return new Link();
    }
}

static void main() {
    CarFactory factory = new LinkCar();
    Car car = factory.createCar();
    car.drive();

}

