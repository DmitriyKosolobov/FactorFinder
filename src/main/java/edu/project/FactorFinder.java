package edu.project;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

public class FactorFinder {

    // Класс для хранения пар множителей
    public record FactorPair(int factor1, int factor2) {

        @Override
        public String toString() {
            return STR."(\{factor1}, \{factor2})";
        }
    }

    // Класс задачи для поиска делителей
    public static class FactorTask implements Callable<List<FactorPair>> {
        private final int number;
        private final int start;
        private final int end;

        public FactorTask(int number, int start, int end) {
            this.number = number;
            this.start = start;
            this.end = end;
        }

        @Override
        public List<FactorPair> call() {
            List<FactorPair> localPairs = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                if (number % i == 0) {
                    localPairs.add(new FactorPair(i, number / i));
                }
            }
            return localPairs;
        }
    }

    // Функция для поиска пар множителей
    public static List<FactorPair> findFactors(int number, int numThreads, int chunkSize) throws InterruptedException {
        List<FactorPair> factorPairs = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        BlockingQueue<FactorTask> divisorPool = new LinkedBlockingQueue<>();
        List<Future<List<FactorPair>>> futures = new ArrayList<>();

        int sqrtNumber = (int) Math.sqrt(number);

        // Заполнение очереди задач
        for (int i = 1; i <= sqrtNumber; i += chunkSize) {
            int end = Math.min(i + chunkSize - 1, sqrtNumber);
            divisorPool.add(new FactorTask(number, i, end));
        }

        // Запуск потоков
        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                List<FactorPair> localPairs = new ArrayList<>();
                try {
                    while (!divisorPool.isEmpty()) {

                        LocalTime localTime = LocalTime.now();
                        System.out.println(STR."Обращение к DIVISOR POOL: \{Thread.currentThread().getName()} \{localTime}");

                        FactorTask task = divisorPool.poll(1, TimeUnit.SECONDS);
                        if (task != null) {
                            localPairs.addAll(task.call());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return localPairs;
            }));
        }

        // Сбор результатов
        for (Future<List<FactorPair>> future : futures) {
            try {
                factorPairs.addAll(future.get());
            } catch (ExecutionException | InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        return factorPairs;
    }

    private static int getNaturalNumber(Scanner scanner, String prompt) {
        int number = -1;
        while (number <= 0) {
            System.out.println(prompt);
            if (scanner.hasNextInt()) {
                number = scanner.nextInt();
                if (number <= 0) {
                    System.out.println("Ошибка! Число должно быть больше 0");
                }
            } else {
                System.out.println("Ошибка! Некорректный ввод");
                scanner.next();
            }
        }
        return number;
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int number = getNaturalNumber(in, "Введите натуральное число:");
        int numThreads = getNaturalNumber(in, "Введите желаемое количество потоков:");
        int chunkSize = getNaturalNumber(in, "Введите желаемый размер пачки:");

        try {
            List<FactorPair> factors = findFactors(number, numThreads, chunkSize);
            factors.sort(Comparator.comparingInt(FactorPair::factor1));
            System.out.println(STR."Factor pairs for \{number}:");
            for (FactorPair pair : factors) {
                System.out.println(pair);
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}
