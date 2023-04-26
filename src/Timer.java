public class Timer {
    private volatile int counter = 0;
    public Timer() {
        new Thread(new Counter()).start();
    }

    public synchronized int getCounter() {
        System.out.println(counter);
        return counter;
    }
    public class Counter implements Runnable {

        @Override
        public void run() {
            try {
                while(true) {
                    Thread.sleep(1);
                    counter++;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
