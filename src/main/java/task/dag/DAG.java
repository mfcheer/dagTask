package task.dag;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.CommonContext;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import task.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class DAG {
    ExecutorService executorService = Executors.newFixedThreadPool(10);

    // 用于DAG间透传字段
    CommonContext commonContext = new CommonContext();

    // 任务依赖关系
    private final Map<Task, ArrayList<Task>> graph = new ConcurrentHashMap<>();

    // 任务的未完成父任务计数
    private Map<Task, Integer> dependencyCounter = new ConcurrentHashMap<>();

    // 已执行总任务计数
    private final AtomicInteger taskFinishCounter = new AtomicInteger(0);

    public DAG(String confFilePath) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, FileNotFoundException {
        // 解析json配置
        Gson gson = new Gson();
        BufferedReader br = new BufferedReader(new FileReader(confFilePath));
        Type mapType = new TypeToken<Map<String, List<String>>>() {}.getType();
        Map<String, List<String>> taskDependencies = gson.fromJson(br, mapType);

        // 构建DAG
        this.commonContext = new CommonContext();
        Map<String, Task> taskMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : taskDependencies.entrySet()) {
            String taskName = entry.getKey();
            List<String> dependencyNames = entry.getValue();
            // 反射为类实例
            Class<?> clazz = Class.forName(taskName);
            Task task = (Task) clazz.getConstructor().newInstance();
            taskMap.put(taskName, task);
            // 解析依赖任务
            List<Task> fatherTasks = new ArrayList<>();
            for (String dependencyName : dependencyNames) {
                fatherTasks.add(taskMap.get(dependencyName));
            }
            // 添加任务到DAG
            this.addTask(task, fatherTasks);
        }
    }

    private void addTask(Task subTask, List<Task> fatherTasks) {
        // 记录依赖的任务数
        dependencyCounter.putIfAbsent(subTask, fatherTasks.size());

        // 构建DAG
        for (Task task : fatherTasks) {
            ArrayList<Task> subTasks = graph.getOrDefault(task, new ArrayList<>());
            subTasks.add(subTask);
            graph.put(task, subTasks);
        }
    }

    public void execute() throws Exception {
        // 如果执行过，再次执行时抛出异常
        if(taskFinishCounter.get() > 0) {
            throw new Exception("taskRun repeat err.");
        }

        // 首次寻找没有依赖项的任务
        Set<Task> taskToRun = new HashSet<>();
        for (Map.Entry<Task, Integer> entry : dependencyCounter.entrySet()) {
            if(entry.getValue() == 0) {
                taskToRun.add(entry.getKey());
            }
        }

        // DAG遍历
        while (!taskToRun.isEmpty()) {
            Set<Task> nextToRun = new HashSet<>();

            // 并发执行同层级的任务
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (Task task : taskToRun) {
                CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                    // 任务执行
                    task.execute(commonContext);
                    taskFinishCounter.incrementAndGet();
                    // 修改其子任务
                    ArrayList<Task> subTasks = graph.getOrDefault(task, new ArrayList<>());
                    for (Task subTask : subTasks) {
                        if (dependencyCounter.containsKey(subTask)) {
                            int counter = dependencyCounter.get(subTask) - 1;
                            dependencyCounter.put(subTask, counter);
                            if (counter == 0) {
                                nextToRun.add(subTask);
                            }
                        }
                    }
                }, executorService);
                futures.add(future);
            }

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

            // 下一轮任务
            taskToRun = nextToRun;
        }

        // 关闭线程池
        executorService.shutdown();

        // 判断任务是否全部执行完成，存在环
        if(dependencyCounter.size() != taskFinishCounter.get()) {
            log.info("taskFinishCounter is {}, totalTaskCnt is {}", taskFinishCounter, dependencyCounter.size());
            throw new Exception("taskRun count mismatch err.");
        }
    }
}
