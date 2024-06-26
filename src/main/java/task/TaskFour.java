package task;

import common.CommonContext;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskFour implements Task {
    @Override
    public void execute(CommonContext commonContext) {
        System.out.println("Executing File: " + this.getClass().getName());
    }
}