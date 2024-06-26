import task.dag.DAG;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
    public static void main(String[] args) throws Exception {
        // 构建执行DAG
        DAG dag = new DAG("./src/main/java/config/conf.json");
        dag.execute(); // 执行整个DAG
    }
}
