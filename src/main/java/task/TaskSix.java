package task;

import common.CommonContext;

public class TaskSix implements Task {
    @Override
    public void execute(CommonContext commonContext) {
        System.out.println("Executing ...: " + this.getClass().getName());
    }
}
