
## 功能
通过解析json文件，构建DAG任务

DAG天然支持有环图的识别，代码里增加了判断，如果存在环则抛出异常

## 使用

### 任务实现说明
每个任务都implements Task，实现execute方法。从commonContext获取需要的参数，并将结果赋值回commonContext的约定字段，供下层DAG或执行完成后读取使用

### 配置文件说明
json的key为task的类名，value为task依赖项的类名列表。被依赖的类，使用前需定义，如task.TaskOne定义在task.TaskTwo前

eg:
```agsl
{
  "task.TaskOne": [],
  "task.TaskTwo": ["task.TaskOne"],
  "task.TaskThree": ["task.TaskOne"],
  "task.TaskFour": ["task.TaskTwo", "task.TaskThree"],
  "task.TaskFive": ["task.TaskThree"],
  "task.TaskSix": []
}
```
## 测试Demo
执行Main.java

Outputs:
```agsl
Executing ...: task.TaskSix
Executing RPC: task.TaskOne
Executing Cellar: task.TaskThree
Executing Redis: task.TaskTwo
Executing File: task.TaskFour
Executing ...: task.TaskFive
```