package main.java.com.example.Patterns;

public class Computer {

    private String cpu;
    private String memory;
    private String type;

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public void setType(String type) {
        this.type = type;
    }


    interface ComputerBuilder {
        ComputerBuilder buildCpu(String cpu);

        ComputerBuilder buildMemory(String memory);

        ComputerBuilder buildType(String type);

        Computer build();
    }

    public static class ComputerBuilderImpl implements ComputerBuilder {
        private String cpu;
        private String memory;
        private String type;
        private Computer computer = new Computer();

        @Override
        public ComputerBuilder buildCpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        @Override
        public ComputerBuilder buildMemory(String memory) {
            this.memory = memory;
            return this;
        }

        @Override
        public ComputerBuilder buildType(String type) {
            this.type = type;
            return this;
        }

        @Override
        public Computer build() {
            return computer;
        }

    }
}